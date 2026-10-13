package com.nateyburr.beyondthestars.machines.directive;

/**
 * P4: the Eye's galaxy-tier Directive codes as pure maths (quasar-p4.md §8.1; the prototype's {@code W.dir.field},
 * {@code W.dir.event}, {@code transBits} and {@code readyToBegin}). The Eye's hatches and the offline proof
 * ({@code QuasarRigSim}) both call these, so a simulated gate rig reads exactly what the hatches put out. Bit n has
 * the value 2^(n−1) (the Degasser's way). No Minecraft types.
 *
 * <pre>
 * Field (T7+):            [1] P &gt; +w: Space trim     [2] P &lt; −w: more bursts to the Eye
 *                         [4] W &gt; +w: aim high       [8] W &lt; −w: aim low
 * Events (T7+):           [1] Λ &gt; +w: more to the halo   [2] Λ &lt; −w: less to the halo
 *                         [4] a countdown is running    [8] that event's sign is −
 * Galaxy Transition (T6-T8): [1] ready to begin   [2] a transition is running
 *                         [4] purge now (the collapse assist: T−80 s if Φ would erupt before the collapse, and the
 *                             collapse second)       [8] hold the regulator's purges (T−80 s to T−1 s)
 * Slots:                  [1] a planet / companion slot is idle   [2] a companion's upkeep is short (r &lt; 1)
 *                         [4] the last planet run lost parallels  [8] link test fire wanted: T6 Stability Link
 *                             (J1 all to the Eye) or T7 Heavy Link (J1 all to the Eye, J3 75 % to the halo)
 * </pre>
 * 
 * In planets mode the Field set's bits 4 and 8 and the whole Events set stay 0. Errors are last second's
 * ({@code prevE}: the 1 s lag); w is the hatch's window (default 5).
 */
public final class GalaxyDirective {

    private GalaxyDirective() {}

    /** The Field set. {@code lit}: the Eye is at T7+ and lit. */
    public static int field(boolean lit, boolean planets, double P, double W, double w) {
        if (!lit) return 0;
        int c = 0;
        if (P > w) c |= 1;
        if (P < -w) c |= 2;
        if (!planets) {
            if (W > w) c |= 4;
            if (W < -w) c |= 8;
        }
        return c;
    }

    /**
     * The Events set. {@code warn} is the field's event-channel warning this second (a minor merger's sign, a pass's
     * sign, the collapse countdown); {@code crossingWarned} is the satellite crossing's 120 s countdown.
     */
    public static int events(boolean lit, boolean planets, double L, int warn, boolean crossingWarned, double w) {
        if (!lit || planets) return 0;
        final int ew = warn != 0 ? warn : crossingWarned ? 1 : 0;
        int c = 0;
        if (L > w) c |= 1;
        if (L < -w) c |= 2;
        if (ew != 0) c |= 4;
        if (ew < 0) c |= 8;
        return c;
    }

    /** The collapse assist's window: the hold runs from overfeed second 219 to 298; the collapse lands at 300. */
    public static final int ASSIST_FROM = 219, ASSIST_LAST = 299;

    /**
     * The Galaxy Transition set. {@code overfeed}: the Magnetar Collapse is in its overfeed phase at second
     * {@code st}; {@code phi} and {@code b} are the linked quasar's last-second Φ and field b.
     */
    public static int transition(boolean running, boolean ready, boolean overfeed, int st, double phi, double b) {
        int c = 0;
        if (running) c |= 2;
        else if (ready) c |= 1;
        if (running && overfeed) {
            if (st >= ASSIST_FROM && st < ASSIST_LAST) {
                c |= 8;
                if (phi >= 20 && phi + b / 2.4 * (ASSIST_LAST - st) >= 100) c |= 4;
            }
            if (st == ASSIST_LAST) c |= 4;
        }
        return c;
    }

    /**
     * Ready to begin (Transition bit 1): no transition, lit, Stable or better, and for the tier's transition its link
     * milestone done, the core in escrow and the charge buffered. T6 → T7 needs the Stability Link; T7 → T8 (galaxy
     * mode) the Heavy Link. Other tiers: false.
     */
    public static boolean ready(int tier, boolean planets, boolean lit, boolean running, int zone, boolean linkDone,
        boolean coreInEscrow, boolean chargeBuffered) {
        if (running || !lit || zone < 0 || zone > 1) return false;
        if (tier == 6 || (tier == 7 && !planets)) return linkDone && coreInEscrow && chargeBuffered;
        return false;
    }

    /**
     * The Slots set. {@code testFireWanted}: {@link #testFireWanted} (the rig turns the link test fire on).
     */
    public static int slots(boolean slotIdle, boolean companionShort, boolean planetLostParallels,
        boolean testFireWanted) {
        int c = 0;
        if (slotIdle) c |= 1;
        if (companionShort) c |= 2;
        if (planetLostParallels) c |= 4;
        if (testFireWanted) c |= 8;
        return c;
    }

    /**
     * Link test fire wanted (Slots bit 4): the next transition's link milestone is not held yet while its core and
     * charge are in place, on a lit Eye with no transition. T6: the Stability Link (the rig sends J1 all to the Eye);
     * T7 galaxy: the Heavy Link (J1 all to the Eye and J3 75 % to the halo ports). Other tiers: false.
     */
    public static boolean testFireWanted(int tier, boolean planets, boolean lit, boolean running, boolean linkDone,
        boolean coreInEscrow, boolean chargeBuffered) {
        if (tier != 6 && !(tier == 7 && !planets)) return false;
        return lit && !running && !linkDone && coreInEscrow && chargeBuffered;
    }
}
