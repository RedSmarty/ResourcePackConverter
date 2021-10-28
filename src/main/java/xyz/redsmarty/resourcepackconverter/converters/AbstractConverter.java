package xyz.redsmarty.resourcepackconverter.converters;

import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.mappings.ConversionResults;

public interface AbstractConverter {

    void convert(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, ConversionOptions options, ConversionResults results);

}
