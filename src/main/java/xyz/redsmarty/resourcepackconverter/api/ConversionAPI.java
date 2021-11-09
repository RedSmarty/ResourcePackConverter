package xyz.redsmarty.resourcepackconverter.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import xyz.redsmarty.resourcepackconverter.converters.AbstractConverter;
import xyz.redsmarty.resourcepackconverter.converters.ConverterList;
import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionResults;
import xyz.redsmarty.resourcepackconverter.utils.type.mappings.BlockMapping;
import xyz.redsmarty.resourcepackconverter.utils.type.mappings.CustomModelDataMapping;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

public class ConversionAPI {
    public static Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static ConversionAPI instance;

    public static ConversionAPI getInstance() {
        if (instance == null) {
            instance = new ConversionAPI();
        }
        return instance;
    }

    public ConversionResults convert(InputStream javaStream, File bedrockFile, ConversionOptions options) throws IOException, InvalidResourcePackException {
        JavaResourcePack javaResourcePack = new JavaResourcePack(new ZipInputStream(javaStream), options.logger());
        BedrockResourcePack bedrockResourcePack = new BedrockResourcePack(options.name(), options.version(), javaResourcePack.getDescription(), javaResourcePack.getFile("pack.png"));

        ConversionResults results = new ConversionResults();

        for (AbstractConverter converter : ConverterList.getConverters()) {
            converter.convert(javaResourcePack, bedrockResourcePack, options, results);
        }
        results.setSuccess(true);

        // Generated item_texture.json file
        options.logger().accept("Generating item_texture.json");
        JsonObject itemTextures = new JsonObject();
        itemTextures.addProperty("resource_pack_name", bedrockResourcePack.getName());
        itemTextures.addProperty("texture_name", "atlas.items");
        JsonObject itemTextureData = new JsonObject();
        for (Map.Entry<String, String> entry : results.getItemTextures().entrySet()) {
            JsonObject itemTextureObject = new JsonObject();
            JsonArray textures = new JsonArray();
            textures.add(entry.getValue());
            itemTextureObject.add("textures", textures);
            itemTextureData.add(entry.getKey(), itemTextureObject);
        }
        itemTextures.add("texture_data", itemTextureData);
        bedrockResourcePack.setFile("textures/item_texture.json", GSON.toJson(itemTextures).getBytes(StandardCharsets.UTF_8));

        // Generated terrain_texture.json file
        options.logger().accept("Generating terrain_texture.json");
        JsonObject blockTextures = new JsonObject();
        blockTextures.addProperty("resource_pack_name", bedrockResourcePack.getName());
        blockTextures.addProperty("texture_name", "atlas.terrain");
        blockTextures.addProperty("padding", 8);
        blockTextures.addProperty("num_mip_levels", 4);
        JsonObject blockTextureData = new JsonObject();
        for (Map.Entry<String, String> entry : results.getBlockTextures().entrySet()) {
            JsonObject blockTexturesObject = new JsonObject();
            JsonArray textures = new JsonArray();
            textures.add(entry.getValue());
            blockTexturesObject.add("textures", textures);
            blockTextureData.add(entry.getKey(), blockTexturesObject);
        }
        blockTextures.add("texture_data", blockTextureData);
        bedrockResourcePack.setFile("textures/terrain_texture.json", GSON.toJson(blockTextures).getBytes(StandardCharsets.UTF_8));

        // Package the bedrock resource pack
        bedrockResourcePack.zipIt(bedrockFile);

        // Generates CustomModelData mappings
        options.logger().accept("Generating CustomModelData mappings.");
        JsonObject mappingsRoot = new JsonObject();
        JsonObject mappingsData = new JsonObject();
        JsonObject textureDimensions = new JsonObject();

        for (Map.Entry<String, int[]> entry : results.getTextureDimensions().entrySet()) {
            JsonArray dimensions = new JsonArray();
            dimensions.add(entry.getValue()[0]);
            dimensions.add(entry.getValue()[1]);
            textureDimensions.add(entry.getKey(), dimensions);
        }

        JsonObject mappingsDataItems = new JsonObject();
        JsonObject mappingsDataItems2d = new JsonObject();
        JsonObject mappingsDataItems3d = new JsonObject();

        JsonObject mappingsDataBlocks = new JsonObject();

        for (Map.Entry<String, List<CustomModelDataMapping>> entry : results.getGeneratedMappings().getItemMappings2d().entrySet()) {
            JsonObject perItemMappings = new JsonObject();
            for (CustomModelDataMapping customModelDataMapping : entry.getValue()) {
                perItemMappings.addProperty(String.valueOf(customModelDataMapping.customModelData()), customModelDataMapping.bedrockItem());
            }
            mappingsDataItems2d.add(entry.getKey(), perItemMappings);
        }

        for (Map.Entry<String, List<CustomModelDataMapping>> entry : results.getGeneratedMappings().getItemMappings3d().entrySet()) {
            JsonObject perItemMappings = new JsonObject();
            for (CustomModelDataMapping customModelDataMapping : entry.getValue()) {
                perItemMappings.addProperty(String.valueOf(customModelDataMapping.customModelData()), customModelDataMapping.bedrockItem());
            }
            mappingsDataItems3d.add(entry.getKey(), perItemMappings);
        }

        mappingsDataItems.add("2d", mappingsDataItems2d);
        mappingsDataItems.add("3d", mappingsDataItems3d);

        for (BlockMapping mapping : results.getGeneratedMappings().getBlockMappings()) {
            JsonObject perBlockMapping = new JsonObject();
            perBlockMapping.addProperty("texture", mapping.bedrockName());
            perBlockMapping.addProperty("custom_geometry", mapping.hasCustomGeometry());
            mappingsDataBlocks.add(mapping.blockState(), perBlockMapping);
        }

        mappingsData.add("items", mappingsDataItems);
        mappingsData.add("blocks", mappingsDataBlocks);

        mappingsRoot.addProperty("hash", options.javaHash());
        mappingsRoot.addProperty("converted_pack_uuid", bedrockResourcePack.getUUID());
        mappingsRoot.addProperty("namespace", options.namespace());
        mappingsRoot.add("texture_dimensions", textureDimensions);
        mappingsRoot.add("mappings_data", mappingsData);

        results.getGeneratedMappings().setJson(GSON.toJson(mappingsRoot));
        return results;
    }

}
