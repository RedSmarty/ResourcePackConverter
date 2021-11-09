package xyz.redsmarty.resourcepackconverter.converters;

import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.type.ConversionResults;

public interface AbstractConverter {

    void convert(JavaResourcePack javaResourcePack, BedrockResourcePack bedrockResourcePack, ConversionOptions options, ConversionResults results);

}
