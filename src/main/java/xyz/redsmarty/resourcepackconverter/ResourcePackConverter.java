package xyz.redsmarty.resourcepackconverter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.Gson;
import xyz.redsmarty.resourcepackconverter.api.ConversionAPI;
import xyz.redsmarty.resourcepackconverter.utils.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;

public class ResourcePackConverter {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("No parameters given. Correct usage is java -jar ResourcePackConverter.jar <java-resource-pack-file> <bedrock-resource-pack-file>");
        }
        File javaFile = new File(args[0]);
        File bedrockFile = new File(args[1]);

        System.out.printf("Converting Minecraft: Java Edition resource pack at %s%n", javaFile.getAbsolutePath());
        System.out.printf("Converting to Minecraft: Bedrock Edition resource pack at %s%n", bedrockFile.getAbsolutePath());
        try {
            File mappingsFile = new File(javaFile.getParentFile(), "cmd_mappings.json");
            FileWriter writer = new FileWriter(mappingsFile);
            long currentTime = System.currentTimeMillis();
            String generatedCustomModelDataMappings = ConversionAPI.getInstance().convert(javaFile, bedrockFile, new ConversionOptions("SamplePack", UUID.randomUUID(), new int[] {1, 0, 0})).getGeneratedCustomModelDataMappings();
            writer.write(generatedCustomModelDataMappings);
            writer.flush();
            writer.close();
            System.out.println("Converted resource pack in " + (System.currentTimeMillis() - currentTime) + "ms.");
        } catch (IOException | InvalidResourcePackException e) {
            e.printStackTrace();
        }
    }
}
