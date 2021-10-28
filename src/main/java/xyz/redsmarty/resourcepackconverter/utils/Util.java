package xyz.redsmarty.resourcepackconverter.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Util {
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private static final String[] int2word = {
            "zero",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine"
    };


    public static String streamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = "";
        StringBuilder text = new StringBuilder();
        while (true) {
            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            text.append(line);
        }
        return text.toString();
    }

    public static String formatModelName(String itemName, String textureName) {
        StringBuilder builder = new StringBuilder(itemName);
        for (char c : textureName.toCharArray()) {
            if (!Character.isDigit(c)) {
                builder.append(c);
                continue;
            }
            builder.append(int2word[Character.getNumericValue(c)]);
        }
        return builder.toString();
    }

    public static byte[] calculateSHA1(File file) {
        byte[] sha1;

        try {
            sha1 = MessageDigest.getInstance("SHA-1").digest(new FileInputStream(file).readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }
        return sha1;
    }
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

}
