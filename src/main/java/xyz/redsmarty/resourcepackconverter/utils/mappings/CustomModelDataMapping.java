package xyz.redsmarty.resourcepackconverter.utils.mappings;

import com.google.gson.annotations.SerializedName;

public class CustomModelDataMapping {
    @SerializedName("custom_model_data")
    private final int customModelData;
    @SerializedName("item")
    private final String bedrockItem;

    public CustomModelDataMapping(int customModelData, String bedrockItem) {
        this.customModelData = customModelData;
        this.bedrockItem = bedrockItem;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getBedrockItem() {
        return bedrockItem;
    }
}
