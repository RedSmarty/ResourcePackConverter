package xyz.redsmarty.resourcepackconverter.resourcepacks;

import com.google.gson.Gson;
import xyz.redsmarty.resourcepackconverter.resourcepacks.manifests.BedrockResourcePackManifest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BedrockResourcePack {
    Map<String, byte[]> files = new HashMap<>();
    private final BedrockResourcePackManifest manifest;

    public BedrockResourcePack(String name, int[] version, String description, byte[] icon) {
        manifest = new BedrockResourcePackManifest(name, version, description);
        if (icon != null) {
            setFile("pack_icon.png", icon);
        }
    }

    public void setFile(String path, byte[] file) {
        files.put(path, file);
    }

    public String getName() {
        return manifest.getHeader().getName();
    }

    public String getUUID() {
        return manifest.getHeader().getUUID();
    }

    public void zipIt(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }

        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file));
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            output.putNextEntry(new ZipEntry(entry.getKey()));
            output.write(entry.getValue());
            output.closeEntry();
        }
        output.putNextEntry(new ZipEntry("manifest.json"));
        output.write(new Gson().toJson(manifest).getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
        output.close();
    }
}
