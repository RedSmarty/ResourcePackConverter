package xyz.redsmarty.resourcepackconverter.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.BedrockGeometry;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.Bone;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.Cube;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.Description;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.Face;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.Geometry;
import xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry.Uv;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GeometryUtils {
    private static final DecimalFormat df = new DecimalFormat("#.####");
    //Used to store the number of bones converted, this is essential as bones must have names in bedrock to indicate their parent, it resets everytime a 3d model is converted
    private static int bonesConverted = 0;
    public static BedrockGeometry convert3dModel(String itemName, JsonObject model, Map<String, byte[]> texturesData, boolean addExtraBones,Consumer<String> logger) {
        // Get the elements and group properties
        JsonArray elements = model.getAsJsonArray("elements");
        JsonArray groups = model.has("groups") ? model.getAsJsonArray("groups") : new JsonArray();

        // Key is the texture name
        Map<String, BufferedImage> textures = new HashMap<>();
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
            totalOffset += entry.getValue().getWidth();
            offsets.put(entry.getKey(), (totalOffset - entry.getValue().getWidth())/uvSize);
//            offsets.put(entry.getKey(), loadedTextures * 16);
//            loadedTextures++;
        }

        List<Bone> bones = new ArrayList<>();

        if (addExtraBones) {
            bones.add(Bone.builder().setName("root").setPivot(new double[]{0, 8, 0}).setBinding("c.item_slot == 'head' ? 'head' : q.item_slot_to_bone_name(c.item_slot)").build());
            bones.add(Bone.builder().setName("root_x").setParent("root").setPivot(new double[]{0, 8, 0}).build());
            bones.add(Bone.builder().setName("root_y").setParent("root_x").setPivot(new double[]{0, 8, 0}).build());
            bones.add(Bone.builder().setName("root_z").setParent("root_y").setPivot(new double[]{0, 8, 0}).build());
        }

        for (int i = 0; i < groups.size(); i++) {
            JsonObject group = groups.get(i).getAsJsonObject();
            List<Bone> convertedBones = convert3dGroup(itemName, "bone" + bonesConverted, null, addExtraBones ? "root_z" : null, group, elements, offsets, logger);
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
                        .setTextureWidth(16)
                        .setTextureHeight(16).build(),
                bones
        ), textureInBytes.toByteArray());
    }

    private static List<Bone> convert3dGroup(String itemName, String boneName, String binding, String parent, JsonObject group, JsonArray elements, Map<String ,Integer> offsets, Consumer<String> logger) {
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
                cubes.add(convert3dElement(elements.get(child.getAsJsonPrimitive().getAsInt()).getAsJsonObject(), offsets, itemName, child.getAsJsonPrimitive().getAsInt(), logger));
            } else if (child.isJsonObject()) {
                List<Bone> convertedBones = convert3dGroup(itemName, "bone" + bonesConverted, null,name, child.getAsJsonObject(), elements, offsets, logger);
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

    private static Cube convert3dElement(JsonObject element, Map<String ,Integer> offsets,String itemName, int elementIndex, Consumer<String> logger) {
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

        // Bedrock is weird, we have to do this stuff, which I'm not sure why, but we have to (Thanks kastle)
        origin[0] = (-1 * toProperty.get(0).getAsDouble()) + 8;
        origin[2] -= 8;

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
                case 'x' -> rotation[0] = rotationAngle * -1;
                case 'y' -> rotation[1] = rotationAngle * -1;
                case 'z' -> rotation[2] = rotationAngle;
            }
            // Bedrock is weird, we have to do this stuff, which I'm not sure why, but we have to (Thanks kastle)
            pivot[0] = -1 * pivotProperty.get(0).getAsDouble() + 8;
            pivot[2] -= 8;
        }

        for (Map.Entry<String, JsonElement> entry: element.getAsJsonObject("faces").entrySet()) {
            String texture = entry.getValue().getAsJsonObject().getAsJsonPrimitive("texture").getAsString().replace("#", "");
            JsonArray uvsProperty = entry.getValue().getAsJsonObject().getAsJsonArray("uv");
            double[] uvs = new double[2];
            double[] bedrockUvSizes = new double[2];

            uvs[0] = offsets.get(texture) + uvsProperty.get(0).getAsJsonPrimitive().getAsDouble();
            uvs[1] = uvsProperty.get(1).getAsJsonPrimitive().getAsDouble();
            bedrockUvSizes[0] = uvsProperty.get(2).getAsJsonPrimitive().getAsDouble() - uvs[0];
            bedrockUvSizes[1] = uvsProperty.get(3).getAsJsonPrimitive().getAsDouble() - uvs[1];

            switch (entry.getKey()) {
                case "north" -> uv.setNorth(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "south" -> uv.setSouth(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "east" -> uv.setEast(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "west" -> uv.setWest(Face.builder().setUv(uvs).setUvSize(bedrockUvSizes).build());
                case "up" -> uv.setUp(Face.builder().setUv(new double[] {uvsProperty.get(2).getAsDouble(), uvsProperty.get(3).getAsDouble()}).setUvSize(new double[] {uvsProperty.get(0).getAsDouble() - uvsProperty.get(2).getAsDouble(), uvsProperty.get(1).getAsDouble() - uvsProperty.get(3).getAsDouble()}).build());
                case "down" -> uv.setDown(Face.builder().setUv(new double[] {uvsProperty.get(2).getAsDouble(), uvsProperty.get(3).getAsDouble()}).setUvSize(new double[] {uvsProperty.get(0).getAsDouble() - uvsProperty.get(2).getAsDouble(), uvsProperty.get(1).getAsDouble() - uvsProperty.get(3).getAsDouble()}).build());
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

    public static JsonObject generateAttachable(String identifier, String itemName, String defaultTexture, String defaultGeometry) {
        JsonObject root = new JsonObject();
        JsonObject attachable = new JsonObject();
        JsonObject description = new JsonObject();

        description.addProperty("identifier", identifier);

        JsonObject materials = new JsonObject();
        materials.addProperty("default", "entity_alphatest");
        materials.addProperty("enchanted", "entity_alphatest_glint");

        JsonObject textures = new JsonObject();
        textures.addProperty("default", defaultTexture);
        textures.addProperty("enchanted", "textures/misc/enchanted_item_glint");

        JsonObject geometry = new JsonObject();
        geometry.addProperty("default", defaultGeometry);

        JsonObject scripts = new JsonObject();

        JsonArray preAnimation = new JsonArray();
        preAnimation.add("v.main_hand = c.item_slot == 'main_hand';");
        preAnimation.add("v.off_hand = c.item_slot == 'off_hand';");
        preAnimation.add("v.head = c.item_slot == 'head';");

        JsonArray animate = new JsonArray();

        JsonObject thirdPersonMainHand = new JsonObject();
        thirdPersonMainHand.addProperty("thirdperson_main_hand", "v.main_hand && !c.is_first_person");
        animate.add(thirdPersonMainHand);

        JsonObject thirdPersonOffHand = new JsonObject();
        thirdPersonOffHand.addProperty("thirdperson_off_hand", "v.off_hand && !c.is_first_person");
        animate.add(thirdPersonOffHand);

        JsonObject thirdPersonHead = new JsonObject();
        thirdPersonHead.addProperty("thirdperson_head", "v.head && !c.is_first_person");
        animate.add(thirdPersonHead);

        JsonObject firstPersonMainHand = new JsonObject();
        firstPersonMainHand.addProperty("firstperson_main_hand", "v.main_hand && c.is_first_person");
        animate.add(firstPersonMainHand);

        JsonObject firstPersonOffHand = new JsonObject();
        firstPersonOffHand.addProperty("firstperson_off_hand", "v.off_hand && c.is_first_person");
        animate.add(firstPersonOffHand);

        JsonObject firstPersonHead = new JsonObject();
        firstPersonHead.addProperty("firstperson_head", "v.head && c.is_first_person");
        animate.add(firstPersonHead);

        scripts.add("pre_animation", preAnimation);
        scripts.add("animate", animate);

        JsonObject animations = new JsonObject();
        animations.addProperty("thirdperson_main_hand", "animation." + itemName + ".thirdperson_main_hand");
        animations.addProperty("thirdperson_off_hand", "animation." + itemName + ".thirdperson_off_hand");
        animations.addProperty("thirdperson_head", "animation." + itemName + ".head");
        animations.addProperty("firstperson_main_hand", "animation." + itemName + ".firstperson_main_hand");
        animations.addProperty("firstperson_off_hand", "animation." + itemName + ".firstperson_off_hand");
        animations.addProperty("firstperson_head", "animation.disable");

        JsonArray renderControllers = new JsonArray();
        renderControllers.add("controller.render.item_default");

        description.addProperty("identifier", identifier);
        description.add("materials", materials);
        description.add("textures", textures);
        description.add("geometry", geometry);
        description.add("scripts", scripts);
        description.add("animations", animations);
        description.add("render_controllers", renderControllers);

        attachable.add("description", description);
        root.addProperty("format_version", "1.10.0");
        root.add("minecraft:attachable", attachable);

        return root;
    }

    public static JsonObject generateAnimations(JsonObject model, String itemName) {
        JsonObject displayProperty = model.getAsJsonObject("display");

        JsonObject root = new JsonObject();
        JsonObject animations = new JsonObject();

        JsonObject thirdPersonMainHand = new JsonObject();
        JsonObject thirdPersonOffHand = new JsonObject();
        JsonObject thirdPersonHead = new JsonObject();
        JsonObject firstPersonMainHand = new JsonObject();
        JsonObject firstPersonOffHand = new JsonObject();

        // Bones of all the animations
        JsonObject thirdPersonMainHandBones = new JsonObject();
        JsonObject thirdPersonOffHandBones = new JsonObject();
        JsonObject thirdPersonHeadBones = new JsonObject();
        JsonObject firstPersonMainHandBones = new JsonObject();
        JsonObject firstPersonOffHandBones = new JsonObject();

        // Add display properties of root bone and set loop to true
        // Third person main and off hand
        {
            JsonObject rootAnimation = new JsonObject();

            JsonArray rotation = new JsonArray();
            JsonArray position = new JsonArray();

            rotation.add(90);
            rotation.add(0);
            rotation.add(0);

            position.add(0);
            position.add(13);
            position.add(-3);
            rootAnimation.add("rotation", rotation);
            rootAnimation.add("position", position);

            thirdPersonMainHand.addProperty("loop", true);
            thirdPersonOffHand.addProperty("loop", true);

            thirdPersonMainHandBones.add("root", rootAnimation);
            thirdPersonOffHandBones.add("root", rootAnimation);
        }
        // Third person head
        {
            JsonObject rootAnimation = new JsonObject();

            JsonArray position = new JsonArray();
            position.add(0);
            position.add(19.5);
            position.add(0);

            rootAnimation.add("position", position);

            thirdPersonHead.addProperty("loop", true);

            thirdPersonHeadBones.add("root", rootAnimation);
        }
        // First person main and off hand
        {
            JsonObject rootAnimation = new JsonObject();

            JsonArray rotation = new JsonArray();
            rotation.add(90);
            rotation.add(60);
            rotation.add(-40);

            JsonArray position = new JsonArray();
            position.add(4);
            position.add(10);
            position.add(4);

            float scale = 1.5f;

            rootAnimation.add("position", position);
            rootAnimation.add("rotation", rotation);
            rootAnimation.addProperty("scale", scale);

            firstPersonMainHand.addProperty("loop", true);
            firstPersonOffHand.addProperty("loop", true);

            firstPersonMainHandBones.add("root", rootAnimation);
            firstPersonOffHandBones.add("root", rootAnimation);
        }

        // Add bones to animations
        if (displayProperty != null) {
            // Third person main hand
            {
                JsonObject javaThirdPersonMainHand = displayProperty.getAsJsonObject("thirdperson_righthand");

                JsonObject rootXAnimation = null;
                JsonObject rootYAnimation = null;
                JsonObject rootZAnimation = null;

                if (javaThirdPersonMainHand != null) {
                    rootXAnimation = new JsonObject();

                    JsonArray javaThirdPersonMainHandRotation = javaThirdPersonMainHand.getAsJsonArray("rotation");
                    JsonArray javaThirdPersonMainHandTranslation = javaThirdPersonMainHand.getAsJsonArray("translation ");
                    JsonArray javaThirdPersonMainHandScale = javaThirdPersonMainHand.getAsJsonArray("scale");

                    if (javaThirdPersonMainHandRotation != null) {
                        rootYAnimation = new JsonObject();
                        rootZAnimation = new JsonObject();

                        JsonArray rootXRotation = new JsonArray();
                        rootXRotation.add(javaThirdPersonMainHandRotation.get(0).getAsDouble() * -1);
                        rootXRotation.add(0);
                        rootXRotation.add(0);

                        JsonArray rootYRotation = new JsonArray();
                        rootYRotation.add(0);
                        rootYRotation.add(javaThirdPersonMainHandRotation.get(1).getAsDouble() * -1);
                        rootYRotation.add(0);

                        JsonArray rootZRotation = new JsonArray();
                        rootZRotation.add(0);
                        rootZRotation.add(0);
                        rootZRotation.add(javaThirdPersonMainHandRotation.get(2).getAsDouble() * -1);

                        rootXAnimation.add("rotation", rootXRotation);
                        rootYAnimation.add("rotation", rootYRotation);
                        rootZAnimation.add("rotation", rootZRotation);
                    }

                    if (javaThirdPersonMainHandTranslation != null) {
                        JsonArray rootXPosition = new JsonArray();
                        rootXPosition.add(javaThirdPersonMainHandTranslation.get(0).getAsDouble() * -1);
                        rootXPosition.add(javaThirdPersonMainHandTranslation.get(1).getAsDouble());
                        rootXPosition.add(javaThirdPersonMainHandTranslation.get(2).getAsDouble());

                        rootXAnimation.add("position", rootXPosition);
                    }

                    if (javaThirdPersonMainHandScale != null) {
                        JsonArray rootXScale = new JsonArray();
                        rootXScale.add(javaThirdPersonMainHandScale.get(0).getAsDouble());
                        rootXScale.add(javaThirdPersonMainHandScale.get(1).getAsDouble());
                        rootXScale.add(javaThirdPersonMainHandScale.get(2).getAsDouble());

                        rootXAnimation.add("scale", rootXScale);
                    }
                }

                thirdPersonMainHandBones.add("root_x", rootXAnimation);
                thirdPersonMainHandBones.add("root_y", rootYAnimation);
                thirdPersonMainHandBones.add("root_z", rootZAnimation);
            }
            // Third person off hand
            {
                JsonObject javaThirdPersonOffHand = displayProperty.getAsJsonObject("thirdperson_lefthand");

                JsonObject rootXAnimation = null;
                JsonObject rootYAnimation = null;
                JsonObject rootZAnimation = null;

                if (javaThirdPersonOffHand != null) {
                    rootXAnimation = new JsonObject();

                    JsonArray javaThirdPersonMainHandRotation = javaThirdPersonOffHand.getAsJsonArray("rotation");
                    JsonArray javaThirdPersonMainHandTranslation = javaThirdPersonOffHand.getAsJsonArray("translation ");
                    JsonArray javaThirdPersonMainHandScale = javaThirdPersonOffHand.getAsJsonArray("scale");

                    if (javaThirdPersonMainHandRotation != null) {
                        rootYAnimation = new JsonObject();
                        rootZAnimation = new JsonObject();

                        JsonArray rootXRotation = new JsonArray();
                        rootXRotation.add(javaThirdPersonMainHandRotation.get(0).getAsDouble() * -1);
                        rootXRotation.add(0);
                        rootXRotation.add(0);

                        JsonArray rootYRotation = new JsonArray();
                        rootYRotation.add(0);
                        rootYRotation.add(javaThirdPersonMainHandRotation.get(1).getAsDouble() * -1);
                        rootYRotation.add(0);

                        JsonArray rootZRotation = new JsonArray();
                        rootZRotation.add(0);
                        rootZRotation.add(0);
                        rootZRotation.add(javaThirdPersonMainHandRotation.get(2).getAsDouble() * -1);

                        rootXAnimation.add("rotation", rootXRotation);
                        rootYAnimation.add("rotation", rootYRotation);
                        rootZAnimation.add("rotation", rootZRotation);
                    }

                    if (javaThirdPersonMainHandTranslation != null) {
                        JsonArray rootXPosition = new JsonArray();
                        rootXPosition.add(javaThirdPersonMainHandTranslation.get(0).getAsDouble() * -1);
                        rootXPosition.add(javaThirdPersonMainHandTranslation.get(1).getAsDouble());
                        rootXPosition.add(javaThirdPersonMainHandTranslation.get(2).getAsDouble());

                        rootXAnimation.add("position", rootXPosition);
                    }

                    if (javaThirdPersonMainHandScale != null) {
                        JsonArray rootXScale = new JsonArray();
                        rootXScale.add(javaThirdPersonMainHandScale.get(0).getAsDouble());
                        rootXScale.add(javaThirdPersonMainHandScale.get(1).getAsDouble());
                        rootXScale.add(javaThirdPersonMainHandScale.get(2).getAsDouble());

                        rootXAnimation.add("scale", rootXScale);
                    }
                }

                thirdPersonOffHandBones.add("root_x", rootXAnimation);
                thirdPersonOffHandBones.add("root_y", rootYAnimation);
                thirdPersonOffHandBones.add("root_z", rootZAnimation);
            }
            // First person main hand
            {
                JsonObject javaFirstPersonMainHand = displayProperty.getAsJsonObject("firstperson_righthand");

                JsonObject rootXAnimation = null;
                JsonObject rootYAnimation = null;
                JsonObject rootZAnimation = null;

                if (javaFirstPersonMainHand != null) {
                    rootXAnimation = new JsonObject();

                    JsonArray javaFirstPersonMainHandRotation = javaFirstPersonMainHand.getAsJsonArray("rotation");
                    JsonArray javaFirstPersonMainHandTranslation = javaFirstPersonMainHand.getAsJsonArray("translation ");
                    JsonArray javaFirstPersonMainHandScale = javaFirstPersonMainHand.getAsJsonArray("scale");

                    if (javaFirstPersonMainHandRotation != null) {
                        rootYAnimation = new JsonObject();
                        rootZAnimation = new JsonObject();

                        JsonArray rootXRotation = new JsonArray();
                        rootXRotation.add(javaFirstPersonMainHandRotation.get(0).getAsDouble() * -1);
                        rootXRotation.add(0);
                        rootXRotation.add(0);

                        JsonArray rootYRotation = new JsonArray();
                        rootYRotation.add(0);
                        rootYRotation.add(javaFirstPersonMainHandRotation.get(1).getAsDouble() * -1);
                        rootYRotation.add(0);

                        JsonArray rootZRotation = new JsonArray();
                        rootZRotation.add(0);
                        rootZRotation.add(0);
                        rootZRotation.add(javaFirstPersonMainHandRotation.get(2).getAsDouble() * -1);

                        rootXAnimation.add("rotation", rootXRotation);
                        rootYAnimation.add("rotation", rootYRotation);
                        rootZAnimation.add("rotation", rootZRotation);
                    }

                    if (javaFirstPersonMainHandTranslation != null) {
                        JsonArray rootXPosition = new JsonArray();
                        rootXPosition.add(javaFirstPersonMainHandTranslation.get(0).getAsDouble() * -1);
                        rootXPosition.add(javaFirstPersonMainHandTranslation.get(1).getAsDouble());
                        rootXPosition.add(javaFirstPersonMainHandTranslation.get(2).getAsDouble());

                        rootXAnimation.add("position", rootXPosition);
                    }

                    if (javaFirstPersonMainHandScale != null) {
                        JsonArray rootXScale = new JsonArray();
                        rootXScale.add(javaFirstPersonMainHandScale.get(0).getAsDouble());
                        rootXScale.add(javaFirstPersonMainHandScale.get(1).getAsDouble());
                        rootXScale.add(javaFirstPersonMainHandScale.get(2).getAsDouble());

                        rootXAnimation.add("scale", rootXScale);
                    }
                }

                firstPersonMainHandBones.add("root_x", rootXAnimation);
                firstPersonMainHandBones.add("root_y", rootYAnimation);
                firstPersonMainHandBones.add("root_z", rootZAnimation);
            }
            // First person off hand
            {
                JsonObject javaFirstPersonOffHand = displayProperty.getAsJsonObject("firstperson_lefthand");

                JsonObject rootXAnimation = null;
                JsonObject rootYAnimation = null;
                JsonObject rootZAnimation = null;

                if (javaFirstPersonOffHand != null) {
                    rootXAnimation = new JsonObject();

                    JsonArray javaFirstPersonOffHandRotation = javaFirstPersonOffHand.getAsJsonArray("rotation");
                    JsonArray javaFirstPersonOffHandTranslation = javaFirstPersonOffHand.getAsJsonArray("translation ");
                    JsonArray javaFirstPersonOffHandScale = javaFirstPersonOffHand.getAsJsonArray("scale");

                    if (javaFirstPersonOffHandRotation != null) {
                        rootYAnimation = new JsonObject();
                        rootZAnimation = new JsonObject();

                        JsonArray rootXRotation = new JsonArray();
                        rootXRotation.add(javaFirstPersonOffHandRotation.get(0).getAsDouble() * -1);
                        rootXRotation.add(0);
                        rootXRotation.add(0);

                        JsonArray rootYRotation = new JsonArray();
                        rootYRotation.add(0);
                        rootYRotation.add(javaFirstPersonOffHandRotation.get(1).getAsDouble() * -1);
                        rootYRotation.add(0);

                        JsonArray rootZRotation = new JsonArray();
                        rootZRotation.add(0);
                        rootZRotation.add(0);
                        rootZRotation.add(javaFirstPersonOffHandRotation.get(2).getAsDouble() * -1);

                        rootXAnimation.add("rotation", rootXRotation);
                        rootYAnimation.add("rotation", rootYRotation);
                        rootZAnimation.add("rotation", rootZRotation);
                    }

                    if (javaFirstPersonOffHandTranslation != null) {
                        JsonArray rootXPosition = new JsonArray();
                        rootXPosition.add(javaFirstPersonOffHandTranslation.get(0).getAsDouble() * -1);
                        rootXPosition.add(javaFirstPersonOffHandTranslation.get(1).getAsDouble());
                        rootXPosition.add(javaFirstPersonOffHandTranslation.get(2).getAsDouble());

                        rootXAnimation.add("position", rootXPosition);
                    }

                    if (javaFirstPersonOffHandScale != null) {
                        JsonArray rootXScale = new JsonArray();
                        rootXScale.add(javaFirstPersonOffHandScale.get(0).getAsDouble());
                        rootXScale.add(javaFirstPersonOffHandScale.get(1).getAsDouble());
                        rootXScale.add(javaFirstPersonOffHandScale.get(2).getAsDouble());

                        rootXAnimation.add("scale", rootXScale);
                    }
                }

                firstPersonOffHandBones.add("root_x", rootXAnimation);
                firstPersonOffHandBones.add("root_y", rootYAnimation);
                firstPersonOffHandBones.add("root_z", rootZAnimation);
            }
            // Third person head
            {
                JsonObject javaHead = displayProperty.getAsJsonObject("head");

                JsonObject rootXAnimation = null;
                JsonObject rootYAnimation = null;
                JsonObject rootZAnimation = null;

                if (javaHead != null) {
                    rootXAnimation = new JsonObject();

                    JsonArray javaHeadRotation = javaHead.getAsJsonArray("rotation");
                    JsonArray javaHeadPosition = javaHead.getAsJsonArray("translation ");
                    JsonArray javaHeadScale = javaHead.getAsJsonArray("scale");

                    if (javaHeadRotation != null) {
                        rootYAnimation = new JsonObject();
                        rootZAnimation = new JsonObject();

                        JsonArray rootXRotation = new JsonArray();
                        rootXRotation.add(javaHeadRotation.get(0).getAsDouble() * -1);
                        rootXRotation.add(0);
                        rootXRotation.add(0);

                        JsonArray rootYRotation = new JsonArray();
                        rootYRotation.add(0);
                        rootYRotation.add(javaHeadRotation.get(1).getAsDouble() * -1);
                        rootYRotation.add(0);

                        JsonArray rootZRotation = new JsonArray();
                        rootZRotation.add(0);
                        rootZRotation.add(0);
                        rootZRotation.add(javaHeadRotation.get(2).getAsDouble());

                        rootXAnimation.add("rotation", rootXRotation);
                        rootYAnimation.add("rotation", rootYRotation);
                        rootZAnimation.add("rotation", rootZRotation);
                    }

                    if (javaHeadPosition != null) {
                        JsonArray rootXPosition = new JsonArray();
                        rootXPosition.add((javaHeadPosition.get(0).getAsDouble() * 0.625) * -1);
                        rootXPosition.add(javaHeadPosition.get(1).getAsDouble() * 0.625);
                        rootXPosition.add(javaHeadPosition.get(2).getAsDouble() * 0.625);

                        rootXAnimation.add("position", rootXPosition);
                    }

                    JsonArray rootXScale = new JsonArray();
                    if (javaHeadScale != null) {
                        rootXScale.add(javaHeadScale.get(0).getAsDouble() * 0.625);
                        rootXScale.add(javaHeadScale.get(1).getAsDouble() * 0.625);
                        rootXScale.add(javaHeadScale.get(2).getAsDouble() * 0.625);

                    } else {
                        rootXScale.add(0.625);
                        rootXScale.add(0.625);
                        rootXScale.add(0.625);
                    }
                    rootXAnimation.add("scale", rootXScale);
                }

                thirdPersonHeadBones.add("root_x", rootXAnimation);
                thirdPersonHeadBones.add("root_y", rootYAnimation);
                thirdPersonHeadBones.add("root_z", rootZAnimation);
            }
        }

        thirdPersonMainHand.add("bones", thirdPersonMainHandBones);
        thirdPersonOffHand.add("bones", thirdPersonOffHandBones);
        thirdPersonHead.add("bones", thirdPersonHeadBones);
        firstPersonMainHand.add("bones", firstPersonMainHandBones);
        firstPersonOffHand.add("bones", firstPersonOffHandBones);

        animations.add("animation." + itemName + ".thirdperson_main_hand", thirdPersonMainHand);
        animations.add("animation." + itemName + ".thirdperson_off_hand", thirdPersonOffHand);
        animations.add("animation." + itemName + ".head", thirdPersonHead);
        animations.add("animation." + itemName + ".firstperson_main_hand", firstPersonMainHand);
        animations.add("animation." + itemName + ".firstperson_off_hand", firstPersonOffHand);

        root.addProperty("format_version", "1.8.0");
        root.add("animations", animations);

        return root;
    }

}
