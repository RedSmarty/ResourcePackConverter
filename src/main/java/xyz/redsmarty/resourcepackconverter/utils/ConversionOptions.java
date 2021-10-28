package xyz.redsmarty.resourcepackconverter.utils;

import java.util.UUID;

public class ConversionOptions {
    private final String name;
    private final UUID uuid;
    private final int[] version;

    public ConversionOptions(String name, UUID uuid, int[] version) {
        this.name = name;
        this.uuid = uuid;
        this.version = version;
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
}
