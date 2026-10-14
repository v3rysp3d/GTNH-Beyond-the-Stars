package com.nateyburr.beyondthestars.rendertest;

import java.lang.reflect.Field;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.nateyburr.beyondthestars.machines.BTSMachines;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.SLConsoleTest;
import com.nateyburr.beyondthestars.machines.eye.EyeKit;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.render.EyeStarLook;
import com.nateyburr.beyondthestars.machines.eye.render.TileEyeStar;
import com.nateyburr.beyondthestars.machines.homogeny.MTEHomogeny;
import com.nateyburr.beyondthestars.machines.kerr.KerrKit;
import com.nateyburr.beyondthestars.machines.kerr.KerrSlot;
import com.nateyburr.beyondthestars.machines.kerr.MTEKerrEngine;
import com.nateyburr.beyondthestars.machines.reality.HRKit;
import com.nateyburr.beyondthestars.machines.reality.MTERealityDiffraction;
import com.nateyburr.beyondthestars.machines.reality.MTERealityRefab;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/**
 * DEV-ONLY video recorder ({@link RecClient}), the server half: the script commands that change the world. Every
 * method runs on the integrated server's thread ({@link RenderTest#onServer}).
 */
final class RecServer {

    private RecServer() {}

    /** The controller the last {@code place} put down (the {@code placed} anchor). */
    static volatile int[] placed;

    /**
     * A GT machine's item: {@code ItemList.<name>} (GT's list, e.g. {@code ItemList.Machine_Multi_BlastFurnace}) or
     * {@code bts.<field>} (this addon's {@link BTSMachines}, e.g. {@code bts.eyeOfHarmony}).
     */
    static ItemStack machine(String spec) {
        if (spec.startsWith("ItemList.")) {
            return ItemList.valueOf(spec.substring("ItemList.".length()))
                .get(1L);
        }
        if (spec.startsWith("bts.")) {
            try {
                final Field f = BTSMachines.class.getField(spec.substring(4));
                final Object v = f.get(null);
                return v instanceof ItemStack ? ((ItemStack) v).copy() : null;
            } catch (ReflectiveOperationException bad) {
                throw new IllegalArgumentException("no BTSMachines." + spec.substring(4));
            }
        }
        throw new IllegalArgumentException("machine must be ItemList.<name> or bts.<field>: " + spec);
    }

    static ForgeDirection facing(String name) {
        try {
            return ForgeDirection.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException bad) {
            return ForgeDirection.NORTH;
        }
    }

    /**
     * Places a GT machine's controller at {@code p} facing {@code facing}; it becomes the {@code placed} anchor (and
     * the place {@code name}, if given).
     */
    static String place(String spec, int[] p, ForgeDirection facing, String name) {
        final String r = place(spec, p, facing);
        if (name != null && placed != null) name(name, p, null);
        return r;
    }

    /** A named place ({@code name}: the controller block; {@code name.c}: the machine's centre, if given). */
    static void name(String name, int[] p, double[] centre) {
        AutoRig.POINTS.put(name.toLowerCase(Locale.ROOT), AutoRigEyeAE.ctr(p));
        if (centre != null) AutoRig.POINTS.put(name.toLowerCase(Locale.ROOT) + ".c", centre);
    }

