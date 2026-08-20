package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_HEAT_EXCHANGER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.MultiblockTooltipBuilder;

/**
 * Cooling Tower - RETIRED CONTROLLER (A3). The hyperboloid itself is very much alive: it is an
 * optional CHECKED PIECE of the main station now ({@code MTEReactorCoreMk1.towerPieceName}),
 * built by the same one hologram projection as everything else, and each standing tower still
 * halves the plant's water draw, doubles passive heat decay and halves the decay-heat demand
 * (max 2). What is retired is this CONTROLLER and the docking mechanic behind it - there is no
 * dock marker to stand it on any more.
 * <p>
 * The MTE id stays registered so existing worlds load cleanly; the recipe is gone and
 * {@link #towerShape()} is now read by the main station, which is the only thing that still
 * checks it.
 */
public class MTEModuleCoolingTower extends MTEPlantModuleBase implements ISurvivalConstructable {

    private static final int HEIGHT = 20;
    private static final int EDGE = 17;

    private static IStructureDefinition<MTEPlantModuleBase> STRUCTURE_DEFINITION = null;

    public MTEModuleCoolingTower(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEModuleCoolingTower(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEModuleCoolingTower(this.mName);
    }

    /** Hyperboloid radius per layer counted from the bottom. */
    private static double radiusAt(int yFromBottom) {
        final double t = yFromBottom / (double) (HEIGHT - 1);
        final double rBase = 7.5, rWaist = 4.6, rTop = 5.6, waistT = 0.62;
        return t < waistT ? rBase + (rWaist - rBase) * (t / waistT)
            : rWaist + (rTop - rWaist) * ((t - waistT) / (1 - waistT));
    }

    /** Package-visible: the main station pre-builds this shell via its own hologram. */
    static String[][] towerShape() {
        final String[][] layers = new String[HEIGHT][];
        for (int i = 0; i < HEIGHT; i++) {
            final int yFromBottom = HEIGHT - 1 - i;
            final double r = radiusAt(yFromBottom);
            final char ring = yFromBottom == 0 ? 'S' : 'N';
            final String[] rows = new String[EDGE];
            for (int z = 0; z < EDGE; z++) {
                final char[] row = new char[EDGE];
                for (int x = 0; x < EDGE; x++) {
                    final double d = Math.hypot(x - EDGE / 2, z - EDGE / 2);
                    row[x] = (d <= r + 0.5 && d >= r - 0.6) ? ring : ' ';
                }
                if (yFromBottom == 0 && z == 0) {
                    // controller replaces the front-center base ring block
                    row[EDGE / 2] = row[EDGE / 2] == 'S' ? '~' : row[EDGE / 2];
                }
                rows[z] = new String(row);
            }
            layers[i] = rows;
        }
        // The base ring's front row may not touch z=0; force a controller anchor row.
        final String[] base = layers[HEIGHT - 1];
        final int frontZ = (int) Math.round(EDGE / 2 - radiusAt(0));
        final char[] frontRow = base[Math.max(0, frontZ)].toCharArray();
        frontRow[EDGE / 2] = '~';
        base[Math.max(0, frontZ)] = new String(frontRow);
        return layers;
    }

    private static int controllerDepth() {
        return Math.max(0, (int) Math.round(EDGE / 2 - radiusAt(0)));
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        moduleCasings = 0;
        if (!checkPiece(mName, EDGE / 2, HEIGHT - 1, controllerDepth(), errors)) return;
        checkCasingMin(errors, moduleCasings, 30);
        // A3: retired, so this museum controller stopped demanding the Maintenance Hatch that
        // 0.19.0 required in the base ring. R1-C: the MAIN STATION TOLERATES one there - its
        // 'R' element checks for Solid Steel OR a Maintenance Hatch tile - but it does NOT
        // adopt it. A ring hatch that joined the plant's maintenance list satisfied the silo's
        // own demand and suppressed a bare platform's self-repair; a legacy ring now forms
        // without contributing anything to the plant-wide lists.
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            mName,
            stackSize,
            EDGE / 2,
            HEIGHT - 1,
            controllerDepth(),
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, EDGE / 2, HEIGHT - 1, controllerDepth());
    }

