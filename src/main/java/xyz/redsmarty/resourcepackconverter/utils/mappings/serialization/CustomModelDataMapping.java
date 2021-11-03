package xyz.redsmarty.resourcepackconverter.utils.mappings.serialization;

import com.google.gson.annotations.SerializedName;

public record CustomModelDataMapping(@SerializedName("custom_model_data") int customModelData,
                                     @SerializedName("item") String bedrockItem) {

    public int getCustomModelData() {
        return customModelData;
    }

    public String getBedrockItem() {
        return bedrockItem;
    }
}
