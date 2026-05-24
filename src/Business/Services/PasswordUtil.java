package Business.Services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256 with a random salt. Stored format:
 * {@code base64(salt):base64(sha256(salt + password))}.
 * <p>
 * The service keeps the business rule in one place before any data is saved, loaded, or shown. This helps
 * the rest of the project call the same logic consistently.
 * </p>
 */
public class PasswordUtil {

    /**
     * Checks whether h exists.
     * <p>
     * This method keeps the business decision in the service layer before anything is sent back to the
     * screen.
     * </p>
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
     * Handles verify.
     * <p>
     * This method keeps the business decision in the service layer before anything is sent back to the
     * screen.
     * </p>
     *
     * @param plainPassword the password to verify
     * @param stored the stored hash produced by {@link #hash(String)}
     * @return true if the password matches the stored hash
     */
    public static boolean verify(String plainPassword, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        String[] parts = stored.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return parts[1].equals(digest(plainPassword, salt));
    }

    /**
     * Handles digest.
     * <p>
     * This method keeps the business decision in the service layer before anything is sent back to the
     * screen.
     * </p>
     *
     * @param password password entered by the user
     * @param salt salt used by this operation
     * @return the result of the operation
     */
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