    @Override
    public IStructureDefinition<MTEPlantModuleBase> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<MTEPlantModuleBase>builder()
                .addShape(mName, transpose(towerShape()))
                .addElement('N', ofBlock(GregTechAPI.sBlockConcretes, 8))
                .addElement(
                    'S',
                    buildHatchAdder(MTEPlantModuleBase.class).atLeast(Maintenance)
                        .casingIndex(Casings.SolidSteelMachineCasing.textureId)
                        .hint(1)
                        .buildAndChain(
                            onElementPass(x -> ++x.moduleCasings, Casings.SolidSteelMachineCasing.asElement())))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        final String aqua = net.minecraft.util.EnumChatFormatting.AQUA.toString();
        final String gray = net.minecraft.util.EnumChatFormatting.GRAY.toString();
        final String reset = net.minecraft.util.EnumChatFormatting.RESET.toString()
            + net.minecraft.util.EnumChatFormatting.GRAY;
        final String yellow = net.minecraft.util.EnumChatFormatting.YELLOW.toString();
        tt.addMachineType("Plant Module - Cooling Tower (RETIRED)")
            .addInfo(yellow + "RETIRED - this controller does nothing any more." + reset)
            .addSeparator()
            .addInfo(aqua + "The tower itself is now part of the MAIN PLANT." + reset)
            .addInfo("Project the Nuclear Plant Foundation: ONE projection raises the platform, the")
            .addInfo("silo, the whole steam chain AND both Cooling Towers, and the main station")
            .addInfo("checks them as optional pieces - no dock, no second controller.")
            .addInfo("Per standing tower: water draw x0.5 - passive heat decay x2 - decay demand x0.5.")
            .addInfo("Two towers: post-shutdown cooling is fully passive. Max 2.")
            .addInfo(yellow + "MIGRATION - exactly one thing to do:" + reset)
            .addInfo(yellow + "The main station wants a CLOSED base ring, and this controller is standing in")
            .addInfo("it. Break this block, put a Solid Steel Machine Casing in its place, and one more")
            .addInfo("in the cell directly OUTWARD of it - two casings per tower, and that is all." + reset)
            .addInfo(aqua + "The 0.19.0 MAINTENANCE HATCH in the base ring can STAY." + reset)
            .addInfo("The main station accepts it as a ring cell, but does NOT count it: the plant's")
            .addInfo("three real maintenance sockets are the silo face, the Spiral floor and the")
            .addInfo("manifold. Replacing it with a Solid Steel casing is equally fine.")
            .addInfo(gray + "Kept only so worlds holding one of these still load. No recipe." + reset)
            .beginStructureBlock(17, 20, 17, true)
            .addController("Museum piece - the tower is checked by the main station now")
            .addMaintenanceHatch("Base ring - OPTIONAL now; a legacy one is tolerated, never counted", 1)
            .addCasingInfoRange("Solid Steel Machine Casing (base ring)", 30, 46, false)
            .addCasingInfoRange("Light Concrete (hyperboloid shell)", 500, 700, false)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            rTexture = new ITexture[] { Casings.SolidSteelMachineCasing.getCasingTexture(), TextureFactory.builder()
                .addIcon(aActive ? OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE : OVERLAY_FRONT_HEAT_EXCHANGER)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(aActive ? OVERLAY_FRONT_HEAT_EXCHANGER_ACTIVE_GLOW : OVERLAY_FRONT_HEAT_EXCHANGER_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        } else {
            rTexture = new ITexture[] { Casings.SolidSteelMachineCasing.getCasingTexture() };
        }
        return rTexture;
    }
}
