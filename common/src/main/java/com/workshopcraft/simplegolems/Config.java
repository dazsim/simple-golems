package com.workshopcraft.simplegolems;

import com.workshopcraft.simplegolems.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// server settings, read from config/simplegolems.toml (NeoForge fills them from its config spec, Fabric reads the same file here)
public final class Config {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleGolems.MOD_ID);

    public static int maxAreaWidth = 32;
    public static double workSpeed = 1.0;
    public static int cookieTicks = 6000;
    public static int blockReserve = 64;
    public static Set<Job> enabledJobs = EnumSet.allOf(Job.class);
    public static boolean voidRescue = true;
    public static boolean digOut = true;
    public static boolean moddedStorage = true;

    public static final List<String> JOBS = List.of("lumber", "farmer", "miner", "courier", "sweeper");

    private Config() {}

    public static boolean jobEnabled(Job job) {
        return job == Job.BLANK || enabledJobs.contains(job);
    }

    public static void setJobs(List<? extends String> names) {
        Set<Job> jobs = EnumSet.of(Job.BLANK);
        for (Job j : Job.values()) {
            if (names.contains(j.getSerializedName())) jobs.add(j);
        }
        enabledJobs = jobs;
    }

    // a flat key = value TOML file, written with the defaults when it is missing
    public static void loadFile(Path file) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, defaults());
                return;
            }
            Map<String, String> v = new LinkedHashMap<>();
            for (String line : Files.readAllLines(file)) {
                String l = line.strip();
                int eq = l.indexOf('=');
                if (l.isEmpty() || l.startsWith("#") || l.startsWith("[") || eq < 0) continue;
                String val = l.substring(eq + 1).strip();
                int hash = val.indexOf(" #");
                if (hash >= 0 && !val.startsWith("\"")) val = val.substring(0, hash).strip();
                v.put(l.substring(0, eq).strip(), val);
            }
            maxAreaWidth = clamp(intOf(v, "maxAreaWidth", 32), 4, 128);
            workSpeed = Math.max(0.1, Math.min(10.0, Double.parseDouble(v.getOrDefault("workSpeed", "1.0"))));
            cookieTicks = clamp(intOf(v, "cookieTicks", 6000), 20, 1_000_000);
            blockReserve = clamp(intOf(v, "blockReserve", 64), 0, 64);
            voidRescue = !"false".equals(v.get("voidRescue"));
            digOut = !"false".equals(v.get("digOut"));
            moddedStorage = !"false".equals(v.get("moddedStorage"));
            String jobs = v.get("enabledJobs");
            if (jobs != null) {
                setJobs(List.of(jobs.replaceAll("[\\[\\]\"\\s]", "").toLowerCase(Locale.ROOT).split(",")));
            }
        } catch (IOException | RuntimeException e) {
            LOG.warn("Could not read {}, using defaults: {}", file, e.toString());
        }
    }

    // work time after the speed multiplier
    public static int scaled(int ticks) {
        return Math.max(1, (int) Math.round(ticks / workSpeed));
    }

    private static int intOf(Map<String, String> v, String key, int def) {
        String s = v.get(key);
        return s == null ? def : Integer.parseInt(s);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String defaults() {
        return """
                # Simple Golems server settings

                # Largest work area a marker stick can mark, in blocks across (X and Z)
                maxAreaWidth = 32
                # Work speed multiplier: 2.0 digs, chops and harvests twice as fast
                workSpeed = 1.0
                # How long one clay cookie powers a golem, in ticks (20 ticks = 1 second)
                cookieTicks = 6000
                # Building blocks a miner keeps back for steps and sealing (0 to 64)
                blockReserve = 64
                # Jobs golems may do; a golem with a job not listed here waits
                enabledJobs = ["lumber", "farmer", "miner", "courier", "sweeper"]
                # Put golems that fall into the void back on safe ground
                voidRescue = true
                # Let stuck golems dig their way back to their area
                digOut = true
                # Use storage blocks from other mods that offer an item inventory
                moddedStorage = true
                """;
    }
}
