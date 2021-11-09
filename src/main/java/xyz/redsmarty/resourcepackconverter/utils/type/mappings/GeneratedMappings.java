package xyz.redsmarty.resourcepackconverter.utils.type.mappings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratedMappings {
    private final Map<String, List<CustomModelDataMapping>> itemMappings2d = new HashMap<>();
    private final Map<String, List<CustomModelDataMapping>> itemMappings3d = new HashMap<>();
    private final List<BlockMapping> blockMappings = new ArrayList<>();
    private String json = "";

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Map<String, List<CustomModelDataMapping>> getItemMappings2d() {
        return itemMappings2d;
    }

    public Map<String, List<CustomModelDataMapping>> getItemMappings3d() {
        return itemMappings3d;
    }

    public List<BlockMapping> getBlockMappings() {
        return blockMappings;
    }
}
