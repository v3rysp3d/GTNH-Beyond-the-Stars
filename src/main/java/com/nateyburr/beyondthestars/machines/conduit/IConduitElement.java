package com.nateyburr.beyondthestars.machines.conduit;

import com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream;

/**
 * P4: a Conduit element the solver can deliver a stream to: the Wormhole Mouth, the Conduit Mirror and the Conduit
 * Junction ({@link MTEConduitElement}). Each registers its in-throats with {@link ConduitNet} once a second while it is
 * formed; a ray from another element's out-throat that crosses one of those openings within range is delivered with
 * {@link #receive}.
 */
public interface IConduitElement {

    /** {@code dim:x,y,z} of the controller: the element's key in a stream's path. */
    String conduitKey();

    /** "Conduit Mirror at 120, 80, -40" (scanner, routes). */
    String conduitLabel();

    /** True while the tile is loaded and valid (stale registrations are dropped). */
    boolean conduitValid();

    /**
     * A stream arrives on throat {@code throat} (server thread, inside the walk of the tick it was sent). Returns the
     * Bu/t the element passed on (after its throat cap); 0 when it is closed or the stream is dropped (cycle, hop
     * limit, a throat that takes nothing in this mode).
     */
    double receive(BurstStream stream, int throat);
}
