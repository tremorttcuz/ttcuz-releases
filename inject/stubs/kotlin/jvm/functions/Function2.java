package kotlin.jvm.functions;

/** Kotlin's two-argument function, in outline: here so a rewritten call keeps the types the verifier saw. */
public interface Function2<P1, P2, R> {

    R invoke(P1 p1, P2 p2);
}
