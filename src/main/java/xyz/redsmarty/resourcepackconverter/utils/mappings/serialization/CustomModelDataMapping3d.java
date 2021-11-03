package xyz.redsmarty.resourcepackconverter.utils.mappings.serialization;

public class CustomModelDataMapping3d {

    public static class Element {
        private double[] from;
        private double[] to;
        private Rotation rotation;
        private Faces faces;

        public double[] getFrom() {
            return from;
        }

        public double[] getTo() {
            return to;
        }

        public Rotation getRotation() {
            return rotation;
        }

        public Faces getFaces() {
            return faces;
        }

        public static class Rotation {
            private int angle;
            private char axis;
            private double[] origin;

            public int getAngle() {
                return angle;
            }

            public char getAxis() {
                return axis;
            }

            public double[] getOrigin() {
                return origin;
            }
        }

        public static class Faces {
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

            public static class Face {
                private int[] uv;
                private String texture;
                // Used to represent real uv (in pixels)
                private int[] realUv;

                public int[] getUv() {
                    return uv;
                }

                public String getTexture() {
                    return texture;
                }

                public int[] getRealUv() {
                    return realUv;
                }

                public void setRealUv(int[] realUv) {
                    this.realUv = realUv;
                }
            }
        }
    }
    public static class Group {
        private String name;
        private double[] origin;
        private int color;
        private int[] children;

        public String getName() {
            return name;
        }

        public double[] getOrigin() {
            return origin;
        }

        public int getColor() {
            return color;
        }

        public int[] getChildren() {
            return children;
        }

        public Group(String name, double[] origin, int color, int[] children) {
            this.name = name;
            this.origin = origin;
            this.color = color;
            this.children = children;
        }

        public Group() {}
    }
}
