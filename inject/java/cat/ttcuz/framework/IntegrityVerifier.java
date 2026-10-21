package cat.ttcuz.framework;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.zip.*;

/** Offline integrity checks. This cannot prevent removal of the verifier itself. */
final class IntegrityVerifier {

    static final String MANIFEST = "assets/ttcuz/integrity.manifest";
    static final String SIGNATURE = "assets/ttcuz/integrity.sig";

    private IntegrityVerifier() {}

    static boolean excluded(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return (
            name.equals(MANIFEST) ||
            name.equals(SIGNATURE) ||
            upper.equals("META-INF/MANIFEST.MF") ||
            (upper.startsWith("META-INF/") &&
                (upper.endsWith(".SF") ||
                    upper.endsWith(".RSA") ||
                    upper.endsWith(".DSA") ||
                    upper.endsWith(".EC")))
        );
    }

    static byte[] read(ZipFile zip, String name, int limit) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null || entry.isDirectory() || entry.getSize() > limit) throw new IOException(
            "Missing or oversized seal"
        );
        try (
            InputStream in = zip.getInputStream(entry);
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            byte[] block = new byte[8192];
            int n;
            while ((n = in.read(block)) != -1) {
                if (out.size() + n > limit) throw new IOException("Oversized seal");
                out.write(block, 0, n);
            }
            return out.toByteArray();
        }
    }

    static String hex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray(),
            out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[i * 2] = digits[(bytes[i] & 255) >>> 4];
            out[i * 2 + 1] = digits[bytes[i] & 15];
        }
        return new String(out);
    }

    static String verify(File file, String packageName, String certificate, byte[] publicKey)
        throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            byte[] payload = read(zip, MANIFEST, 4 * 1024 * 1024),
                signed = read(zip, SIGNATURE, 4096);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(
                KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey))
            );
            verifier.update(payload);
            if (!verifier.verify(signed)) throw new SecurityException("Invalid seal signature");
            Map<String, String> expected = new HashMap<>();
            String launcher;
            try (
                BufferedReader lines = new BufferedReader(
                    new StringReader(new String(payload, StandardCharsets.UTF_8))
                )
            ) {
                if (
                    !"TTCUZ-INTEGRITY-1".equals(lines.readLine()) ||
                    !("package=" + packageName).equals(lines.readLine()) ||
                    !("certificate=" + certificate).equals(lines.readLine())
                ) throw new SecurityException("App identity differs");
                String next = lines.readLine();
                if (next == null || !next.startsWith("launcher=")) throw new SecurityException(
                    "Launcher missing"
                );
                launcher = next.substring(9);
                if (
                    !launcher.startsWith(packageName + ".") &&
                    !launcher.startsWith("com.ss.android.")
                ) throw new SecurityException("Unexpected launcher");
                String line;
                while ((line = lines.readLine()) != null) {
                    if (
                        line.length() < 66 ||
                        line.charAt(64) != '\t' ||
                        !line.substring(0, 64).matches("[0-9a-f]{64}")
                    ) throw new SecurityException("Invalid entry");
                    String name = line.substring(65);
                    if (
                        name.isEmpty() ||
                        name.contains("\t") ||
                        name.contains("\\") ||
                        name.startsWith("/") ||
                        name.contains("../") ||
                        excluded(name) ||
                        expected.put(name, line.substring(0, 64)) != null ||
                        expected.size() > 40000
                    ) throw new SecurityException("Invalid entry name");
                }
            }
            if (
                !expected.containsKey("classes.dex") ||
                !expected.containsKey("AndroidManifest.xml") ||
                !expected.containsKey("resources.arsc")
            ) throw new SecurityException("Incomplete seal");
            Set<String> seen = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            byte[] block = new byte[65536];
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!seen.add(name)) throw new SecurityException("Duplicate ZIP entry");
                if (entry.isDirectory() || excluded(name)) continue;
                String wanted = expected.remove(name);
                if (wanted == null) throw new SecurityException("Unexpected APK entry");
                hash.reset();
                try (InputStream in = zip.getInputStream(entry)) {
                    int n;
                    while ((n = in.read(block)) != -1) hash.update(block, 0, n);
                }
                if (!wanted.equals(hex(hash.digest()))) throw new SecurityException(
                    "APK content differs: " + name
                );
            }
            if (!expected.isEmpty()) throw new SecurityException("APK entries missing");
            return launcher;
        }
    }
}
