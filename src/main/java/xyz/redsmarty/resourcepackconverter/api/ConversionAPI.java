package xyz.redsmarty.resourcepackconverter.api;

import xyz.redsmarty.resourcepackconverter.converters.AbstractConverter;
import xyz.redsmarty.resourcepackconverter.converters.ConverterList;
import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;
import xyz.redsmarty.resourcepackconverter.utils.mappings.ConversionResults;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class ConversionAPI {
    private static ConversionAPI instance;

    public static ConversionAPI getInstance() {
        if (instance == null) {
            instance = new ConversionAPI();
        }
        return instance;
    }

    public ConversionResults convert(InputStream javaStream, File bedrockFile, ConversionOptions options) throws IOException, InvalidResourcePackException {
        JavaResourcePack javaResourcePack = new JavaResourcePack(new ZipInputStream(javaStream), options.getLogger());
        BedrockResourcePack bedrockResourcePack = new BedrockResourcePack(options.getName(), options.getVersion(), javaResourcePack.getDescription(), javaResourcePack.getFile("pack.png"));

        ConversionResults results = new ConversionResults();

        for (AbstractConverter converter : ConverterList.getConverters()) {
            converter.convert(javaResourcePack, bedrockResourcePack, options, results);
        }
        bedrockResourcePack.zipIt(bedrockFile);
        results.setSuccess(true);
        return results;
    }

}
