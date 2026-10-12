package com.nateyburr.beyondthestars.machines.quasar;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarGas;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarParams;
import com.nateyburr.beyondthestars.machines.quasar.engine.QuasarState;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;

/**
 * P4: the Quasar Engine's console (quasar-p4.md §1.5), GT's standard MUI2 multiblock layout in the Kerr console's
 * style.
 * Nothing a player needs takes a screwdriver.
 * <ul>
 * <li><b>Terminal:</b> the hole (state, coil tier); a mass bar (M over the coil cap); spin and λ; the E dial (a bar
 * centred on 0, zone-coloured, with the Resonant / Stable / Strained bands); the flux bar (the purge setpoint and the
 * 90 eruption warning as ticks); Integrity, Y_q and Stability; each jet (Bu/t and its dock, or "venting"); the feed
 * and the gas buffer; the coils (b, EU/t, power); links; the last event.</li>
 * <li><b>Gap row:</b> the feed f (a number field with - / +) on the left; the Disc control and Jets &amp; links side
 * panels and the render toggle on the right.</li>
 * <li><b>Disc control panel:</b> the gas, the feed f, the field b, the Directive purge setpoint, Purge now, Open
 * (the Quasar Seed, or a Black Hole Seed for a dormant hole, in the controller slot) and Collapse (two clicks).</li>
 * <li><b>Jets &amp; links panel:</b> each port, the hosts fed (as the conduit reports them), counters and
 * milestones.</li>
 * </ul>
 * Every value is server state and comes across as a sync value; every setter is clamped on the server.
 */
public class MTEQuasarEngineGui extends MTEMultiBlockBaseGui<MTEQuasarEngine> {

    private static final int BAR_TRACK = 0xFF2A2D33;
    private static final int BAR_EDGE = 0xFF4A4D55;
    /** Mass: the disc's orange. */
    private static final int MASS_FROM = 0xFF7A3A10;
    private static final int MASS_TO = 0xFFFFA040;
    /** Flux: the jets' cyan. */
    private static final int FLUX_FROM = 0xFF1F4A7A;
    private static final int FLUX_TO = 0xFF7FE0FF;
    private static final int TICK = 0xFFFFFF55;
    private static final int TICK_WARN = 0xFFFF5555;
    /** Zone colours, 0 Resonant .. 4 Breach (the D-49 palette). */
    private static final int[] ZONE_ARGB = { 0xFF7FE0FF, 0xFF55DD55, 0xFFFFC040, 0xFFFF6A30, 0xFFFF2020 };
    private static final EnumChatFormatting[] ZONE_TEXT = { EnumChatFormatting.AQUA, EnumChatFormatting.GREEN,
        EnumChatFormatting.GOLD, EnumChatFormatting.RED, EnumChatFormatting.DARK_RED };

    private static final long COLLAPSE_ARM_MS = 4000L;
    private static final int DISC_PANEL_W = 160;
    private static final int DISC_PANEL_H = 214;
    private static final int JETS_PANEL_W = 190;
    private static final int JETS_PANEL_H = 190;

    public MTEQuasarEngineGui(MTEQuasarEngine multiblock) {
        super(multiblock);
    }

