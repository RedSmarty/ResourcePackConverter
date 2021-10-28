package xyz.redsmarty.resourcepackconverter.resourcepacks.manifests;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class BedrockResourcePackManifest {
    @SerializedName("format_version")
    private final int formatVersion = 2;
    private final Header header;
    private final Module[] modules;

    public BedrockResourcePackManifest(String name, int[] version, String description) {
        header = new Header(name, version, description);
        modules = new Module[] {new Module(description, version)};
    }

    public Header getHeader() {
        return header;
    }

    public Module[] getModules() {
        return modules;
    }

    public class Header {
        private final String name;
        private final String description;
        private final String uuid;
        private final int[] version;
        @SerializedName("min_engine_version")
        private final int[] minimumEngineVersion = {1, 17, 0};

        public Header(String name, int[] version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.uuid = UUID.randomUUID().toString();
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUUID() {
            return uuid;
        }

        public int[] getVersion() {
            return version;
        }

        public int[] getMinimumEngineVersion() {
            return minimumEngineVersion;
        }
    }
    public class Module {
        private final String description;
        private final int[] version;
        private final String uuid;
        private final String type;

        public Module(String description, int[] version) {
            this.description = description;
            this.version = version;
            this.uuid = UUID.randomUUID().toString();
            this.type = "resources";
        }

        public String getDescription() {
            return description;
        }

        public String getUuid() {
            return uuid;
        }

        public int[] getVersion() {
            return version;
        }

        public String getType() {
            return type;
        }
    }
}
