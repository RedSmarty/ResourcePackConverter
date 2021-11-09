package xyz.redsmarty.resourcepackconverter.utils.type;

import xyz.redsmarty.resourcepackconverter.utils.type.mappings.GeneratedMappings;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConversionResults {
    private boolean success = false;
    private final GeneratedMappings generatedMappings = new GeneratedMappings();
    private final Map<String, String> itemTextures = new LinkedHashMap<>();
    private final Map<String, String> blockTextures = new LinkedHashMap<>();
    final Map<String, int[]> textureDimensions = new LinkedHashMap<>();

    public Map<String, int[]> getTextureDimensions() {
        return textureDimensions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, String> getItemTextures() {
        return itemTextures;
    }

    public Map<String, String> getBlockTextures() {
        return blockTextures;
    }

    public GeneratedMappings getGeneratedMappings() {
        return generatedMappings;
    }
}
