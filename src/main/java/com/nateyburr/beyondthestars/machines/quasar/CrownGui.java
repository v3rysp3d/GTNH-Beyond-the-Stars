package com.nateyburr.beyondthestars.machines.quasar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Grad;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.engine.BurstRouting;

/**
 * P4: the Burst Thermaliser Crown's console on the controller GUI kit (controller-gui-spec.md §5.5, B-116). The header
 * says RUNNING (a stream is turned into EU), IDLE or NOT FORMED, the Mk and its cap. The rows: the stream against the
 * cap (its parts in the tooltip), the EU/t onto the owner's wireless network, overflow handed up the stack and what the
 * cap clipped, the stack, the jet port it is docked on, the receptor's reading and the lifetime EU. Nothing to set;
 * <b>i</b> has the questbook's "Bursts into power" and "Docking a crown".
 */
public class CrownGui extends BTSControllerGui<MTEBurstThermaliserCrown> {

    private static final String K = "beyondthestars.gui.crown.";

    public CrownGui(MTEBurstThermaliserCrown multiblock) {
        super(multiblock, false, null);
    }

    private static String bu(double v) {
        return MTEQuasarEngine.bu(v);
    }

    @Override
    protected Badge badgeCode() {
        if (!multiblock.mMachine) return Badge.NOT_FORMED;
        return multiblock.getUsed() > 1e-9 ? Badge.RUNNING : Badge.IDLE;
    }

    @Override
    protected String subject() {
        final int mk = multiblock.getMk();
        return mk == 0 ? "" : BTSText.lang(K + "mk", MTEBurstThermaliserCrown.roman(mk));
    }

    @Override
    protected String rightChip() {
        final int mk = multiblock.getMk();
        if (mk == 0) return "";
        final double cap = BurstRouting.crownCap(mk);
        return BTSText.lang(K + "cap", BTSFormat.buCompact(cap), BTSFormat.euSci(cap * BTSFormat.EU_PER_BU));
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        final int mk = multiblock.getMk();
        if (mk > 0) out.add(BTSText.lang(K + "cap.this", BTSFormat.buEu(BurstRouting.crownCap(mk))));
        out.add(BTSText.lang(K + "cap.tip"));
        return out;
    }

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        final MTEBurstThermaliserCrown c = multiblock;
        if (badgeCode() == Badge.NOT_FORMED) {
            rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "notformed"));
            rows.stat(BTSText.lang(K + "lifetime"), BTSText.lit(BTSFormat.eu(c.getLifetimeEU()) + " EU"));
            return;
        }
        final double cap = Math.max(1e-9, BurstRouting.crownCap(c.getMk()));
        rows.bar(
            BTSText.lang(K + "stream"),
            c.getUsed() / cap,
            BTSText.lit(BTSFormat.buCompact(c.getUsed()) + " Bu/t = " + BTSFormat.euOfBu(c.getUsed())),
            Grad.MAGENTA,
            null,
            BTSText.lang(
                K + "stream.tip",
                BTSFormat.buEuShort(c.getStream()),
                BTSFormat.buEuShort(c.getJets()),
                BTSFormat.buEuShort(c.getRemote()),
                BTSFormat.buEuShort(c.getOverIn())),
            BTSText.cat(
                BTSText.lit("§7"),
                BTSText.lang(K + "stream.tip2", BTSFormat.buEuShort(c.getUsed()), BTSFormat.buEuShort(cap))));
        final boolean owner = c.getOwner() != null;
        rows.stat(
            BTSText.lang(K + "power"),
            BTSText.lit(
                (c.getEUt() > 0 ? "§a" : "") + BTSFormat.euSci(c.getEUt())
                    + " EU/t §7("
                    + BTSFormat.euWords(c.getEUt())
                    + ")"),
            BTSText.lang(K + "power.tip"));
        final String shed = c.getShedSource();
        if (!shed.isEmpty()) {
            rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lang(K + "shed"), BTSText.lang(K + "shed.tip", shed));
        }
        if (!owner) {
            rows.check(
                BTSChecklist.State.BAD,
                BTSText.lang(K + "owner"),
                BTSText.lang(K + "owner.none"),
                -1,
                BTSText.lang(K + "owner.tip"));
        }
        if (c.getOver() > 1e-9) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang(K + "overflow", BTSFormat.buEuShort(c.getOver())));
        }
        if (c.getClipped() > 0.005) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang(K + "clipped", BTSFormat.buEuShort(c.getClipped())),
                BTSText.lang(K + "clipped.tip"));
        }
        if (!c.getStackLine()
            .isEmpty()) rows.stat(BTSText.lang(K + "stack"), BTSText.lit(c.getStackLine()));
        final String dock = c.getDockNote();
        rows.stat(
            BTSText.lang(K + "port"),
            dock.isEmpty() ? BTSText.cat(BTSText.lit("§8"), BTSText.lang(K + "port.none"))
                : BTSText.lit(portText(dock)),
            dock.isEmpty() ? BTSText.lang(K + "port.none.tip") : BTSText.lit(dock));
        if (c.receptorCount() > 0) {
            final String line = c.remoteHost()
                .last()
                .line();
            rows.stat(BTSText.lang(K + "receptor"), BTSText.lit(line), BTSText.lit(line));
        }
        rows.stat(
            BTSText.lang(K + "lifetime"),
            BTSText.lit(BTSFormat.eu(c.getLifetimeEU()) + " EU"),
            BTSText.lit(String.format(Locale.ROOT, "%,d EU", c.getLifetimeEU())));
    }

    /** The dock note in a few words: "§btop port" / "§bbottom port", "§6misaligned" (the full note in the tooltip). */
    static String portText(String dock) {
        if (dock.startsWith("misaligned")) return "§6" + tr(K + "port.misaligned");
        if (dock.contains(" top ")) return "§b" + tr(K + "port.top");
        if (dock.contains(" bottom ")) return "§b" + tr(K + "port.bottom");
        return "§b" + dock;
    }

    private static String tr(String key) {
        return net.minecraft.util.StatCollector.translateToLocal(key);
    }

    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        for (String k : new String[] { "power", "dock", "stack", "receptor", "auto" }) {
            rows.para(BTSText.lang(K + "info." + k), BTSGuiKit.VALUE);
        }
    }

    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
    }

    @Override
    protected String powerSwitchDisabledKey() {
        return K + "switch";
    }

    @Override
    protected boolean shouldDisplayVoidExcess() {
        return false;
    }

    @Override
    protected boolean shouldDisplayInputSeparation() {
        return false;
    }

    @Override
    protected boolean shouldDisplayBatchMode() {
        return false;
    }

    @Override
    protected boolean shouldDisplayRecipeLock() {
        return false;
    }
}
