package trazzo.back.corehr.infrastructure.adapters.out.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import trazzo.back.corehr.application.port.out.CryptoKeyProviderPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Clave RSA con la que el middleware envuelve la llave AES de cada template. Tiene que sobrevivir a
 * los reinicios: una clave nueva en cada arranque dejaba indescifrables los templates enrolados y la
 * cola offline del agente (AUD-02). Orden de resolución:
 * <ol>
 *   <li>{@code app.biometric.rsa.private-key}: PKCS#8 en PEM o Base64 (p. ej. desde un secreto).</li>
 *   <li>{@code app.biometric.rsa.key-file}: se lee; si no existe se genera y se guarda (volumen Docker).</li>
 *   <li>Ninguna: clave efímera, solo aceptable en tests y local (StartupConfigValidator lo impide fuera).</li>
 * </ol>
 * {@code app.biometric.rsa.previous-private-keys} (separadas por comas) siguen descifrando tras rotar.
 */
@Slf4j
@Component
public class RsaKeyProviderAdapter implements CryptoKeyProviderPort {

    static final String PRIVATE_KEY_PROPERTY = "app.biometric.rsa.private-key";
    static final String KEY_FILE_PROPERTY = "app.biometric.rsa.key-file";
    static final String PREVIOUS_KEYS_PROPERTY = "app.biometric.rsa.previous-private-keys";

    private final PrivateKey privateKey;
    private final List<PrivateKey> decryptionKeys;
    private final String publicKeyPem;
    private final String kid;

    public RsaKeyProviderAdapter(
            @Value("${" + PRIVATE_KEY_PROPERTY + ":}") String configuredPrivateKey,
            @Value("${" + KEY_FILE_PROPERTY + ":}") String keyFile,
            @Value("${" + PREVIOUS_KEYS_PROPERTY + ":}") String previousPrivateKeys) {
        KeyPair current = resolveCurrent(configuredPrivateKey, keyFile);
        this.privateKey = current.getPrivate();
        this.publicKeyPem = toPem(current.getPublic());
        this.kid = kidOf(current.getPublic());

        List<PrivateKey> keys = new ArrayList<>();
        keys.add(current.getPrivate());
        for (String encoded : splitKeys(previousPrivateKeys)) {
            keys.add(parsePrivateKey(encoded, PREVIOUS_KEYS_PROPERTY).getPrivate());
        }
        this.decryptionKeys = List.copyOf(keys);
        log.info("Clave RSA biométrica lista. kid={}, claves de descifrado={}", kid, decryptionKeys.size());
    }

    @Override
    public PublicKeyInfo getCurrentPublicKey() {
        return new PublicKeyInfo(publicKeyPem, kid);
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public String getCurrentKid() {
        return kid;
    }

    @Override
    public List<PrivateKey> getDecryptionKeys() {
        return decryptionKeys;
    }

    private static KeyPair resolveCurrent(String configuredPrivateKey, String keyFile) {
        if (configuredPrivateKey != null && !configuredPrivateKey.isBlank()) {
            return parsePrivateKey(configuredPrivateKey, PRIVATE_KEY_PROPERTY);
        }
        if (keyFile != null && !keyFile.isBlank()) {
            return loadOrCreate(Path.of(keyFile));
        }
        log.warn("Sin {} ni {}: se usa una clave RSA efímera. Los templates enrolados dejarán de "
                + "descifrarse al reiniciar; solo aceptable en tests y desarrollo local.",
                PRIVATE_KEY_PROPERTY, KEY_FILE_PROPERTY);
        return generate();
    }

    private static KeyPair loadOrCreate(Path file) {
        try {
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                return load(file);
            }
            KeyPair generated = generate();
            createPrivateParent(file);
            try {
                writeNewOwnerOnly(file, toPkcs8Pem(generated.getPrivate()).getBytes(StandardCharsets.US_ASCII));
            } catch (FileAlreadyExistsException concurrentStart) {
                // Otra instancia la creó al mismo tiempo: se usa la suya.
                return load(file);
            }
            log.info("Clave RSA biométrica generada y guardada en {}", file);
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException(KEY_FILE_PROPERTY + " no se pudo leer ni crear: " + file, e);
        }
    }

    private static boolean posix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    private static KeyPair load(Path file) throws IOException {
        if (Files.isSymbolicLink(file)) {
            throw new IllegalStateException(KEY_FILE_PROPERTY + " es un enlace simbólico; se rechaza: " + file);
        }
        if (posix()) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
            if (permissions.stream().anyMatch(p -> p.name().startsWith("GROUP_") || p.name().startsWith("OTHERS_"))) {
                throw new IllegalStateException(KEY_FILE_PROPERTY + " es accesible por otros usuarios ("
                        + PosixFilePermissions.toString(permissions) + "); debe tener permisos 600: " + file);
            }
        }
        return parsePrivateKey(Files.readString(file, StandardCharsets.US_ASCII), KEY_FILE_PROPERTY);
    }

    // Solo se crea (con 700) el directorio que falta; uno existente, como /app/keys de la imagen, no se toca.
    private static void createPrivateParent(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent == null || Files.isDirectory(parent)) {
            return;
        }
        if (posix()) {
            Files.createDirectories(parent, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } else {
            Files.createDirectories(parent);
        }
    }

    // CREATE_NEW con 600 desde el primer byte: no hay ventana en la que la clave sea legible por otros.
    private static void writeNewOwnerOnly(Path file, byte[] content) throws IOException {
        FileAttribute<?>[] attributes = posix()
                ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))}
                : new FileAttribute<?>[0];
        try (SeekableByteChannel channel = Files.newByteChannel(file,
                EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), attributes)) {
            channel.write(ByteBuffer.wrap(content));
        }
    }

    private static KeyPair parsePrivateKey(String encoded, String property) {
        try {
            String body = encoded.replace("\\n", "\n")
                    .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey key = factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
            if (!(key instanceof RSAPrivateCrtKey crt)) {
                throw new IllegalStateException(property + " no es una clave RSA PKCS#8 completa");
            }
            PublicKey publicKey = factory.generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
            return new KeyPair(publicKey, key);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(property + " no contiene una clave privada RSA PKCS#8 válida", e);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar la clave RSA biométrica", e);
        }
    }

    private static List<String> splitKeys(String keys) {
        if (keys == null || keys.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keys.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String kidOf(PublicKey publicKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            return "rsa-" + HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private static String toPem(PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(publicKey.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    private static String toPkcs8Pem(PrivateKey key) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(key.getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }
}
