package server;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getHash(String string) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] encodedhash = digest.digest(
                string.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
    }

    public static BigInteger value(String hash) {
        return new BigInteger(hash, 16);
    }

    public static void main(String[] args) {
        String test = Hash.getHash("14342342");
        System.out.println(test);
    }

    public static Boolean hlt(String h1, String h2) {
        Integer result = Hash.value(h1).compareTo(Hash.value(h2));
        if (result == -1) {
            return true;
        }
        return false;
    }

    public static Boolean hle(String h1, String h2) {
        Integer result = Hash.value(h1).compareTo(Hash.value(h2));
        if (result == -1 || result == 0) {
            return true;
        }
        return false;
    }

    public static Boolean hbe(String h1, String h2) {
        Integer result = Hash.value(h1).compareTo(Hash.value(h2));
        if (result == 1 || result == 0) {
            return true;
        }
        return false;
    }

    public static Boolean hbt(String h1, String h2) {
        Integer result = Hash.value(h1).compareTo(Hash.value(h2));
        if (result == 1) {
            return true;
        }
        return false;
    }
}
