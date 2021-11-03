package xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock;

public class Uv {
    private Face north;
    private Face south;
    private Face east;
    private Face west;
    private Face up;
    private Face down;

    public Face getNorth() {
        return north;
    }

    public Face getSouth() {
        return south;
    }

    public Face getEast() {
        return east;
    }

    public Face getWest() {
        return west;
    }

    public Face getUp() {
        return up;
    }

    public Face getDown() {
        return down;
    }

    private Uv(Builder builder) {
        this.north = builder.north;
        this.south = builder.south;
        this.east = builder.east;
        this.west = builder.west;
        this.up = builder.up;
        this.down = builder.down;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Face north;
        private Face south;
        private Face east;
        private Face west;
        private Face up;
        private Face down;

        public Builder setNorth(Face north) {
            this.north = north;
            return this;
        }

        public Builder setSouth(Face south) {
            this.south = south;
            return this;
        }

        public Builder setEast(Face east) {
            this.east = east;
            return this;
        }

        public Builder setWest(Face west) {
            this.west = west;
            return this;
        }

        public Builder setUp(Face up) {
            this.up = up;
            return this;
        }

        public Builder setDown(Face down) {
            this.down = down;
            return this;
        }

        public Uv build() {
            return new Uv(this);
        }
    }
}
