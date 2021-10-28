package xyz.redsmarty.resourcepackconverter.converters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.Util;
import xyz.redsmarty.resourcepackconverter.utils.mappings.ConversionResults;
import xyz.redsmarty.resourcepackconverter.utils.mappings.CustomModelDataMapping;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CustomModelDataConverter implements AbstractConverter {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, List<CustomModelDataMapping>> mappings = new HashMap<>();
    private final Set<String> converted = new HashSet<>();
    private final Map<String, String> itemTextures = new TreeMap<>();

    /**
     * Converts the java model data to bedrock custom items and puts CustomModelData mappings in results
     * @param javaResourcePack Java resource pack to convert
     * @param bedrockResourcePack Bedrock resource pack to convert to
     * @param options Conversion options
     * @param results Results to put CustomModelData mappings in
     */
    @Override
    public void convert(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, ConversionOptions options, ConversionResults results) {
        // loops through the model files
        for (Map.Entry<String, InputStream> entry : javaResourcePack.getFilesExact("assets/minecraft/models/item/").entrySet()) {
            if (!entry.getKey().endsWith(".json")) continue;
            String[] fileName = entry.getKey().split("/");
            prepareMapping(javaResourcePack, bedrockResourcePack, fileName[fileName.length - 1], Util.streamToString(entry.getValue()));
        }

        // Generated item_texture.json file
        JsonObject itemTextures = new JsonObject();
        itemTextures.addProperty("resource_pack_name", bedrockResourcePack.getName());
        itemTextures.addProperty("texture_name", "atlas.items");
        JsonObject textureData = new JsonObject();
        for (Map.Entry<String, String> entry : this.itemTextures.entrySet()) {
            JsonObject itemTextureObject = new JsonObject();
            JsonArray textures = new JsonArray();
            textures.add(entry.getValue());
            itemTextureObject.add("textures", textures);
            textureData.add(entry.getKey(), itemTextureObject);
        }
        itemTextures.add("texture_data", textureData);
        bedrockResourcePack.setFile("textures/item_texture.json", gson.toJson(itemTextures).getBytes(StandardCharsets.UTF_8));

        // Generates CustomModelData mappings
        JsonObject mappingsRoot = new JsonObject();
        JsonArray convertedPackVersion = new JsonArray();
        convertedPackVersion.add(bedrockResourcePack.getVersion()[0]);
        convertedPackVersion.add(bedrockResourcePack.getVersion()[1]);
        convertedPackVersion.add(bedrockResourcePack.getVersion()[2]);
        JsonObject mappingsData = new JsonObject();
        for (Map.Entry<String, List<CustomModelDataMapping>> entry : mappings.entrySet()) {
            JsonArray perItemMappings = gson.toJsonTree(entry.getValue()).getAsJsonArray();
            mappingsData.add(entry.getKey(), perItemMappings);
        }
        mappingsRoot.addProperty("hash", javaResourcePack.getHash());
        mappingsRoot.addProperty("converted_pack_uuid", bedrockResourcePack.getUUID());
        mappingsRoot.add("mappings_data", mappingsData);
        results.setGeneratedCustomModelDataMappings(gson.toJson(mappingsRoot));
    }

    private void prepareMapping(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, String fileName, String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray overrides = root.getAsJsonArray("overrides");
        for (JsonElement overrideElement : overrides) {
            String javaItemName = "minecraft:" + fileName.replace(".json", "");

            JsonObject override = overrideElement.getAsJsonObject();
            JsonObject predicate = override.getAsJsonObject("predicate");
            JsonPrimitive customModelDataPrimitive = predicate.getAsJsonPrimitive("custom_model_data");
            if (customModelDataPrimitive == null) continue;
            String modelFile = override.getAsJsonPrimitive("model").getAsString();
            String[] splitModelName = modelFile.split("/");
            String modelName = splitModelName[splitModelName.length - 1];
            String modelPath = "assets/minecraft/models/" + modelFile + ".json";
            String formattedName = Util.formatModelName(fileName.replace(".json", ""), modelName);

            if (!converted.contains(modelName)) {
                InputStream modelStream = javaResourcePack.getFile(modelPath);
                if (modelStream == null) continue;
                JsonObject model = JsonParser.parseString(Util.streamToString(modelStream)).getAsJsonObject();
                JsonPrimitive javaTexturePath = model.getAsJsonObject("textures").getAsJsonPrimitive("layer0");
                if (javaTexturePath == null) continue;
                InputStream javaTexture = javaResourcePack.getFile("assets/minecraft/textures/" + javaTexturePath.getAsString() + ".png");
                if (javaTexture == null) continue;
                String bedrockTexturePath = "textures/items/custom/" + javaTexturePath.getAsString() + ".png";
                bedrockResourcePack.setFile(bedrockTexturePath, javaTexture);

//                JsonObject textureDefinition = new JsonObject();
//                JsonObject item = new JsonObject();
                String[] splitTextureName = javaTexturePath.getAsString().split("/");
//                JsonObject description = new JsonObject();
//                description.addProperty("identifier", namespace + ":" + modelName);
//                description.addProperty("category", "Nature");
//                JsonObject components = new JsonObject();
//                components.addProperty("minecraft:icon", splitTextureName[splitTextureName.length - 1]);
//                item.add("description", description);
//                item.add("components", components);
//                textureDefinition.addProperty("format_version", "1.10");
//                textureDefinition.add("minecraft:item", item);
//                bedrockResourcePack.setFile("items/" + splitModelName[splitModelName.length - 1] + ".json", gson.toJson(textureDefinition).getBytes(StandardCharsets.UTF_8));
                itemTextures.put(formattedName, bedrockTexturePath);
                converted.add(modelName);
            }
            if (!mappings.containsKey(javaItemName)) mappings.put(javaItemName, new ArrayList<>());
            mappings.get(javaItemName).add(new CustomModelDataMapping(customModelDataPrimitive.getAsInt(), formattedName));
        }
    }
}
