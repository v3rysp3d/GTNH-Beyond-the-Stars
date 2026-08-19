package com.nateyburr.beyondthestars.machines;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SCREEN;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * Turbine Control Block (TC-2/3) - the turbine panel socketed in the rotor gallery's front
 * wall, and the plant's THROTTLE LEVER: every redstone RISING EDGE on any side requests one
 * turbine cycle from the main station (one buffered steam slug + one +/-2t injector window
 * + RPM gain). Right-click reads the turbine out loud: RPM, cycles/min, stages, rotor
 * health, propellant, EU. Same chat-readout ruling as the Silo Control Block.
 */
public class MTETurbineControlBlock extends MTEHatch {

    /** The main station this panel is socketed into; set by the structure walk. */
    private MTEReactorCoreMk1 parent;
    /**
     * Last observed max input signal, for rising-edge detection (persisted so a held-high
     * signal cannot fire a spurious cycle across a chunk reload).
     */
    private boolean prevPowered;

    public MTETurbineControlBlock(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            4,
            0,
            new String[] { "The turbine panel - socket it in the rotor gallery's front wall",
                "A redstone RISING EDGE runs one turbine cycle:",
                "one buffered steam slug + one +/-2t injector window + RPM gain",
                "No clock = RPM decays to zero. Right-click for the live turbine readout" });
    }

    public MTETurbineControlBlock(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTETurbineControlBlock(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public ITexture[] getTexturesActive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY_SCREEN) };
    }

    @Override
    public ITexture[] getTexturesInactive(ITexture aBaseTexture) {
        return new ITexture[] { aBaseTexture, TextureFactory.of(OVERLAY_SCREEN) };
    }

    public void setParent(MTEReactorCoreMk1 parent) {
        this.parent = parent;
    }

    public MTEReactorCoreMk1 getParent() {
        return parent;
    }

    private boolean parentValid() {
        return parent != null && parent.isValid();
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (!aBaseMetaTileEntity.isServerSide()) return;
        byte strongest = 0;
        for (final ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            final byte signal = aBaseMetaTileEntity.getInternalInputRedstoneSignal(side);
            if (signal > strongest) strongest = signal;
        }
        final boolean powered = strongest > 0;
        if (powered && !prevPowered && parentValid()) {
            parent.requestTurbineCycle();
        }
        prevPowered = powered;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("btsPrevPowered", prevPowered);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        prevPowered = aNBT.getBoolean("btsPrevPowered");
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!aBaseMetaTileEntity.isServerSide()) return true;
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String reset = EnumChatFormatting.RESET.toString();
        aPlayer.addChatMessage(new ChatComponentText(aqua + "== Turbine Control Block ==" + reset));
        if (!parentValid()) {
            aPlayer.addChatMessage(new ChatComponentText("Not linked - reform the main station's structure"));
            return true;
        }
        final MTEReactorCoreMk1 silo = parent;
        aPlayer.addChatMessage(
            new ChatComponentText(
                "RPM: " + String.format("%,d", silo.getRpm())
                    + " / "
                    + String.format("%,d", MTEReactorCoreMk1.RPM_REDLINE)
                    + " redline"));
        aPlayer.addChatMessage(
            new ChatComponentText(
                "Cycles: " + silo.getCyclesPerMinute()
                    + "/min measured - last cycle timed "
                    + silo.getLastGoodInjectors()
                    + "/8"));
        aPlayer.addChatMessage(
            new ChatComponentText(
                "Propellant factor: x" + String.format("%.2f", silo.getLastCyclePropMilli() / 1000.0)));
        aPlayer.addChatMessage(
            new ChatComponentText(
                "Stages: " + silo.getGalleryStages()
                    + "/8 - avg rotor factor x"
                    + String.format("%.2f", silo.getAvgRotorFactorMilli() / 1000.0)));
        aPlayer
            .addChatMessage(new ChatComponentText("Output: " + String.format("%,d", silo.getLastEUOutput()) + " EU/t"));
        return true;
    }
}
