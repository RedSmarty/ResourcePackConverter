package xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock;

import java.util.List;

public class Bone {
    private String name;
    private String parent;
    private double[] pivot;
    private String binding;
    private List<Cube> cubes;

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public double[] getPivot() {
        return pivot;
    }

    public String getBinding() {
        return binding;
    }

    public List<Cube> getCubes() {
        return cubes;
    }

    public Bone(Builder builder) {
        this.name = builder.name;
        this.parent = builder.parent;
        this.binding = builder.binding;
        this.pivot = builder.pivot;
        this.cubes = builder.cubes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String parent;
        private double[] pivot;
        private String binding;
        private List<Cube> cubes;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setParent(String parent) {
            this.parent = parent;
            return this;
        }

        public Builder setPivot(double[] pivot) {
            this.pivot = pivot;
            return this;
        }

        public Builder setBinding(String binding) {
            this.binding = binding;
            return this;
        }

        public Builder setCubes(List<Cube> cubes) {
            this.cubes = cubes;
            return this;
        }

        public Bone build() {
            return new Bone(this);
        }
    }
}