    /**
     * A whole machine by its dev kit, as {@code /btseye} and {@code /btskerr} raise it: {@code eye} (tier 1-6,
     * generators at {@code gen} 0-8, every hatch fitted, unlocked to the tier, formed) or {@code kerr} (T1 or T2 with
     * both modules). Dark / closed: nothing is lit. It becomes the {@code placed} anchor (and {@code name}, if given).
     */
    static String kit(String what, int tier, int[] p, ForgeDirection facing, int gen, String name) {
        final WorldServer world = RenderTest.world();
        RenderTest.loadAndKeep(world, new int[] { p[0] - 100, 0, p[2] - 100, p[0] + 100, 255, p[2] + 160 });
        final StringBuilder out = new StringBuilder();
        double[] centre = null;
        if (what.equals("eye")) {
            final EyeKit.Result r = EyeKit.build(
                world,
                p[0],
                p[1],
                p[2],
                facing,
                Math.max(1, Math.min(8, tier)),
                gen,
                false,
                AutoRig.OWNER,
                "BTS rec",
                false);
            out.append(r.failure != null ? "Eye kit FAILED: " + r.failure : "Eye kit T" + tier);
            for (String l : r.lines) out.append(" | ")
                .append(RenderTest.strip(l));
            final MTEEyeOfHarmonyBTS eye = AutoRig.mteAt(world, p, MTEEyeOfHarmonyBTS.class);
            if (eye != null) {
                final int[] c = com.nateyburr.beyondthestars.machines.eye.render.EyeCentreRender
                    .centrePos(eye.getExtendedFacing(), p[0], p[1], p[2]);
                centre = AutoRigEyeAE.ctr(c);
            }
        } else if (what.equals("kerr")) {
            final KerrKit.Result r = KerrKit
                .build(world, p[0], p[1], p[2], facing, tier >= 2, false, AutoRig.OWNER, "BTS rec");
            out.append(r.failure != null ? "Kerr kit FAILED: " + r.failure : "Kerr kit T" + (tier >= 2 ? 2 : 1));
            for (String l : r.lines) out.append(" | ")
                .append(RenderTest.strip(l));
            final MTEKerrEngine kee = AutoRig.mteAt(world, p, MTEKerrEngine.class);
            if (kee != null) {
                centre = AutoRigEyeAE
                    .ctr(KerrSlot.worldPos(kee.getExtendedFacing(), p[0], p[1], p[2], KerrSlot.centreRel()));
            }
        } else {
            return "kit eye|kerr, not " + what;
        }
        placed = p.clone();
        if (name != null) name(name, p, centre);
        return out.toString();
    }

