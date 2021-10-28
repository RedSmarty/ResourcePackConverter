package xyz.redsmarty.resourcepackconverter.converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConverterList {

    private static final List<AbstractConverter> converters = new ArrayList<>();

    static {
        converters.add(new ItemTextureConverter());
        converters.add(new CustomModelDataConverter());
    }

    public static List<AbstractConverter> getConverters() {
        return Collections.unmodifiableList(converters);
    }

}
