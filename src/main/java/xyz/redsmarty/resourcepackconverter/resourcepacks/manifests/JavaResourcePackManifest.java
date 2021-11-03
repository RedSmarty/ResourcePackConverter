package xyz.redsmarty.resourcepackconverter.resourcepacks.manifests;

public class JavaResourcePackManifest {
    private final int version;
    private final String description;

    public JavaResourcePackManifest(int version, String description) {
        this.version = version;
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }
}
