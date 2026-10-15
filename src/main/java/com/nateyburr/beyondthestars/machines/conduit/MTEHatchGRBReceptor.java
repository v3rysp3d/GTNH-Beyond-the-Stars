package com.nateyburr.beyondthestars.machines.conduit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.nateyburr.beyondthestars.machines.quasar.MTEQuasarEngine;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstReading;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource;
import com.nateyburr.beyondthestars.machines.quasar.burst.BurstStream;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPacket;
import com.nateyburr.beyondthestars.machines.quasar.burst.JetPort;

import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * P4: the GRB RECEPTOR PORT, MTE base+25 = 28125 (quasar-p4.md §3, §3.1, §7): the conduit's sink. A stream whose ray
 * reaches this block (from any side) is taken in, and the port records <b>last second's delivery per source quasar</b>
 * (and per jet port, so a stream that reached it by two paths in one second adds up). That record is what a link is:
 * the host reads it once a second through {@link #reading} (or a whole set of ports through
 * {@link com.nateyburr.beyondthestars.machines.quasar.burst.BurstHost}).
 * <p>
 * It sits in a host's receptor socket (the Eye's rail and halo, a crown's base, the Kerr's utility sockets, the
 * Homogeny's stirring ring, Reality Diffraction's portal), or stands alone; a port in no host just records. Bursts on a
 * host are consumed: the port keeps nothing.
 * <p>
 * <b>Q-8.</b> The port remembers which quasars fed it ({@code known}, saved as positions only, never as a link). When a
 * remembered source stops arriving and is FROZEN or not loaded, {@link #reading} says {@code paused}: the host holds
 * its
 * field still instead of reading zero. A source that is running but no longer reaches the port (a broken path), or is
 * gone, is forgotten and felt in full; so is a paused one silent for {@link #FORGET_PAUSED_TICKS} while another running
 * source feeds the port (P4-83). After a load, every remembered source pauses the host for
 * {@link #RESTORE_WARMUP_TICKS} until it arrives again, so a restart never reads one empty second (P4-92).
 */
public class MTEHatchGRBReceptor extends MTEHatch {

    /** A delivery counts for this many ticks (two seconds: one jet second plus a tick's jitter). */
    public static final long FRESH_TICKS = JetPacket.STALE_TICKS;
    /** A host that stops reading for this long no longer counts as the port's host. */
    public static final long HOST_STALE_TICKS = 60;
    /** Sources remembered for Q-8 (a port fed by more than this many quasars forgets the oldest). */
    private static final int MAX_KNOWN = 8;
    /**
     * A remembered source that pauses (frozen or not loaded) is forgotten once it has been silent this long while
     * another
     * running source feeds the port (10 min; P4-83): the port was re-aimed at another quasar, so the old one must not
     * pause the host for ever. With no other source arriving it is kept, and Q-8 holds the host still.
     */
    static final long FORGET_PAUSED_TICKS = 12_000;
    /**
     * After a load, the remembered sources hold the host paused for this long (from the port's first reading) while
     * they have not arrived yet, even if running (P4-92): at a restart the quasar forms before every Mouth, Mirror and
     * Junction on its path has formed and re-registered, so the first second or two deliver nothing. Reading that as
     * zero reset a saved link streak (the Eye's Stability / Heavy Link). 200 ticks = the startup check plus two
     * element seconds.
     */
    static final long RESTORE_WARMUP_TICKS = 200;

    private static final IIconContainer OVERLAY = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_GRB_RECEPTOR");
    private static final IIconContainer OVERLAY_GLOW = Textures.BlockIcons
        .custom("beyondthestars:iconsets/OVERLAY_HATCH_GRB_RECEPTOR_GLOW");

    /** One source and jet port's delivery, the last second it arrived. */
    private static final class Rec {

        final BurstSource source;
        double bu;
        boolean linkable;
        long tick;
        /** P4-88: the tightest Junction out-1 limit on this second's streams (+infinity: none). */
        double limit = Double.POSITIVE_INFINITY;
        /** SMBH (smbh-v1.md §1.3): the source record this second, the lattice in force and the hole's mass, SM. */
        int lattice;
        double mass;

        Rec(BurstSource source) {
            this.source = source;
        }
    }

    // ---- live (not saved: nothing about a link is saved)
    private final Map<String, Rec> recs = new LinkedHashMap<>();
    private String host = "";
    private long lastPoll = Long.MIN_VALUE / 2;
    private double lifetimeBuS;

    // ---- saved (positions only, for Q-8 across a restart)
    private final Map<BurstSource, Long> known = new LinkedHashMap<>();
    /** True from an NBT load with remembered sources until the first {@link #reading}, which starts the warm-up. */
    private boolean restored;
    /** The world tick the post-load warm-up ends (P4-92); 0 when none runs. Not saved. */
    private long warmUntil;

    // ---- dev-server bench only ({@code /btsconduit inject}), never saved
    private BurstSource benchFrom;
    private double benchBu;

    public MTEHatchGRBReceptor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 11, 0, "Takes a gamma-ray burst stream into a machine");
    }

    public MTEHatchGRBReceptor(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchGRBReceptor(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public String[] getDescription() {
        return new String[] { "Takes a gamma-ray burst stream (Bu/t) into a machine. Any side: aim a conduit at it.",
            "1 Bu/t is worth 10^13 EU/t in a crown (a 4,096 SM quasar's 9,796 Bu/t: 9.8e16 EU/t, 98 quadrillion).",
            "Records each second how much arrived from which Quasar Engine: that is the machine's §bquasar link§7.",
            "Linked: bursts arrived last second from a formed quasar with its hole open and coils powered,",
            "  through an open path (every element fuelled, every ray clear). Nothing is saved or paired by hand.",
            "Used by: Eye of Harmony T7/T8 (rail and halo), Burst Thermaliser Crown (remote feed), Kerr (utility",
            "  socket), Homogeny T2/T3 (stirring ring), Reality Diffraction (portal). Bursts here are consumed.",
            "If its quasar is not loaded or frozen, the machine pauses (it does not read zero). Keep it chunk-loaded.",
            "Keep every Conduit element and Wormhole Mouth on the path chunk-loaded too: an unloaded one stops the stream.",
            "Right-click: what arrives, from where." };
    }

    // ------------------------------------------------------------------ the conduit side

    private long now() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        return base == null || base.getWorld() == null ? 0
            : base.getWorld()
                .getTotalWorldTime();
    }

    /** A stream arrives (the conduit solver, server thread). Same-second arrivals by two paths add up. */
    public void receive(BurstStream s) {
        if (s == null || s.source == null) return;
        final long now = now();
        final Rec r = recs.computeIfAbsent(s.key(), k -> new Rec(s.source));
        if (r.tick != now) {
            r.tick = now;
            r.bu = 0;
            r.linkable = false;
            r.limit = Double.POSITIVE_INFINITY;
            r.lattice = 0;
            r.mass = 0;
        }
        r.bu += s.buPerTick;
        r.linkable |= s.linkable;
        r.limit = Math.min(r.limit, s.limit);
        r.lattice = Math.max(r.lattice, s.lattice);
        r.mass = Math.max(r.mass, s.mass);
        lifetimeBuS += s.buPerTick;
        known.remove(s.source);
        known.put(s.source, now);
        while (known.size() > MAX_KNOWN) {
            final Iterator<BurstSource> it = known.keySet()
                .iterator();
            it.next();
            it.remove();
        }
    }

    /**
     * Last second at this port: total, per source, linked sources, and Q-8's pause. Also forgets sources that are
     * running (or gone) but no longer arrive. Server thread.
     */
    public BurstReading reading() {
        final long now = now();
        if (restored) {
            // the entries keep tick 0, so P4-83's forget clock is not restarted by a load
            restored = false;
            warmUntil = now + RESTORE_WARMUP_TICKS;
        }
        final boolean warming = now < warmUntil;
        final BurstReading out = new BurstReading();
        out.ports = 1;
        for (Iterator<Rec> it = recs.values()
            .iterator(); it.hasNext();) {
            final Rec r = it.next();
            if (now - r.tick > FRESH_TICKS) {
                it.remove();
                continue;
            }
            out.add(r.source, r.bu, r.linkable);
            out.record(r.source, r.limit, r.lattice, r.mass);
        }
        for (Iterator<Map.Entry<BurstSource, Long>> it = known.entrySet()
            .iterator(); it.hasNext();) {
            final Map.Entry<BurstSource, Long> e = it.next();
            if (out.bySource.containsKey(e.getKey())) continue;
            if (warming || e.getKey()
                .pauses()) {
                // QA-B20: never during the warm-up: restored sources carry tick 0, and the first to arrive would
                // make the port forget the others at once
                if (!warming && !out.bySource.isEmpty() && now - e.getValue() > FORGET_PAUSED_TICKS) {
                    it.remove();
                    continue;
                }
                if (!out.paused) {
                    out.paused = true;
                    out.pausedBy = e.getKey();
                }
            } else if (now - e.getValue() > FRESH_TICKS) {
                it.remove();
            }
        }
        return out;
    }

    /** The host names itself once a second (scanner, GUI). */
    public void poll(String hostName) {
        lastPoll = now();
        host = hostName == null ? "" : hostName;
    }

    public String getHost() {
        return now() - lastPoll <= HOST_STALE_TICKS ? host : "";
    }

    /** Lifetime Bu·s received (GUI). */
    public double getLifetime() {
        return lifetimeBuS;
    }

    /** Lines for the GUI and the scanner. Server state. */
    public List<String> statusLines() {
        final List<String> out = new ArrayList<>();
        final String h = getHost();
        out.add("GRB Receptor Port" + (h.isEmpty() ? " (in no machine: it only records)" : " in " + h));
        final BurstReading r = reading();
        out.add(
            "Last second: " + com.nateyburr.beyondthestars.machines.gui.BTSFormat.buEuShort(r.total)
                + (r.linked() ? ", LINKED" : ""));
        out.add(r.line());
        return out;
    }

    public String statusText() {
        return String.join("\n", statusLines());
    }

    @Override
    public String[] getInfoData() {
        final List<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.addAll(statusLines());
        return info.toArray(new String[0]);
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    // ------------------------------------------------------------------ GT plumbing

    /**
     * Test bench ({@code /btsconduit inject}): every second this port receives {@code bu} Bu/t from the real quasar
     * {@code from}, as if a conduit delivered it - linkable exactly when that quasar's own jets are (formed, OPEN,
     * b &gt; 0); nothing while it is frozen or unloaded, so Q-8 is the real one. {@code from == null} stops it.
     */
    public void debugInject(BurstSource from, double bu) {
        benchFrom = from;
        benchBu = Math.max(0, bu);
    }

    private void benchSecond() {
        if (benchFrom == null || benchFrom.status() != BurstSource.Status.RUNNING) return;
        final MTEQuasarEngine q = benchFrom.resolve();
        if (q == null) return;
        // the source record too (SMBH: the lattice in force and the mass), so the bench feeds a host what a real stream
        // from that quasar would carry (the Eye's Supermassive Link reads it)
        receive(
            BurstStream.of(
                new JetPacket(
                    benchFrom,
                    JetPort.TOP,
                    benchBu,
                    q.linkable(),
                    now(),
                    q.getLatticeInForce(),
                    q.getMass())));
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && aTick % 20 == 0) {
            benchSecond();
            boolean lit = false;
            final long now = now();
            for (Rec r : recs.values()) lit |= now - r.tick <= FRESH_TICKS && r.bu > 1e-9;
            aBaseMetaTileEntity.setActive(lit);
            if (lit) aBaseMetaTileEntity.markDirty(); // the remembered sources and lifetime are saved (P4-85)
        }
    }

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
        final NBTTagList list = new NBTTagList();
        for (BurstSource s : known.keySet()) list.appendTag(new NBTTagIntArray(s.toArray()));
        aNBT.setTag("btsReceptorKnown", list);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        known.clear();
        final NBTTagList list = aNBT.getTagList("btsReceptorKnown", Constants.NBT.TAG_INT_ARRAY);
        for (int i = 0; i < list.tagCount(); i++) {
            final BurstSource s = BurstSource.fromArray(list.func_150306_c(i));
            if (s != null) known.put(s, 0L);
        }
        restored = !known.isEmpty(); // the warm-up starts at the first reading (P4-92)
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
        return new GRBReceptorGui(this).build(data, syncManager, uiSettings);
    }
}
