package Business.Services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256 with a random salt.
 * Stored format: {@code base64(salt):base64(sha256(salt + password))}.
 */
public class PasswordUtil {

    /**
     * Hashes a plain-text password with a freshly generated random salt.
     *
     * @param plainPassword the password to hash
     * @return a salted hash string suitable for database storage
     */
    public static String hash(String plainPassword) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        return saltB64 + ":" + digest(plainPassword, salt);
    }

    /**
     * Verifies a plain-text password against a stored salted hash.
     *
     * @param plainPassword the password to verify
     * @param stored        the stored hash produced by {@link #hash(String)}
     * @return true if the password matches the stored hash
     */
    public static boolean verify(String plainPassword, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        String[] parts = stored.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return parts[1].equals(digest(plainPassword, salt));
    }

    /** Creates a SHA-256 digest for a password and salt. */
    private static String digest(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return Base64.getEncoder().encodeToString(md.digest(password.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
