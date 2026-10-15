package com.nateyburr.beyondthestars.machines.eye;

import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;

import com.nateyburr.beyondthestars.items.SLP5Items;
import com.nateyburr.beyondthestars.machines.eye.field.AttractorField;
import com.nateyburr.beyondthestars.machines.eye.star.EyeAttractor;
import com.nateyburr.beyondthestars.machines.gui.BTSTip;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;

/**
 * P5: the Pellet Injector Hatch (MTE 28128; p5-t9-t10.md §3.3), the Eye of Harmony's pellet beam. An input bus for the
 * top collar of the T9 collimator stack: it holds Gated Pellets (any grade; it fires the highest it holds, or the grade
 * locked here) and an Injector Barrel (any slot; never consumed) whose Mk sets the cap, 4 / 8 / 16 pellets per second.
 * The rate is the Junction formula on the injector's own Burst Control Hatch (the other top-collar socket):
 * {@code n = cap · clamp(c + k·(s/15 − 0.5), 0, 1)}; the centre c is "auto" (ρ = 1 at V = 0) or set here, the gain k
 * defaults to 1. The Eye reads it once a field second and draws whole pellets; below T9 it fires only for the Deep
 * Link's test fire.
 * <p>
 * Settings (grade lock, centre, gain) are tile NBT and kept on the item when wrenched; the screwdriver cycles the grade
 * lock, OpenComputers and the Eye's GUI set all three.
 */
public class MTEHatchPelletInjector extends MTEHatchInputBus {

    /** 4 × 4 pellet slots (a barrel goes in any of them). */
    public static final int SLOTS = 16;

    /** 0 = fire the highest grade in stock, 1..3 = Magnetar / Ergomatter / Superdense only. */
    private int lockedGrade;
    private boolean centreAuto = true;
    private double centre = 0.5, gain = 1;

