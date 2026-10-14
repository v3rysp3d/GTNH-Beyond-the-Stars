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
import com.nateyburr.beyondthestars.Config;
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
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarResult;
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
 * <li><b>Supermassive Lattice panel</b> (SMBH, smbh-v1.md §2-§6, §10.2): the lattice in force, the mass setpoint
 * (a log slider from 4,096 SM to the top, and a number field), Overdrive (0-100 % in 5 % steps) with its upkeep, Shed
 * to
 * the safe limit, and Unbind (two clicks; L4 and the config).</li>
 * </ul>
 * With a lattice the terminal adds its section: Saturation or "Reach Saturation first", the band (x, with the setpoint
 * tick), the <b>intake</b> as one stacked bar (Disc white, Hold-up amber with a red gap when short, Growth green; the
 * red tick is the 5x cap) with the line under it (the split, then the mass change per hour, or the leak), the
 * Accretion Buffer's seconds, Overdrive, Unbound and a shed. <b>Every Bu/t shows its EU/t beside it</b> (§10.3, one
 * formatter: {@link BTSFormat#buEu}).
 * <p>
 * The rows are decided on the server ({@link BTSRows}); every setter is clamped on the server.
 */
public class MTEQuasarEngineGui extends BTSControllerGui<MTEQuasarEngine> {

    private static final String K = "beyondthestars.gui.quasar.";

    private static final long COLLAPSE_ARM_MS = 4000L;
    private static final int DISC_PANEL_W = 160;
    private static final int DISC_PANEL_H = 244;
    private static final int JETS_PANEL_W = 190;
    private static final int JETS_PANEL_H = 214;
    private static final int HORIZON_PANEL_W = 176;
    private static final int HORIZON_PANEL_H = 214;

    /**
     * The intake bar's segment colours (§2.3): Disc white, Hold-up amber, the hold-up's shortfall red, Growth green.
     */
    static final int SEG_DISC = 0xFFE6E6E6, SEG_HOLD = 0xFFFFAA00, SEG_SHORT = 0xFFB0302A, SEG_GROWTH = 0xFF55DD55;
    /** The intake bar's full scale, x the 5x cap, so the cap's red tick stands inside the bar. */
    static final double INTAKE_SCALE = 1.1;

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
        // SMBH (smbh-v1.md §2.2, §3.2, §5.1): the Supermassive Lattice panel
        syncManager.syncValue("qLattice", new IntSyncValue(m::getLatticeInForce));
        syncManager.syncValue("qSetpoint", new DoubleSyncValue(m::getMassCap, m::setMassSetpoint).allowC2S());
        syncManager.syncValue("qSlideTop", new DoubleSyncValue(() -> sliderTop(m)));
        syncManager.syncValue("qOverdrive", new DoubleSyncValue(m::getOverdrive, m::setOverdrive).allowC2S());
        syncManager.syncValue("qUnbound", new BooleanSyncValue(m::isUnbound));
        syncManager.syncValue(
            "qCanUnbind",
            new BooleanSyncValue(() -> Config.smbhAllowUnbound && m.getLatticeInForce() == QuasarParams.LATTICE_MAX));
        syncManager.syncValue("qHoldHatch", new BooleanSyncValue(m::holdFromHatch));
        syncManager.syncValue("qOdHatch", new BooleanSyncValue(m::overdriveFromHatch));
        syncManager.syncValue("qLatticeLine", new StringSyncValue(() -> latticeLine(m)));
        syncManager.syncValue("qOdLine", new StringSyncValue(() -> overdriveLine(m)));
        syncManager.syncValue("qPanelNote", new StringSyncValue(() -> panelNote(m)));
        syncManager.registerServerSyncedAction("qShed", packet -> m.shedToSafe());
        syncManager.registerServerSyncedAction("qUnbind", packet -> m.unbind(true));
        syncManager.registerServerSyncedAction("qBind", packet -> m.unbind(false));
    }

    // ===================================================================== SMBH helpers (server)

    /** The setpoint slider's top, SM: the band top; on an Unbound L4, four doublings past the L4 top. */
    static double sliderTop(MTEQuasarEngine m) {
        final int L = m.getLatticeInForce();
        if (L == 0) return QuasarParams.M_REF * 2;
        if (L == QuasarParams.LATTICE_MAX && m.isUnbound()) return QuasarParams.LATTICE_TOP[L] * 16;
        return QuasarParams.LATTICE_TOP[L];
    }

    /** The panel's lattice line: "L3 Universium: safe 262,144, top 4,194,304 SM" (or what is missing). */
    static String latticeLine(MTEQuasarEngine m) {
        final int L = m.getLatticeInForce();
        if (L == 0) {
            if (m.getLatticeBuilt() > 0 && !m.isSaturated()) return tr(K + "lat.notyet", m.getLatticeBuilt());
            return tr(K + "lat.none");
        }
        return tr(
            K + "lat.line",
            L,
            QuasarParams.LATTICE_NAME[L],
            MTEQuasarEngine.fmt(QuasarParams.LATTICE_SAFE[L]),
            MTEQuasarEngine.fmt(QuasarParams.LATTICE_TOP[L]));
    }

    /** The panel's Overdrive line: the upkeep each and the output it lifts to, or why it is idle. */
    static String overdriveLine(MTEQuasarEngine m) {
        if (m.getLatticeInForce() < QuasarParams.LATTICE_OVERDRIVE) return tr(K + "od.needs");
        final QuasarResult r = m.lastResult();
        final double o = m.getOverdriveInUse();
        if (o <= 0) return tr(K + "od.off");
        final double need = QuasarParams.overdriveUpkeep(o, r.n);
        return tr(
            K + "od.line",
            BTSFormat.litres(need),
            String.format(Locale.ROOT, "%.1f", QuasarParams.outputX(m.getMass(), o)),
            BTSFormat.pct(r.overdriveNeed > 0 ? r.overdriveCoverage : 0));
    }

    /** The panel's last line: a hatch in control, a shed, the anchor, or the last Unbind answer. */
    static String panelNote(MTEQuasarEngine m) {
        if (m.holdFromHatch()) return tr(K + "lat.holdhatch");
        if (m.overdriveFromHatch()) return tr(K + "od.hatch");
        final QuasarResult r = m.lastResult();
        if (r.anchorShort) return tr(K + "anchor.short", String.format(Locale.ROOT, "%.2f", r.anchorPaysN));
        if (m.state().shed) return tr(K + "shed.v", MTEQuasarEngine.fmt(r.shedTarget));
        return "";
    }

    /** "+2.2 % per hour" for a fraction of M per hour (two significant figures under 0.1 %). */
    static String perHour(double frac) {
        final double pct = 100 * frac;
        if (Math.abs(pct) < 1e-4) return "0 %";
        return (Math.abs(pct) >= 0.1 ? String.format(Locale.ROOT, "%+.1f", pct)
            : String.format(Locale.ROOT, "%+.2g", pct)) + " %";
    }

    // ===================================================================== server-side lines

    /** One port's line for the Jets &amp; links panel and the jet rows' tooltip (plain text). */
    static String jetLine(MTEQuasarEngine m, JetPort port) {
        final String dock = m.jetDock(port);
        final double bu = m.jetBu(port);
        final String head = tr(K + "jet." + port.key) + " " + BTSFormat.buEuShort(bu) + " -> ";
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
            + BTSFormat.buEuShort(h.bu)
            + ", σ "
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
        final int L = multiblock.getLatticeInForce();
        if (L > 0) return BTSText.lang(K + "right.lattice", multiblock.getCoilTier(), L);
        return BTSText.lang(K + "right", multiblock.getCoilTier());
    }

    @Override
    protected List<String> rightChipTooltip() {
        final List<String> out = new ArrayList<>();
        out.add(BTSText.lang(K + "right.tip", multiblock.getCoilTier(), MTEQuasarEngine.fmt(multiblock.getMassCap())));
        out.add(BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "right.tip2")));
        final int L = multiblock.getLatticeInForce();
        if (L > 0) {
            out.add(
                BTSText.lang(
                    K + "right.tip3",
                    L,
                    QuasarParams.LATTICE_NAME[L],
                    MTEQuasarEngine.fmt(QuasarParams.LATTICE_SAFE[L]),
                    MTEQuasarEngine.fmt(QuasarParams.LATTICE_TOP[L])));
        }
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
        final boolean past = m.getMass() > QuasarParams.M_REF * (1 + 1e-9) || m.getLatticeInForce() > 0 || q.oEff > 0;
        final String spinTip = BTSText.lang(K + "spin.tip", String.format(Locale.ROOT, "%.2f", q.lam));
        final String spinUpTip = spinUp ? BTSText.cat(BTSText.lit("§b"), BTSText.lang(K + "spinup")) : "";
        if (past) {
            rows.stat2(
                BTSText.lang(K + "spin"),
                BTSText.lit((spinUp ? "§b" : "") + String.format(Locale.ROOT, "%.3f", q.a)),
                BTSText.lang(K + "output"),
                BTSText.lit(String.format(Locale.ROOT, "x%.1f", m.getOutputX())),
                spinTip,
                BTSText.lang(K + "output.tip", String.format(Locale.ROOT, "%.2f", m.getOutputX())),
                spinUpTip);
        } else {
            rows.stat(
                BTSText.lang(K + "spin"),
                BTSText.lit((spinUp ? "§b" : "") + String.format(Locale.ROOT, "%.3f", q.a)),
                spinTip,
                spinUpTip);
        }
        burstsRow(rows, q.G);
        jetRows(rows);
        feedRow(rows);
        coilRow(rows);
        latticeRows(rows);
        linksRow(rows);
    }

    /** The stream, both jets: "9,796 Bu/t = 9.8e16 EU/t", the words in the tooltip (§10.3). */
    private void burstsRow(BTSRows.Builder rows, double g) {
        rows.stat(
            BTSText.lang(K + "bursts"),
            BTSText.lit(MTEQuasarEngine.bu(g) + " Bu/t §7= §f" + BTSFormat.euOfBu(g)),
            BTSText.lang(K + "bursts.tip", BTSFormat.buEu(g)),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "bursts.tip2")));
    }

    /**
     * SMBH (smbh-v1.md §1.2, §2.3, §3.2, §5): the lattice section. Saturation while it is not held on MagMatter coils;
     * with a lattice in force the band, the intake bar and its line, the buffer, Overdrive, Unbound and a shed.
     */
    private void latticeRows(BTSRows.Builder rows) {
        final MTEQuasarEngine m = multiblock;
        final QuasarResult r = m.lastResult();
        final int L = m.getLatticeInForce();
        final boolean saturationShown = !m.isSaturated() && m.getCoilTier() >= QuasarParams.LATTICE_COIL;
        if (L == 0 && !saturationShown && m.getLatticeBuilt() == 0) return;
        rows.section(
            BTSText.lang(K + "sec.lattice"),
            L > 0 ? BTSText.lang(K + "sec.lattice.v", L, QuasarParams.LATTICE_NAME[L]) : "");
        if (!m.isSaturated()) {
            final int sec = m.getSaturationSeconds();
            final boolean built = m.getLatticeBuilt() > 0;
            rows.check(
                built ? BTSChecklist.State.BAD : BTSChecklist.State.WARN,
                BTSText.lang(built ? K + "chk.saturation.first" : K + "chk.saturation"),
                BTSText.lit(BTSFormat.duration(sec) + " / 1h 0m"),
                sec / (double) MTEQuasarEngine.SATURATION_SECONDS,
                BTSText.lang(K + "chk.saturation.tip"),
                BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "chk.saturation.tip2")));
            return;
        }
        if (L == 0) {
            if (m.getLatticeBuilt() > 0) rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lang(K + "lat.coils"));
            return;
        }
        // the band: x, the setpoint as a tick
        final double safe = QuasarParams.LATTICE_SAFE[L], top = QuasarParams.LATTICE_TOP[L];
        final double sp = m.getMassCap();
        final double spX = sp <= safe ? 0 : sp >= top ? 1 : Math.log(sp / safe) / Math.log(top / safe);
        rows.bar(
            BTSText.lang(K + "band"),
            r.x,
            BTSText.lit(String.format(Locale.ROOT, "x %.2f", r.x)),
            Grad.GOLD,
            BTSGuiKit.tick(spX, BTSGuiKit.AQUA),
            BTSText.lang(
                K + "band.tip",
                MTEQuasarEngine.fmt(m.getMass()),
                MTEQuasarEngine.fmt(safe),
                MTEQuasarEngine.fmt(top),
                MTEQuasarEngine.fmt(sp)),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "band.tip2")));
        // the intake: Disc, Hold-up (its shortfall red), Growth, on 1.1x the 5x cap
        final double cap = Math.max(1e-9, r.intakeCap);
        final double full = cap * INTAKE_SCALE;
        final double disc = m.getGasDrawn(), hold = r.holdUpLitres, grow = r.growthLitres;
        final double shortL = Math.max(0, r.holdUpNeed - hold);
        final double total = disc + hold + grow;
        rows.stack(
            BTSText.lang(K + "intake"),
            new double[][] { { disc / full, SEG_DISC }, { hold / full, SEG_HOLD }, { shortL / full, SEG_SHORT },
                { grow / full, SEG_GROWTH } },
            BTSText.lit(BTSFormat.pct(total / cap)),
            BTSGuiKit.tick(1 / INTAKE_SCALE, BTSGuiKit.TICK_RED),
            BTSText.lang(K + "intake.tip", BTSFormat.litres(total), BTSFormat.litres(cap), m.getGas().displayName),
            BTSText.lang(
                K + "intake.tip.t",
                BTSFormat.litres(total / 20),
                BTSFormat.litres(cap / 20),
                BTSFormat.litres(disc / 20),
                BTSFormat.litres(hold / 20),
                BTSFormat.litres(grow / 20)),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "intake.tip2")),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "intake.tip3")));
        final boolean shed = m.state().shed;
        final String split = BTSText.lang(
            shed ? K + "intake.line.shed" : K + "intake.line",
            BTSFormat.litres(disc),
            BTSFormat.litres(hold),
            r.holdUpNeed > 0 ? BTSFormat.pct(r.holdUpCoverage) : "-",
            BTSFormat.litres(grow),
            perHour(m.getMassRatePerHour()));
        rows.note(BTSRows.ICON_NONE, BTSGuiKit.VALUE, split, BTSText.lang(K + "intake.line.tip"));
        if (r.leaking) {
            rows.note(
                BTSRows.ICON_BAD,
                BTSGuiKit.BAD,
                BTSText.lang(K + "leaking", perHour(m.getLeakRatePerHour()), BTSFormat.pct(r.holdUpCoverage)),
                BTSText.lang(K + "leaking.tip"));
        }
        // the Accretion Buffer
        final double bufS = m.getBufferSeconds();
        final boolean bufShort = bufS < QuasarParams.FEED_SHORT_SECONDS;
        rows.stat(
            BTSText.lang(K + "buffer"),
            BTSText.lit((bufShort ? "§c" : "") + (Double.isInfinite(bufS) ? "-" : BTSFormat.duration((long) bufS))),
            BTSText.lang(
                K + "buffer.tip",
                BTSFormat.exact(m.buffer(MTEQuasarEngine.gasFluid(m.getGas()))),
                BTSFormat.exact(m.bufferCap())),
            BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "buffer.tip2")));
        // Overdrive (L3+)
        final double o = m.getOverdriveInUse();
        if (L >= QuasarParams.LATTICE_OVERDRIVE) {
            final boolean shortOd = !shed && r.overdriveNeed > 0 && r.overdriveCoverage < 1 - 1e-9;
            final double need = QuasarParams.overdriveUpkeep(o, r.n);
            rows.bar(
                BTSText.lang(K + "overdrive"),
                o,
                BTSText.lit((shortOd ? "§c" : "") + BTSFormat.pct(o)),
                Grad.LAVENDER,
                null,
                BTSText.lang(
                    K + "overdrive.tip",
                    BTSFormat.pct(o),
                    BTSFormat.litres(need),
                    BTSFormat.pct(r.overdriveNeed > 0 ? r.overdriveCoverage : 0)),
                BTSText.lang(
                    K + "overdrive.tip.x",
                    String.format(Locale.ROOT, "%.1f", QuasarParams.outputX(m.getMass(), 0)),
                    String.format(Locale.ROOT, "%.1f", QuasarParams.outputX(m.getMass(), o))),
                BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "overdrive.tip2")),
                m.overdriveFromHatch() ? BTSText.cat(BTSText.lit("§b"), BTSText.lang(K + "od.hatch")) : "");
            if (o > 0 && shed) {
                rows.stat(BTSText.lang(K + "upkeep"), BTSText.lang(K + "upkeep.shed"), BTSText.lang(K + "upkeep.tip"));
            } else if (o > 0) {
                rows.stat(
                    BTSText.lang(K + "upkeep"),
                    BTSText.lit(
                        (shortOd ? "§c" : "") + BTSFormat.litres(need)
                            + " L/s §7each, §f"
                            + BTSFormat.pct(r.overdriveNeed > 0 ? r.overdriveCoverage : 0)
                            + " paid"),
                    BTSText.lang(K + "upkeep.tip"));
            }
        } else if (o > 0) {
            rows.note(BTSRows.ICON_WARN, BTSGuiKit.WARN, BTSText.lang(K + "od.needs"));
        }
        // Unbound (L4)
        if (m.isUnbound() && L == QuasarParams.LATTICE_MAX) {
            rows.stat(
                BTSText.lang(K + "unbound"),
                BTSText.lit(
                    String.format(Locale.ROOT, "n %.2f §7anchor §f", r.n) + BTSFormat.litres(r.anchorNeed) + " L/s"),
                BTSText.lang(
                    K + "unbound.tip",
                    String.format(Locale.ROOT, "%.2f", r.n),
                    BTSFormat.litres(r.anchorNeed),
                    BTSFormat.pct(r.anchorCoverage)),
                BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "unbound.tip2")));
            if (r.anchorShort) {
                rows.note(
                    BTSRows.ICON_BAD,
                    BTSGuiKit.BAD,
                    BTSText.lang(K + "anchor.short", String.format(Locale.ROOT, "%.2f", r.anchorPaysN)),
                    BTSText.lang(K + "anchor.short.tip"));
            }
        }
        if (m.state().shed) {
            rows.note(
                BTSRows.ICON_WARN,
                BTSGuiKit.WARN,
                BTSText.lang(
                    K + "shedding",
                    MTEQuasarEngine.fmt(r.shedTarget),
                    BTSFormat.duration(Math.round(QuasarParams.shedSeconds(m.getMass(), r.shedTarget)))),
                BTSText.lang(K + "shedding.tip", MTEQuasarEngine.shedWhy(r.shedReason)));
        }
    }

    private void massRow(BTSRows.Builder rows) {
        final double mass = multiblock.getMass(), cap = Math.max(1, multiblock.getMassCap());
        final int L = multiblock.getLatticeInForce();
        if (L > 0) {
            // with a lattice: a log scale from 4,096 SM to the band top (an Unbound hole: four doublings past it);
            // the yellow tick is the safe limit, the aqua one the mass setpoint
            final double lo = QuasarParams.M_REF, hi = sliderTop(multiblock);
            final double span = Math.log(hi / lo);
            final double f = mass <= lo ? 0 : Math.log(mass / lo) / span;
            rows.bar(
                BTSText.lang(K + "mass"),
                f,
                BTSText.lit(BTSFormat.fmt1(mass) + " SM"),
                Grad.GOLD,
                BTSGuiKit.ticks(
                    BTSGuiKit.tick(Math.log(QuasarParams.LATTICE_SAFE[L] / lo) / span, BTSGuiKit.TICK),
                    BTSGuiKit.tick(Math.log(Math.max(lo, cap) / lo) / span, BTSGuiKit.AQUA)),
                BTSText.lang(
                    K + "mass.tip.lattice",
                    BTSFormat.fmt1(mass),
                    MTEQuasarEngine.fmt(cap),
                    MTEQuasarEngine.fmt(QuasarParams.LATTICE_SAFE[L]),
                    MTEQuasarEngine.fmt(QuasarParams.LATTICE_TOP[L])),
                BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "mass.tip.lattice2")));
            return;
        }
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
                value = bu > 0
                    ? "§6" + tr(K + "venting") + " §f" + MTEQuasarEngine.bu(bu) + " §7= §f" + BTSFormat.euOfBu(bu)
                    : "§8" + tr(K + "nodock");
            } else {
                final boolean clipped = bu - multiblock.jetTaken(port) > 0.005;
                // the Bu/t figure and its EU/t (§10.3); the dock's full name and the exact Bu/t are in the tooltip
                value = "§b" + tinyDock(
                    dock) + " " + (clipped ? "§6" : "§f") + MTEQuasarEngine.bu(bu) + " §7= §f" + BTSFormat.euOfBu(bu);
            }
            rows.stat(
                BTSText.lang(K + "jet." + port.key),
                BTSText.lit(value),
                BTSText.lit(jetLine(multiblock, port)),
                BTSText.cat(BTSText.lit("§7"), BTSText.lang(K + "jet.tip")));
        }
    }

    /** The jet row's one-word dock: "Crown", "Mouth", else the first word. */
    static String tinyDock(String dock) {
        final String d = shortDock(dock);
        if (d.contains("Crown")) return "Crown";
        if (d.contains("Mouth")) return "Mouth";
        final int sp = d.indexOf(' ');
        return sp > 0 ? d.substring(0, sp) : d;
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
            BTSText.lang(
                K + "feed.buffer",
                BTSFormat.exact(buffer),
                BTSFormat.exact(m.bufferCap()),
                Double.isInfinite(m.getBufferSeconds()) ? "-" : BTSFormat.duration((long) m.getBufferSeconds())),
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
        if (dormant) {
            final boolean fits = m.getMass() <= m.getStructureTop() * (1 + 1e-9);
            c.add(
                fits,
                BTSText.lang(K + "chk.holds"),
                BTSText.lit(MTEQuasarEngine.fmt(m.getStructureTop()) + " SM"),
                -1,
                BTSText.lang(K + "chk.holds.tip", MTEQuasarEngine.fmt(m.getMass())));
        }
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
        for (String k : new String[] { "open", "feed", "disc", "trims", "flux", "coils", "jets", "eu", "burst", "auto",
            "lattice", "intake", "overdrive", "unbound", "horizon" }) {
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
        final IPanelHandler[] panels = new IPanelHandler[3];
        panels[0] = syncManager.syncedPanel("qDiscPanel", true, (sub, handler) -> buildDiscPanel(syncManager, parent));
        panels[1] = syncManager.syncedPanel("qJetsPanel", true, (sub, handler) -> buildJetsPanel(syncManager, parent));
        panels[2] = syncManager
            .syncedPanel("qHorizonPanel", true, (sub, handler) -> buildHorizonPanel(syncManager, parent));
        row.child(panelButton(panels, 1, GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS, K + "button.jets"))
            .child(panelButton(panels, 0, GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED, K + "button.disc"))
            .child(panelButton(panels, 2, GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_SINGULARITY, K + "button.horizon"));
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

    // ================================================================= Supermassive Lattice panel

    /** The setpoint slider's position (0-1, log) for a mass, between 4,096 SM and {@code top}. */
    private static double massToPos(double mass, double top) {
        final double lo = QuasarParams.M_REF;
        if (top <= lo || mass <= lo) return 0;
        return Math.min(1, Math.log(mass / lo) / Math.log(top / lo));
    }

    /** The mass at a slider position, snapped to 3 significant figures. */
    private static double posToMass(double pos, double top) {
        final double lo = QuasarParams.M_REF;
        final double v = lo * Math.pow(Math.max(lo, top) / lo, Math.max(0, Math.min(1, pos)));
        return BTSFormat.sig3(v);
    }

    private ModularPanel buildHorizonPanel(PanelSyncManager main, ModularPanel parent) {
        final IntSyncValue lattice = main.findSyncHandler("qLattice", IntSyncValue.class);
        final DoubleSyncValue setpoint = main.findSyncHandler("qSetpoint", DoubleSyncValue.class);
        final DoubleSyncValue slideTop = main.findSyncHandler("qSlideTop", DoubleSyncValue.class);
        final DoubleSyncValue od = main.findSyncHandler("qOverdrive", DoubleSyncValue.class);
        final BooleanSyncValue unboundV = main.findSyncHandler("qUnbound", BooleanSyncValue.class);
        final BooleanSyncValue canUnbind = main.findSyncHandler("qCanUnbind", BooleanSyncValue.class);
        final BooleanSyncValue holdHatch = main.findSyncHandler("qHoldHatch", BooleanSyncValue.class);
        final BooleanSyncValue odHatch = main.findSyncHandler("qOdHatch", BooleanSyncValue.class);
        final StringSyncValue latLine = main.findSyncHandler("qLatticeLine", StringSyncValue.class);
        final StringSyncValue odLine = main.findSyncHandler("qOdLine", StringSyncValue.class);
        final StringSyncValue note = main.findSyncHandler("qPanelNote", StringSyncValue.class);
        final int w = HORIZON_PANEL_W - 8;
        final int half = (w - 4) / 2;
        final long[] armedAt = { 0L };
        final BooleanSupplier hasLattice = () -> lattice.getIntValue() > 0;
        final BooleanSupplier odOk = () -> lattice.getIntValue() >= QuasarParams.LATTICE_OVERDRIVE;
        final BooleanSupplier armed = () -> System.currentTimeMillis() - armedAt[0] < COLLAPSE_ARM_MS;

        final Flow column = Flow.column()
            .full()
            .padding(4)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(title(K + "panel.horizon.title", w))
            .child(panelPara(latLine::getStringValue, w, 2))
            // the mass setpoint: a log slider (4,096 SM .. the top) and a number field
            .child(
                keyValue(
                    () -> tr(K + "panel.setpoint"),
                    () -> holdHatch.getBoolValue() ? tr(K + "panel.hatch")
                        : MTEQuasarEngine.fmt(setpoint.getDoubleValue()) + " SM",
                    w))
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(
                        new DoubleValue.Dynamic(
                            () -> massToPos(setpoint.getDoubleValue(), slideTop.getDoubleValue()),
                            v -> {
                                if (!hasLattice.getAsBoolean()) return;
                                final double m = posToMass(v, slideTop.getDoubleValue());
                                if (Math.abs(m - setpoint.getDoubleValue()) > 0.5) setpoint.setDoubleValue(m);
                            }))
                    .background(SLIDER_TRACK)
                    .size(w, 10)
                    .tooltip(
                        t -> t.addLine(tr(K + "panel.setpoint.tip1"))
                            .addLine(EnumChatFormatting.GRAY + tr(K + "panel.setpoint.tip2"))
                            .addLine(EnumChatFormatting.GRAY + tr(K + "panel.setpoint.tip3")))
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                Flow.row()
                    .width(w)
                    .height(14)
                    .childPadding(3)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .child(
                        IKey.lang(K + "panel.setpoint.exact")
                            .color(BTSGuiKit.PANEL_TEXT)
                            .asWidget()
                            .heightRel(1f))
                    .child(
                        new TextFieldWidget().numbersDouble(v -> Math.max(0, Math.min(QuasarParams.UNBOUND_MAX, v)))
                            .value(new DoubleValue.Dynamic(setpoint::getDoubleValue, v -> {
                                if (hasLattice.getAsBoolean() && v != setpoint.getDoubleValue()) {
                                    setpoint.setDoubleValue(v);
                                }
                            }))
                            .setTextAlignment(Alignment.Center)
                            .size(w - 60, 14)
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.setpoint.field1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.setpoint.field2")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY)))
            // Overdrive: 0-100 % in 5 % steps, and its upkeep
            .child(
                keyValue(
                    () -> tr(K + "panel.overdrive"),
                    () -> !odOk.getAsBoolean() ? tr(K + "panel.od.l3")
                        : odHatch.getBoolValue() ? tr(K + "panel.hatch") : Math.round(100 * od.getDoubleValue()) + " %",
                    w))
            .child(
                new SliderWidget().bounds(0, 1)
                    .value(new DoubleValue.Dynamic(od::getDoubleValue, v -> {
                        final double c = Math.round(v / QuasarParams.OVERDRIVE_STEP) * QuasarParams.OVERDRIVE_STEP;
                        if (Math.abs(c - od.getDoubleValue()) > 1e-6) od.setDoubleValue(c);
                    }))
                    .background(SLIDER_TRACK)
                    .size(w, 10)
                    .tooltip(
                        t -> t.addLine(tr(K + "panel.od.tip1"))
                            .addLine(EnumChatFormatting.GRAY + tr(K + "panel.od.tip2"))
                            .addLine(EnumChatFormatting.GRAY + tr(K + "panel.od.tip3")))
                    .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(panelPara(odLine::getStringValue, w, 2))
            // Shed to the safe limit; Unbind / Bind (two clicks to Unbind)
            .child(
                Flow.row()
                    .width(w)
                    .height(16)
                    .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                    .child(
                        new ButtonWidget<>().size(half, 16)
                            .overlay(
                                IKey.dynamic(
                                    () -> (hasLattice.getAsBoolean() ? EnumChatFormatting.WHITE
                                        : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.shed")))
                            .onMousePressed(btn -> {
                                if (hasLattice.getAsBoolean()) main.callSyncedAction("qShed", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.shed.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.shed.tip2")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY))
                    .child(
                        new ButtonWidget<>().size(half, 16)
                            .overlay(IKey.dynamic(() -> {
                                if (unboundV.getBoolValue()) return EnumChatFormatting.WHITE + tr(K + "panel.bind");
                                if (armed.getAsBoolean()) return EnumChatFormatting.RED + tr(K + "panel.confirm");
                                return (canUnbind.getBoolValue() ? EnumChatFormatting.WHITE
                                    : EnumChatFormatting.DARK_GRAY) + tr(K + "panel.unbind");
                            }))
                            .onMousePressed(btn -> {
                                if (unboundV.getBoolValue()) {
                                    armedAt[0] = 0L;
                                    main.callSyncedAction("qBind", packet -> {});
                                } else if (!canUnbind.getBoolValue()) {
                                    armedAt[0] = 0L;
                                } else if (!armed.getAsBoolean()) {
                                    armedAt[0] = System.currentTimeMillis();
                                } else {
                                    armedAt[0] = 0L;
                                    main.callSyncedAction("qUnbind", packet -> {});
                                }
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine(tr(K + "panel.unbind.tip1"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.unbind.tip2"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.unbind.tip3"))
                                    .addLine(EnumChatFormatting.GRAY + tr(K + "panel.unbind.tip4")))
                            .tooltipShowUpTimer(TOOLTIP_DELAY)))
            .child(panelPara(() -> {
                if (armed.getAsBoolean() && !unboundV.getBoolValue()) {
                    return EnumChatFormatting.DARK_RED + tr(K + "panel.hint.unbind");
                }
                final String n = note.getStringValue();
                if (!n.isEmpty()) return n;
                return EnumChatFormatting.DARK_GRAY
                    + tr(hasLattice.getAsBoolean() ? K + "panel.hint.lattice" : K + "panel.hint.nolattice");
            }, w, 4));
        return new ModularPanel("qHorizonPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(HORIZON_PANEL_W, HORIZON_PANEL_H)
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
