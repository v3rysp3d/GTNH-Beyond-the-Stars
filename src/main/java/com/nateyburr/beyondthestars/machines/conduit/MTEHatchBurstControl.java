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
 * adopts the host's first mode (as the Directive Hatch adopts its host's first set). Settings live in the tile (a
 * wrench
 * keeps them through GT's own NBT); the level and edge are live.
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
        SHED("Shed", "a rising edge lowers the mass setpoint to the safe limit (Quasar Engine, lattice)");

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

    // ---- live
    private boolean edge;
    /** Rising edges seen since placement (GUI only). */
    private int edges;
    private String host = "";
    private String hostLine = "";
    private long lastPoll = Long.MIN_VALUE / 2;

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
        return new String[] {
            "Reads redstone (0-15, any side, covers too) into a burst machine. Right-click: the mode.",
            "§bQuasar Engine§7 (the 4 cap-rim sockets):",
            "  Purge: a rising edge dumps the disc's flux early (refused below 20). Wire Disc bit 3 here.",
            "  Field b: b = 0.5 + (coil tier - 0.5) x s/15, instead of the GUI's slider.",
            "  Hold (lattice): the mass setpoint from the safe limit (1) to the band top (15), on a log scale;",
            "    on an Unbound L4, from 4,194,304 SM (1) to 268,435,456 SM (15, two doublings). 0 leaves the GUI's.",
            "  Overdrive (L3 lattice): o = s/15 in 5 % steps. Shed: a rising edge sets the mass setpoint to the safe limit.",
            "§bConduit Junction§7: Junction split: share 1 = c + k x (s/15 - 0.5), clamped 0..1 (c, k in its GUI).",
            "§bEye of Harmony§7: Begin: a rising edge begins the galaxy transition when Transition bit 1 is on.",
            "A rising edge is 0 -> 1 or more; even a one-tick pulse counts. A hatch adopts its machine's mode." };
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

    public void setModeOrdinal(int o) {
        mode = Mode.of(o);
        settingsChanged();
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
