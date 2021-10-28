package xyz.redsmarty.resourcepackconverter.utils;

import java.util.UUID;

public class ConversionOptions {
    private final String name;
    private final UUID uuid;
    private final int[] version;
    private final String namespace;

    public ConversionOptions(String name, UUID uuid, int[] version, String nameSpace) {
        this.name = name;
        this.uuid = uuid;
        this.version = version;
        this.namespace = nameSpace;
    }

    public String getName() {
        return name;
    }

    public int[] getVersion() {
        return version;
    }

    public String getNamespace() {
        return namespace;
    }

    public UUID getUuid() {
        return uuid;
    }
}