    public MTEHatchPelletInjector(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, 13, SLOTS, new String[] { "Fires pellets into the Great Attractor." });
    }

    public MTEHatchPelletInjector(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, SLOTS, aDescription, aTextures);
    }

    @Override
    public gregtech.api.metatileentity.MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEHatchPelletInjector(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public String[] getDescription() {
        return BTSTip.withDetails(
            new String[] { "Fires pellets into the Great Attractor. Higher-GeV pellets hold bigger flows.",
                "Goes on the top collar of the Eye's T9 collimator stack.",
                "Rate cap by barrel: Mk I " + BTSTip
                    .num(4) + ", Mk II " + BTSTip.num(8) + ", Mk III " + BTSTip.num(16) + " pellets/s.",
                "Ignored below Eye T9 except for the Deep Link test fire.",
                BTSTip.hint("Screwdriver: lock the grade fired.") },
            BTSTip.heading("Pellets") + ": Magnetar "
                + BTSTip.num("1.25 EeV")
                + ", Ergomatter "
                + BTSTip.num("5 EeV")
                + ", Superdense "
                + BTSTip.num("20 EeV")
                + ".",
            BTSTip.heading("Rate") + ": pellets/s = cap x clamp(c + k (s/15 - 0.5)),",
            "s from the Burst Control Hatch beside it (mode Pellet beam).",
            "c auto fires rho 1 at V 0; k defaults to " + BTSTip.num(1) + ".",
            BTSTip.heading("Hold rating") + ": clamp(floor(4 (rho_max - 2)), 0, 12),",
            "rho_max = cap x GeV / 10^10. The largest Galactic Flow it holds.",
            "Superdense on Mk I: rho_max " + BTSTip.num(8) + ", rating " + BTSTip.num(12) + ".");
    }

    @Override
    public boolean allowSelectCircuit() {
        return false;
    }

    /** Only Gated Pellets and Injector Barrels go in. */
    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return (SLP5Items.pelletGrade(aStack) > 0 || SLP5Items.barrelTier(aStack) > 0)
            && super.allowPutStack(aBaseMetaTileEntity, aIndex, side, aStack);
    }

    // ------------------------------------------------------------------ what the Eye reads

    /** The best Injector Barrel in the slots (1..3), 0 none. */
    public int barrelMk() {
        int best = 0;
        for (int i = 0; i < SLOTS; i++) best = Math.max(best, SLP5Items.barrelTier(mInventory[i]));
        return best;
    }

    /** Pellets of a grade (1..3) in the slots. */
    public long pellets(int grade) {
        long n = 0;
        for (int i = 0; i < SLOTS; i++) {
            final ItemStack s = mInventory[i];
            if (s != null && s.stackSize > 0 && SLP5Items.pelletGrade(s) == grade) n += s.stackSize;
        }
        return n;
    }

    /** Takes up to {@code count} pellets of a grade; returns how many. */
    public long takePellets(int grade, long count) {
        long got = 0;
        for (int i = 0; i < SLOTS && got < count; i++) {
            final ItemStack s = mInventory[i];
            if (s == null || s.stackSize <= 0 || SLP5Items.pelletGrade(s) != grade) continue;
            final int t = (int) Math.min(s.stackSize, count - got);
            s.stackSize -= t;
            got += t;
            if (s.stackSize <= 0) mInventory[i] = null;
        }
        if (got > 0) {
            updateSlots();
            final IGregTechTileEntity base = getBaseMetaTileEntity();
            if (base != null) base.markDirty();
        }
        return got;
    }

    public int lockedGrade() {
        return lockedGrade;
    }

    public boolean centreAuto() {
        return centreAuto;
    }

    public double centre() {
        return centre;
    }

    public double gain() {
        return gain;
    }

    /** OpenComputers / the Eye's GUI: the grade lock (0 auto, 1..3). */
    public void setLockedGrade(int g) {
        lockedGrade = Math.max(0, Math.min(3, g));
        changed();
    }

    /** OpenComputers / the Eye's GUI: the centre (NaN or negative: auto) and the gain (0..4). */
    public void setBeam(double c, double k) {
        if (Double.isNaN(c) || c < 0) centreAuto = true;
        else {
            centreAuto = false;
            centre = Math.max(0, Math.min(1, c));
        }
        if (!Double.isNaN(k)) gain = Math.max(0, Math.min(4, k));
        changed();
    }

    private void changed() {
        final IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base != null) base.markDirty();
    }

    /** One status line (scanner, the Eye's GUI). */
    public String statusLine() {
        final int mk = barrelMk();
        final StringBuilder sb = new StringBuilder();
        sb.append(mk > 0 ? "barrel Mk " + mk + " (" + AttractorField.cap(mk) + " /s)" : "no barrel");
        for (int g = 3; g >= 1; g--) {
            final long n = pellets(g);
            if (n > 0) sb.append(", ")
                .append(EyeAttractor.gradeName(g))
                .append(' ')
                .append(n);
        }
        sb.append(lockedGrade > 0 ? ", locked to " + EyeAttractor.gradeName(lockedGrade) : ", highest grade first");
        sb.append(centreAuto ? ", centre auto" : String.format(Locale.ROOT, ", centre %.3f", centre));
        sb.append(String.format(Locale.ROOT, ", gain %.2f", gain));
        return sb.toString();
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        lockedGrade = (lockedGrade + 1) % 4;
        changed();
        if (aPlayer != null) aPlayer.addChatComponentMessage(
            new ChatComponentText(
                "Pellet Injector: " + (lockedGrade == 0 ? "fires the highest grade in stock"
                    : "fires " + EyeAttractor.gradeName(lockedGrade) + " pellets only")));
    }

    // ------------------------------------------------------------------ NBT (tile and item: trap 2)

    private void writeSettings(NBTTagCompound nbt) {
        nbt.setByte("btsInjGrade", (byte) lockedGrade);
        nbt.setBoolean("btsInjAuto", centreAuto);
        nbt.setDouble("btsInjCentre", centre);
        nbt.setDouble("btsInjGain", gain);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        writeSettings(aNBT);
    }

    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        writeSettings(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (!aNBT.hasKey("btsInjAuto")) return;
        lockedGrade = Math.max(0, Math.min(3, aNBT.getByte("btsInjGrade")));
        centreAuto = aNBT.getBoolean("btsInjAuto");
        centre = Math.max(0, Math.min(1, aNBT.getDouble("btsInjCentre")));
        gain = aNBT.hasKey("btsInjGain") ? Math.max(0, Math.min(4, aNBT.getDouble("btsInjGain"))) : 1;
    }
}