    /**
     * One round of the Hologram Projector's survival auto-place on the multiblock at {@code p}
     * ({@code survivalConstruct(projector of stack size tier, budget, env)}), drawing on an endless item source as a
     * creative player's projector does: returns the elements placed this round, -1 when everything stands, -2 if
     * nothing survival-constructable is there.
     */
    static int survivalRound(int[] p, int tier, int budget) {
        final WorldServer world = RenderTest.world();
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        Object c = null;
        if (te instanceof IGregTechTileEntity) c = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (!(c instanceof ISurvivalConstructable) && te instanceof IConstructableProvider)
            c = ((IConstructableProvider) te).getConstructable();
        if (!(c instanceof ISurvivalConstructable)) return -2;
        final ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment
            .create(SLConsoleTest.endlessSource(), FakePlayerFactory.getMinecraft(world));
        return ((ISurvivalConstructable) c).survivalConstruct(
            new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(1, tier)),
            Math.max(1, budget),
            env);
    }

    /** The overworld's game time when the current wait or time-lapse began ({@code game:<s>} counts from it). */
    static volatile long markTick;

    /** Marks the game time now (server thread): the {@code game:<s>} condition counts from here. */
    static String markGame() {
        markTick = RenderTest.world()
            .getTotalWorldTime();
        return "game time mark " + markTick;
    }

    /**
     * A condition the recorder can wait for ({@code wait <cond>}, {@code timelapse ... until <cond>}). Plain:
     * {@code eyelit}, {@code holeopen}, {@code aelit}, {@code aedark}, {@code formed} (the {@code placed} machine).
     * With arguments after colons, on a named place (a {@code kit} / {@code place} name or a rig point):
     * {@code formed:<name>}, {@code active:<name>} (the GT machine's active flag), {@code lit:<name>} (the Eye's star
     * burns), {@code dark:<name>}, {@code tier:<name>:<n>} (the Eye's star is tier n or more),
     * {@code state:<name>:<STATE>} (the Eye runtime's state), {@code trans:<name>} / {@code notrans:<name>} (a
     * transition runs / none), {@code unlocked:<name>:<n>}, {@code game:<s>} (s seconds of game time since the wait
     * began: under the time warp that is less real time), {@code me:<place>:<fluid or item name>:<amount>} (the ME
     * network behind the cable at the place holds that much); the circuit chain bench ({@code /btsrecipes chain}):
     * {@code chainbusy} / {@code chainidle} (a {@code make} runs / none), {@code chainstep:<SL-Xnn>} (it is on that
     * step).
     */
    static boolean cond(String spec) {
        final String[] a = spec.split(":");
        final String name = a[0].toLowerCase(Locale.ROOT);
        switch (name) {
            case "eyelit":
                return AutoRig.eyeLit();
            case "holeopen":
                return AutoRig.kee() != null && AutoRig.kee()
                    .isHoleOpen();
            case "aelit":
                return AutoRigEyeAE.lit();
            case "aedark":
                return !AutoRigEyeAE.lit();
            case "game":
                return RenderTest.world()
                    .getTotalWorldTime() - markTick >= (long) (Double.parseDouble(a[1]) * 20);
            case "formed": {
                final int[] p = a.length > 1 ? at(a[1]) : placed;
                if (p == null) return false;
                final MTEMultiBlockBase m = AutoRig.mteAt(RenderTest.world(), p, MTEMultiBlockBase.class);
                return m != null && m.mMachine;
            }
            case "active": {
                final TileEntity te = tile(a[1]);
                return te instanceof IGregTechTileEntity && ((IGregTechTileEntity) te).isActive();
            }
            case "me":
                return meAmount(a[1], a[2]) >= Long.parseLong(a[3]);
            case "chainidle":
                return !com.nateyburr.beyondthestars.CircuitChainBench.busy();
            case "chainbusy":
                return com.nateyburr.beyondthestars.CircuitChainBench.busy();
            case "chainstep":
                return com.nateyburr.beyondthestars.CircuitChainBench.currentStep()
                    .equalsIgnoreCase(a[1]);
            default:
                break;
        }
        final MTEEyeOfHarmonyBTS eye = AutoRig.mteAt(RenderTest.world(), at(a[1]), MTEEyeOfHarmonyBTS.class);
        if (eye == null) return false;
        final com.nateyburr.beyondthestars.machines.eye.star.EyeStarRuntime rt = eye.starRuntime();
        switch (name) {
            case "lit":
                return rt.burning();
            case "dark":
                return !rt.live();
            case "tier":
                return rt.star() != null && rt.live() && rt.star().tier.n >= Integer.parseInt(a[2]);
            case "unlocked":
                return rt.unlockedTier() >= Integer.parseInt(a[2]);
            case "state":
                return rt.state()
                    .name()
                    .equalsIgnoreCase(a[2]);
            case "trans":
                return rt.transition() != null;
            case "notrans":
                return rt.transition() == null;
            default:
                throw new IllegalArgumentException("unknown condition " + spec);
        }
    }

    /** A named place as a block position (throws if unknown). */
    static int[] at(String place) {
        final String k = place.toLowerCase(Locale.ROOT);
        if (k.equals("keectrl") && AutoRig.keeCtrl != null) return AutoRig.keeCtrl.clone();
        if (k.equals("eyectrl") && AutoRig.eyeCtrl != null) return AutoRig.eyeCtrl.clone();
        final double[] d = AutoRig.POINTS.get(k);
        if (d == null) throw new IllegalArgumentException("no place " + place);
        return new int[] { (int) Math.floor(d[0]), (int) Math.floor(d[1]), (int) Math.floor(d[2]) };
    }

    private static TileEntity tile(String place) {
        final int[] p = at(place);
        return RenderTest.world()
            .getTileEntity(p[0], p[1], p[2]);
    }

    /** Litres (a fluid's registry name) or items (an item's display or registry name) in the ME network at a place. */
    static long meAmount(String place, String what) {
        final appeng.api.networking.storage.IStorageGrid g = MeParts.storage(RenderTest.world(), at(place));
        if (g == null) return -1;
        final net.minecraftforge.fluids.Fluid fl = net.minecraftforge.fluids.FluidRegistry.getFluid(what);
        if (fl != null && g.getFluidInventory() != null) {
            for (appeng.api.storage.data.IAEFluidStack st : g.getFluidInventory()
                .getStorageList()) if (st.getFluid() == fl) return st.getStackSize();
            return 0;
        }
        long n = 0;
        for (appeng.api.storage.data.IAEItemStack st : g.getItemInventory()
            .getStorageList()) {
            final ItemStack is = st.getItemStack();
            final String reg = String.valueOf(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()));
            if (is.getDisplayName()
                .equalsIgnoreCase(what.replace('_', ' ')) || reg.equalsIgnoreCase(what)) n += st.getStackSize();
        }
        return n;
    }

    /** One status line for the rigs built (the recorder logs it at the end of each scene). */
    static String status() {
        final StringBuilder b = new StringBuilder();
        if (AutoRig.eyeCtrl != null || AutoRig.keeCtrl != null) b.append(AutoRig.status());
        if (AutoRigEyeAE.ctrl != null) b.append(b.length() > 0 ? " || " : "")
            .append(AutoRigEyeAE.status());
        return b.toString();
    }

    private static String place(String spec, int[] p, ForgeDirection facing) {
        final ItemStack stack = machine(spec);
        if (stack == null) return "no item for " + spec;
        final WorldServer world = RenderTest.world();
        RenderTest.loadAndKeep(world, new int[] { p[0] - 48, 0, p[2] - 48, p[0] + 48, 255, p[2] + 48 });
        final boolean ok = MTEReactorCoreMk1
            .placeGtMachine(world, p[0], p[1], p[2], (short) stack.getItemDamage(), facing);
        if (ok) placed = p.clone();
        return (ok ? "placed " : "COULD NOT place ") + spec + " at " + RenderTest.pos(p) + " facing " + facing;
    }

    /**
     * Builds the multiblock whose controller stands at {@code p} the way a Hologram Projector does when a creative
     * player sneak-uses it ({@code IConstructable.construct(stack, false)}); {@code tier} is the projector's stack
     * size.
     */
    static String construct(int[] p, int tier) {
        final WorldServer world = RenderTest.world();
        final TileEntity te = world.getTileEntity(p[0], p[1], p[2]);
        IConstructable c = null;
        if (te instanceof IConstructableProvider) c = ((IConstructableProvider) te).getConstructable();
        else if (te instanceof IConstructable) c = (IConstructable) te;
        if (c == null) return "nothing constructable at " + RenderTest.pos(p);
        c.construct(new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(1, tier)), false);
        return "built the multiblock at " + RenderTest.pos(p) + " (projector tier " + tier + ")";
    }

    /**
     * Uses (right-clicks) the block at {@code p} as the player, on the face {@code side}, with an empty hand: what a
     * player does to open an AE2 terminal or any block's screen. The hit point is the middle of that face, just
     * inside it, so on an AE2 cable the part on that face is the one used.
     */
    static String use(int[] p, ForgeDirection side) {
        final EntityPlayerMP player = RenderTest.player();
        if (player == null) return "no player";
        final WorldServer world = RenderTest.world();
        final net.minecraft.block.Block block = world.getBlock(p[0], p[1], p[2]);
        final ItemStack held = player.getCurrentEquippedItem();
        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        player.setSneaking(false);
        final float hx = 0.5f + side.offsetX * 0.45f, hy = 0.5f + side.offsetY * 0.45f,
            hz = 0.5f + side.offsetZ * 0.45f;
        final boolean ok = block.onBlockActivated(world, p[0], p[1], p[2], player, side.ordinal(), hx, hy, hz);
        if (held != null) player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
        return "used " + block.getLocalizedName() + " at " + RenderTest.pos(p) + " on its " + side + " face: " + ok;
    }

    /** An ME terminal on the AE2 cable at {@code p} (cable added if the cell has none), named if asked. */
    static String terminal(int[] p, ForgeDirection side, String name) {
        final java.util.List<String> log = new java.util.ArrayList<>();
        final WorldServer world = RenderTest.world();
        final appeng.api.parts.IPartHost h = MeParts.host(world, p);
        final appeng.api.parts.IPart cable = h == null ? null : h.getPart(ForgeDirection.UNKNOWN);
        final appeng.api.util.AEColor color = h == null ? appeng.api.util.AEColor.Transparent : h.getColor();
        if (cable == null) MeParts.cable(world, p, MeParts.Kind.SMART, color, RenderTest.player());
        MeParts.terminal(world, p, side, MeParts.Kind.SMART, color, RenderTest.player(), log, "");
        if (name != null) name(name, p, null);
        return String.join("; ", log);
    }

    /**
     * Named points for the parts of the machine at a named place, so a script can film their screens: an Eye's
     * Directive Hatches ({@code <name>.dir.<set>}, e.g. {@code climb.dir.eye_status}), its Field hatches
     * ({@code <name>.field<i>}) and plinth buses ({@code <name>.bus<i>}); a KEE's module controllers
     * ({@code <name>.modx}, {@code <name>.modc}). Returns what was named.
     */
    static String points(String name) {
        final WorldServer world = RenderTest.world();
        final int[] p = at(name);
        final String n = name.toLowerCase(Locale.ROOT);
        final StringBuilder b = new StringBuilder();
        final MTEEyeOfHarmonyBTS eye = AutoRig.mteAt(world, p, MTEEyeOfHarmonyBTS.class);
        if (eye != null) {
            for (com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective h : eye.directiveHatches()) {
                final IGregTechTileEntity t = h.getBaseMetaTileEntity();
                if (t == null) continue;
                final String k = n + ".dir."
                    + h.getSet()
                        .name()
                        .toLowerCase(Locale.ROOT);
                AutoRig.POINTS.put(k, new double[] { t.getXCoord() + 0.5, t.getYCoord() + 0.5, t.getZCoord() + 0.5 });
                b.append(k)
                    .append(' ');
            }
            int i = 0;
            for (gregtech.api.metatileentity.implementations.MTEHatchInput h : eye.fieldHatches()) {
                final IGregTechTileEntity t = h.getBaseMetaTileEntity();
                if (t != null) AutoRig.POINTS.put(
                    n + ".field" + i++,
                    new double[] { t.getXCoord() + 0.5, t.getYCoord() + 0.5, t.getZCoord() + 0.5 });
            }
            i = 0;
            for (gregtech.api.metatileentity.implementations.MTEHatchInputBus h : eye.itemBuses()) {
                final IGregTechTileEntity t = h.getBaseMetaTileEntity();
                if (t != null) AutoRig.POINTS.put(
                    n + ".bus" + i++,
                    new double[] { t.getXCoord() + 0.5, t.getYCoord() + 0.5, t.getZCoord() + 0.5 });
            }
            b.append(
                eye.fieldHatches()
                    .size())
                .append(" field hatches, ")
                .append(
                    eye.itemBuses()
                        .size())
                .append(" plinth buses");
            return b.toString();
        }
        final MTEKerrEngine kee = AutoRig.mteAt(world, p, MTEKerrEngine.class);
        if (kee != null) {
            for (KerrSlot slot : new KerrSlot[] { KerrSlot.EXTRACTOR, KerrSlot.COMPRESSOR }) {
                final int[] m = slot.modulePos(kee.getExtendedFacing(), p[0], p[1], p[2]);
                final String k = n + (slot == KerrSlot.EXTRACTOR ? ".modx" : ".modc");
                AutoRig.POINTS.put(k, AutoRigEyeAE.ctr(m));
                b.append(k)
                    .append(' ')
                    .append(RenderTest.pos(m))
                    .append(' ');
            }
            return b.toString();
        }
        // renders showcase: a Homogeny / Reality machine's render centre (the manifest's centre.rel) as <name>.c
        final MTEEnhancedMultiBlockBase<?> hr = AutoRig.mteAt(world, p, MTEEnhancedMultiBlockBase.class);
        final HRKit.Machine m = hr instanceof MTEHomogeny ? HRKit.Machine.HOMOGENY
            : hr instanceof MTERealityDiffraction ? HRKit.Machine.DIFFRACTION
                : hr instanceof MTERealityRefab ? HRKit.Machine.REFAB : null;
        if (m != null) {
            final int[] c = SLConsoleTest.worldPos(hr.getExtendedFacing(), p[0], p[1], p[2], HRKit.centreRel(m));
            AutoRig.POINTS.put(n + ".c", new double[] { c[0] + 0.5, c[1] + 0.5, c[2] + 0.5 });
            return n + ".c " + RenderTest.pos(c) + " (" + m.title() + " render centre)";
        }
        return "no Eye, KEE, Homogeny or Reality machine at " + name;
    }

    /**
     * Renders showcase (dev only): the Eye at a named place shows {@code look} instead of its star (null: back to its
     * own star), through the render QA's forced look. The look also goes straight to the centre's render tile (named
     * {@code <name>.c} by {@code kit eye}) at once, not at the centre's next 10-tick send, so a ramp ({@code eyeramp})
     * moves every tick. Render tile only: nothing of the machine changes.
     */
    static String eyeLook(String name, EyeStarLook look) {
        final WorldServer world = RenderTest.world();
        final int[] p = at(name);
        final MTEEyeOfHarmonyBTS eye = AutoRig.mteAt(world, p, MTEEyeOfHarmonyBTS.class);
        if (eye == null) return "no Eye at " + name;
        eye.debugForceLook(look);
        if (look == null) return "Eye " + name + ": its own star again";
        final double[] c = AutoRig.POINTS.get(name.toLowerCase(Locale.ROOT) + ".c");
        if (c != null) {
            final TileEntity te = world
                .getTileEntity((int) Math.floor(c[0]), (int) Math.floor(c[1]), (int) Math.floor(c[2]));
            if (te instanceof TileEyeStar) {
                ((TileEyeStar) te).setLook(look.at(world.getTotalWorldTime()));
            }
        }
        return "Eye " + name + " shows " + look.label();
    }

    /**
     * Renders showcase: an Eye look by name and value ({@code v}: a fraction, a tier or the merger's seconds), or
     * null for {@code off}. Throws on an unknown kind.
     */
    static EyeStarLook look(String kind, double v) {
        final float f = (float) v;
        switch (kind) {
            case "off":
                return null;
            case "protostar": // T1 igniting at f (0..1)
                return EyeStarLook.protostar(1, f);
            case "lit": // tier v's lit star
                return EyeStarLook.lit((int) v, 0, 0);
            case "unstable": // tier v's star at stability 20 on a warning
                return EyeStarLook.lit((int) v, 0.8f, 1);
            case "infusion": // the Mass Infusion at f (Sol toward Sirius A)
                return EyeStarLook.growing(1, f);
            case "shell": // the Shell Ignition at f (Sirius A toward Arcturus)
                return EyeStarLook.growing(2, f);
            case "swell": // Core Collapse phase 1 at f
                return EyeStarLook.swell(f);
            case "bounce": // Core Collapse phase 3: the proto-neutron star and its shock
                return EyeStarLook.bounce(0f);
            case "nova": // a failed Core Collapse's supernova
                return EyeStarLook.NOVA_FLASH;
            case "dwarf": // Sirius B's cooling batch at f
                return EyeStarLook.dwarf(v);
            case "firstcooling": // the First Cooling (T4 -> T5) at f: the first dwarf assembling
                return EyeStarLook.dwarf(Math.max(0.01, v))
                    .transition(4, f);
            case "magmerger": // the Magnetar Merger (T5 -> T6) at f
                return EyeStarLook.lit(6, 0, 0)
                    .transition(5, f);
            case "overfed": // the overfed magnetar at f of the way to the Magnetar Collapse
                return EyeStarLook.overfedMagnetar(f, 0);
            case "collapse": // the Magnetar Collapse (T6 -> T7) at f: Triangulum forming
                return EyeStarLook.galaxy(7, 0.3f, 0)
                    .transition(6, f);
            case "merger": // the Galactic Merger (T7 -> T8) at v seconds of its 2,100
                return EyeStarLook.galaxy(7, 0.3f, 0.5f)
                    .transition(7, (float) Math.max(0, Math.min(1, v / 2100.0)));
            case "galaxy": // tier v's galaxy (7 Triangulum, 8 the Milky Way)
                return EyeStarLook.galaxy((int) v, 0, 0);
            default:
                throw new IllegalArgumentException(
                    "eye look must be off, protostar, lit, unstable, infusion, shell, swell, bounce, nova, dwarf,"
                        + " firstcooling, magmerger, overfed, collapse, merger or galaxy: "
                        + kind);
        }
    }

    /**
     * Every energy hatch of the multiblock at a named place swapped for GT's creative Debug Energy Hatch (tier 13, 64
     * A,
     * the same face), then the structure re-checked: a machine whose bench kit fits charge-once hatches keeps running
     * through a long warped scene. Set up by script (the caption says so).
     */
    static String creativePower(String name) {
        final WorldServer world = RenderTest.world();
        final int[] p = at(name);
        final MTEMultiBlockBase m = AutoRig.mteAt(world, p, MTEMultiBlockBase.class);
        if (m == null) return "no GT multiblock at " + name;
        final java.util.List<gregtech.api.metatileentity.implementations.MTEHatch> hatches = new java.util.ArrayList<>(
            m.mEnergyHatches);
        if (m instanceof gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase) {
            for (gregtech.api.metatileentity.implementations.MTEHatch h : ((gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase<?>) m)
                .getExoticAndNormalEnergyHatchList()) if (!hatches.contains(h)) hatches.add(h);
        }
        int n = 0;
        for (gregtech.api.metatileentity.implementations.MTEHatch h : hatches) {
            final IGregTechTileEntity t = h.getBaseMetaTileEntity();
            if (t == null) continue;
            final int x = t.getXCoord(), y = t.getYCoord(), z = t.getZCoord();
            final ForgeDirection f = t.getFrontFacing();
            world.setBlockToAir(x, y, z);
            if (!MTEReactorCoreMk1.placeGtMachine(
                world,
                x,
                y,
                z,
                (short) ItemList.DebugEnergyHatch.get(1L)
                    .getItemDamage(),
                f)) continue;
            final gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug d = AutoRig.mteAt(
                world,
                new int[] { x, y, z },
                gregtech.api.metatileentity.implementations.MTEHatchEnergyDebug.class);
            if (d != null) {
                d.setVoltageTier(13);
                d.setAmperage(64);
            }
            n++;
        }
        m.checkStructure(true, m.getBaseMetaTileEntity());
        if (m.getBaseMetaTileEntity() != null) m.getBaseMetaTileEntity()
            .enableWorking();
        return n + " energy hatches of "
            + name
            + " swapped for creative Debug Energy Hatches (set up by script); formed "
            + m.mMachine;
    }

    /**
     * The scanner lines of the GT machine at {@code p} (what a Portable Scanner prints: {@code getInfoData}), kept if
     * they match {@code keep} (a regex on the line without colour codes, or null for all).
     */
    static java.util.List<String> scan(int[] p, java.util.regex.Pattern keep) {
        final java.util.List<String> out = new java.util.ArrayList<>();
        final TileEntity te = RenderTest.world()
            .getTileEntity(p[0], p[1], p[2]);
        if (!(te instanceof IGregTechTileEntity)) return out;
        final gregtech.api.interfaces.metatileentity.IMetaTileEntity m = ((IGregTechTileEntity) te).getMetaTileEntity();
        if (m == null) return out;
        final String[] info = m.getInfoData();
        if (info == null) return out;
        for (String l : info) {
            if (l == null || l.trim()
                .isEmpty()) continue;
            if (keep == null || keep.matcher(l.replaceAll("\u00a7.", ""))
                .find()) out.add(l);
        }
        return out;
    }

    /** What the ME network behind the cable at a named place holds (items and fluids), for the log. */
    static String meList(String place) {
        final appeng.api.networking.storage.IStorageGrid g = MeParts.storage(RenderTest.world(), at(place));
        if (g == null) return "no ME network at " + place;
        final StringBuilder b = new StringBuilder("ME at " + place + ":");
        if (g.getItemInventory() != null) for (appeng.api.storage.data.IAEItemStack st : g.getItemInventory()
            .getStorageList())
            b.append(' ')
                .append(st.getStackSize())
                .append("x ")
                .append(
                    st.getItemStack()
                        .getDisplayName())
                .append(';');
        if (g.getFluidInventory() != null) for (appeng.api.storage.data.IAEFluidStack st : g.getFluidInventory()
            .getStorageList())
            b.append(' ')
                .append(st.getStackSize())
                .append(" L ")
                .append(
                    st.getFluidStack()
                        .getLocalizedName())
                .append(';');
        return b.toString();
    }

    /** Runs a server command as the player (op in the integrated world). */
    static String command(String cmd) {
        final EntityPlayerMP player = RenderTest.player();
        if (player == null) return "no player";
        final int r = MinecraftServer.getServer()
            .getCommandManager()
            .executeCommand(player, cmd.startsWith("/") ? cmd.substring(1) : cmd);
        return "command '" + cmd + "' -> " + r;
    }

    /**
     * {@code day} 6000, {@code noon} 6000, {@code sunset} 12000, {@code night} 18000, {@code sunrise} 23000 or ticks.
     */
    static String time(String when) {
        final long t;
        switch (when.toLowerCase(Locale.ROOT)) {
            case "day":
            case "noon":
                t = 6000;
                break;
            case "sunset":
                t = 12000;
                break;
            case "night":
            case "midnight":
                t = 18000;
                break;
            case "sunrise":
                t = 23000;
                break;
            default:
                t = Long.parseLong(when);
                break;
        }
        RenderTest.world()
            .setWorldTime(t);
        return "time " + t;
    }

    /** The daylight cycle on (a time-lapse's moving sky) or off (the recorder's default). */
    static String daylight(boolean on) {
        RenderTest.world()
            .getGameRules()
            .setOrCreateGameRule("doDaylightCycle", String.valueOf(on));
        return "daylight cycle " + (on ? "on" : "off");
    }
}
