package xyz.redsmarty.resourcepackconverter.resourcepacks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import xyz.redsmarty.resourcepackconverter.resourcepacks.manifests.JavaResourcePackManifest;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;
import xyz.redsmarty.resourcepackconverter.utils.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class JavaResourcePack {
    private final Map<String, byte[]> files = new HashMap<>();
    private final JavaResourcePackManifest manifest;

    public JavaResourcePack(ZipInputStream stream, Consumer<String> logger) throws InvalidResourcePackException, IOException {
        readFiles(stream, logger);
        this.manifest = readMeta(logger);
    }

    public byte[] getFile(String path) {
        if (!files.containsKey(path)) return null;
        return files.get(path);
    }

    public Map<String, byte[]> getFiles(String path) {
        Map<String, byte[]> fileMap = new HashMap<>();
        files.entrySet().stream().filter(entry -> entry.getKey().startsWith(path) && !entry.getKey().equals(path)).forEach(entry -> {
            fileMap.put(entry.getKey(), entry.getValue());
        });
        return fileMap;
    }

    public Map<String, byte[]> getFilesExact(String path) {
        Map<String, byte[]> fileMap = new HashMap<>();
        files.entrySet().stream().filter(entry -> entry.getKey().startsWith(path) && !entry.getKey().equals(path) && !entry.getKey().replace(path, "").contains("/")).forEach(entry -> {
            fileMap.put(entry.getKey(), entry.getValue());
        });
        return fileMap;
    }

    public String getDescription() {
        return manifest.getDescription();
    }

    private JavaResourcePackManifest readMeta(Consumer<String> logger) throws InvalidResourcePackException {
        int version;
        String description;
        byte[] metaStream = getFile("pack.mcmeta");
        if (metaStream == null) {
            throw new InvalidResourcePackException(InvalidResourcePackException.Reason.NO_PACK_MCMETA);
        }
        try {
            JsonObject meta = JsonParser.parseString(Util.bytesToString(metaStream)).getAsJsonObject().getAsJsonObject("pack");
            version = meta.getAsJsonPrimitive("pack_format").getAsInt();
            if (version < 6 ) throw new InvalidResourcePackException(InvalidResourcePackException.Reason.OUTDATED_VERSION);
            description = meta.getAsJsonPrimitive("description").getAsString();
        } catch (IllegalStateException | JsonSyntaxException e) {
            logger.accept("pack.mcmeta is in illegal format.");
            throw new InvalidResourcePackException(InvalidResourcePackException.Reason.INVALID_PACK_MCMETA);
        }
        return new JavaResourcePackManifest(version, description);
    }

    private void readFiles(ZipInputStream stream, Consumer<String> logger) throws IOException {
        ZipEntry entry;

        while ((entry = stream.getNextEntry()) != null) {
            files.put(entry.getName(), stream.readAllBytes());
            logger.accept(String.format("Loaded %s from Minecraft: Java Edition resource pack.", entry.getName()));
        }
    }
}
