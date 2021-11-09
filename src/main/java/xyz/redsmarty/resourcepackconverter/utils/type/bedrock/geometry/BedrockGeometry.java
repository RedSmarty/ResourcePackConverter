package xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("FieldMayBeFinal")
public class BedrockGeometry {
    @SerializedName("format_version")
    private String formatVersion = "1.16.0";
    @SerializedName("minecraft:geometry")
    private Geometry[] geometry = new Geometry[1];

    private transient byte[] texture;

    public Geometry[] getGeometry() {
        return geometry;
    }

    public byte[] getTexture() {
        return texture;
    }

    public BedrockGeometry(Geometry geometry, byte[] texture) {
        this.geometry[0] = geometry;
        this.texture = texture;
    }

}
