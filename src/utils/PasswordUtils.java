package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtils {
    private static final String ALGORITHM = "MD5";
    
    /**
     * Hash a password using MD5
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] hashedPassword = md.digest(password.getBytes("UTF-8")); // Added UTF-8 encoding
            
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) { // Catch both NoSuchAlgorithmException and UnsupportedEncodingException
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verify a password against a stored hash
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            if (password == null || storedHash == null) {
                return false;
            }
            
            // Hash the provided password
            String providedHashString = hashPassword(password);
            
            // Debug output
            System.out.println("DEBUG - Provided password: '" + password + "'");
            System.out.println("DEBUG - Generated hash: '" + providedHashString + "'");
            System.out.println("DEBUG - Stored hash: '" + storedHash + "'");
            System.out.println("DEBUG - Hashes match: " + storedHash.equals(providedHashString));
            
            // Compare hashes
            return storedHash.equals(providedHashString);
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if password meets minimum requirements
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 4) {
            return false;
        }
        return true;
    }
    
    // Test method to verify hashing works correctly
    public static void testHashing() {
        String testPassword = "password123";
        String hash1 = hashPassword(testPassword);
        String hash2 = hashPassword(testPassword);
        
        System.out.println("Test password: " + testPassword);
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Hashes are identical: " + hash1.equals(hash2));
        System.out.println("Verification works: " + verifyPassword(testPassword, hash1));
    }
}