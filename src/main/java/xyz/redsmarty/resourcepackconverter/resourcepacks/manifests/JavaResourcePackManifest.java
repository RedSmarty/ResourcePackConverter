package xyz.redsmarty.resourcepackconverter.resourcepacks.manifests;

public class JavaResourcePackManifest {
    private final String name;
    private final int version;
    private final String description;

    public JavaResourcePackManifest(String name, int version, String description) {
        this.name = name;
        this.version = version;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }
}