    // ============================================================================= sync values

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        final MTEQuasarEngine m = multiblock;
        syncManager.syncValue("qFormed", new BooleanSyncValue(m::isFormed));
        syncManager.syncValue("qFrozen", new BooleanSyncValue(m::isFrozen));
        syncManager.syncValue("qHole", new IntSyncValue(m::holeOrdinal));
        syncManager.syncValue("qCoil", new IntSyncValue(m::getCoilTier));
        syncManager.syncValue("qMass", new DoubleSyncValue(m::getMass));
        syncManager.syncValue("qCap", new DoubleSyncValue(m::getMassCap));
        syncManager.syncValue("qSpin", new DoubleSyncValue(() -> m.state().a));
        syncManager.syncValue("qSpinUp", new BooleanSyncValue(m::spinningUp));
        syncManager.syncValue("qLambda", new DoubleSyncValue(() -> m.state().lam));
        syncManager.syncValue("qE", new DoubleSyncValue(() -> m.state().E));
        syncManager.syncValue("qZone", new IntSyncValue(() -> m.state().zone));
        syncManager.syncValue("qPhi", new DoubleSyncValue(() -> m.state().Phi));
        syncManager.syncValue("qI", new DoubleSyncValue(() -> m.state().I));
        syncManager.syncValue("qYq", new DoubleSyncValue(() -> m.state().Yq));
        syncManager.syncValue("qReform", new IntSyncValue(() -> m.state().reform));
        syncManager.syncValue("qG", new DoubleSyncValue(() -> m.state().G));
        for (JetPort port : JetPort.VALUES) {
            syncManager.syncValue("qJet." + port.key, new StringSyncValue(() -> jetLine(m, port)));
        }
        syncManager.syncValue("qFeedLine", new StringSyncValue(() -> feedLine(m)));
        syncManager.syncValue("qCoilLine", new StringSyncValue(() -> coilLine(m)));
        syncManager.syncValue("qLinks", new StringSyncValue(() -> linksText(m)));
        syncManager.syncValue("qEvent", new StringSyncValue(m::getLastEvent));
        syncManager.syncValue("qCounters", new StringSyncValue(() -> countersLine(m)));
        syncManager.syncValue("qMilestones", new StringSyncValue(() -> milestonesLine(m)));
        syncManager.syncValue("qRenderNote", new StringSyncValue(m::getRenderNote));
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

    static String jetLine(MTEQuasarEngine m, JetPort port) {
        final String dock = m.jetDock(port);
        final double bu = m.jetBu(port);
        final String head = EnumChatFormatting.GRAY + port.label
            + " jet "
            + EnumChatFormatting.WHITE
            + MTEQuasarEngine.bu(bu)
            + " Bu/t"
            + EnumChatFormatting.GRAY;
        if (dock == null) return head + (bu > 0 ? EnumChatFormatting.GOLD + " -> venting" : " -> nothing docked");
        final double clipped = bu - m.jetTaken(port);
        return head + " -> "
            + EnumChatFormatting.AQUA
            + dock
            + (clipped > 0.005 ? EnumChatFormatting.GOLD + " (clipped " + MTEQuasarEngine.bu(clipped) + ")" : "");
    }

    static String feedLine(MTEQuasarEngine m) {
        final QuasarState q = m.state();
        if (q.hole == QuasarState.Hole.NONE) return EnumChatFormatting.DARK_GRAY + "Feed: no hole yet";
        final double medd = QuasarParams.eddingtonFeed(q.M, m.getGas());
        return EnumChatFormatting.GRAY + m.getGas().displayName
            + " f "
            + EnumChatFormatting.WHITE
            + String.format(Locale.ROOT, "%.2f", m.getFeed())
            + EnumChatFormatting.GRAY
            + ": "
            + MTEQuasarEngine.fmt(m.getGasDrawn())
            + " / "
            + MTEQuasarEngine.fmt(medd)
            + " L/s, buffer "
            + compact(m.buffer(MTEQuasarEngine.gasFluid(m.getGas())))
            + " L"
            + (m.getTrimGasUnits() + m.getTrimSpaceUnits() > 0
                ? String.format(Locale.ROOT, ", trims +%.0f / -%.0f", m.getTrimGasUnits(), m.getTrimSpaceUnits())
                : "");
    }

    static String coilLine(MTEQuasarEngine m) {
        final QuasarState q = m.state();
        if (q.hole != QuasarState.Hole.OPEN && q.hole != QuasarState.Hole.REFORM) {
            return EnumChatFormatting.DARK_GRAY + "Coils idle (T" + m.getCoilTier() + ")";
        }
        if (!m.isPowerOk()) return EnumChatFormatting.RED + "Coil power SHORT: b = 0, jets stopped, flux frozen";
        return EnumChatFormatting.GRAY + "Coils T"
            + m.getCoilTier()
            + ": b "
            + EnumChatFormatting.WHITE
            + String.format(Locale.ROOT, "%.2f", m.getBInUse())
            + EnumChatFormatting.GRAY
            + (m.bFromHatch() ? " (Burst Control Hatch)" : "")
            + ", "
            + compact(m.getCoilEUt())
            + " EU/t";
    }

