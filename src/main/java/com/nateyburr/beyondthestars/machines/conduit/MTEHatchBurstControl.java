package com.nateyburr.beyondthestars.machines.conduit;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.nateyburr.beyondthestars.machines.gui.BTSTip;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * P4: the BURST CONTROL HATCH, MTE base+26 = 28126 (quasar-p4.md §1.3, §3, §8.1). An analog redstone INPUT: it reads
 * the strongest redstone on any side (covers included) every tick, keeps the level s (0-15) and latches a rising edge
 * (s going from 0 to 1 or more), so a one-tick pulse is never missed between its host's once-a-second reads. Its
 * <b>mode</b> (GUI) says what the host does with it:
 * <ul>
 * <li>{@link Mode#PURGE} (Quasar Engine, the cap-rim {@code c} sockets): a rising edge dumps the disc's flux Φ early
 * (refused below Φ 20). The standard rig wires Disc bit 3 AND NOT Transition bit 4 here.</li>
 * <li>{@link Mode#FIELD} (Quasar Engine, "b mode"): the coil field b = 0.5 + (tier − 0.5)·s/15, overriding the GUI's
 * slider while the hatch is in this mode.</li>
 * <li>{@link Mode#SPLIT} (Conduit Junction): share₁ = clamp(c + k·(s/15 − 0.5), 0, 1) with the Junction's GUI ratio c
 * and gain k.</li>
 * <li>{@link Mode#BEGIN} (Eye of Harmony, "begin mode"): a rising edge begins the galaxy transition when Galaxy
 * Transition bit 1 is on; ignored otherwise.</li>
 * </ul>
 * The host calls {@link #poll} once a second with the modes it understands; a hatch whose mode its host does not use
 * adopts the host's first mode (as the Directive Hatch adopts its host's first set). The mode is saved with the tile
 * and rides a wrenched item ({@link #setItemNBT}); the last level is saved with the tile only (QA-B05), the edge is
 * live.
 */
public class MTEHatchBurstControl extends MTEHatch {

    /** What the host does with the level and the edge. NBT keeps the name; never rename one. */
    public enum Mode {

        PURGE("Purge", "a rising edge purges the disc's flux (Quasar Engine)"),
        FIELD("Field b", "b = 0.5 + (tier - 0.5) x s / 15 (Quasar Engine)"),
        SPLIT("Junction split", "share 1 = c + k (s/15 - 0.5) (Conduit Junction)"),
        BEGIN("Begin", "a rising edge begins the galaxy transition when ready (Eye of Harmony)"),
        // ---- SMBH (smbh-v1.md §6): a Quasar Engine with a Supermassive Lattice. Appended: NBT keeps the name.
        HOLD("Hold",
            "the mass setpoint, safe limit (1) .. band top (15), log scale; 0 leaves the GUI's (Quasar Engine, lattice)"),
        OVERDRIVE("Overdrive", "Overdrive o = s / 15 in 5 % steps (Quasar Engine, L3 lattice)"),
        SHED("Shed", "a rising edge lowers the mass setpoint to the safe limit (Quasar Engine, lattice)"),
        // ---- P5 (p5-t9-t10.md §3.3): beside a Pellet Injector Hatch on the Eye's T9 top collar. Appended.
        BEAM("Pellet beam", "pellets/s = cap x clamp(c + k (s/15 - 0.5)) (Eye of Harmony, Pellet Injector)");

        public static final Mode[] VALUES = values();

        public final String label;
        public final String help;

        Mode(String label, String help) {
            this.label = label;
            this.help = help;
        }

        public static Mode of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }

        public static Mode parse(String name) {
            if (name == null) return null;
            for (Mode m : VALUES) if (m.name()
                .equalsIgnoreCase(name.trim())) return m;
            return null;
        }
    }

    /** A host that stops polling for this long (3 s) no longer counts as the hatch's host. */
    public static final long STALE_TICKS = 60;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_BURST_CONTROL");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_BURST_CONTROL_GLOW");

    // ---- saved
    private Mode mode = Mode.PURGE;

    // ---- saved with the tile (QA-B05): the last level, so a signal already on before a save or chunk unload is not
    // a rising edge when it loads again
    private int level;
    /** False until the first tick after placement or a load that found no saved level: that tick only primes. */
    private boolean primed;
    /**
     * B-221: true once any redstone above 0 has reached this hatch since it was placed (tile NBT only: a wrenched hatch
     * is a new wiring job). The Eye's pellet beam reads a hatch that never carried a signal as no hatch at all (the
     * centre level 8), so an unwired hatch cannot hold the injector at 0 pellets/s.
     */
    private boolean signalled;

    // ---- live
    private boolean edge;
    /** Rising edges seen since placement (GUI only). */
    private int edges;
    private String host = "";
    private String hostLine = "";
    private long lastPoll = Long.MIN_VALUE / 2;
    /** The modes the host accepted at its last poll (live; empty: no host). */
    private Mode[] accepted = new Mode[0];

    public MTEHatchBurstControl(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 11, 0, "Redstone in: purges, the coil field b, a Junction's split");
    }

    public MTEHatchBurstControl(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchBurstControl(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public String[] getDescription() {
        return BTSTip.withDetails(
            new String[] { "Feeds a redstone level into a burst machine.",
                "Reads " + BTSTip.num("0-15") + " from any side, covers included.",
                "A rising edge (" + BTSTip.num("0") + " to " + BTSTip.num("1+") + ") counts, even for one tick.",
                "Takes the mode of the machine it sits in.", BTSTip.hint("Right-click: the mode.") },
            BTSTip.heading("Quasar Engine") + " (cap-rim sockets):",
            BTSTip.bullet(BTSTip.term("Purge") + ": an edge dumps the disc flux early (from 20)."),
            BTSTip.bullet(BTSTip.term("Field b") + ": the level sets the field instead of the GUI."),
            BTSTip.bullet(BTSTip.term("Hold") + ": the lattice mass setpoint, safe limit to top."),
            BTSTip.bullet(BTSTip.term("Overdrive") + " (L3 lattice): level/15, in 5 % steps."),
            BTSTip.bullet(BTSTip.term("Shed") + ": an edge sets the setpoint to the safe limit."),
            BTSTip.heading("Conduit Junction") + ": the level moves the split share.",
            BTSTip.heading("Eye of Harmony") + ": an edge begins the galaxy transition",
            "once its Transition bit [1] is on.",
            BTSTip.heading("Pellet beam") + " (Eye T9, beside the Pellet Injector):",
            "the level is s in the injector's rate law (V sensor or two Directive bits).",
            "Level " + BTSTip.num("0") + " leaves the GUI's own value.",
            "A hatch that never had a signal fires the beam at its centre (" + BTSTip.num("8") + ").");
    }

    // ------------------------------------------------------------------ live input

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        final int s = Math.max(0, Math.min(15, aBaseMetaTileEntity.getStrongestRedstone()));
        // QA-B05: a rising edge is a real 0 -> 1+ change of the input. The first tick of a freshly placed hatch only
        // reads the level; after a load the saved level is the "before", so a signal held through a restart or a
        // chunk unload never fires a Shed, Purge or Begin again.
        if (primed && level == 0 && s > 0) {
            edge = true;
            edges++;
        }
        primed = true;
        if (s > 0) signalled = true;
        if (s != level) {
            level = s;
            aBaseMetaTileEntity.markDirty(); // the chunk must save the new "before" level
        }
        aBaseMetaTileEntity.setActive(s > 0);
    }

    /** The redstone level s, 0-15, as of this tick. */
    public int level() {
        return level;
    }

    /** B-221: true once a signal above 0 has reached this hatch since it was placed (it is wired to something). */
    public boolean signalled() {
        return signalled || level > 0;
    }

    /**
     * Test bench (the fast-forward gate rigs, {@code /btsquasar autorig}): the redstone level {@code s} as if it had
     * just arrived on a face - a rising edge latches exactly as {@link #onPostTick} would latch it. Only for seconds
     * stepped without world ticks; a live rig puts real redstone against the hatch.
     */
    public void benchSignal(int s) {
        final int v = Math.max(0, Math.min(15, s));
        if (level == 0 && v > 0) {
            edge = true;
            edges++;
        }
        level = v;
        primed = true;
        if (v > 0) signalled = true;
    }

    /** True once per rising edge since the last call (the host consumes it once a second). */
    public boolean consumeRisingEdge() {
        final boolean e = edge;
        edge = false;
        return e;
    }

    /**
     * The host, once a second: names itself, the modes it uses (the first is the default), and a line saying what the
     * hatch drives now ("b = 1.83", "purge armed"). A hatch in a mode the host does not use adopts the first.
     */
    public Mode poll(String hostName, String line, Mode... accepted) {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) lastPoll = base.getTimer();
        host = hostName == null ? "" : hostName;
        hostLine = line == null ? "" : line;
        this.accepted = accepted.clone();
        if (accepted.length > 0) {
            boolean ok = false;
            for (Mode m : accepted) ok |= m == mode;
            if (!ok) {
                mode = accepted[0];
                settingsChanged();
            }
        }
        return mode;
    }

    private boolean fresh() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.getTimer() - lastPoll <= STALE_TICKS;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode m) {
        if (m != null) {
            mode = m;
            settingsChanged();
        }
    }

    public int getModeOrdinal() {
        return mode.ordinal();
    }

    /**
     * The GUI's mode button. QA-B21: with a host reading the hatch, the button steps over the modes that host does not
     * use (in the direction clicked), so Hold, Overdrive and Shed are reachable on a quasar instead of snapping back
     * to Purge from Split or Begin.
     */
    public void setModeOrdinal(int o) {
        Mode next = Mode.of(o);
        if (fresh() && accepted.length > 0 && !accepts(next)) {
            final int n = Mode.VALUES.length;
            final int dir = Math.floorMod(next.ordinal() - mode.ordinal(), n) == n - 1 ? -1 : 1;
            int i = next.ordinal();
            for (int k = 0; k < n && !accepts(Mode.of(i)); k++) i = Math.floorMod(i + dir, n);
            next = Mode.of(i);
        }
        mode = next;
        settingsChanged();
    }

    private boolean accepts(Mode m) {
        for (Mode a : accepted) if (a == m) return true;
        return false;
    }

    /**
     * The mode changed (GUI, right-click, a host's adoption, bench): mark the chunk for saving. GT does that only for
     * inventory changes, so a mode set with nothing else happening in the chunk was lost on a restart (B-112, as the
     * Directive and sensor hatches since B-71).
     */
    private void settingsChanged() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) base.markDirty();
    }

    public String getHost() {
        return fresh() ? host : "";
    }

    /** What the host says the hatch drives now ("b = 1.83", "purge armed"), or "" when no host reads it (B-116). */
    public String getHostLine() {
        return fresh() ? hostLine : "";
    }

    public int getEdges() {
        return edges;
    }

    /** One line for the GUI and the host's scanner. */
    public String statusLine() {
        if (!fresh()) return mode.label + ": no machine reading it (place it in a burst machine's c socket)";
        return String
            .format(Locale.ROOT, "%s, redstone %d%s", mode.label, level, hostLine.isEmpty() ? "" : " -> " + hostLine);
    }

    // ------------------------------------------------------------------ GT plumbing

    @Override
    public boolean isValidSlot(int aIndex) {
        return false;
    }

    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer, ForgeDirection side,
        float aX, float aY, float aZ) {
        openGui(aPlayer);
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setString("btsBurstMode", mode.name());
        aNBT.setByte("btsBurstLevel", (byte) level);
        aNBT.setBoolean("btsBurstSeen", signalled);
    }

    /** QA-B16: a wrenched hatch keeps its mode (not its level: a placed hatch primes on its first tick). */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        aNBT.setString("btsBurstMode", mode.name());
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        final Mode m = Mode.parse(aNBT.getString("btsBurstMode"));
        if (m != null) mode = m;
        if (aNBT.hasKey("btsBurstLevel")) {
            level = Math.max(0, Math.min(15, aNBT.getByte("btsBurstLevel")));
            primed = true;
        }
        // B-221: a hatch saved before the flag counts as wired if it held a signal then
        signalled = aNBT.hasKey("btsBurstSeen") ? aNBT.getBoolean("btsBurstSeen") : level > 0;
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY), TextureFactory.builder()
            .addIcon(OVERLAY_GLOW)
            .glow()
            .build() };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY) };
    }

    @Override
    protected boolean useMui2() {
        return true;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return new BurstControlHatchGui(this).build(data, syncManager, uiSettings);
    }
}
