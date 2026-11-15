"""Patching the dex files several at a time, and remembering the answers.

Taking a dex apart and putting it back is where the build spends its time,
and there are forty-odd of them. Two things make that shorter.

They are done side by side. Each is a job of its own -- its own room, its
own java -- so nothing about one depends on another, and the only limit is
how much memory the machine has to give.

And three things are kept between builds, each under a name made from what
it depends on, so that a wrong answer cannot be found by accident:

  the answer    the patched dex, for this dex under these rules. A rebuild
                that changed only the mod's Java gets every one from here.

  the pieces    the dex taken apart, before anything is rewritten. That
                depends on the dex alone, so it survives any change to the
                rules, and a changed rule starts from here rather than from
                baksmali.

  the outcome   the patched dex again, but named by what was actually
                rewritten in it. Change one hook and the rules are "new" for
                every dex, yet nearly all of them come out rewritten exactly
                as before -- and those are recognised here and not put back
                together a second time.

TTCUZ_NO_CACHE=1 ignores all of it, TTCUZ_CACHE=dir moves it,
TTCUZ_WORKERS=n says how many to do at once.
"""

from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
import time
from concurrent.futures import FIRST_COMPLETED, ProcessPoolExecutor, as_completed, wait
from typing import Callable, Dict, List, Optional, Tuple

from . import dexpatch, dexskip
from .dexpatch import Smali


def _env(name):
    """TTCUZ_* or, as earlier builds spelled it, ttcuz_*."""
    return os.environ.get(name) or os.environ.get("ttcuz" + name[5:])

#: bumped when what is kept changes shape, so nothing old is read as new
SHAPE = "ttcuz-4"

#: something not asked for in this long is not coming back: an old TikTok,
#: or rules nobody builds with any more
KEEP_DAYS = 30

#: what one java is allowed, and what one job is counted as when working out
#: how many fit. A job is either taking a dex apart or putting it together,
#: never both at once. Measured on the largest dex of 47.2.41: given 4g java
#: takes 3.7 GB and given 2g it takes 2.4, and is not a tenth of a second
#: slower for it.
HEAP = "3g"  # the one dex done again at the end runs alone; 2g was too small for classes3 (smali wrote nothing)
ROOM_PER_JOB = 2.5


#: where the build keeps every piece of the dex cache: ``ttcuzcache/`` beside the sources, so it travels with the
#: project and is easy to find or delete. ``--cache DIR`` (sets TTCUZ_CACHE) or the TTCUZ_CACHE variable move it.
DEFAULT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ttcuzcache")
#: where builds before this one kept it; moved over once, so nothing is rebuilt for nothing
LEGACY = os.path.join(os.path.expanduser("~"), ".cache", "ttcuz-dex")


def folder() -> str:
    chosen = _env("TTCUZ_CACHE") or DEFAULT
    if chosen == DEFAULT and os.path.isdir(LEGACY) and not os.path.exists(DEFAULT):
        try:
            shutil.move(LEGACY, DEFAULT)
        except OSError:
            pass
    os.makedirs(chosen, exist_ok=True)
    return chosen


def disabled() -> bool:
    return bool(_env("TTCUZ_NO_CACHE"))


def _sha(*parts: bytes) -> str:
    digest = hashlib.sha256()
    for part in parts:
        digest.update(len(part).to_bytes(8, "big"))
        digest.update(part)
    return digest.hexdigest()


def _stamp(path: str) -> bytes:
    """A tool is the same tool while its size and date are the same."""
    try:
        seen = os.stat(path)
        return ("%s:%d:%d" % (os.path.basename(path), seen.st_size,
                              seen.st_mtime_ns)).encode()
    except OSError:
        return b"?"


def _touch(path: str) -> None:
    try:
        os.utime(path, None)
    except OSError:
        pass


