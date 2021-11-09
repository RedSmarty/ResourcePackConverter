package xyz.redsmarty.resourcepackconverter.converters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.Util;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionResults;
import xyz.redsmarty.resourcepackconverter.utils.type.mappings.BlockMapping;

import java.util.HashMap;
import java.util.Map;

public final class CustomBlocksConverter implements AbstractConverter{

    @Override
    public void convert(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, ConversionOptions options, ConversionResults results) {
        for (Map.Entry<String, byte[]> entry : javaResourcePack.getFilesExact("assets/minecraft/blockstates/").entrySet()) {
            if (!entry.getKey().endsWith(".json")) continue;
            JsonObject root = JsonParser.parseString(Util.bytesToString(entry.getValue())).getAsJsonObject();

            if (root.has("multipart")) {
                JsonArray blockStates = root.getAsJsonArray("multipart");

                for (JsonElement blockStateEntry : blockStates) {
                    JsonObject blockState = blockStateEntry.getAsJsonObject();
                    JsonObject conditions = blockState.getAsJsonObject("when");
                    JsonObject apply = blockState.getAsJsonObject("apply");
                    String modelPath = apply.getAsJsonPrimitive("model").getAsString();

                    String blockName = entry.getKey().substring(entry.getKey().lastIndexOf("/") + 1).replace(".json", "");

                    StringBuilder blockStateNameBuilder = new StringBuilder("minecraft:" + blockName + "[");

                    for (Map.Entry<String, JsonElement> condition : conditions.entrySet()) {
                        blockStateNameBuilder.append(condition.getKey()).append("=").append(condition.getValue().getAsJsonPrimitive().getAsString()).append(",");
                    }

                    String blockStateName = blockStateNameBuilder.deleteCharAt(blockStateNameBuilder.length() - 1).append("]").toString();

                    JsonObject model = JsonParser.parseString(Util.bytesToString(javaResourcePack.getFile(Util.resolveNamespace(modelPath, "models") + ".json"))).getAsJsonObject();

                    JsonObject texturePaths = model.getAsJsonObject("textures");
                    Map<String, byte[]> textures = new HashMap<>();

                    for (Map.Entry<String, JsonElement> textureEntry : texturePaths.entrySet()) {
                        textures.put(textureEntry.getKey(), javaResourcePack.getFile(Util.resolveNamespace(textureEntry.getValue().getAsJsonPrimitive().getAsString(), "textures") + ".png"));
                    }

                    String formattedName = Util.formatBlockName(blockStateName);
                    String bedrockTexturePath = "textures/blocks/" + formattedName + ".png";

                    if (model.has("elements")) {
                        // Still have to add custom geometry support in blocks
                    } else {
                        if (!textures.containsKey("all")) {
                            options.logger().accept(String.format("%s doesn't have an \"all\" texture. Only \"all\" textures are supported at the moment.", modelPath));
                            continue;
                        }
                        if (textures.get("all") == null) {
                            options.logger().accept(String.format("%s's all texture (%s) does not exist in this resource pack.", modelPath, texturePaths.getAsJsonPrimitive("all").getAsString()));
                            continue;
                        }
                        bedrockResourcePack.setFile(bedrockTexturePath, textures.get("all"));
                        results.getBlockTextures().put(formattedName, bedrockTexturePath);
                        results.getGeneratedMappings().getBlockMappings().add(new BlockMapping(blockStateName, formattedName, false));
                    }
                }
            }

        }
    }
}
