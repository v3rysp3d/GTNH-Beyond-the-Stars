package com.nateyburr.beyondthestars.machines.quasar.burst;

/**
 * P4: one of the Quasar Engine's two jet ports (quasar-p4.md §2). Each carries half the stream
 * ({@code QuasarResult.jet}
 * = G/2). The port cell is the jet axis just past the cap, voxel (0, ±81, 0); its offset from the controller is
 * {@code manifest.json machines.quasar.slots.<key>.portRel}.
 */
public enum JetPort {

    TOP("top", "Top"),
    BOTTOM("bottom", "Bottom");

    public static final JetPort[] VALUES = values();

    /** The key in the manifest's slot table. */
    public final String key;
    public final String label;

    JetPort(String key, String label) {
        this.key = key;
        this.label = label;
    }
}