def workers(jobs: int) -> int:
    """How many dex files to do at once.

    Memory is what runs out first, not processors: each java here may take
    gigabytes, and a build that pushes the machine into swap is slower than
    one that waits its turn.
    """
    asked = _env("TTCUZ_WORKERS")
    if asked:
        return max(1, min(int(asked), jobs or 1))
    by_memory = int(free_gb() // ROOM_PER_JOB)
    by_cores = (os.cpu_count() or 2) // 2
    return max(1, min(by_memory, by_cores, 6, jobs or 1))


def free_gb() -> float:
    """Memory the build may use now, in GB (MemAvailable on Linux and Termux)."""
    free = 4.0
    if os.name == "nt":
        import ctypes
        class MemoryStatus(ctypes.Structure):
            _fields_=[("length",ctypes.c_ulong),("load",ctypes.c_ulong)]+[(n,ctypes.c_ulonglong) for n in ("total","available","total_page","available_page","total_virtual","available_virtual","extended")]
        memory=MemoryStatus();memory.length=ctypes.sizeof(memory)
        if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)):
            free=max(0.0,memory.available/(1024**3)-1.5)
    try:
        with open("/proc/meminfo", encoding="ascii") as handle:
            for line in handle:
                if line.startswith("MemAvailable:"):
                    free = int(line.split()[1]) / (1024.0 * 1024.0)
                    break
    except (OSError, ValueError):
        pass
    return free


def room_for(size: int) -> float:
    """GB one java needs for a dex of this many bytes: 2.4 GB measured for the 14.8 MB one, roughly in proportion
    below it, never under 0.8 GB (the JVM and smali themselves)."""
    return min(ROOM_PER_JOB, max(0.8, 0.17 * size / (1024.0 * 1024.0) + 0.1))


def heap_for(size: int) -> str:
    return "%dm" % int(room_for(size) * 0.8 * 1024)


def prune(root: Optional[str] = None) -> None:
    """Throw away what has not been asked for in a month."""
    root = root or folder()
    limit = time.time() - KEEP_DAYS * 86400
    try:
        names = os.listdir(root)
    except OSError:
        return
    for name in names:
        path = os.path.join(root, name)
        try:
            if name == "pieces":
                for tree in os.listdir(path):
                    whole = os.path.join(path, tree)
                    mark = os.path.join(whole, "whole")
                    # one without its mark was interrupted half way
                    if not os.path.exists(mark) or os.path.getmtime(mark) < limit:
                        if os.path.getmtime(whole) < time.time() - 3600:
                            shutil.rmtree(whole, ignore_errors=True)
            elif os.path.isfile(path) and os.path.getmtime(path) < limit:
                os.remove(path)
        except OSError:
            pass


def _pieces(dex: bytes, dex_sha: str, smali: Smali, root: str, scratch: str) -> str:
    """The dex taken apart and untouched, made once and kept."""
    home = os.path.join(root,"pieces",_sha(dex_sha.encode(),str(smali.api).encode(),_stamp(smali.jar)))
    mark = os.path.join(home, "whole")
    tree = os.path.join(home, "smali")
    if os.path.exists(mark):
        _touch(mark)
        return tree

    # made beside where it will live and moved in whole, so a build that is
    # stopped half way leaves nothing that looks finished
    making = "%s.%d" % (home, os.getpid())
    shutil.rmtree(making, ignore_errors=True)
    os.makedirs(making, exist_ok=True)
    dex_in = os.path.join(scratch, "in.dex")
    with open(dex_in, "wb") as handle:
        handle.write(dex)
    smali.disassemble(dex_in, os.path.join(making, "smali"))
    with open(os.path.join(making, "whole"), "w") as handle:
        handle.write("")
    shutil.rmtree(home, ignore_errors=True)
    try:
        os.rename(making, home)
    except OSError:
        # somebody else finished the same one first; theirs will do
        shutil.rmtree(making, ignore_errors=True)
        if not os.path.exists(mark):
            raise
    return tree


def _copy(tree: str, into: str) -> None:
    """Never share mutable smali files with cached originals."""
    shutil.copytree(tree, into)


