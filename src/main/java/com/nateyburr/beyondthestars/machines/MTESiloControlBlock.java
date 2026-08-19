package com.nateyburr.beyondthestars.machines;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SCREEN;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * Silo Control Block (TC-2) - the reactor panel socketed in the silo face directly above the
 * main station. Right-click reads the reactor out loud: tier, cascade, heat, decay heat,
 * radiation. GUI-risk ruling: a hatch cannot cleanly host the main controller's MUI2 screen
 * in .52, so the panel is an explicit chat readout - testable and always in sync.
 */
public class MTESiloControlBlock extends MTEHatch {

    /** The main station this panel is socketed into; set by the structure walk. */
    private MTEReactorCoreMk1 parent;

    public MTESiloControlBlock(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            4,
            0,
            new String[] { "The reactor panel - socket it in the silo face above the main station",
                "Right-click for the live reactor readout:", "tier, rod cascade, core heat, decay heat, radiation" });
    }

    public MTESiloControlBlock(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTESiloControlBlock(mName, mTier, mDescriptionArray, mTextures);
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
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!aBaseMetaTileEntity.isServerSide()) return true;
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String reset = EnumChatFormatting.RESET.toString();
        aPlayer.addChatMessage(new ChatComponentText(aqua + "== Silo Control Block ==" + reset));
        if (!parentValid()) {
            aPlayer.addChatMessage(new ChatComponentText("Not linked - reform the main station's structure"));
            return true;
        }
        final MTEReactorCoreMk1 silo = parent;
        aPlayer.addChatMessage(
            new ChatComponentText(
                silo.isSiloBuilt()
                    ? "Steam Generator: tier " + silo.getReactorTier() + " (" + silo.getEdge() + " wide octagon)"
                    : "Steam Generator: NOT BUILT"));
        aPlayer.addChatMessage(
            new ChatComponentText(
                silo.isCascadeMode()
                    ? "Cascade: " + silo.getLiveColumns() + " live / " + silo.getTotalColumns() + " pillars"
                    : "Fuel: assembly fallback (output halved) - " + silo.getFuelColumns()
                        + "/"
                        + silo.getColumnCap()
                        + " columns"));
        final int heat = silo.getHeat();
        final String heatColor = (heat >= 8000 ? EnumChatFormatting.RED
            : heat >= 4000 ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN).toString();
        aPlayer.addChatMessage(new ChatComponentText(heatColor + "Heat: " + heat + " / 10,000 (SCRAM 8,000)" + reset));
        aPlayer.addChatMessage(
            new ChatComponentText(
                silo.getDecaySeconds() > 0 ? "Decay heat: cooling required for " + silo.getDecaySeconds() + "s"
                    : "Decay heat: none"));
        aPlayer.addChatMessage(
            new ChatComponentText(
                silo.getRadiationSeconds() > 0
                    ? EnumChatFormatting.DARK_RED + "RADIATION FIELD: " + silo.getRadiationSeconds() + "s" + reset
                    : "Radiation: site clear"));
        return true;
    }
}
