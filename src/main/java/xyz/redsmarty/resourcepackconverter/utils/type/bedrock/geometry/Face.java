package xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry;

import com.google.gson.annotations.SerializedName;

public class Face {
    double[] uv;
    @SerializedName("uv_size")
    double[] uvSize;

    public double[] getUv() {
        return uv;
    }

    public double[] getUvSize() {
        return uvSize;
    }

    private Face(Builder builder) {
        this.uv = builder.uv;
        this.uvSize = builder.uvSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        double[] uv = new double[2];
        double[] uvSize = new double[2];

        public Builder setUv(double[] uv) {
            this.uv = uv;
            return this;
        }

        public Builder setUvSize(double[] uvSize) {
            this.uvSize = uvSize;
            return this;
        }

        private Builder() {}

        public Face build() {
            return new Face(this);
        }
    }
}