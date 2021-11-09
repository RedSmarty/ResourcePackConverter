package xyz.redsmarty.resourcepackconverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import xyz.redsmarty.resourcepackconverter.api.ConversionAPI;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;
import xyz.redsmarty.resourcepackconverter.utils.Util;

public class ResourcePackConverter {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("No parameters given. Correct usage is java -jar ResourcePackConverter.jar <java-resource-pack-file> <bedrock-resource-pack-file>");
        }
        File javaFile = new File(args[0]);
        File bedrockFile = new File(args[1]);

        final boolean logMessages = args.length >= 3 && Boolean.parseBoolean(args[2]);

        System.out.printf("Converting Minecraft: Java Edition resource pack at %s%n", javaFile.getAbsolutePath());
        System.out.printf("Converting to Minecraft: Bedrock Edition resource pack at %s%n", bedrockFile.getAbsolutePath());
        try {
            long currentTime = System.currentTimeMillis();

            StringBuilder logs = new StringBuilder();
            String generatedCustomModelDataMappings = ConversionAPI.getInstance().convert(new FileInputStream(javaFile), bedrockFile, new ConversionOptions("SamplePack", UUID.randomUUID(), new int[]{1, 0, 0}, Util.bytesToHex(Util.calculateSHA1(javaFile)), "rpconverter",message -> {
                if (logMessages) logs.append(message).append("\n");
            })).getGeneratedMappings().getJson();

            File mappingsFile = new File(javaFile.getParentFile(), "cmd_mappings.json");

            if (!mappingsFile.exists()) {
                mappingsFile.createNewFile();
            }
            try (FileWriter writer = new FileWriter(mappingsFile)) {
                writer.write(generatedCustomModelDataMappings);
                writer.flush();
            }
            if (logMessages) {
                File logFile = new File(javaFile.getParentFile(), "resource_pack_conversion.log");
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                try (FileWriter writer = new FileWriter(logFile)) {
                    writer.write(logs.toString());
                }
            }
            System.out.println("Converted resource pack in " + (System.currentTimeMillis() - currentTime) + "ms.");
        } catch (IOException | InvalidResourcePackException e) {
            e.printStackTrace();
        }
    }
}
