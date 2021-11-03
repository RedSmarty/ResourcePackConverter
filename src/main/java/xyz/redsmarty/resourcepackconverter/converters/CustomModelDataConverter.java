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
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.BedrockGeometry;
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.Bone;
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.Cube;
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.Description;
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.Face;
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.Geometry;
import xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock.Uv;
import xyz.redsmarty.resourcepackconverter.utils.mappings.serialization.CustomModelDataMapping;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

public class CustomModelDataConverter implements AbstractConverter {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final DecimalFormat df = new DecimalFormat("#.####");

    private final Map<String, List<CustomModelDataMapping>> mappings = new HashMap<>();
    private final Set<String> converted = new HashSet<>();
    private final Map<String, String> itemTextures = new TreeMap<>();
    private final Map<String, int[]> textureDimensions = new HashMap<>();

    //Used to store the number of bones converted, this is essential as bones must have names in bedrock to indicate their parent, it resets everytime a 3d model is converted
    private int bonesConverted = 0;

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
        // loops through the model files
        for (Map.Entry<String, byte[]> entry : javaResourcePack.getFilesExact("assets/minecraft/models/item/").entrySet()) {
            if (!entry.getKey().endsWith(".json")) continue;
            String[] fileName = entry.getKey().split("/");
            options.getLogger().accept(String.format("Converting %s", entry.getKey()));
            prepareMapping(javaResourcePack, bedrockResourcePack, fileName[fileName.length - 1], Util.bytesToString(entry.getValue()), options.getLogger());
        }

        // Generated item_texture.json file
        options.getLogger().accept("Generating item_texture.json");
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
        options.getLogger().accept("Generating CustomModelData mappings.");
        JsonObject mappingsRoot = new JsonObject();
        JsonObject mappingsData = new JsonObject();
        JsonObject textureDimensions = new JsonObject();

        for (Map.Entry<String, int[]> entry : this.textureDimensions.entrySet()) {
            JsonArray dimensions = new JsonArray();
            dimensions.add(entry.getValue()[0]);
            dimensions.add(entry.getValue()[1]);
            textureDimensions.add(entry.getKey(), dimensions);
        }

        for (Map.Entry<String, List<CustomModelDataMapping>> entry : mappings.entrySet()) {
            JsonObject perItemMappings = new JsonObject();
            for (CustomModelDataMapping customModelDataMapping : entry.getValue()) {
                perItemMappings.addProperty(String.valueOf(customModelDataMapping.getCustomModelData()), customModelDataMapping.getBedrockItem());
            }
            mappingsData.add(entry.getKey(), perItemMappings);
        }