def _rewrite(root: str, literals: Dict[str, str],
             dex: Optional[bytes] = None) -> Tuple[Dict[str, int], List[str]]:
    """Run the rules, and say which files they wrote to.

    The rules open what they change with plain `open`, so that is where they
    are watched: nothing in them has to know it is being counted.
    """
    written = set()
    real = open

    def watched(path, mode="r", *rest, **named):
        if any(letter in mode for letter in "wax+"):
            written.add(os.path.abspath(path))
        return real(path, mode, *rest, **named)

    dexpatch.open = watched
    try:
        counts = dexpatch.rewrite_everything(root, literals, dex)
    finally:
        del dexpatch.open
    return counts, sorted(written)


def _outcome(dex_sha: str, smali: Smali, tree: str, room: str,
             written: List[str]) -> str:
    """A name for exactly this rewriting of exactly this dex."""
    parts = [SHAPE.encode(), dex_sha.encode(), str(smali.api).encode(),
             _stamp(smali.jar)]
    for path in written:
        inside = os.path.relpath(path, room)
        with open(path, "rb") as handle:
            now = handle.read()
        try:
            with open(os.path.join(tree, inside), "rb") as handle:
                if handle.read() == now:
                    continue   # written, and written back the same
        except OSError:
            pass
        parts.append(inside.encode("utf-8"))
        parts.append(hashlib.sha256(now).digest())
    return _sha(*parts)


def _save(path: str, data: bytes) -> None:
    part = "%s.%d.part" % (path, os.getpid())
    with open(part, "wb") as handle:
        handle.write(data)
    os.replace(part, path)


def lookup(job: dict) -> Optional[Tuple[str, Optional[str], Dict[str, int], str]]:
    """The answer for one dex if the cache already has it, without any patching: cheap enough for the main process,
    so a cached dex is never shipped to a worker (megabytes pickled across) just to be told it is done."""
    return one(job, only_cached=True)


def one(job: dict, only_cached: bool = False) -> Optional[Tuple[str, Optional[str], Dict[str, int], str]]:
    """Patch one dex. Runs in a process of its own.

    Hands back where the patched dex is rather than the dex: it is megabytes,
    and it is already on disk. With only_cached, answers from the cache or not at all.
    """
    name = job["name"]
    dex = job["dex"]
    literals = job["literals"] or {}
    root = job["root"]
    smali = Smali(job["jar"], job["api"], job["jobs"], job["heap"])
    dexpatch.FOUND = dict(job["found"])
    os.makedirs(root, exist_ok=True)

    dex_sha = hashlib.sha256(dex).hexdigest()
    # the rules and their data (transplants.json): a change of either is a change of the answer
    rules = dexskip.rules_text()
    context = repr((SHAPE, smali.api, _stamp(smali.jar).hex(), sorted(literals.items()),
                    sorted(dexpatch.FOUND.items())))
    answer = _sha(SHAPE.encode(), dex_sha.encode(), rules, _stamp(smali.jar),
                  repr((smali.api, sorted(literals.items()),
                        sorted(dexpatch.FOUND.items()))).encode("utf-8"))
    answer_path = os.path.join(root, "a-%s.json" % answer)

    def known(said: dict, why: str):
        """An answer on disk, if what it points at is still there."""
        counts = said["counts"]
        if not counts:
            return name, None, {}, "nothing to rewrite, " + ("as before" if why == "from the cache" else why)
        out = os.path.join(root, "o-%s.dex" % said["outcome"])
        if os.path.isfile(out):
            _touch(out)
            return name, out, counts, why
        return None

    try:
        with open(answer_path, encoding="utf-8") as handle:
            said = json.load(handle)
        found = known(said, "from the cache")
        if found:
            _touch(answer_path)
            dexskip.remember(root, dex_sha, rules, context, said)
            return found
    except (OSError, ValueError, KeyError):
        pass

    # The rules changed since this dex was last patched: if the change cannot touch it, the old answer stands,
    # and the dex is not even copied out.
    said, why = dexskip.recall(root, dex, dex_sha, rules, context)
    if said is None and why == "no earlier answer":
        said, why = _legacy_answer(root, dex, dex_sha, smali, literals, rules)
    if said is not None:
        found = known(said, why)
        if found:
            _save(answer_path, json.dumps(said).encode("utf-8"))
            dexskip.remember(root, dex_sha, rules, context, said)
            return found

    if only_cached:
        return None
    scratch = os.path.join(job["workspace"], name.replace(".dex", ""))
    parent=os.path.realpath(job["workspace"]);child=os.path.realpath(scratch)
    if os.path.commonpath([parent,child])!=parent or child==parent:
        raise ValueError("Scratch escaped workspace")
    shutil.rmtree(scratch, ignore_errors=True)
    os.makedirs(scratch, exist_ok=True)
    try:
        tree = _pieces(dex, dex_sha, smali, root, scratch)
        room = os.path.join(scratch, "smali")
        _copy(tree, room)
        counts, written = _rewrite(room, literals, dex)
        if not counts:
            _save(answer_path, json.dumps({"counts": {}}).encode("utf-8"))
            dexskip.remember(root, dex_sha, rules, context, {"counts": {}})
            return name, None, {}, "nothing to rewrite after all"

        outcome = _outcome(dex_sha, smali, tree, room, written)
        out = os.path.join(root, "o-%s.dex" % outcome)
        if os.path.isfile(out):
            _touch(out)
            said = "rewritten as before, not reassembled"
        else:
            dex_out = os.path.join(scratch, "out.dex")
            smali.assemble(room, dex_out)
            with open(dex_out, "rb") as handle:
                patched = handle.read()
            if dexpatch.dex_format(patched) != dexpatch.dex_format(dex):
                raise RuntimeError(
                    "%s came back as dex %s, it went in as dex %s -- an Android old "
                    "enough to be in the apk's minSdk would refuse to load it"
                    % (name, dexpatch.dex_format(patched), dexpatch.dex_format(dex)))
            _save(out, patched)
            said = "reassembled"
        _save(answer_path, json.dumps({"counts": counts, "outcome": outcome})
              .encode("utf-8"))
        dexskip.remember(root, dex_sha, rules, context, {"counts": counts, "outcome": outcome})
        return name, out, counts, said
    finally:
        shutil.rmtree(scratch, ignore_errors=True)


