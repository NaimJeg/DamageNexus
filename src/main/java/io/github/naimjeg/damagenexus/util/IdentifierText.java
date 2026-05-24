package io.github.naimjeg.damagenexus.util;

import net.minecraft.resources.Identifier;

public final class IdentifierText {

    private IdentifierText() {
    }

    public static String namespace(Identifier id) {
        String raw = id.toString();
        int colon = raw.indexOf(':');
        return colon >= 0 ? raw.substring(0, colon) : "minecraft";
    }

    public static String path(Identifier id) {
        String raw = id.toString();
        int colon = raw.indexOf(':');
        return colon >= 0 && colon + 1 < raw.length()
                ? raw.substring(colon + 1)
                : raw;
    }

    public static String langPath(Identifier id) {
        return namespace(id) + "." + path(id);
    }

    public static String debug(Identifier id) {
        return id.toString();
    }
}
