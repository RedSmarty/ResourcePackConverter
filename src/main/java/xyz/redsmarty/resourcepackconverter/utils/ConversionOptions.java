package xyz.redsmarty.resourcepackconverter.utils;

import java.util.UUID;
import java.util.function.Consumer;

public class ConversionOptions {
    private final String name;
    private final UUID uuid;
    private final int[] version;
    private final String javaHash;
    private final Consumer<String> logger;

    public ConversionOptions(String name, UUID uuid, int[] version, String javaHash, Consumer<String> logger) {
        this.name = name;
        this.uuid = uuid;
        this.version = version;
        this.javaHash = javaHash;
        this.logger = logger;
    }

    public String getName() {
        return name;
    }

    public int[] getVersion() {
        return version;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getJavaHash() {
        return javaHash;
    }

    public Consumer<String> getLogger() {
        return logger;
    }
}
