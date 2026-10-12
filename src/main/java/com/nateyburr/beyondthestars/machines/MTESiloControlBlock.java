package com.nateyburr.beyondthestars.machines;

import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_SCREEN;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.render.TextureFactory;

/**
 * Silo Control Block (TC-2) - the reactor panel socketed in the silo face directly above the
 * Nuclear Plant Foundation controller. Right-click reads the reactor out loud: tier, cascade, heat, decay heat,
 * radiation. GUI-risk ruling: a hatch cannot cleanly host the main controller's MUI2 screen
 * in .52, so the panel is an explicit chat readout - testable and always in sync.
 */
public class MTESiloControlBlock extends MTEHatch {

    /** The Nuclear Plant Foundation this panel is socketed into; set by the structure walk. */
    private MTEReactorCoreMk1 parent;

    public MTESiloControlBlock(int aID, String aName, String aNameRegional) {
        super(
            aID,
            aName,
            aNameRegional,
            4,
            0,
            new String[] { "Reactor panel: silo face, directly above the controller",
                "Right-click: reactor readout (tier, rods, heat, supplies)" });
    }

    public MTESiloControlBlock(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTESiloControlBlock(mName, mTier, mDescriptionArray, mTextures);
    }

    /**
     * MTEHatch does NOT override this and CommonMetaTileEntity's default is FALSE, which
     * makes BaseMetaTileEntity.setFrontFacing a silent no-op: the panel would stay pinned at
     * the freshly-placed default (ForgeDirection.DOWN), painting its screen overlay on the
     * buried bottom face and the plain casing background on every visible side - i.e.
     * indistinguishable from the Radiation Proof casing it replaced. That is exactly the
     * "the build never placed the control blocks" report. Every stock GT hatch that is meant
     * to be aimed (Dynamo, Energy, Input, Maintenance...) overrides this the same way.
     */
    @Override
    public boolean isFacingValid(ForgeDirection facing) {
        return true;
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

    /**
     * Tidy C6: the reactor's own section of the plant panel (the REACTOR group and the weakest
     * term), not the whole 35-45 line panel - that is the controller GUI's job. Same formatters,
     * so the two cannot disagree.
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!aBaseMetaTileEntity.isServerSide()) return true;
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String reset = EnumChatFormatting.RESET.toString();
        aPlayer.addChatMessage(new ChatComponentText(aqua + "== Silo Control Block ==" + reset));
        if (!parentValid()) {
            aPlayer.addChatMessage(new ChatComponentText(MTETurbineControlBlock.NOT_LINKED));
            return true;
        }
        for (final String line : parent.controlBlockReport(false)) {
            aPlayer.addChatMessage(new ChatComponentText(line));
        }
        return true;
    }
}
