package com.workshopcraft.simplegolems.entity;

// what the thought bubble above a golem shows
public enum Trouble {
    NONE,
    STORAGE,
    SAPLING,
    FOOD,
    TOOL,
    // not a problem: a short thank-you once the last problem is sorted
    THANKS;

    public static Trouble byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }
}
