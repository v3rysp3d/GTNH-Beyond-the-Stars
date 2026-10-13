package com.nateyburr.beyondthestars.machines.quasar;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.nateyburr.beyondthestars.items.SLQuasarItems;
import com.nateyburr.beyondthestars.machines.gui.BTSChecklist;
import com.nateyburr.beyondthestars.machines.gui.BTSControllerGui;
import com.nateyburr.beyondthestars.machines.gui.BTSFormat;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Badge;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit.Grad;
import com.nateyburr.beyondthestars.machines.gui.BTSRows;
import com.nateyburr.beyondthestars.machines.gui.BTSText;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarState;

import gregtech.api.enums.ItemList;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.util.GTUtility;

/**
 * P4: the Quasar Engine's console on the controller GUI kit (controller-gui-spec.md §5.4, B-116). The header says the
 * hole's state (NOT FORMED, FROZEN, NO HOLE, DORMANT, RE-FORMING, or the zone of E while it is open), its mass and the
 * coil tier. An open hole shows the E gauge (the Eye's zone bands, ±10 / 40 / 70 / 100: the old dial drew ±30 / ±60),
 * the flux bar (the purge setpoint and the 90 warning as ticks), mass against the coil cap, Integrity, spin and the
 * burst stream, each jet's dock, the feed and the coils, the hosts fed and the last event; a hole that is not open
 * shows the checklist of what Open needs; a re-forming disc its 300 s and the feed it needs. Long text moved to the
 * row tooltips and the <b>i</b> page (the questbook's "Opening the quasar", "Reading the disc" and "Burst Control").
 * Nothing a player needs takes a screwdriver:
 * <ul>
 * <li><b>Gap row:</b> the feed f (a number field with − / +) on the left; the Disc control and Jets &amp; links side
 * panels and <b>i</b> on the right. The render toggle is at the top of the button column; the power switch shows the
 * kit's disabled icon (it does not stop the hole: Collapse does).</li>
 * <li><b>Disc control panel:</b> the gas, the feed f, the field b, the Directive purge setpoint, Purge now, Open (the
 * Quasar Seed, or a Black Hole Seed for a dormant hole, in the controller slot) and Collapse (two clicks).</li>
 * <li><b>Jets &amp; links panel:</b> each port and what it feeds, the hosts fed (as the conduit reports them),
 * counters and milestones.</li>
 * </ul>
 * The rows are decided on the server ({@link BTSRows}); every setter is clamped on the server.
 */
public class MTEQuasarEngineGui extends BTSControllerGui<MTEQuasarEngine> {

    private static final String K = "beyondthestars.gui.quasar.";

    private static final long COLLAPSE_ARM_MS = 4000L;
    private static final int DISC_PANEL_W = 160;
    private static final int DISC_PANEL_H = 244;
    private static final int JETS_PANEL_W = 190;
    private static final int JETS_PANEL_H = 214;

    public MTEQuasarEngineGui(MTEQuasarEngine multiblock) {
        super(multiblock, false, "qRender");
    }

