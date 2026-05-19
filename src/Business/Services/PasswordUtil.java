package Business.Services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    public static String hash(String plainPassword) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        return saltB64 + ":" + digest(plainPassword, salt);
    }

    public static boolean verify(String plainPassword, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        String[] parts = stored.split(":", 2);
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        return parts[1].equals(digest(plainPassword, salt));
    }

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
