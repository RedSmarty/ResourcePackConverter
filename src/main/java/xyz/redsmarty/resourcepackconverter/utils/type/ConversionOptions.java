package xyz.redsmarty.resourcepackconverter.utils.type;

import java.util.UUID;
import java.util.function.Consumer;

public record ConversionOptions(String name, UUID uuid, int[] version, String javaHash,
                                String namespace, Consumer<String> logger) {
}