    // ============================================================================= sync values

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        final MTEQuasarEngine m = multiblock;
        syncManager.syncValue("qFormed", new BooleanSyncValue(m::isFormed));
        syncManager.syncValue("qHole", new IntSyncValue(m::holeOrdinal));
        syncManager.syncValue("qCoil", new IntSyncValue(m::getCoilTier));
        syncManager.syncValue("qPhi", new DoubleSyncValue(() -> m.state().Phi));
        for (JetPort port : JetPort.VALUES) {
            syncManager.syncValue("qJet." + port.key, new StringSyncValue(() -> jetLine(m, port)));
        }
        syncManager.syncValue("qLinks", new StringSyncValue(() -> linksText(m)));
        syncManager.syncValue("qCounters", new StringSyncValue(() -> countersLine(m)));
        syncManager.syncValue("qMilestones", new StringSyncValue(() -> milestonesLine(m)));
        syncManager.syncValue("qSlot", new StringSyncValue(() -> slotLine(m)));
        // settable from the client; every setter clamps on the server
        syncManager.syncValue("qFeed", new DoubleSyncValue(m::getFeed, m::setFeed).allowC2S());
        syncManager.syncValue("qGas", new IntSyncValue(m::getGasOrdinal, m::setGasOrdinal).allowC2S());
        syncManager.syncValue("qB", new DoubleSyncValue(m::getBSetting, m::setBSetting).allowC2S());
        syncManager.syncValue("qBHatch", new BooleanSyncValue(m::bFromHatch));
        syncManager.syncValue("qPurgeAt", new DoubleSyncValue(m::getPurgeAt, m::setPurgeAt).allowC2S());
        syncManager.syncValue("qRender", new BooleanSyncValue(m::isRenderEnabled, m::setRenderEnabled).allowC2S());
        // server-only actions: the server re-checks everything
        syncManager.registerServerSyncedAction("qOpen", packet -> m.openHole());
        syncManager.registerServerSyncedAction("qCollapse", packet -> m.collapseHole());
        syncManager.registerServerSyncedAction("qPurge", packet -> m.requestPurge());
    }

    // ===================================================================== server-side lines

    /** One port's line for the Jets &amp; links panel and the jet rows' tooltip (plain text). */
    static String jetLine(MTEQuasarEngine m, JetPort port) {
        final String dock = m.jetDock(port);
        final double bu = m.jetBu(port);
        final String head = tr(K + "jet." + port.key) + " " + MTEQuasarEngine.bu(bu) + " Bu/t -> ";
        if (dock == null) return head + tr(bu > 0 ? K + "jet.venting" : K + "jet.nothing");
        if (m.jetRefused(port)) return head + tr(K + "jet.misaligned", dock);
        final double clipped = bu - m.jetTaken(port);
        return head + dock + (clipped > 0.005 ? " " + tr(K + "jet.clipped", MTEQuasarEngine.bu(clipped)) : "");
    }

    static String linksText(MTEQuasarEngine m) {
        final List<MTEQuasarEngine.HostReport> hosts = m.hostReports();
        if (hosts.isEmpty()) return "";
        final StringBuilder sb = new StringBuilder();
        for (MTEQuasarEngine.HostReport h : hosts) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(hostLine(h));
        }
        return sb.toString();
    }

    static String hostLine(MTEQuasarEngine.HostReport h) {
        return h.label + ": "
            + MTEQuasarEngine.bu(h.bu)
            + " Bu/t, σ "
            + String.format(Locale.ROOT, "%.2f", h.sigma)
            + (h.streak > 0 ? String.format(Locale.ROOT, ", streak %.0f%%", 100 * h.streak) : "");
    }

    static String countersLine(MTEQuasarEngine m) {
        return tr(K + "counters", m.getEruptions(), m.getPurges(), m.getBlowOuts());
    }

    static String milestonesLine(MTEQuasarEngine m) {
        final QuasarState q = m.state();
        final StringBuilder sb = new StringBuilder();
        sb.append(q.firstLight ? "First Light" : "-");
        if (q.firstErupt) sb.append(", First Eruption");
        if (q.eddDone) sb.append(", Eddington hour");
        for (int i = 0; i < QuasarParams.MASS_MILESTONES.length; i++) {
            if ((q.massMilestones & (1 << i)) != 0) {
                sb.append(", ")
                    .append(MTEQuasarEngine.fmt(QuasarParams.MASS_MILESTONES[i]))
                    .append(" SM");
            }
        }
        return sb.toString();
    }

    /** What the controller slot needs now (the Open button's hint), "" when the hole runs. */
    static String slotLine(MTEQuasarEngine m) {
        switch (m.state().hole) {
            case NONE:
                return tr(K + "slot.seed");
            case DORMANT:
                return tr(K + "slot.bhseed");
            default:
                return "";
        }
    }

    private static String tr(String key, Object... args) {
        return args.length == 0 ? StatCollector.translateToLocal(key)
            : StatCollector.translateToLocalFormatted(key, args);
    }

    // =============================================================================== the header

    private QuasarState.Hole hole() {
        return multiblock.state().hole;
    }

    /** §5.4 badge table, first match wins. */
    @Override
    protected Badge badgeCode() {
        if (!multiblock.isFormed()) return Badge.NOT_FORMED;
        if (multiblock.isFrozen()) return Badge.FROZEN;
        switch (hole()) {
            case NONE:
                return Badge.NO_HOLE;
            case DORMANT:
                return Badge.DORMANT;
            case REFORM:
                return Badge.REFORMING;
            default:
                return Badge.ofZone(multiblock.state().zone < 0 ? 1 : multiblock.state().zone);
        }
    }

    @Override
    protected String subject() {
        if (hole() == QuasarState.Hole.NONE) return "";
        return BTSText.lang(K + "hole", BTSFormat.fmt1(multiblock.getMass()));
    }

    @Override
    protected String rightChip() {
        return BTSText.lang(K + "right", multiblock.getCoilTier());
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        out.add(BTSText.lang(K + "right.tip", multiblock.getCoilTier(), MTEQuasarEngine.fmt(multiblock.getMassCap())));
        out.add(BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "right.tip2")));
        return out;
    }

    // ============================================================================= status rows

    @Override
    protected void addStatusRows(BTSRows.Builder rows) {
        switch (badgeCode()) {
            case NOT_FORMED:
                rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "notformed"));
                if (hole() != QuasarState.Hole.NONE) massRow(rows);
                break;
            case FROZEN:
                rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "frozen"), BTSText.lang(K + "frozen.tip"));
                if (hole() != QuasarState.Hole.NONE) massRow(rows);
                break;
            case NO_HOLE:
            case DORMANT:
                closedRows(rows);
                break;
            case REFORMING:
                reformRows(rows);
                break;
            default:
                openRows(rows);
        }
        notesRows(rows);
    }

    /** §5.4 Open. */
    private void openRows(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        final QuasarState q = m.state();
        final int zone = Math.max(0, Math.min(4, q.zone));
        final double e = Math.round(q.E * 10) / 10.0;
        rows.gauge(
            "E",
            e,
            Double.NaN,
            Double.NaN,
            BTSText.lang(
                K + "e.tip",
                BTSFormat.signed(e),
                BTSText.key("beyondthestars.gui.zone." + zone),
                Math.round(QuasarParams.stability(q.E))),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "e.tip2")));
        // flux: the purge setpoint (yellow, when set) and the 90 warning (red) as ticks
        final double phi = q.Phi;
        final boolean erupting = phi >= QuasarParams.ERUPT_WARN;
        final double purgeAt = m.getPurgeAt();
        final String ticks = purgeAt > 0
            ? BTSGuiKit.ticks(
                BTSGuiKit.tick(purgeAt / QuasarParams.PHI_MAX, BTSGuiKit.TICK),
                BTSGuiKit.tick(QuasarParams.ERUPT_WARN / QuasarParams.PHI_MAX, BTSGuiKit.TICK_RED))
            : BTSGuiKit.tick(QuasarParams.ERUPT_WARN / QuasarParams.PHI_MAX, BTSGuiKit.TICK_RED);
        rows.bar(
            BTSText.lang(K + "flux"),
            phi / QuasarParams.PHI_MAX,
            BTSText.lit((erupting ? "§c" : "") + Math.round(phi)),
            Grad.CYAN,
            ticks,
            BTSText.lang(K + "flux.tip", Math.round(phi), String.format(Locale.ROOT, "%.2f", q.lam)),
            purgeAt > 0 ? BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "flux.purge", Math.round(purgeAt)))
                : BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "flux.nopurge")));
        if (erupting) rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lang(K + "erupting"));
        massRow(rows);
        rows.bar(
            BTSText.lang("beyondthestars.gui.kit.integrity"),
            q.I / 100,
            BTSText.lit(BTSGuiKit.healthText(q.I) + Math.round(q.I)),
            BTSGuiKit.healthGrad(q.I),
            null,
            BTSText.lang(
                K + "integrity.tip",
                String.format(Locale.ROOT, "%.2f", q.Yq),
                Math.round(QuasarParams.stability(q.E))));
        final boolean spinUp = m.spinningUp();
        rows.stat2(
            BTSText.lang(K + "spin"),
            BTSText.lit((spinUp ? "§b" : "") + String.format(Locale.ROOT, "%.3f", q.a)),
            BTSText.lang(K + "bursts"),
            BTSText.lit(MTEQuasarEngine.bu(q.G)),
            BTSText.lang(K + "spin.tip", String.format(Locale.ROOT, "%.2f", q.lam), MTEQuasarEngine.bu(q.G)),
            spinUp ? BTSText.cat(BTSText.lit("§b"), BTSText.lang(K + "spinup")) : "");
        jetRows(rows);
        feedRow(rows);
        coilRow(rows);
        linksRow(rows);
    }

    private void massRow(BTSRows.Builder rows) {
        final double mass = multiblock.getMass(), cap = Math.max(1, multiblock.getMassCap());
        rows.bar(
            BTSText.lang(K + "mass"),
            mass / cap,
            BTSText.lit((mass >= cap - 1e-6 ? "§6" : "") + BTSFormat.fmt1(mass) + " SM"),
            Grad.GOLD,
            null,
            BTSText.lang(
                K + "mass.tip",
                BTSFormat.fmt1(mass),
                MTEQuasarEngine.fmt(cap),
                multiblock.getCoilTier(),
                String.format(Locale.ROOT, "%.3f", multiblock.state().a)));
    }

    private void jetRows(BTSRows.Builder rows) {
        for (JetPort port : JetPort.VALUES) {
            final String dock = multiblock.jetDock(port);
            final double bu = multiblock.jetBu(port);
            final String value;
            if (dock == null || multiblock.jetRefused(port)) {
                value = bu > 0 ? "§6" + tr(K + "venting") + " §f" + MTEQuasarEngine.bu(bu) : "§8" + tr(K + "nodock");
            } else {
                final boolean clipped = bu - multiblock.jetTaken(port) > 0.005;
                value = "§b" + shortDock(dock) + " " + (clipped ? "§6" : "§f") + MTEQuasarEngine.bu(bu);
            }
            rows.stat(
                BTSText.lang(K + "jet." + port.key),
                BTSText.lit(value),
                BTSText.lit(jetLine(multiblock, port)),
                BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "jet.tip")));
        }
    }

    /** A dock's name without where it leads ("Wormhole Mouth to 1, 2, 3 (dim 0)" reads "Wormhole Mouth"). */
    static String shortDock(String dock) {
        final int to = dock.indexOf(" to ");
        return to > 0 ? dock.substring(0, to) : dock;
    }

    private void feedRow(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        final QuasarGas gas = m.getGas();
        final double medd = QuasarParams.eddingtonFeed(m.state().M, gas);
        final long buffer = m.buffer(MTEQuasarEngine.gasFluid(gas));
        rows.stat(
            BTSText.lit(gas.displayName + " f " + String.format(Locale.ROOT, "%.2f", m.getFeed())),
            BTSText.lit(BTSFormat.litres(m.getGasDrawn()) + " / " + BTSFormat.litres(medd) + " L/s"),
            BTSText.lang(K + "feed.tip", MTEQuasarEngine.fmt(m.getGasDrawn()), MTEQuasarEngine.fmt(medd)),
            BTSText.lang(K + "feed.buffer", BTSFormat.exact(buffer), BTSFormat.exact(m.bufferCap())),
            BTSText.lang(
                K + "feed.trims",
                String.format(Locale.ROOT, "%.0f", m.getTrimGasUnits()),
                String.format(Locale.ROOT, "%.0f", m.getTrimSpaceUnits())));
    }

    private void coilRow(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        if (!m.isPowerOk()) {
            rows.check(
                BTSChecklist.State.BAD,
                BTSText.lang(K + "coil.short"),
                BTSText.lit("b = 0"),
                -1,
                BTSText.lang(K + "coil.short.tip"));
            return;
        }
        rows.stat(
            BTSText.lang(K + "coils", String.format(Locale.ROOT, "%.2f", m.getBInUse())),
            BTSText.lit(BTSFormat.euT(m.getCoilEUt()) + " EU/t"),
            BTSText.lang(K + "coils.tip", m.getCoilTier()),
            m.bFromHatch() ? BTSText.cat(BTSText.lit("§b"), BTSText.lang(K + "coils.hatch")) : "");
    }

    /** The machines this quasar feeds (as the conduit reports them): a count, each host in the tooltip. */
    private void linksRow(BTSRows.Builder rows) {
        final List<MTEQuasarEngine.HostReport> hosts = multiblock.hostReports();
        if (hosts.isEmpty()) return;
        final List<String> tip = new ArrayList<>();
        for (MTEQuasarEngine.HostReport h : hosts) tip.add(BTSText.lit(hostLine(h)));
        tip.add(BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "links.tip")));
        rows.stat(
            BTSText.lang(K + "links"),
            BTSText.lit("§a" + (hosts.size() == 1 ? hosts.get(0).label : tr(K + "links.n", hosts.size()))),
            tip.toArray(new String[0]));
    }

    /** §5.4 No hole / Dormant: what Open needs, every line met or not. Consumes nothing. */
    private void closedRows(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        final boolean dormant = hole() == QuasarState.Hole.DORMANT;
        rows.section(BTSText.lang(dormant ? K + "sec.reopen" : K + "sec.open"), "");
        final BTSChecklist c = new BTSChecklist();
        final ItemStack slot = m.getControllerSlot();
        final boolean seedIn;
        if (dormant) {
            final ItemStack opener = ItemList.Black_Hole_Opener.get(1L);
            seedIn = slot != null && opener != null && GTUtility.areStacksEqual(slot, opener, true);
        } else {
            seedIn = slot != null && SLQuasarItems.quasarSeed != null && slot.getItem() == SLQuasarItems.quasarSeed;
        }
        c.add(
            seedIn,
            BTSText.lang(dormant ? K + "chk.bhseed" : K + "chk.seed"),
            BTSText.lang(seedIn ? K + "chk.inslot" : K + "chk.slot"),
            -1,
            BTSText.lang(dormant ? K + "chk.bhseed.tip" : K + "chk.seed.tip"));
        final int gas = m.gasHatchCount();
        c.add(
            gas > 0,
            BTSText.lang(K + "chk.gas"),
            BTSText.lit(String.valueOf(gas)),
            -1,
            BTSText.lang(K + "chk.gas.tip"));
        final String power = m.coilPowerKinds();
        c.add(
            !power.isEmpty(),
            BTSText.lang(K + "chk.power"),
            power.isEmpty() ? BTSText.lang(K + "chk.none") : BTSText.lit(power),
            -1,
            BTSText.lang(K + "chk.power.tip"));
        final int coil = m.getCoilTier();
        c.add(
            coil > 0,
            BTSText.lang(K + "chk.coils"),
            coil > 0 ? BTSText.lang(K + "chk.coils.v", coil) : BTSText.lang(K + "chk.coils.mixed"),
            -1,
            BTSText.lang(K + "chk.coils.tip"));
        rows.checks(c);
        if (dormant) massRow(rows);
        rows.para(BTSText.lang(K + "open.hint"), BTSGuiKit.DIM);
    }

    /** §5.4 Re-forming: the 300 s, the feed it needs, the mass; the jets stopped; the blow-out. */
    private void reformRows(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        final int r = m.state().reform;
        rows.bar(
            BTSText.lang(K + "disc"),
            r / (double) QuasarParams.REFORM_SECONDS,
            BTSText.lit(r + "/" + QuasarParams.REFORM_SECONDS + " s"),
            Grad.GOLD,
            null,
            BTSText.lang(K + "disc.tip", QuasarParams.REFORM_SECONDS));
        final double f = m.getFeed();
        final boolean fed = f >= QuasarParams.REFORM_MIN_FEED - 1e-9;
        rows.check(
            fed ? BTSChecklist.State.OK : BTSChecklist.State.BAD,
            BTSText.lang(K + "chk.feed"),
            BTSText.lit(String.format(Locale.ROOT, "%.2f", f)),
            -1,
            BTSText.lang(K + "chk.feed.tip"));
        massRow(rows);
        rows.stat(BTSText.lang(K + "jets"), BTSText.cat(BTSText.lit("§8"), BTSText.lang(K + "stopped")));
        final String ev = m.getLastEvent();
        if (ev != null && !ev.isEmpty()) rows.note(BTSRows.ICON_BAD, BTSGuiKit.BAD, BTSText.lit(ev));
    }

    private void notesRows(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        final String conduit = m.conduitClosedWhere();
        if (!conduit.isEmpty()) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang(K + "conduit", conduit),
                BTSText.lang(K + "conduit.tip"));
        }
        final String ev = m.getLastEvent();
        if (hole() != QuasarState.Hole.REFORM && ev != null && !ev.isEmpty()) {
            rows.note(BTSRows.ICON_NONE, BTSGuiKit.DIM, BTSText.lang("beyondthestars.gui.kit.last", ev));
        }
        final String render = m.getRenderNote();
        if (render != null && !render.isEmpty()) rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lit(render));
    }

    // ================================================================================= Info page

    /** §5.4 Info: the questbook's "Opening the quasar", "Reading the disc" and "Burst Control". */
    @Override
    protected void addInfoRows(BTSRows.Builder rows) {
        for (String k : new String[] { "open", "feed", "disc", "trims", "flux", "coils", "jets", "burst", "auto" }) {
            rows.para(BTSText.lang(K + "info." + k), BTSGuiKit.VALUE);
        }
    }

    // ============================================================================ button column

    /** The power switch does not stop the hole (Collapse does): the kit's disabled icon, with this tooltip. */
    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
    }

    @Override
    protected String powerSwitchDisabledKey() {
        return K + "switch";
    }

    @Override
    protected void renderTooltip(RichTooltip t) {
        t.addLine(tr(K + "render.tip1"));
        t.addLine(EnumChatFormatting.GRAY + tr(K + "render.tip2"));
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

    // ================================================================================ gap row

    private static double clampFeed(double v) {
        return Math.max(QuasarParams.F_MIN, Math.min(QuasarParams.F_MAX, v));
    }

    private static TextFieldWidget feedField(DoubleSyncValue feed) {
        return new TextFieldWidget().numbersDouble(MTEQuasarEngineGui::clampFeed)
            .value(new DoubleValue.Dynamic(feed::getDoubleValue, v -> {
                final double c = clampFeed(v);
                if (c != feed.getDoubleValue()) feed.setDoubleValue(c);
            }))
            .setTextAlignment(Alignment.Center)
            .tooltip(
                t -> t.addLine(tr(K + "feed.field.tip1"))
                    .addLine(EnumChatFormatting.GRAY + tr(K + "feed.field.tip2"))
                    .addLine(EnumChatFormatting.GRAY + tr(K + "feed.field.tip3")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Left: the Feed f stepper (§5.4): − field + ; right-click steps 0.25. */
    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final DoubleSyncValue feed = syncManager.findSyncHandler("qFeed", DoubleSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.lang(K + "feedf")
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .marginRight(2)
                    .heightRel(1f))
            .child(stepButton(feed, -0.05))
            .child(
                feedField(feed).width(32)
                    .height(14))
            .child(stepButton(feed, 0.05));
    }

    private static IWidget stepButton(DoubleSyncValue feed, double step) {
        return new ButtonWidget<>().size(9, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final double d = button == 1 ? step * 5 : step;
                final double next = clampFeed(Math.round((feed.getDoubleValue() + d) * 100) / 100.0);
                if (next != feed.getDoubleValue()) feed.setDoubleValue(next);
                return true;
            })
            .tooltip(t -> t.addLine(tr(step < 0 ? K + "feed.minus" : K + "feed.plus")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    /** Right, on screen left to right: [Disc control] [Jets &amp; links] [i] (the row is reversed). */
    @Override
    protected void addRightButtons(Flow row, ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler[] panels = new IPanelHandler[2];
        panels[0] = syncManager.syncedPanel("qDiscPanel", true, (sub, handler) -> buildDiscPanel(syncManager, parent));
        panels[1] = syncManager.syncedPanel("qJetsPanel", true, (sub, handler) -> buildJetsPanel(syncManager, parent));
        row.child(panelButton(panels, 1, GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS, K + "button.jets"))
            .child(panelButton(panels, 0, GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED, K + "button.disc"));
    }

    /** Toggles side panel {@code which}; opening it closes the other (they share one spot). */
    private static IWidget panelButton(IPanelHandler[] panels, int which, IDrawable icon, String tooltipKey) {
        return new ButtonWidget<>().size(18, 18)
            .marginLeft(2)
            .overlay(icon)
            .onMousePressed(button -> {
                final IPanelHandler panel = panels[which];
                if (panel.isPanelOpen()) {
                    panel.closePanel();
                } else {
                    for (int i = 0; i < panels.length; i++) {
                        if (i != which && panels[i].isPanelOpen()) panels[i].closePanel();
                    }
                    panel.openPanel();
                }
                return true;
            })
            .tooltip(t -> t.addLine(IKey.lang(tooltipKey)))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ============================================================================ panel parts

    /** One line of panel text at scale 1.0 that scrolls on hover instead of wrapping. */
    private static ScrollingTextWidget panelLine(Supplier<String> text, int width) {
        return new ScrollingTextWidget(IKey.dynamic(text::get)).width(width)
            .height(9)
            .color(BTSGuiKit.PANEL_TEXT)
            .shadow(false);
    }

    /** Wrapped panel text at scale 1.0, {@code lines} lines high. */
    private static TextWidget<?> panelPara(Supplier<String> text, int width, int lines) {
        return IKey.dynamic(text::get)
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .textAlign(Alignment.TopLeft)
            .size(width, 9 * lines);
    }

    private static IWidget title(String key, int width) {
        return IKey.lang(key)
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .style(EnumChatFormatting.BOLD)
            .width(width)
            .textAlign(Alignment.Center);
    }

    private static IWidget heading(String key) {
        return IKey.lang(key)
            .color(BTSGuiKit.PANEL_TEXT)
            .asWidget()
            .style(EnumChatFormatting.BOLD);
    }

    /** "label ......... value" across the panel, both at 1.0. */
    private static IWidget keyValue(Supplier<String> label, Supplier<String> value, int width) {
        return Flow.row()
            .width(width)
            .height(9)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(
                IKey.dynamic(label::get)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget())
            .child(
                IKey.dynamic(value::get)
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget());
    }

    /** GT's theme gives a slider no track (dev client round 1, the Kerr panel): a sunken one. */
    private static final IDrawable SLIDER_TRACK = (context, x, y, width, height, theme) -> {
        GuiDraw.drawRect(x, y, width, height, 0xFF373737);
        GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, 0xFF8B8B8B);
    };

    // ======================================================================= disc control panel

    private ModularPanel buildDiscPanel(PanelSyncManager main, ModularPanel parent) {
        final BooleanSyncValue formed = main.findSyncHandler("qFormed", BooleanSyncValue.class);
        final IntSyncValue hole = main.findSyncHandler("qHole", IntSyncValue.class);
        final IntSyncValue coil = main.findSyncHandler("qCoil", IntSyncValue.class);
        final DoubleSyncValue feed = main.findSyncHandler("qFeed", DoubleSyncValue.class);
        final IntSyncValue gas = main.findSyncHandler("qGas", IntSyncValue.class);
        final DoubleSyncValue b = main.findSyncHandler("qB", DoubleSyncValue.class);
        final BooleanSyncValue bHatch = main.findSyncHandler("qBHatch", BooleanSyncValue.class);
        final DoubleSyncValue purgeAt = main.findSyncHandler("qPurgeAt", DoubleSyncValue.class);
        final DoubleSyncValue phi = main.findSyncHandler("qPhi", DoubleSyncValue.class);
        final StringSyncValue slot = main.findSyncHandler("qSlot", StringSyncValue.class);
        final int w = DISC_PANEL_W - 8;
        final int third = (w - 6) / 3;

        final long[] armedAt = { 0L };
        final BooleanSupplier running = () -> hole.getIntValue() == QuasarState.Hole.OPEN.ordinal()
            || hole.getIntValue() == QuasarState.Hole.REFORM.ordinal();
        final BooleanSupplier armed = () -> running.getAsBoolean()
            && System.currentTimeMillis() - armedAt[0] < COLLAPSE_ARM_MS;
        final BooleanSupplier canOpen = () -> formed.getBoolValue() && !running.getAsBoolean();
        final BooleanSupplier canPurge = () -> running.getAsBoolean() && phi.getDoubleValue() >= QuasarParams.PURGE_MIN;

        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(title(K + "panel.disc.title", w))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(
                        new ButtonWidget<>().size(14, 14)
                            .overlay(GTGuiTextures.OVERLAY_BUTTON_CYCLIC)
                            .onMousePressed(btn -> {
                                gas.setIntValue(Math.floorMod(gas.getIntValue() + 1, QuasarGas.values().length));
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.gas.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.gas.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.gas.tip3"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.gas.tip4"))))
                    .child(
                        panelLine(
                            () -> tr(
                                K + "panel.gas",
                                QuasarGas.values()[Math
                                    .floorMod(gas.getIntValue(), QuasarGas.values().length)].displayName),
                            w - 17)))
            .child(keyValue(() -> tr(K + "feedf"), () -> String.format(Locale.ROOT, "%.2f", feed.getDoubleValue()), w))
            .child(
                new SliderWidget().bounds(QuasarParams.F_MIN, QuasarParams.F_MAX)
                    .value(new DoubleValue.Dynamic(feed::getDoubleValue, v -> {
                        final double c = Math.round(v * 100) / 100.0;
                        if (c != feed.getDoubleValue()) feed.setDoubleValue(c);
                    }))
                    .background(SLIDER_TRACK)
                    .size(w, 10))
            .child(
                keyValue(
                    () -> tr(K + "panel.b"),
                    () -> bHatch.getBoolValue() ? tr(K + "panel.b.hatch")
                        : tr(
                            K + "panel.b.v",
                            String.format(Locale.ROOT, "%.2f", b.getDoubleValue()),
                            coil.getIntValue()),
                    w))
            .child(
                new SliderWidget().bounds(QuasarParams.B_MIN, 4)
                    .value(new DoubleValue.Dynamic(b::getDoubleValue, v -> {
                        final double c = Math.round(v * 20) / 20.0;
                        if (c != b.getDoubleValue()) b.setDoubleValue(c);
                    }))
                    .background(SLIDER_TRACK)
                    .size(w, 10)
                    .tooltip(
                        t -> t.addLine(tr(K + "panel.b.tip1"))
                            .addLine(EnumChatFormatting.GRAY + tr(K + "panel.b.tip2")))
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                keyValue(
                    () -> tr(K + "panel.purge"),
                    () -> purgeAt.getDoubleValue() <= 0 ? tr(K + "panel.purge.off")
                        : tr(K + "panel.purge.v", Math.round(purgeAt.getDoubleValue())),
                    w))
            .child(
                new SliderWidget().bounds(0, 100)
                    .value(new DoubleValue.Dynamic(purgeAt::getDoubleValue, v -> {
                        final double c = Math.round(v);
                        if (c != purgeAt.getDoubleValue()) purgeAt.setDoubleValue(c);
                    }))
                    .background(SLIDER_TRACK)
                    .size(w, 10)
                    .tooltip(
                        t -> t.addLine(tr(K + "panel.purge.tip1"))
                            .addLine(EnumChatFormatting.GRAY + tr(K + "panel.purge.tip2")))
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                Flow.row()
                    .width(w)
                    .height(16)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(
                        new ButtonWidget<>().size(third, 16)
                            .overlay(
                                IKey.dynamic(
                                    () -> (canOpen.getAsBoolean() ? EnumChatFormatting.WHITE
                                        : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.open")))
                            .onMousePressed(btn -> {
                                if (canOpen.getAsBoolean()) main.callSyncedAction("qOpen", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.open.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.open.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.open.tip3")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(
                        new ButtonWidget<>().size(third, 16)
                            .overlay(
                                IKey.dynamic(
                                    () -> (canPurge.getAsBoolean() ? EnumChatFormatting.WHITE
                                        : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.purgenow")))
                            .onMousePressed(btn -> {
                                if (canPurge.getAsBoolean()) main.callSyncedAction("qPurge", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.purgenow.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.purgenow.tip2")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(
                        new ButtonWidget<>().size(third, 16)
                            .overlay(IKey.dynamic(() -> {
                                if (armed.getAsBoolean()) return EnumChatFormatting.RED + tr(K + "panel.confirm");
                                return (running.getAsBoolean() ? EnumChatFormatting.WHITE
                                    : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.collapse");
                            }))
                            .onMousePressed(btn -> {
                                if (!running.getAsBoolean()) {
                                    armedAt[0] = 0L;
                                } else if (!armed.getAsBoolean()) {
                                    armedAt[0] = System.currentTimeMillis();
                                } else {
                                    armedAt[0] = 0L;
                                    main.callSyncedAction("qCollapse", packet -> {});
                                }
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.collapse.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.collapse.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.collapse.tip3")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY)))
            .child(panelPara(() -> {
                if (armed.getAsBoolean()) return EnumChatFormatting.DARK_RED + tr(K + "panel.hint.armed");
                final String s = slot.getStringValue();
                if (!s.isEmpty()) return s;
                return EnumChatFormatting.DARK_GRAY + tr(K + "panel.hint.purge");
            }, w, 2))
            .child(panelPara(() -> EnumChatFormatting.DARK_GRAY + tr(K + "panel.hint.trims"), w, 4));

        return new ModularPanel("qDiscPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(DISC_PANEL_W, DISC_PANEL_H)
            .child(column);
    }

    // =========================================================================== jets panel

    /** The terminal's colours do not read on GT's grey panels. */
    private static String strip(String s) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(s);
    }

    private ModularPanel buildJetsPanel(PanelSyncManager main, ModularPanel parent) {
        final StringSyncValue top = main.findSyncHandler("qJet.top", StringSyncValue.class);
        final StringSyncValue bottom = main.findSyncHandler("qJet.bottom", StringSyncValue.class);
        final StringSyncValue links = main.findSyncHandler("qLinks", StringSyncValue.class);
        final StringSyncValue counters = main.findSyncHandler("qCounters", StringSyncValue.class);
        final StringSyncValue milestones = main.findSyncHandler("qMilestones", StringSyncValue.class);
        final int w = JETS_PANEL_W - 8;
        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(title(K + "panel.jets.title", w))
            .child(panelLine(() -> strip(top.getStringValue()), w))
            .child(panelLine(() -> strip(bottom.getStringValue()), w))
            .child(panelPara(() -> EnumChatFormatting.DARK_GRAY + tr(K + "panel.jets.hint"), w, 3))
            .child(heading(K + "panel.hosts"))
            .child(panelPara(() -> {
                final String l = links.getStringValue();
                return l.isEmpty() ? EnumChatFormatting.DARK_GRAY + tr(K + "panel.hosts.none") : l;
            }, w, 4))
            .child(heading(K + "panel.record"))
            .child(panelLine(counters::getStringValue, w))
            .child(panelPara(() -> tr(K + "panel.milestones", milestones.getStringValue()), w, 2));
        return new ModularPanel("qJetsPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(JETS_PANEL_W, JETS_PANEL_H)
            .child(column);
    }
}
