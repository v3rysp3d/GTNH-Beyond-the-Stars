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
 * Turbine Control Block (TC-2/3) - the turbine panel socketed in the output manifold's back
 * face, above its Maintenance Hatch, and the plant's THROTTLE LEVER: every redstone RISING EDGE
 * on any side requests one turbine cycle from the Nuclear Plant Foundation (one buffered steam
 * slug + one +/-2t injector window + RPM gain). Right-click reads the turbine out loud: RPM, cycles/min, stages, rotor
 * health, propellant, EU. Same chat-readout ruling as the Silo Control Block.
 */
public class MTETurbineControlBlock extends MTEHatch {

    /** The Nuclear Plant Foundation this panel is socketed into; set by the structure walk. */
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
            new String[] { "Turbine panel: output manifold back face, above its Maintenance Hatch",
                "Each redstone pulse runs one turbine cycle (one steam slug + one injector window)",
                "No pulses: the RPM decays to zero", "Right-click: turbine and output readout" });
    }

    public MTETurbineControlBlock(String aName, int aTier, String[] aDescription,
        gregtech.api.interfaces.ITexture[][][] aTextures) {
        super(aName, aTier, 0, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTETurbineControlBlock(mName, mTier, mDescriptionArray, mTextures);
    }

    /**
     * MTEHatch does NOT override this and CommonMetaTileEntity's default is FALSE, so
     * BaseMetaTileEntity.setFrontFacing is a silent no-op without it: the panel stays at the
     * freshly-placed default (ForgeDirection.DOWN), showing its screen overlay only on the
     * buried bottom face and plain Tungstensteel Turbine casing on every visible side - the
     * "the build never placed the control blocks" report. Stock GT aims its hatches the same
     * way (MTEHatchDynamo, MTEHatchEnergy, MTEHatchMaintenance all return true here).
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

    /** Both control blocks' reply while the structure walk has not linked them to a formed plant. */
    static final String NOT_LINKED = EnumChatFormatting.YELLOW
        + "Not linked: the Nuclear Plant Foundation is not formed - fix its structure, then right-click again.";

    /**
     * Tidy C6: the turbine's own section of the plant panel (the TURBINE and OUTPUT groups and
     * the weakest term), not the whole panel. The hold line in the TURBINE group already names
     * the reactor or coolant fact that stops the fan, and the weakest term names the rest.
     */
    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (!aBaseMetaTileEntity.isServerSide()) return true;
        final String aqua = EnumChatFormatting.AQUA.toString();
        final String reset = EnumChatFormatting.RESET.toString();
        aPlayer.addChatMessage(new ChatComponentText(aqua + "== Turbine Control Block ==" + reset));
        if (!parentValid()) {
            aPlayer.addChatMessage(new ChatComponentText(NOT_LINKED));
            return true;
        }
        for (final String line : parent.controlBlockReport(true)) {
            aPlayer.addChatMessage(new ChatComponentText(line));
        }
        return true;
    }
}
