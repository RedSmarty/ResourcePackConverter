package xyz.redsmarty.resourcepackconverter.utils.mappings.models.bedrock;

import java.util.List;

public class Geometry {

    private Description description;
    private List<Bone> bones;

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
