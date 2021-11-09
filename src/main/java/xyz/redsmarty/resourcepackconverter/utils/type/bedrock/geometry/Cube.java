package xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry;

@SuppressWarnings("FieldMayBeFinal")
public class Cube {
    private double[] origin;
    private double[] size;
    private double[] pivot;
    private double[] rotation;
    private Uv uv;

    public double[] getOrigin() {
        return origin;
    }

    public double[] getSize() {
        return size;
    }

    public Uv getUv() {
        return uv;
    }

    public double[] getPivot() {
        return pivot;
    }

    public double[] getRotation() {
        return rotation;
    }

    private Cube(Builder builder) {
        this.origin = builder.origin;
        this.size = builder.size;
        this.uv = builder.uv;
        this.pivot = builder.pivot;
        this.rotation = builder.rotation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double[] origin;
        private double[] size;
        private double[] pivot;
        private double[] rotation;

        public Builder setPivot(double[] pivot) {
            this.pivot = pivot;
            return this;
        }

        public Builder setRotation(double[] rotation) {
            this.rotation = rotation;
            return this;
        }

        private Uv uv;

        private Builder() {}

        public Builder setOrigin(double[] origin) {
            this.origin = origin;
            return this;
        }

        public Builder setSize(double[] size) {
            this.size = size;
            return this;
        }

        public Builder setUv(Uv uv) {
            this.uv = uv;
            return this;
        }

        public Cube build() {
            return new Cube(this);
        }
    }
}
