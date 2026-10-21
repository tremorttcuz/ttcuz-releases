import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;

/** Runs only on the author's build machine; the private key is never bundled. */
public final class SealSigner {

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3) throw new IllegalArgumentException(
            "keystore [payload signature]"
        );
        String password = System.getenv("TTCUZ_INTEGRITY_PASSWORD");
        if (password == null) throw new IllegalStateException(
            "TTCUZ_INTEGRITY_PASSWORD is missing"
        );
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(args[0])) {
            store.load(in, password.toCharArray());
        }
        java.security.cert.Certificate certificate = store.getCertificate("ttcuz-integrity");
        if (certificate == null) throw new IllegalStateException("ttcuz-integrity alias missing");
        System.out.println(
            Base64.getEncoder().encodeToString(certificate.getPublicKey().getEncoded())
        );
        if (args.length == 3) {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign((PrivateKey) store.getKey("ttcuz-integrity", password.toCharArray()));
            signer.update(Files.readAllBytes(Paths.get(args[1])));
            Files.write(Paths.get(args[2]), signer.sign());
        }
    }
}
