package com.nateyburr.beyondthestars.machines.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;

import gregtech.api.enums.GTValues;
import gregtech.api.util.tooltip.TooltipHelper;

/**
 * The tooltip vocabulary every machine, hatch and item of the addon uses, so they read like GregTech's own
 * (docs/tooltip-style.md). The colours are GT's {@link TooltipHelper} ones; every helper returns to grey after its
 * coloured part, so a line reads {@code "Holds " + BTSTip.term("a black hole") + " ..."} with no colour bookkeeping.
 * <ul>
 * <li>Body text: grey. Names of things the player handles (items, machines, hatches, blocks): white.</li>
 * <li>EU, EU/t, Bu/t and voltages: yellow, the voltage tier in its GT tier colour. Amperes: aqua.</li>
 * <li>Parallels: gold. Speed: green. Efficiency: aqua. Fluid amounts and other plain numbers: white.</li>
 * <li>Warnings: red; irreversible or destructive: dark red. Hints ("Right-click ...", "Hold Shift ..."): dark
 * grey.</li>
 * </ul>
 */
public final class BTSTip {

    public static final String GRAY = EnumChatFormatting.GRAY.toString();
    public static final String WHITE = EnumChatFormatting.WHITE.toString();
    public static final String YELLOW = EnumChatFormatting.YELLOW.toString();
    public static final String GOLD = EnumChatFormatting.GOLD.toString();
    public static final String GREEN = EnumChatFormatting.GREEN.toString();
    public static final String AQUA = EnumChatFormatting.AQUA.toString();
    public static final String RED = EnumChatFormatting.RED.toString();
    public static final String DARK_RED = EnumChatFormatting.DARK_RED.toString();
    public static final String DARK_GRAY = EnumChatFormatting.DARK_GRAY.toString();
    public static final String LIGHT_PURPLE = EnumChatFormatting.LIGHT_PURPLE.toString();
    public static final String BOLD = EnumChatFormatting.BOLD.toString();
    public static final String UNDERLINE = EnumChatFormatting.UNDERLINE.toString();
    public static final String RESET = EnumChatFormatting.RESET.toString();

    /** Voltage tier indices (GTValues.VN). */
    public static final int UHV = 9, UEV = 10, UIV = 11, UMV = 12, UXV = 13, MAX = 14;

    /** Dump bench only ({@code /btstooltips}): answers {@link #shift()} without a keyboard. Null: the real keys. */
    public static Boolean forceShift;

    private BTSTip() {}

    /** A thing the player handles (item, block, machine, hatch): white. */
    public static String term(String s) {
        return WHITE + s + GRAY;
    }

    /** A plain number or amount: white (GT's tier / litre colour). */
    public static String num(Object n) {
        return WHITE + n + GRAY;
    }

    /** "1,234" with grouping. */
    public static String fmt(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** An EU/t figure: yellow (GT's EU colour). */
    public static String euT(long eut) {
        return YELLOW + fmt(eut) + " EU/t" + GRAY;
    }

    /** An EU/t figure too big for a long's grouping to read well: "1.0e12 EU/t". */
    public static String euT(double eut) {
        return YELLOW + sci(eut) + " EU/t" + GRAY;
    }

    /** A burst rate with its EU/t beside it (1 Bu/t = 1e13 EU/t): "0.1 Bu/t (1.0e12 EU/t)". */
    public static String bu(double buPerTick) {
        return YELLOW + trim(buPerTick) + " Bu/t" + GRAY + " (" + euT(buPerTick * 1e13) + ")";
    }

    /** A voltage tier name in its GT tier colour: "UEV". */
    public static String tier(int index) {
        return TooltipHelper.voltageTierText(index, false);
    }

    /** "1 A UEV": amperes in aqua, the tier in its colour. */
    public static String amps(long amps, int tierIndex) {
        return AQUA + amps + " A " + tier(tierIndex);
    }

    /** Parallels: gold. */
    public static String parallel(String s) {
        return TooltipHelper.parallelText(s);
    }

    /** Speed: green. */
    public static String speed(String s) {
        return TooltipHelper.speedText(s);
    }

    /** Efficiency: aqua. */
    public static String eff(String s) {
        return TooltipHelper.effText(s);
    }

    /** A fluid amount: "1,000 L" in white. */
    public static String litres(long l) {
        return TooltipHelper.fluidText(l);
    }

    /** A warning: red. */
    public static String warn(String s) {
        return RED + s + GRAY;
    }

    /** Irreversible or destructive: dark red. */
    public static String danger(String s) {
        return DARK_RED + s + GRAY;
    }

    /** A hint or control line: dark grey. */
    public static String hint(String s) {
        return DARK_GRAY + s;
    }

    /** A flavour line: light purple, as GT's Black Hole Compressor opens. */
    public static String flavour(String s) {
        return LIGHT_PURPLE + s;
    }

    /** A sub-heading inside the info: white, underlined (GT's LHC "Accelerator mode"). */
    public static String heading(String s) {
        return WHITE + UNDERLINE + s + RESET + GRAY;
    }

    /** A list line: "- text" with the dash in dark grey. */
    public static String bullet(String s) {
        return DARK_GRAY + "- " + GRAY + s;
    }

    /** GT's own "Hold [LSHIFT]" wording, for a hatch or item whose details sit behind Shift. */
    public static String holdShift() {
        return GRAY + "Hold " + BOLD + "[LSHIFT]" + RESET + GRAY + " for details";
    }

    /** True while Shift is held (client), or when the dump bench forces it. Never throws on a server. */
    public static boolean shift() {
        if (forceShift != null) return forceShift;
        try {
            return org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
        } catch (Throwable noKeyboard) {
            return false;
        }
    }

    /**
     * A hatch's tooltip: the short lines always, then either "Hold [LSHIFT] for details" or, with Shift held, the
     * details.
     */
    public static String[] withDetails(String[] always, String... details) {
        final List<String> out = new ArrayList<>(Arrays.asList(always));
        if (details.length > 0) {
            if (shift()) out.addAll(Arrays.asList(details));
            else out.add(holdShift());
        }
        return out.toArray(new String[0]);
    }

    /** "8.4e12" style for very large numbers, plain grouping below a million. */
    public static String sci(double v) {
        if (Math.abs(v) < 1e6) return String.format(Locale.ROOT, "%,.0f", v);
        final String s = String.format(Locale.ROOT, "%.1e", v);
        return s.replace("e+0", "e")
            .replace("e+", "e");
    }

    /** 0.1, 0.25, 9796: no trailing zeros. */
    public static String trim(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return fmt((long) v);
        String s = String.format(Locale.ROOT, "%.4f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    /** The colour code of a GT voltage tier, for callers composing their own text. */
    public static String tierColour(int index) {
        return GTValues.TIER_COLORS[index];
    }
}