    static String linksText(MTEQuasarEngine m) {
        final List<MTEQuasarEngine.HostReport> hosts = m.hostReports();
        if (hosts.isEmpty()) return "";
        final StringBuilder sb = new StringBuilder();
        for (MTEQuasarEngine.HostReport h : hosts) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(h.label)
                .append(": ")
                .append(MTEQuasarEngine.bu(h.bu))
                .append(" Bu/t, σ ")
                .append(String.format(Locale.ROOT, "%.2f", h.sigma));
            if (h.streak > 0) sb.append(String.format(Locale.ROOT, ", streak %.0f%%", 100 * h.streak));
        }
        return sb.toString();
    }

    static String countersLine(MTEQuasarEngine m) {
        return "Eruptions " + m.getEruptions() + ", purges " + m.getPurges() + ", blow-outs " + m.getBlowOuts();
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

    /** What the controller slot needs now (the Open button's hint). */
    static String slotLine(MTEQuasarEngine m) {
        switch (m.state().hole) {
            case NONE:
                return "Open: a Quasar Seed in the controller slot";
            case DORMANT:
                return "Open: a Black Hole Seed in the controller slot";
            default:
                return "";
        }
    }

    // ================================================================================ terminal

    private static IWidget row(Supplier<String> text) {
        return IKey.dynamic(text::get)
            .asWidget()
            .fullWidth()
            .marginBottom(2);
    }

    /** A terminal line shown only while {@code shown} holds. */
    private static IWidget rowIf(Supplier<String> text, BooleanSupplier shown) {
        return IKey.dynamic(text::get)
            .asWidget()
            .fullWidth()
            .marginBottom(2)
            .setEnabledIf(w -> shown.getAsBoolean());
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        final BooleanSyncValue formed = syncManager.findSyncHandler("qFormed", BooleanSyncValue.class);
        final BooleanSyncValue frozen = syncManager.findSyncHandler("qFrozen", BooleanSyncValue.class);
        final IntSyncValue hole = syncManager.findSyncHandler("qHole", IntSyncValue.class);
        final IntSyncValue coil = syncManager.findSyncHandler("qCoil", IntSyncValue.class);
        final DoubleSyncValue mass = syncManager.findSyncHandler("qMass", DoubleSyncValue.class);
        final DoubleSyncValue cap = syncManager.findSyncHandler("qCap", DoubleSyncValue.class);
        final DoubleSyncValue spin = syncManager.findSyncHandler("qSpin", DoubleSyncValue.class);
        final BooleanSyncValue spinUp = syncManager.findSyncHandler("qSpinUp", BooleanSyncValue.class);
        final DoubleSyncValue lambda = syncManager.findSyncHandler("qLambda", DoubleSyncValue.class);
        final DoubleSyncValue e = syncManager.findSyncHandler("qE", DoubleSyncValue.class);
        final IntSyncValue zone = syncManager.findSyncHandler("qZone", IntSyncValue.class);
        final DoubleSyncValue phi = syncManager.findSyncHandler("qPhi", DoubleSyncValue.class);
        final DoubleSyncValue integrity = syncManager.findSyncHandler("qI", DoubleSyncValue.class);
        final DoubleSyncValue yq = syncManager.findSyncHandler("qYq", DoubleSyncValue.class);
        final IntSyncValue reform = syncManager.findSyncHandler("qReform", IntSyncValue.class);
        final DoubleSyncValue g = syncManager.findSyncHandler("qG", DoubleSyncValue.class);
        final DoubleSyncValue purgeAt = syncManager.findSyncHandler("qPurgeAt", DoubleSyncValue.class);
        final StringSyncValue top = syncManager.findSyncHandler("qJet.top", StringSyncValue.class);
        final StringSyncValue bottom = syncManager.findSyncHandler("qJet.bottom", StringSyncValue.class);
        final StringSyncValue feedLine = syncManager.findSyncHandler("qFeedLine", StringSyncValue.class);
        final StringSyncValue coilLine = syncManager.findSyncHandler("qCoilLine", StringSyncValue.class);
        final StringSyncValue links = syncManager.findSyncHandler("qLinks", StringSyncValue.class);
        final StringSyncValue event = syncManager.findSyncHandler("qEvent", StringSyncValue.class);
        final StringSyncValue renderNote = syncManager.findSyncHandler("qRenderNote", StringSyncValue.class);
        final BooleanSupplier hasHole = () -> hole.getIntValue() != QuasarState.Hole.NONE.ordinal();
        final BooleanSupplier discOn = () -> hole.getIntValue() == QuasarState.Hole.OPEN.ordinal();

        return super.createTerminalTextWidget(syncManager, parent)
            .child(
                row(
                    () -> holeLine(
                        formed.getBoolValue(),
                        frozen.getBoolValue(),
                        hole.getIntValue(),
                        coil.getIntValue(),
                        reform.getIntValue())))
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.WHITE + fmt1(mass.getDoubleValue())
                        + " / "
                        + MTEQuasarEngine.fmt(cap.getDoubleValue())
                        + " SM ")
                    .asWidget()
                    .textAlign(Alignment.CenterRight)
                    .scale(0.8f)
                    .widthRel(0.94f)
                    .height(9)
                    .background(
                        fillBar(
                            () -> mass.getDoubleValue() / Math.max(1, cap.getDoubleValue()),
                            MASS_FROM,
                            MASS_TO,
                            -1,
                            -1))
                    .marginBottom(3)
                    .setEnabledIf(w -> hasHole.getAsBoolean()))
            .child(
                rowIf(
                    () -> EnumChatFormatting.GRAY + "Spin a "
                        + EnumChatFormatting.WHITE
                        + String.format(Locale.ROOT, "%.3f", spin.getDoubleValue())
                        + EnumChatFormatting.GRAY
                        + (spinUp.getBoolValue() ? " (spinning up)" : "")
                        + ", lambda "
                        + EnumChatFormatting.WHITE
                        + String.format(Locale.ROOT, "%.2f", lambda.getDoubleValue())
                        + EnumChatFormatting.GRAY
                        + ", bursts "
                        + EnumChatFormatting.WHITE
                        + MTEQuasarEngine.bu(g.getDoubleValue())
                        + " Bu/t",
                    hasHole))
            .child(
                IKey.dynamic(() -> eLine(e.getDoubleValue(), zone.getIntValue()))
                    .asWidget()
                    .textAlign(Alignment.Center)
                    .scale(0.8f)
                    .widthRel(0.94f)
                    .height(9)
                    .background(eBar(e, zone))
                    .marginBottom(3)
                    .setEnabledIf(w -> discOn.getAsBoolean()))
            .child(
                IKey.dynamic(
                    () -> EnumChatFormatting.WHITE + "Flux "
                        + Math.round(phi.getDoubleValue())
                        + (phi.getDoubleValue() >= QuasarParams.ERUPT_WARN ? EnumChatFormatting.RED + " ERUPTING SOON"
                            : "")
                        + " ")
                    .asWidget()
                    .textAlign(Alignment.CenterRight)
                    .scale(0.8f)
                    .widthRel(0.94f)
                    .height(9)
                    .background(
                        fillBar(
                            () -> phi.getDoubleValue() / 100.0,
                            FLUX_FROM,
                            FLUX_TO,
                            purgeAt.getDoubleValue() > 0 ? purgeAt.getDoubleValue() / 100.0 : -1,
                            QuasarParams.ERUPT_WARN / 100.0))
                    .marginBottom(3)
                    .setEnabledIf(w -> discOn.getAsBoolean()))
            .child(
                rowIf(
                    () -> EnumChatFormatting.GRAY + "Integrity "
                        + colourFor(integrity.getDoubleValue())
                        + Math.round(integrity.getDoubleValue())
                        + EnumChatFormatting.GRAY
                        + ", yield "
                        + EnumChatFormatting.WHITE
                        + String.format(Locale.ROOT, "%.2f", yq.getDoubleValue())
                        + EnumChatFormatting.GRAY
                        + ", Stability "
                        + EnumChatFormatting.WHITE
                        + Math.round(QuasarParams.stability(e.getDoubleValue())),
                    discOn))
            .child(rowIf(top::getStringValue, hasHole))
            .child(rowIf(bottom::getStringValue, hasHole))
            .child(row(feedLine::getStringValue))
            .child(row(coilLine::getStringValue))
            .child(
                rowIf(
                    () -> EnumChatFormatting.GRAY + "Links: "
                        + EnumChatFormatting.WHITE
                        + links.getStringValue()
                            .replace("\n", EnumChatFormatting.GRAY + "; " + EnumChatFormatting.WHITE),
                    () -> !links.getStringValue()
                        .isEmpty()))
            .child(
                IKey.dynamic(() -> EnumChatFormatting.AQUA + event.getStringValue())
                    .asWidget()
                    .fullWidth()
                    .marginBottom(2)
                    .setEnabledIf(
                        w -> !event.getStringValue()
                            .isEmpty()))
            .child(
                IKey.dynamic(() -> EnumChatFormatting.GOLD + renderNote.getStringValue())
                    .asWidget()
                    .fullWidth()
                    .marginBottom(2)
                    .setEnabledIf(
                        w -> !renderNote.getStringValue()
                            .isEmpty()));
    }

    static String holeLine(boolean formed, boolean frozen, int hole, int coil, int reform) {
        final String coils = "coils T" + coil;
        if (!formed) return EnumChatFormatting.RED + "Not formed: the disc is frozen";
        if (frozen) return EnumChatFormatting.RED + "FROZEN: a chunk of the structure is unloaded (hosts pause)";
        if (hole == QuasarState.Hole.OPEN.ordinal()) {
            return EnumChatFormatting.GREEN + "Hole OPEN" + EnumChatFormatting.GRAY + " - " + coils;
        }
        if (hole == QuasarState.Hole.REFORM.ordinal()) {
            return EnumChatFormatting.GOLD + "Disc RE-FORMING "
                + reform
                + " / "
                + QuasarParams.REFORM_SECONDS
                + " s"
                + EnumChatFormatting.GRAY
                + " (feed f 0.3 or more)";
        }
        if (hole == QuasarState.Hole.DORMANT.ordinal()) {
            return EnumChatFormatting.GRAY + "Hole DORMANT (mass kept): a Black Hole Seed, then Open";
        }
        return EnumChatFormatting.GRAY + "No hole: a Quasar Seed in the controller slot, then Open";
    }

    static String eLine(double e, int zone) {
        final EnumChatFormatting c = zone >= 0 && zone < ZONE_TEXT.length ? ZONE_TEXT[zone] : EnumChatFormatting.GRAY;
        return EnumChatFormatting.WHITE + "E "
            + String.format(Locale.ROOT, "%+.1f", e)
            + "  "
            + c
            + MTEQuasarEngine.zoneName(zone);
    }

    private static EnumChatFormatting colourFor(double integrity) {
        if (integrity >= 75) return EnumChatFormatting.GREEN;
        if (integrity >= 40) return EnumChatFormatting.GOLD;
        return EnumChatFormatting.RED;
    }

    /** A 0..1 fill bar with up to two ticks (fraction, or -1 for none). */
    private static IDrawable fillBar(Supplier<Double> frac, int from, int to, double tick, double warnTick) {
        return (context, x, y, width, height, theme) -> {
            final double f = Math.max(0, Math.min(1, frac.get()));
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, BAR_TRACK);
            final float fillW = (float) ((width - 2) * f);
            if (fillW > 0) GuiDraw.drawHorizontalGradientRect(x + 1, y + 1, fillW, height - 2, from, to);
            if (warnTick >= 0) {
                GuiDraw.drawRect(x + 1 + (float) ((width - 2) * warnTick) - 0.5f, y, 1, height, TICK_WARN);
            }
            if (tick >= 0) GuiDraw.drawRect(x + 1 + (float) ((width - 2) * tick) - 0.5f, y, 1, height, TICK);
        };
    }

    /**
     * The E dial: a bar from −120 to +120 with the zone bands faint behind it (Resonant |E| under 10, Stable under 30,
     * Strained under 60), and a fill from 0 to E in the zone's colour.
     */
    private static IDrawable eBar(DoubleSyncValue e, IntSyncValue zone) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, BAR_TRACK);
            final float inner = width - 2;
            final float mid = x + 1 + inner / 2;
            final float[] bands = { 60, 30, 10 };
            final int[] bandCol = { 0x30FF6A30, 0x30FFC040, 0x3055DD55 };
            for (int i = 0; i < bands.length; i++) {
                final float half = inner / 2 * bands[i] / 120f;
                GuiDraw.drawRect(mid - half, y + 1, 2 * half, height - 2, bandCol[i]);
            }
            final double v = Math.max(-120, Math.min(120, e.getDoubleValue()));
            final float w = (float) (inner / 2 * Math.abs(v) / 120);
            final int z = zone.getIntValue();
            final int col = z >= 0 && z < ZONE_ARGB.length ? ZONE_ARGB[z] : 0xFF888888;
            if (w > 0) GuiDraw.drawRect(v >= 0 ? mid : mid - w, y + 2, w, height - 4, col);
            GuiDraw.drawRect(mid - 0.5f, y, 1, height, TICK);
        };
    }

    static String fmt1(double v) {
        return v >= 100 ? String.format(Locale.ROOT, "%,.0f", v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** "8.39M", "1.25G", "512k". */
    static String compact(double v) {
        if (v >= 1e12) return String.format(Locale.ROOT, "%.2fT", v / 1e12);
        if (v >= 1e9) return String.format(Locale.ROOT, "%.2fG", v / 1e9);
        if (v >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
        if (v >= 1e4) return String.format(Locale.ROOT, "%.0fk", v / 1e3);
        return String.format(Locale.ROOT, "%,.0f", v);
    }

    /** The power switch does not stop the hole (Collapse does): shown greyed out. */
    @Override
    protected boolean isPowerSwitchDisabled() {
        return true;
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

    private static TextFieldWidget feedField(DoubleSyncValue feed) {
        return new TextFieldWidget().numbersDouble(v -> Math.max(QuasarParams.F_MIN, Math.min(QuasarParams.F_MAX, v)))
            .value(new DoubleValue.Dynamic(feed::getDoubleValue, v -> {
                final double c = Math.max(QuasarParams.F_MIN, Math.min(QuasarParams.F_MAX, v));
                if (c != feed.getDoubleValue()) feed.setDoubleValue(c);
            }))
            .setTextAlignment(Alignment.Center)
            .tooltip(
                t -> t.addLine("Feed setpoint f: the share of the Eddington feed the injectors draw")
                    .addLine(EnumChatFormatting.GRAY + "0 to 1.5. 0.85 holds the disc Stable on its own;")
                    .addLine(
                        EnumChatFormatting.GRAY + "1.0 rides the Eddington limit (needs trims); above it E runs away."))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final DoubleSyncValue feed = syncManager.findSyncHandler("qFeed", DoubleSyncValue.class);
        return super.createLeftPanelGapRow(parent, syncManager).childPadding(1)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.str("Feed f")
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
        return new ButtonWidget<>().size(10, 14)
            .overlay(IKey.str(step < 0 ? "-" : "+"))
            .onMousePressed(button -> {
                final double d = button == 1 ? step * 5 : step;
                final double next = Math.max(
                    QuasarParams.F_MIN,
                    Math.min(QuasarParams.F_MAX, Math.round((feed.getDoubleValue() + d) * 100) / 100.0));
                if (next != feed.getDoubleValue()) feed.setDoubleValue(next);
                return true;
            })
            .tooltip(t -> t.addLine((step < 0 ? "Lower" : "Raise") + " f by 0.05 (right-click: 0.25)"))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    @Override
    protected Flow createRightPanelGapRow(ModularPanel parent, PanelSyncManager syncManager) {
        final IPanelHandler[] panels = new IPanelHandler[2];
        panels[0] = syncManager.syncedPanel("qDiscPanel", true, (sub, handler) -> buildDiscPanel(syncManager, parent));
        panels[1] = syncManager.syncedPanel("qJetsPanel", true, (sub, handler) -> buildJetsPanel(syncManager, parent));
        final BooleanSyncValue render = syncManager.findSyncHandler("qRender", BooleanSyncValue.class);
        return super.createRightPanelGapRow(parent, syncManager).child(
            new ToggleButton().size(18, 18)
                .marginLeft(2)
                .value(render)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_SHOW_WORK_AREA)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_WORK_AREA)
                .tooltipBuilder(
                    t -> t.addLine("Hole, disc and jet render on / off (this machine)")
                        .addLine(EnumChatFormatting.GRAY + "The config can turn it off everywhere."))
                .tooltipShowUpTimer(TOOLTIP_DELAY))
            .child(
                panelButton(
                    panels,
                    1,
                    GTGuiTextures.TT_OVERLAY_BUTTON_STATISTICS,
                    "Jets & links: each port's dock, the hosts fed, counters"))
            .child(
                panelButton(
                    panels,
                    0,
                    GTGuiTextures.OVERLAY_BUTTON_EDIT_PARAMETERS_ENABLED,
                    "Disc control: gas, feed, field b, purge, Open, Collapse"));
    }

    private static IWidget panelButton(IPanelHandler[] panels, int which, IDrawable icon, String tooltip) {
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
            .tooltip(t -> t.addLine(tooltip))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    // ======================================================================= disc control panel

    private static TextWidget<?> panelText(Supplier<String> text, int width, float scale) {
        return IKey.dynamic(text::get)
            .asWidget()
            .scale(scale)
            .maxWidth(width);
    }

    private static IWidget keyValue(String label, Supplier<String> value, int width) {
        return Flow.row()
            .width(width)
            .height(9)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(
                IKey.str(label)
                    .asWidget())
            .child(
                IKey.dynamic(value::get)
                    .asWidget());
    }

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
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Disc control")
                    .asWidget()
                    .width(w)
                    .textAlign(Alignment.Center))
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
                                t -> t.addLine("Feed gas (the injectors draw only this one):")
                                    .addLine(
                                        EnumChatFormatting.GRAY + "Hydrogen (k 0.4), Helium (0.2, twice the feed),")
                                    .addLine(
                                        EnumChatFormatting.GRAY + "Raw Star Matter (0.1, the hole grows 2x faster).")
                                    .addLine(EnumChatFormatting.GRAY + "The same gas is the +E trim.")))
                    .child(
                        panelText(
                            () -> "Gas: " + QuasarGas.values()[Math
                                .floorMod(gas.getIntValue(), QuasarGas.values().length)].displayName,
                            w - 18,
                            1f).heightRel(1f)))
            .child(keyValue("Feed f", () -> String.format(Locale.ROOT, "%.2f", feed.getDoubleValue()), w))
            .child(
                new SliderWidget().bounds(QuasarParams.F_MIN, QuasarParams.F_MAX)
                    .value(new DoubleValue.Dynamic(feed::getDoubleValue, v -> {
                        final double c = Math.round(v * 100) / 100.0;
                        if (c != feed.getDoubleValue()) feed.setDoubleValue(c);
                    }))
                    .size(w, 10))
            .child(
                keyValue(
                    "Field b",
                    () -> bHatch.getBoolValue() ? "from a Burst Control Hatch"
                        : String.format(Locale.ROOT, "%.2f (coils cap %d)", b.getDoubleValue(), coil.getIntValue()),
                    w))
            .child(
                new SliderWidget().bounds(QuasarParams.B_MIN, 4)
                    .value(new DoubleValue.Dynamic(b::getDoubleValue, v -> {
                        final double c = Math.round(v * 20) / 20.0;
                        if (c != b.getDoubleValue()) b.setDoubleValue(c);
                    }))
                    .size(w, 10))
            .child(
                keyValue(
                    "Purge setpoint",
                    () -> purgeAt.getDoubleValue() <= 0 ? "off" : "flux " + Math.round(purgeAt.getDoubleValue()),
                    w))
            .child(
                new SliderWidget().bounds(0, 100)
                    .value(new DoubleValue.Dynamic(purgeAt::getDoubleValue, v -> {
                        final double c = Math.round(v);
                        if (c != purgeAt.getDoubleValue()) purgeAt.setDoubleValue(c);
                    }))
                    .size(w, 10)
                    .tooltip(
                        t -> t.addLine("Directive Disc bit 3 turns on at this flux (0: never).")
                            .addLine(EnumChatFormatting.GRAY + "Wire it to a Burst Control Hatch to purge there.")))
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
                                        : EnumChatFormatting.DARK_GRAY) + "Open"))
                            .onMousePressed(btn -> {
                                if (canOpen.getAsBoolean()) main.callSyncedAction("qOpen", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Opens the hole from the controller slot:")
                                    .addLine(EnumChatFormatting.GRAY + "a Quasar Seed (first time: 64 SM, spin 0),")
                                    .addLine(EnumChatFormatting.GRAY + "or a Black Hole Seed (a dormant hole).")))
                    .child(
                        new ButtonWidget<>().size(third, 16)
                            .overlay(
                                IKey.dynamic(
                                    () -> (canPurge.getAsBoolean() ? EnumChatFormatting.WHITE
                                        : EnumChatFormatting.DARK_GRAY) + "Purge"))
                            .onMousePressed(btn -> {
                                if (canPurge.getAsBoolean()) main.callSyncedAction("qPurge", packet -> {});
                                return true;
                            })
                            .tooltip(
                                t -> t.addLine("Dumps the flux now (from 20): a smaller burst spike, E -30 x flux/100.")
                                    .addLine(
                                        EnumChatFormatting.GRAY
                                            + "Purging small and often: a smoother, lower stream.")))
                    .child(
                        new ButtonWidget<>().size(third, 16)
                            .overlay(IKey.dynamic(() -> {
                                if (armed.getAsBoolean()) return EnumChatFormatting.RED + "Confirm";
                                return (running.getAsBoolean() ? EnumChatFormatting.WHITE
                                    : EnumChatFormatting.DARK_GRAY) + "Collapse";
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
                                t -> t.addLine("Makes the hole dormant: the mass and spin are kept,")
                                    .addLine(EnumChatFormatting.GRAY + "the jets stop. Re-open with a Black Hole Seed.")
                                    .addLine(EnumChatFormatting.GRAY + "Click twice."))))
            .child(panelText(() -> {
                if (armed.getAsBoolean()) return EnumChatFormatting.DARK_RED + "Click Collapse again: the jets stop.";
                final String s = slot.getStringValue();
                if (!s.isEmpty()) return s;
                return EnumChatFormatting.DARK_GRAY + "Purge needs flux 20 or more.";
            }, w, 0.75f))
            .child(
                panelText(
                    () -> EnumChatFormatting.DARK_GRAY
                        + "Hold E near 0: trims in the tank hatches (+ gas, - Space), or f. Riding f 1.0 with trims"
                        + " gives about 1.5x the output of f 0.85.",
                    w,
                    0.75f));

        return new ModularPanel("qDiscPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(DISC_PANEL_W, DISC_PANEL_H)
            .child(column);
    }

    // =========================================================================== jets panel

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
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Jets & links")
                    .asWidget()
                    .width(w)
                    .textAlign(Alignment.Center))
            .child(panelText(() -> strip(top.getStringValue()), w, 0.8f))
            .child(panelText(() -> strip(bottom.getStringValue()), w, 0.8f))
            .child(
                panelText(
                    () -> EnumChatFormatting.DARK_GRAY
                        + "Dock a Burst Thermaliser Crown or a Wormhole Mouth on each cap's port. An empty port vents.",
                    w,
                    0.75f))
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Hosts fed")
                    .asWidget())
            .child(panelText(() -> {
                final String l = links.getStringValue();
                return l.isEmpty() ? EnumChatFormatting.DARK_GRAY + "none reported (the conduit reports hosts)" : l;
            }, w, 0.75f))
            .child(
                IKey.str(EnumChatFormatting.BOLD + "Record")
                    .asWidget())
            .child(panelText(counters::getStringValue, w, 0.75f))
            .child(panelText(() -> "Milestones: " + milestones.getStringValue(), w, 0.75f));
        return new ModularPanel("qJetsPanel").relative(parent)
            .leftRel(1)
            .topRel(0)
            .size(JETS_PANEL_W, JETS_PANEL_H)
            .child(column);
    }

    /** The terminal's colours do not read on GT's grey panels. */
    private static String strip(String s) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(s);
    }
}