        mappingsRoot.addProperty("hash", options.getJavaHash());
        mappingsRoot.addProperty("converted_pack_uuid", bedrockResourcePack.getUUID());
        mappingsRoot.add("texture_dimensions", textureDimensions);
        mappingsRoot.add("mappings_data", mappingsData);
        results.setGeneratedCustomModelDataMappings(gson.toJson(mappingsRoot));
    }

    private void prepareMapping(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, String fileName, String json, Consumer<String> logger) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray overrides = root.getAsJsonArray("overrides");
        if (overrides == null) {
            logger.accept(String.format("%s contains no overrides, does not have custom model data.", fileName));
            return;
        }
        for (JsonElement overrideElement : overrides) {
            String javaItemName = "minecraft:" + fileName.replace(".json", "");

            JsonObject override = overrideElement.getAsJsonObject();
            JsonObject predicate = override.getAsJsonObject("predicate");
            JsonPrimitive customModelDataPrimitive = predicate.getAsJsonPrimitive("custom_model_data");
            if (customModelDataPrimitive == null) {
                logger.accept(String.format("No custom model data found in predicate of %s.", fileName));
                continue;
            }
            String modelPath = override.getAsJsonPrimitive("model").getAsString();
            String[] splitModelName = modelPath.split("/");
            String modelName = splitModelName[splitModelName.length - 1];
            String namespace = modelPath.split(":")[0].equals(modelPath) ? "minecraft" : modelPath.split(":")[0];
            String modelFile = "assets/" + namespace + "/models/" + modelPath.replace(namespace + ":", "") + ".json";
            String formattedName = Util.formatModelName(modelName);

            if (!converted.contains(formattedName)) {
                byte[] modelStream = javaResourcePack.getFile(modelFile);
                if (modelStream == null) {
                    logger.accept(String.format("Resource pack does not have custom model file (%s) specified in %s.", modelFile,fileName));
                    continue;
                }
                JsonObject model = JsonParser.parseString(Util.bytesToString(modelStream)).getAsJsonObject();

                JsonObject javaTexturesProperty = model.getAsJsonObject("textures");
                if (javaTexturesProperty == null) {
                    logger.accept(String.format("Model file (%s) has no textures property.", modelFile));
                    continue;
                }

                Map<String, String> javaTextures = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : javaTexturesProperty.entrySet()) {
                    if (!entry.getValue().isJsonPrimitive() && !entry.getValue().getAsJsonPrimitive().isString()) continue;
                    String javaTexturePath = entry.getValue().getAsJsonPrimitive().getAsString();

                    String textureNamespace = javaTexturePath.split(":")[0].equals(javaTexturePath) ? "minecraft" : javaTexturePath.split(":")[0];
                    String javaTextureFile = "assets/" + textureNamespace + "/textures/" + javaTexturePath.replace(namespace + ":", "") + ".png";
                    javaTextures.put(entry.getKey(), javaTextureFile);
                }

                if (model.has("elements")) {
                    Map<String, byte[]> textures = new HashMap<>();

                    for (Map.Entry<String, String> entry : javaTextures.entrySet()) {
                        textures.put(entry.getKey(), javaResourcePack.getFile(entry.getValue()));
                    }

                    BedrockGeometry geometry = convert3dModel(modelName, model, textures,logger);
                    bedrockResourcePack.setFile("models/blocks/" + formattedName + ".geo.json", gson.toJson(geometry, BedrockGeometry.class).getBytes(StandardCharsets.UTF_8));
                    bedrockResourcePack.setFile("textures/items/custom/3d/" + formattedName + ".png", geometry.getTexture());
                    continue;
                }

                if (!(javaTextures.containsKey("layer0"))) {
                    logger.accept(String.format("%s textures has no layer0 property, this converter only supports layer0 atm.", modelFile));
                    continue;
                }

                byte[] javaTexture = javaResourcePack.getFile(javaTextures.get("layer0"));
                if (javaTexture == null) {
                    logger.accept(String.format("Texture %s referenced in %s not found.", javaTextures.get("layer0"), modelFile));
                    continue;
                }
                String bedrockTexturePath = "textures/items/custom/" + javaTextures.get("layer0").replace(":", "_") + ".png";
                bedrockResourcePack.setFile(bedrockTexturePath, javaTexture);
                try {
                    textureDimensions.put(formattedName, Util.getImageDimensions(javaTexture));
                } catch (IOException e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logger.accept(String.format("Could not get height and width of %s, an IOException has occurred, here is the stack trace: \n%s", formattedName, sw));
                }
                itemTextures.put(formattedName, bedrockTexturePath);
                converted.add(formattedName);
                logger.accept(String.format("Successfully converted %s", modelFile));
            }
            if (!mappings.containsKey(javaItemName)) mappings.put(javaItemName, new ArrayList<>());
            mappings.get(javaItemName).add(new CustomModelDataMapping(customModelDataPrimitive.getAsInt(), formattedName));
            logger.accept(String.format("Mapped %s to %s", javaItemName + ":" + customModelDataPrimitive.getAsInt(), formattedName));
        }
    }

    private BedrockGeometry convert3dModel(String itemName, JsonObject model, Map<String, byte[]> texturesData, Consumer<String> logger) {
        // Get the elements and group properties
        JsonArray elements = model.getAsJsonArray("elements");
        JsonArray groups = model.has("groups") ? model.getAsJsonArray("groups") : new JsonArray();

        // Key is the texture name
        Map<String, BufferedImage> textures = new HashMap<>();
        Map<String, Integer> uvSizes = new HashMap<>();
        Map<String, Integer> offsets = new HashMap<>();

        // If no group is found then create a group with all elements as children
        if (groups.isEmpty()) {
            JsonArray children = new JsonArray();
            for (int i = 0; i < elements.size(); i++) {
                children.add(i);
            }
            JsonObject bone = new JsonObject();
            bone.addProperty("name", "bone");
            JsonArray origin = new JsonArray();
            origin.add(0d);
            origin.add(0d);
            origin.add(0d);
            bone.add("origin", origin);
            bone.addProperty("color", 0);
            bone.add("children", children);
            groups.add(bone);
        }

        // Dimensions of the stitched texture
        int jointWidth = 0;
        int jointHeight = 0;

        for (Map.Entry<String, byte[]> data : texturesData.entrySet()) {
            try {
                textures.put(data.getKey(), ImageIO.read(new ByteArrayInputStream(data.getValue())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (BufferedImage texture : textures.values()) {
            jointWidth += texture.getWidth();
            jointHeight = Math.max(jointHeight, texture.getHeight());
        }

        BufferedImage jointTexture = new BufferedImage(jointWidth, jointHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D jointTextureGraphics = jointTexture.createGraphics();
        jointTextureGraphics.setBackground(new Color(0 , 0, 0, 0));

        int totalOffset = 0;

        // Textures are stitched together in a horizontal pattern
        for (Map.Entry<String, BufferedImage> entry : textures.entrySet()) {
            jointTextureGraphics.drawImage(entry.getValue(), null, totalOffset, 0);
            int uvSize = (Math.min(entry.getValue().getWidth(), entry.getValue().getHeight()))/16;
            uvSizes.put(entry.getKey(), uvSize);
            totalOffset += entry.getValue().getWidth();
            offsets.put(entry.getKey(), totalOffset - entry.getValue().getWidth());
        }

        List<Bone> bones = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            JsonObject group = groups.get(i).getAsJsonObject();
            List<Bone> convertedBones = convert3dGroup(itemName, "bone" + bonesConverted, null, null, group, elements, uvSizes, offsets, logger);
            if (convertedBones == null) {
                return null;
            }
            bones.addAll(convertedBones);
        }

        ByteArrayOutputStream textureInBytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(jointTexture, "png", textureInBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new BedrockGeometry(new Geometry(
                Description.builder()
                        .setIdentifier("geometry." + itemName)
                        .setTextureWidth(jointWidth)
                        .setTextureHeight(jointHeight).build(),
                bones
        ), textureInBytes.toByteArray());
    }

    private List<Bone> convert3dGroup(String itemName, String boneName, String binding, String parent,JsonObject group, JsonArray elements, Map<String, Integer> uvSizes, Map<String ,Integer> offsets, Consumer<String> logger) {
        List<Bone> bones = new ArrayList<>();
        String name = group.has("name") ? group.getAsJsonPrimitive("name").getAsString() : boneName;
        double[] pivot = new double[3];
        int color = group.has("color") ? group.getAsJsonPrimitive("color").getAsInt() : 0;
        List<Cube> cubes = new ArrayList<>();

        JsonArray children = group.getAsJsonArray("children");

        if (children == null) {
            logger.accept(String.format("%s's group %s has no children, cannot convert model.", itemName, name));
            return null;
        }

        JsonArray originProperty = group.has("origin") && group.get("origin").isJsonArray() ? group.getAsJsonArray("origin") : null;

        // If pivot is not found, initialize it with 0, 0, 0
        for (int j = 0; j < pivot.length; j++) {
            pivot[j] = originProperty == null ? 0 : (originProperty.size() > j ? originProperty.get(j).getAsDouble(): 0);
        }

        for (JsonElement child : children) {
            if (child.isJsonPrimitive()) {
                cubes.add(convert3dElement(elements.get(child.getAsJsonPrimitive().getAsInt()).getAsJsonObject() , uvSizes, offsets, itemName, child.getAsJsonPrimitive().getAsInt(), logger));
            } else if (child.isJsonObject()) {
                List<Bone> convertedBones = convert3dGroup(itemName, "bone" + bonesConverted, null,name, child.getAsJsonObject(), elements, uvSizes, offsets,logger);
                if (convertedBones == null) {
                    return null;
                }
                bones.addAll(convertedBones);
                bonesConverted++;
            }
        }

        bones.add(Bone.builder()
                .setName(name)
                .setBinding(binding)
                .setParent(parent)
                .setPivot(pivot)
                .setCubes(cubes)
                .build()
        );

        return bones;
    }

    public Cube convert3dElement(JsonObject element, Map<String, Integer> uvSizes, Map<String ,Integer> offsets,String itemName, int elementIndex, Consumer<String> logger) {
        JsonObject rotationProperty = element.getAsJsonObject("rotation");
        double[] origin = new double[3];
        double[] size = new double[3];
        double[] pivot = null;
        double[] rotation = null;
        Uv.Builder uv = Uv.builder();

        JsonArray fromProperty = element.has("from") && element.get("from").isJsonArray() ? element.getAsJsonArray("from") : null;
        if (fromProperty == null) {
            logger.accept(String.format("%s's element of index %d has no from property, cannot convert model", itemName, elementIndex));
            return null;
        }
        for (int i = 0; i < origin.length; i++) {
            origin[i] = fromProperty.get(i).getAsDouble();
        }

        JsonArray toProperty = element.has("to") && element.get("to").isJsonArray() ? element.getAsJsonArray("to") : null;
        if (toProperty == null) {
            logger.accept(String.format("%s's element of index %d has no to property, cannot convert model", itemName, elementIndex));
            return null;
        }
        for (int i = 0; i < size.length; i++) {
            size[i] = Double.parseDouble(df.format((toProperty.get(i).getAsDouble()) - origin[i]));
        }

        if (rotationProperty != null) {
            JsonArray pivotProperty = rotationProperty.getAsJsonArray("origin");
            pivot = new double[3];
            for (int i = 0; i < pivot.length; i++) {
                pivot[i] = pivotProperty.get(i).getAsDouble();
            }
            double rotationAngle = rotationProperty.getAsJsonPrimitive("angle").getAsDouble();
            char axis = rotationProperty.getAsJsonPrimitive("axis").getAsCharacter();
            rotation = new double[3];

            switch (axis) {
                case 'x' -> rotation[0] = rotationAngle;
                case 'y' -> rotation[1] = rotationAngle;
                case 'z' -> rotation[2] = rotationAngle;
            }
        }

        // Bedrock is weird, we have to add 8 to x axis and subtract 8 from z axis
        origin[0] += -8;
        origin[2] -= 8;
        if (pivot != null) {
            pivot[0] += -8;
            pivot[2] -= 8;
        }

        for (Map.Entry<String, JsonElement> entry: element.getAsJsonObject("faces").entrySet()) {
            String texture = entry.getValue().getAsJsonObject().getAsJsonPrimitive("texture").getAsString().replace("#", "");
            JsonArray uvsProperty = entry.getValue().getAsJsonObject().getAsJsonArray("uv");
            double[] uvs = new double[2];
            double[] bedrockUvSizes = new double[2];

            uvs[0] = offsets.get(texture) + uvsProperty.get(0).getAsJsonPrimitive().getAsDouble();
            uvs[1] = uvsProperty.get(1).getAsJsonPrimitive().getAsDouble();
            bedrockUvSizes[0] = uvs[0] - (offsets.get(texture) + uvsProperty.get(2).getAsJsonPrimitive().getAsDouble());
            bedrockUvSizes[1] = uvs[1] - (uvsProperty.get(3).getAsJsonPrimitive().getAsDouble());

            switch (entry.getKey()) {
                case "north" -> uv.setNorth(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "south" -> uv.setSouth(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "east" -> uv.setEast(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "west" -> uv.setWest(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "up" -> uv.setUp(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "down" -> uv.setDown(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
            }
        }

        return Cube.builder()
                .setOrigin(origin)
                .setSize(size)
                .setPivot(pivot)
                .setRotation(rotation)
                .setUv(uv.build())
                .build();
    }
}
