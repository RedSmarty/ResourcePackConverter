package xyz.redsmarty.resourcepackconverter.utils.type;

public class TextureMapping {
    private final String javaName;
    private final String bedrockName;
    private final int[] uvs;

    public TextureMapping(String key, String value, int[] uvs) {
        this.javaName = key;
        this.bedrockName = value;
        this.uvs = uvs;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getBedrockName() {
        return bedrockName;
    }

    public int[] getUvs() {
        return uvs;
    }
}
