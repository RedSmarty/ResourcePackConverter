package xyz.redsmarty.resourcepackconverter.resourcepacks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.redsmarty.resourcepackconverter.resourcepacks.manifests.JavaResourcePackManifest;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;
import xyz.redsmarty.resourcepackconverter.utils.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class JavaResourcePack {
    private final ZipFile zip;
    private final String hash;
    private final Map<String, ZipEntry> files = new HashMap<>();
    private final JavaResourcePackManifest manifest;

    public JavaResourcePack(String name, ZipFile zip, String sha1) throws InvalidResourcePackException {
        this.zip = zip;
        this.hash = sha1;
        readFiles(zip);
        this.manifest = readMeta(name);
    }

    public InputStream getFile(String path) {
        if (!files.containsKey(path)) return null;
        try {
            return zip.getInputStream(files.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, InputStream> getFiles(String path) {
        Map<String, InputStream> fileMap = new HashMap<>();
        files.entrySet().stream().filter(entry -> entry.getKey().startsWith(path) && !entry.getKey().equals(path)).forEach(entry -> {
            try {
                fileMap.put(entry.getKey(), zip.getInputStream(entry.getValue()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return fileMap;
    }

    public Map<String, InputStream> getFilesExact(String path) {
        Map<String, InputStream> fileMap = new HashMap<>();
        files.entrySet().stream().filter(entry -> entry.getKey().startsWith(path) && !entry.getKey().equals(path) && !entry.getKey().replace(path, "").contains("/")).forEach(entry -> {
            try {
                fileMap.put(entry.getKey(), zip.getInputStream(entry.getValue()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return fileMap;
    }

    public String getName() {
        return manifest.getName();
    }

    public String getDescription() {
        return manifest.getDescription();
    }

    public int getVersion() {
        return manifest.getVersion();
    }

    private JavaResourcePackManifest readMeta(String name) throws InvalidResourcePackException {
        int version;
        String description;
        InputStream metaStream = getFile("pack.mcmeta");
        if (metaStream == null) {
            throw new InvalidResourcePackException(name, InvalidResourcePackException.Reason.NO_PACK_MCMETA);
        }
        try {
            JsonObject meta = JsonParser.parseString(Util.streamToString(metaStream)).getAsJsonObject().getAsJsonObject("pack");
            version = meta.getAsJsonPrimitive("pack_format").getAsInt();
            if (version < 6 ) throw new InvalidResourcePackException(name, InvalidResourcePackException.Reason.OUTDATED_VERSION);
            description = meta.getAsJsonPrimitive("description").getAsString();
        } catch (IllegalStateException e) {
            throw new InvalidResourcePackException(name, InvalidResourcePackException.Reason.INVALID_PACK_MCMETA);
        }
        return new JavaResourcePackManifest(name, version, description);
    }

    private void readFiles(ZipFile zip) {
        zip.entries().asIterator().forEachRemaining(zipEntry -> files.put(zipEntry.getName(), zipEntry));
    }

    public String getHash() {
        return hash;
    }
}