def _legacy_answer(root: str, dex: bytes, dex_sha: str, smali, literals, rules: bytes):
    """A cache made before answers were remembered: keyed by dexpatch.py alone. Earlier versions of it kept in
    <cache>/legacy-rules/ are tried; one that has an answer for this dex is compared with the rules of now."""
    folder = os.path.join(root, "legacy-rules")
    try:
        names = sorted(os.listdir(folder))
    except OSError:
        return None, "no earlier answer"
    for entry in names:
        try:
            with open(os.path.join(folder, entry), "rb") as handle:
                old = handle.read()
            key = _sha(SHAPE.encode(), dex_sha.encode(), old, _stamp(smali.jar),
                       repr((smali.api, sorted(literals.items()),
                             sorted(dexpatch.FOUND.items()))).encode("utf-8"))
            with open(os.path.join(root, "a-%s.json" % key), encoding="utf-8") as handle:
                said = json.load(handle)
        except (OSError, ValueError):
            continue
        before = b"dexpatch.py\0" + old.replace(b"\r\n", b"\n") + b"\1" + os.path.join("data", "transplants.json").encode() + b"\0"
        names_changed = dexskip.changed_names(before, rules)
        if names_changed is None:
            return None, "a general part of the rules changed"
        if dexskip.mentions(dex, names_changed):
            return None, "the changed rules name what is in it"
        return said, "untouched by the changed rules (earlier cache)"
    return None, "no earlier answer"


