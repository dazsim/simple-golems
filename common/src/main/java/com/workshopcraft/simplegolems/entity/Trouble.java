package com.workshopcraft.simplegolems.entity;

// what the thought bubble above a golem shows
public enum Trouble {
    NONE,
    STORAGE,
    SAPLING,
    FOOD,
    TOOL,
    BLOCKS,
    // a program is missing something it works with: torches, feed, bottles, fuel, a mesh
    SUPPLIES,
    // not a problem: a short thank-you once the last problem is sorted
    THANKS,
    // a guard is badly hurt or knocked down
    HURT;

    public static Trouble byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }
}
