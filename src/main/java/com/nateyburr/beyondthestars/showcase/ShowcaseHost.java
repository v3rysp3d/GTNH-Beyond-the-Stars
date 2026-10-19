package com.nateyburr.beyondthestars.showcase;

/**
 * A controller that can run in SHOWCASE MODE (designs/docs/showcase-worlds.md): a flag saved in the machine that keeps
 * it running forever with nobody at the console, for the downloadable showcase world. While it is on, the machine
 * feeds and steers itself (each implementation says how), and {@link BTSShowcase#second} keeps its footprint loaded,
 * its maintenance fixed and it switched back on after a structure, power or maintenance stop.
 * <p>
 * Every implementation saves the flag in its tile NBT under {@link BTSShowcase#NBT_KEY} (not in the item NBT: a
 * wrenched controller is an ordinary machine again) and calls {@link BTSShowcase#second} once a second on the server.
 */
public interface ShowcaseHost {

    boolean isShowcase();

    /** Turns showcase mode on or off. Off releases the machine's chunk tickets on its next second. */
    void setShowcase(boolean on);

    /**
     * The footprint to keep loaded, as world coordinates {x0, y0, z0, x1, y1, z1}, or null while it is not known (the
     * machine has not formed yet).
     */
    int[] showcaseBox();

    /** The machine's name for /btsshowcase status. */
    String showcaseName();

    /**
     * True (the default) when {@link BTSShowcase#second} may keep the input hatches and buses topped up with what is
     * already in them. False for a machine with trim sockets among its input hatches (the Eye, the quasar), where a
     * full hatch would over-trim: those feed themselves.
     */
    default boolean showcaseTopsUpHatches() {
        return true;
    }
}
