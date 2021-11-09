package xyz.redsmarty.resourcepackconverter.utils.type.bedrock.geometry;

import java.util.List;

public class Geometry {

    private final Description description;
    private final List<Bone> bones;

    public Description getDescription() {
        return description;
    }

    public List<Bone> getBones() {
        return bones;
    }

    public Geometry(Description description, List<Bone> bones) {
        this.description = description;
        this.bones = bones;
    }
}
