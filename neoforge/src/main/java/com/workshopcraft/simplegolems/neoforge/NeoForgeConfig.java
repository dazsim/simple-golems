package com.workshopcraft.simplegolems.neoforge;

import com.workshopcraft.simplegolems.Config;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// the same settings as Config, as a NeoForge config spec in config/simplegolems.toml
final class NeoForgeConfig {
    static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue MAX_AREA_WIDTH;
    private static final ModConfigSpec.DoubleValue WORK_SPEED;
    private static final ModConfigSpec.IntValue COOKIE_TICKS;
    private static final ModConfigSpec.IntValue BLOCK_RESERVE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_JOBS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_PROGRAMS;
    private static final ModConfigSpec.BooleanValue VOID_RESCUE;
    private static final ModConfigSpec.BooleanValue DIG_OUT;
    private static final ModConfigSpec.BooleanValue MODDED_STORAGE;
    private static final ModConfigSpec.IntValue RANCHER_MAX;
    private static final ModConfigSpec.BooleanValue SKYBLOCK;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ENABLED_COMBAT;
    private static final ModConfigSpec.IntValue GUARD_HEALTH;
    private static final ModConfigSpec.IntValue GUARD_CHASE;
    private static final ModConfigSpec.ConfigValue<String> GUARD_CREEPERS;
    private static final ModConfigSpec.IntValue GUARD_DOWN;
    private static final ModConfigSpec.IntValue GUARD_HEAL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MAX_AREA_WIDTH = b.comment("Largest work area a marker stick can mark, in blocks across (X and Z)").defineInRange("maxAreaWidth", 32, 4, 128);
        WORK_SPEED = b.comment("Work speed multiplier: 2.0 digs, chops and harvests twice as fast").defineInRange("workSpeed", 1.0, 0.1, 10.0);
        COOKIE_TICKS = b.comment("How long one clay cookie powers a golem, in ticks (20 ticks = 1 second)").defineInRange("cookieTicks", 6000, 20, 1_000_000);
        BLOCK_RESERVE = b.comment("Building blocks a miner keeps back for steps and sealing (0 to 64)").defineInRange("blockReserve", 64, 0, 64);
        ENABLED_JOBS = b.comment("Jobs golems may do; a golem with a job not listed here waits")
                .defineList("enabledJobs", Config.JOBS, () -> "lumber", o -> o instanceof String s && Config.JOBS.contains(s));
        ENABLED_PROGRAMS = b.comment("Programs golems may run; a golem with a program not listed here waits")
                .defineList("enabledPrograms", Config.PROGRAMS, () -> "cobblegen", o -> o instanceof String s && Config.PROGRAMS.contains(s));
        VOID_RESCUE = b.comment("Put golems that fall into the void back on safe ground").define("voidRescue", true);
        DIG_OUT = b.comment("Let stuck golems dig their way back to their area").define("digOut", true);
        MODDED_STORAGE = b.comment("Use storage blocks from other mods that offer an item inventory").define("moddedStorage", true);
        SKYBLOCK = b.comment("The skyblock programs (cobblegen, sifter and hammer) as a group").define("skyblockPrograms", true);
        RANCHER_MAX = b.comment("A rancher stops breeding a kind of animal once its area holds this many of them").defineInRange("rancherMaxAnimals", 8, 2, 64);
        ENABLED_COMBAT = b.comment("Combat jobs golems may do; a guard not listed here waits")
                .defineList("enabledCombat", Config.COMBAT, () -> "guard", o -> o instanceof String s && Config.COMBAT.contains(s));
        GUARD_HEALTH = b.comment("Max health of a guard golem (10 to 200)").defineInRange("guardHealth", 30, 10, 200);
        GUARD_CHASE = b.comment("How many blocks past the edge of its area a guard chases a mob (0 to 16)").defineInRange("guardChaseRange", 4, 0, 16);
        GUARD_CREEPERS = b.comment("What a guard does about creepers: hit_and_back_off, attack or ignore")
                .define("guardCreepers", "hit_and_back_off", o -> o instanceof String s && Config.CREEPER_MODES.contains(s));
        GUARD_DOWN = b.comment("Seconds a guard stays down when its health runs out (5 to 600)").defineInRange("guardDownTime", 60, 5, 600);
        GUARD_HEAL = b.comment("Health a guard gets back from eating a clay cookie after a fight").defineInRange("guardCookieHeal", 8, 1, 200);
        SPEC = b.build();
    }

    private NeoForgeConfig() {}

    static void onLoad(ModConfigEvent e) {
        if (e.getConfig().getSpec() != SPEC) return;
        Config.maxAreaWidth = MAX_AREA_WIDTH.get();
        Config.workSpeed = WORK_SPEED.get();
        Config.cookieTicks = COOKIE_TICKS.get();
        Config.blockReserve = BLOCK_RESERVE.get();
        Config.setJobs(Config.JOBS, ENABLED_JOBS.get());
        Config.setJobs(Config.PROGRAMS, ENABLED_PROGRAMS.get());
        Config.voidRescue = VOID_RESCUE.get();
        Config.digOut = DIG_OUT.get();
        Config.moddedStorage = MODDED_STORAGE.get();
        Config.rancherMaxAnimals = RANCHER_MAX.get();
        Config.skyblockPrograms = SKYBLOCK.get();
        Config.setJobs(Config.COMBAT, ENABLED_COMBAT.get());
        Config.guardHealth = GUARD_HEALTH.get();
        Config.guardChaseRange = GUARD_CHASE.get();
        Config.guardCreepers = GUARD_CREEPERS.get();
        Config.guardDownTime = GUARD_DOWN.get();
        Config.guardCookieHeal = GUARD_HEAL.get();
    }
}
