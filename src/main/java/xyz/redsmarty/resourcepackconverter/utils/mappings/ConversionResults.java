package xyz.redsmarty.resourcepackconverter.utils.mappings;

public class ConversionResults {
    private boolean success;
    private String generatedCustomModelDataMappings;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getGeneratedCustomModelDataMappings() {
        return generatedCustomModelDataMappings;
    }

    public void setGeneratedCustomModelDataMappings(String generatedCustomModelDataMappings) {
        this.generatedCustomModelDataMappings = generatedCustomModelDataMappings;
    }
}
