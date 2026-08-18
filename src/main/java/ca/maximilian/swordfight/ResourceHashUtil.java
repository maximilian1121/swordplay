package ca.maximilian.swordfight;

import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for fetching external resources and calculating cryptographic hashes.
 */
public final class ResourceHashUtil {

    private static final String HASH_ALGORITHM = "SHA-1";
    private static final int BUFFER_SIZE = 8192;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    // Suppress default constructor for noninstantiability
    private ResourceHashUtil() {
        throw new AssertionError("No ResourceHashUtil instances for you!");
    }

    /**
     * Downloads a file from the specified URI string and calculates its SHA-1 hash.
     *
     * @param uriString The direct URL path to the target file.
     * @return A 40-character lowercase hexadecimal SHA-1 string.
     * @throws Exception If a network error occurs, the stream fails, or the hashing algorithm is missing.
     */
    public static @NonNull String fetchSHA1(String uriString) throws Exception {
        Objects.requireNonNull(uriString, "URI string cannot be null");
        return fetchSHA1(URI.create(uriString));
    }

    /**
     * Downloads a file from the specified URI and calculates its SHA-1 hash.
     *
     * @param uri The direct URI path to the target file.
     * @return A 40-character lowercase hexadecimal SHA-1 string.
     * @throws Exception If a network error occurs, the stream fails, or the hashing algorithm is missing.
     */
    public static @NonNull String fetchSHA1(URI uri) throws Exception {
        Objects.requireNonNull(uri, "URI cannot be null");
        validateScheme(uri);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " algorithm not available in this runtime environment", e);
        }

        // Direct stream download to minimize heap memory pressure
        try (InputStream inputStream = uri.toURL().openStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return bytesToHex(digest.digest());
    }

    /**
     * Ensures the URI uses an allowed scheme (http or https) before attempting a connection.
     * This avoids accidental use of schemes like file:// or jar:// via openStream().
     */
    private static void validateScheme(@NonNull URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported URI scheme: " + scheme + ". Only http and https are allowed.");
        }
    }

    /**
     * Converts raw byte arrays into a structured, lowercase hexadecimal string.
     */
    private static @NonNull String bytesToHex(byte @NonNull [] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}