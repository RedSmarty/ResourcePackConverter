package xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("FieldMayBeFinal")
public class Description {
    private String identifier;
    @SerializedName("texture_width")
    private int textureWidth;
    @SerializedName("texture_height")
    private int textureHeight;
    @SerializedName("visible_bounds_width")
    private double visibleTextureBoundWidth;
    @SerializedName("visible_bounds_height")
    private double visibleTextureBoundHeight;
    @SerializedName("visible_bounds_offset")
    private double[] visibleTextureBoundOffset;

    public String getIdentifier() {
        return identifier;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public double getVisibleTextureBoundWidth() {
        return visibleTextureBoundWidth;
    }

    public double getVisibleTextureBoundHeight() {
        return visibleTextureBoundHeight;
    }

    public double[] getVisibleTextureBoundOffset() {
        return visibleTextureBoundOffset;
    }

    public static Builder builder() {
        return new Builder();
    }

    private Description(Builder builder) {
        this.identifier = builder.identifier;
        this.textureWidth = builder.textureWidth;
        this.textureHeight = builder.textureHeight;
        this.visibleTextureBoundWidth = builder.visibleTextureBoundWidth;
        this.visibleTextureBoundHeight = builder.visibleTextureBoundHeight;
        this.visibleTextureBoundOffset = builder.visibleTextureBoundOffset;
    }

    public static class Builder {
        private String identifier;
        private int textureWidth;
        private int textureHeight;
        private double visibleTextureBoundWidth;
        private double visibleTextureBoundHeight;
        private double[] visibleTextureBoundOffset;

        private Builder() {}

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setTextureWidth(int textureWidth) {
            this.textureWidth = textureWidth;
            return this;
        }

        public Builder setTextureHeight(int textureHeight) {
            this.textureHeight = textureHeight;
            return this;
        }

        public Builder setVisibleTextureBoundWidth(double visibleTextureBoundWidth) {
            this.visibleTextureBoundWidth = visibleTextureBoundWidth;
            return this;
        }

        public Builder setVisibleTextureBoundHeight(double visibleTextureBoundHeight) {
            this.visibleTextureBoundHeight = visibleTextureBoundHeight;
            return this;
        }

        public Builder setVisibleTextureBoundOffset(double[] visibleTextureBoundOffset) {
            this.visibleTextureBoundOffset = visibleTextureBoundOffset;
            return this;
        }

        public Description build() {
            return new Description(this);
        }
    }
}