package com.workshopcraft.simplegolems;

import net.minecraft.resources.Identifier;

public final class SimpleGolems {
    public static final String MOD_ID = "simplegolems";

    private SimpleGolems() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
