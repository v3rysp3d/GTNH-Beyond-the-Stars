package com.nateyburr.beyondthestars.machines.directive;

/**
 * AUTOMATION PASS: a machine a Directive Hatch ({@link MTEHatchDirective}) reports for. The host pushes itself to each
 * of its Directive Hatches once a second ({@link MTEHatchDirective#publish}); the hatch keeps nothing of it but the
 * last code and labels, so a host that stops pushing (unloaded, broken) leaves the hatch at 0 after a few seconds.
 * Server side only.
 */
public interface DirectiveHost {

    DirectiveSet.Host directiveHost();

    /**
     * The 4-bit code of an output set of this host, from the host's last solved second. {@code window} is the hatch's
     * window w (the Eye: the gauge band ±w; the Kerr: the bolt reserve in minutes).
     */
    int directiveCode(DirectiveSet set, int window);

    /** What bit {@code i} (0..3) of {@code set} means on this host now (the Eye's trims follow the star's tier). */
    default String directiveBitLabel(DirectiveSet set, int i) {
        return set.bit(i);
    }

    /**
     * A control set's redstone input, 1-15 (never called with 0: no signal leaves the setpoint alone). The host applies
     * it with the same clamps as its GUI.
     */
    void directiveControl(DirectiveSet set, int strength);

    /** The setpoint a control set drives, as text for the hatch GUI and the scanner. */
    String directiveControlLine(DirectiveSet set);
}
