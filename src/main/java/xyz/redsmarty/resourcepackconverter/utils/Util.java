package xyz.redsmarty.resourcepackconverter.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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

    public static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String formatItemName(String name) {
        StringBuilder builder = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (!Character.isDigit(c)) {
                builder.append(c);
                continue;
            }
            builder.append(int2word[Character.getNumericValue(c)]);
        }
        return builder.toString();
    }

    public static String formatBlockName(String name) {
        // Remove the namespace
        name = name.replace("minecraft:", "");

        // Replace "[", "=" and "," with underscore and remove "]"
        name = name.replaceAll("\\[", "_").replaceAll("]", "").replaceAll("=", "_").replaceAll(",", "_");

        return formatItemName(name);
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

    public static int[] getImageDimensions(byte[] data) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        return new int[] {image.getWidth(), image.getHeight()};
    }

    public static String resolveNamespace(String path, String subDir) {
        String namespace = path.split(":")[0].equals(path) ? "minecraft" : path.split(":")[0];
        return "assets/" + namespace + "/" + subDir + "/" + path.replace(namespace + ":", "");
    }
}
