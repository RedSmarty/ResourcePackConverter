package xyz.redsmarty.resourcepackconverter.api;

import xyz.redsmarty.resourcepackconverter.converters.AbstractConverter;
import xyz.redsmarty.resourcepackconverter.converters.ConverterList;
import xyz.redsmarty.resourcepackconverter.resourcepacks.BedrockResourcePack;
import xyz.redsmarty.resourcepackconverter.resourcepacks.JavaResourcePack;
import xyz.redsmarty.resourcepackconverter.utils.ConversionOptions;
import xyz.redsmarty.resourcepackconverter.utils.InvalidResourcePackException;
import xyz.redsmarty.resourcepackconverter.utils.Util;
import xyz.redsmarty.resourcepackconverter.utils.mappings.ConversionResults;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ConversionAPI {
    private static ConversionAPI instance;

    public static ConversionAPI getInstance() {
        if (instance == null) {
            instance = new ConversionAPI();
        }
        return instance;
    }

    public ConversionResults convert(File javaFile, File bedrockFile, ConversionOptions options) throws IOException, InvalidResourcePackException {
        JavaResourcePack javaResourcePack = new JavaResourcePack(javaFile.getName(), new ZipFile(javaFile), Util.bytesToHex(Util.calculateSHA1(javaFile)));
        BedrockResourcePack bedrockResourcePack = new BedrockResourcePack(javaResourcePack.getName(), options.getVersion(), javaResourcePack.getDescription(), javaResourcePack.getFile("pack.png"));

        ConversionResults results = new ConversionResults();

        for (AbstractConverter converter : ConverterList.getConverters()) {
            converter.convert(javaResourcePack, bedrockResourcePack, options, results);
        }
        bedrockResourcePack.zipIt(bedrockFile);
        results.setSuccess(true);
        return results;
    }

}
