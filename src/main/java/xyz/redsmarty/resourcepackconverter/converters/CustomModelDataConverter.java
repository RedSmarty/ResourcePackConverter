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
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.GeometryUtils;
import xyz.redsmarty.resourcepackconverter.utils.Util;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionResults;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.BedrockGeometry;
import xyz.redsmarty.resourcepackconverter.utils.type.mappings.CustomModelDataMapping;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomModelDataConverter implements AbstractConverter {

    /**
     * Converts the java model data to bedrock custom items and puts CustomModelData mappings in results
     *
     * @param javaResourcePack    Java resource pack to convert
     * @param bedrockResourcePack Bedrock resource pack to convert to
     * @param options             Conversion options
     * @param results             Results to put CustomModelData mappings in
     */
    @Override
    public void convert(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, ConversionOptions options, ConversionResults results) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        final List<String> converted = new ArrayList<>();

        // Loops through the model files
        for (Map.Entry<String, byte[]> entry : javaResourcePack.getFilesExact("assets/minecraft/models/item/").entrySet()) {
            if (!entry.getKey().endsWith(".json")) continue;
            String[] fileNameSplit = entry.getKey().split("/");
            String fileName = fileNameSplit[fileNameSplit.length - 1];
            
            
            JsonObject root = JsonParser.parseString(Util.bytesToString(entry.getValue())).getAsJsonObject();
            JsonArray overrides = root.getAsJsonArray("overrides");
            if (overrides == null) {
                options.logger().accept(String.format("%s contains no overrides, does not have custom model data.", fileName));
                return;
            }
            
            options.logger().accept(String.format("Converting %s", entry.getKey()));
            
            for (JsonElement overrideElement : overrides) {
                String javaItemName = "minecraft:" + fileName.replace(".json", "");

                JsonObject override = overrideElement.getAsJsonObject();
                JsonObject predicate = override.getAsJsonObject("predicate");
                JsonPrimitive customModelDataPrimitive = predicate.getAsJsonPrimitive("custom_model_data");
                if (customModelDataPrimitive == null) {
                    options.logger().accept(String.format("No custom model data found in predicate of %s.", fileName));
                    continue;
                }
                String modelPath = override.getAsJsonPrimitive("model").getAsString();
                String[] splitModelName = modelPath.split("/");
                String modelName = splitModelName[splitModelName.length - 1];
                String modelFile = Util.resolveNamespace(modelPath, "models") + ".json";
                String formattedName = Util.formatItemName(modelName);

                if (!converted.contains(formattedName)) {
                    byte[] modelStream = javaResourcePack.getFile(modelFile);
                    if (modelStream == null) {
                        options.logger().accept(String.format("Resource pack does not have custom model file (%s) specified in %s.", modelFile,fileName));
                        continue;
                    }
                    JsonObject model = JsonParser.parseString(Util.bytesToString(modelStream)).getAsJsonObject();

                    JsonObject javaTexturesProperty = model.getAsJsonObject("textures");
                    if (javaTexturesProperty == null) {
                        options.logger().accept(String.format("Model file (%s) has no textures property.", modelFile));
                        continue;
                    }

                    Map<String, String> javaTextures = new HashMap<>();

                    for (Map.Entry<String, JsonElement> texture : javaTexturesProperty.entrySet()) {
                        if (!texture.getValue().isJsonPrimitive() && !texture.getValue().getAsJsonPrimitive().isString()) continue;
                        String javaTexturePath = texture.getValue().getAsJsonPrimitive().getAsString();

                        String javaTextureFile = Util.resolveNamespace(javaTexturePath, "textures") + ".png";
                        javaTextures.put(texture.getKey(), javaTextureFile);
                    }

                    if (model.has("elements")) {
                        Map<String, byte[]> textures = new HashMap<>();

                        for (Map.Entry<String, String> texture : javaTextures.entrySet()) {
                            textures.put(texture.getKey(), javaResourcePack.getFile(texture.getValue()));
                        }

                        BedrockGeometry geometry = GeometryUtils.convert3dModel(formattedName, model, textures, true, options.logger());
                        String bedrockTexturePath = "textures/items/custom/3d/" + formattedName + ".png";
                        bedrockResourcePack.setFile("models/blocks/" + formattedName + ".geo.json", gson.toJson(geometry, BedrockGeometry.class).getBytes(StandardCharsets.UTF_8));

                        JsonObject animations = GeometryUtils.generateAnimations(model, formattedName);
                        JsonObject attachable = GeometryUtils.generateAttachable("geysermc:zzzzz" + formattedName, formattedName, bedrockTexturePath, geometry.getGeometry()[0].getDescription().getIdentifier());

                        String bedrockAnimationsPath = "animations/" + formattedName + ".animation.json";
                        String bedrockAttachablePath = "attachables/" + formattedName + ".json";

                        bedrockResourcePack.setFile(bedrockTexturePath, geometry.getTexture());
                        bedrockResourcePack.setFile(bedrockAnimationsPath, gson.toJson(animations).getBytes(StandardCharsets.UTF_8));
                        bedrockResourcePack.setFile(bedrockAttachablePath, gson.toJson(attachable).getBytes(StandardCharsets.UTF_8));

                        results.getBlockTextures().put(formattedName, bedrockTexturePath);

                        if (!results.getGeneratedMappings().getItemMappings3d().containsKey(javaItemName)) results.getGeneratedMappings().getItemMappings3d().put(javaItemName, new ArrayList<>());
                        results.getGeneratedMappings().getItemMappings3d().get(javaItemName).add(new CustomModelDataMapping(customModelDataPrimitive.getAsInt(), formattedName));
                    } else {
                        if (!(javaTextures.containsKey("layer0"))) {
                            options.logger().accept(String.format("%s textures has no layer0 property, this converter only supports layer0 atm.", modelFile));
                            continue;
                        }

                        byte[] javaTexture = javaResourcePack.getFile(javaTextures.get("layer0"));
                        if (javaTexture == null) {
                            options.logger().accept(String.format("Texture %s referenced in %s not found.", javaTextures.get("layer0"), modelFile));
                            continue;
                        }
                        String bedrockTexturePath = "textures/items/custom/" + javaTextures.get("layer0").replace(":", "_") + ".png";
                        bedrockResourcePack.setFile(bedrockTexturePath, javaTexture);
                        try {
                            results.getTextureDimensions().put(formattedName, Util.getImageDimensions(javaTexture));
                        } catch (IOException e) {
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            options.logger().accept(String.format("Could not get height and width of %s, an IOException has occurred, here is the stack trace: \n%s", formattedName, sw));
                        }
                        results.getItemTextures().put(formattedName, bedrockTexturePath);


                        if (!results.getGeneratedMappings().getItemMappings2d().containsKey(javaItemName)) results.getGeneratedMappings().getItemMappings2d().put(javaItemName, new ArrayList<>());
                        results.getGeneratedMappings().getItemMappings2d().get(javaItemName).add(new CustomModelDataMapping(customModelDataPrimitive.getAsInt(), formattedName));
                    }
                    converted.add(formattedName);
                    options.logger().accept(String.format("Successfully converted %s", modelFile));
                }
                options.logger().accept(String.format("Mapped %s to %s", javaItemName + ":" + customModelDataPrimitive.getAsInt(), formattedName));
            }
        }
    }
}
