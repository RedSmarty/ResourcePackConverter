package xyz.redsmarty.resourcepackconverter.utils.type.bedrock.attachable;

@SuppressWarnings("FieldMayBeFinal")
public class Description {
    private String identifier;
    private Materials materials = new Materials();

    public Description(String identifier) {
        this.identifier = identifier;
    }
}
