package xyz.redsmarty.resourcepackconverter.converters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.redsmarty.resourcepackconverter.ResourcePackConverter;
import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionResults;
import xyz.redsmarty.resourcepackconverter.utils.type.TextureMapping;
import xyz.redsmarty.resourcepackconverter.utils.Util;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemTextureConverter implements AbstractConverter{
    Map<String, TextureMapping> mappings = new LinkedHashMap<>();

    public  void convert(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, ConversionOptions options, ConversionResults results) {
        if (mappings.size() == 0) {
            readMappings();
        }

        for (Map.Entry<String, byte[]> javaTexture: javaResourcePack.getFiles("assets/minecraft/textures/item/").entrySet()) {
            if (!mappings.containsKey(javaTexture.getKey())) continue;
            bedrockResourcePack.setFile(mappings.get(javaTexture.getKey()).getBedrockName(), javaTexture.getValue());
            options.logger().accept(String.format("Mapped java texture %s to bedrock texture %s.", javaTexture.getKey(), mappings.get(javaTexture.getKey()).getBedrockName()));
        }
    }

    private void readMappings() {
        JsonObject json = JsonParser.parseString(Util.streamToString(ResourcePackConverter.class.getClassLoader().getResourceAsStream("item_texture_mappings.json"))).getAsJsonObject();

        for (Map.Entry<String, JsonElement> mapping : json.entrySet()) {
            JsonObject mappingObject = mapping.getValue().getAsJsonObject();
            String bedrockName = mappingObject.getAsJsonPrimitive("name").getAsString();
            JsonArray uvsArray = mappingObject.getAsJsonArray("uvs");
            int[] uvs = uvsArray == null ? null : new int[] {
                uvsArray.get(0).getAsInt(),
                uvsArray.get(1).getAsInt(),
                uvsArray.get(2).getAsInt(),
                uvsArray.get(3).getAsInt()
            };
            mappings.put(mapping.getKey(), new TextureMapping(mapping.getKey(), bedrockName, uvs));
        }
    }
}