def patch_all(dexes: Dict[str, bytes], jar: str, api: int, workspace: str,
              literals: Optional[Dict[str, str]],
              say: Callable[[str], None]) -> Dict[str, Tuple[Optional[bytes], Dict[str, int]]]:
    """Patch every dex given, and hand back the patched ones by name."""
    names = sorted(dexes)
    if _env("TTCUZ_NO_CACHE"):
        # a place nothing was ever kept in is the same as not looking
        root = os.path.join(workspace, "no-cache")
        shutil.rmtree(root, ignore_errors=True)
    else:
        root = folder()
        prune(root)
    os.makedirs(root, exist_ok=True)

    # Scheduled by memory, dex by dex: each java gets a heap sized to its dex, and as many run at once as fit in the
    # memory free at the start (TTCUZ_WORKERS still forces a fixed count, TTCUZ_HEAP a fixed heap). Most of TikTok's
    # dex files are 7-10 MB and need far less than the largest, so this runs several where a flat 2.5 GB ran one.
    cores = os.cpu_count() or 2
    forced = _env("TTCUZ_WORKERS")
    budget = free_gb()
    slots = max(1, min(int(forced) if forced else min(6, max(1, cores // 2)), len(names) or 1))
    forced_heap = _env("TTCUZ_HEAP")
    typical = sorted(len(dexes[n]) for n in names)[len(names) // 2] if names else 0
    estimate = slots if forced else max(1, min(slots, int(budget // room_for(typical))))
    threads = int(_env("TTCUZ_JOBS") or 0) or max(2, (2 * cores) // estimate)
    threads = min(threads, cores)
    say("%d dex files, up to %d at a time (%.1f GB free)" % (len(names), estimate, budget))

    def job(name: str) -> dict:
        return {"name": name, "dex": dexes[name], "literals": literals,
                "root": root, "jar": jar, "api": api, "jobs": threads,
                "heap": forced_heap or heap_for(len(dexes[name])), "found": dexpatch.FOUND, "workspace": workspace}

    found: Dict[str, Tuple[Optional[bytes], Dict[str, int]]] = {}
    started = time.time()
    # what the cache already answers is taken here, in this process; only the rest goes to the workers
    remaining = []
    for name in names:
        try:
            hit = lookup(job(name))
        except Exception:
            hit = None
        if hit is None:
            remaining.append(name)
            continue
        _name, path, counts, said = hit
        patched = None
        if path:
            with open(path, "rb") as handle:
                patched = handle.read()
        found[name] = (patched, counts)
    if found:
        say("%d of %d from the cache at once (%.1fs)" % (len(found), len(names), time.time() - started))
    # the largest first: they are the ones that decide how long it all takes
    pending = sorted(remaining, key=lambda name: -len(dexes[name]))
    running: Dict = {}
    names_of: Dict = {}
    retry: List[str] = []
    with ProcessPoolExecutor(max_workers=slots) as pool:
        try:
            while pending or running:
                used = sum(running.values())
                for name in list(pending):
                    if len(running) >= slots:
                        break
                    need = room_for(len(dexes[name]))
                    # always keep one going; otherwise start whatever still fits in the memory left
                    if running and not forced and used + need > budget:
                        continue
                    future = pool.submit(one, job(name))
                    running[future] = need
                    names_of[future] = name
                    used += need
                    pending.remove(name)
                done_set, _ = wait(list(running), return_when=FIRST_COMPLETED)
                for done in done_set:
                    running.pop(done)
                    try:
                        name, path, counts, said = done.result()
                    except Exception as error:
                        # A smaller heap that turned out too small: done again at the end with the full one.
                        if forced_heap:
                            raise
                        say("%s: again later with %s (%s)" % (names_of[done], HEAP, str(error).splitlines()[0][:80]))
                        retry.append(names_of[done])
                        continue
                    patched = None
                    if path:
                        with open(path, "rb") as handle:
                            patched = handle.read()
                    found[name] = (patched, counts)
                    say("%s: %s (%d of %d, %ds)" % (name, said, len(found), len(names),
                                                     time.time() - started))
        except BaseException:
            for other in running:
                other.cancel()
            raise
        for name in retry:
            again = job(name)
            again["heap"] = HEAP
            name, path, counts, said = pool.submit(one, again).result()
            patched = None
            if path:
                with open(path, "rb") as handle:
                    patched = handle.read()
            found[name] = (patched, counts)
            say("%s: %s (%d of %d, %ds)" % (name, said, len(found), len(names), time.time() - started))
    return found
