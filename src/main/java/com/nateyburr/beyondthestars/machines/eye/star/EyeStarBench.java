package com.nateyburr.beyondthestars.machines.eye.star;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.items.SLItems;
import com.nateyburr.beyondthestars.machines.MTEReactorCoreMk1;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchStellarFieldSensor;
import com.nateyburr.beyondthestars.machines.eye.field.FieldReadout;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldSim;

import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.common.misc.WirelessNetworkManager;

/**
 * D-49 test bench for the Eye's live star (d49-build-plan.md §15.2): {@code /btseye star <x> <y> <z> <sub> …}, typed
 * on the dev-server console or by an op. Every subcommand acts on the Eye controller at x y z ("~" relative to the
 * sender).
 * <ul>
 * <li>{@code status} - the scanner lines;</li>
 * <li>{@code target <sol|siriusa|arcturus|neutron|siriusb|magnetar|none>} - the controller-slot chart;</li>
 * <li>{@code lit [chart]} - light the slot's (or the named) star at once, without a charge (T1-T4), and start the
 * bench ledger;</li>
 * <li>{@code seed <n>} - a fresh field with that noise seed (a burning star relights at the gauges' zero);</li>
 * <li>{@code rig <off|bang|fine|none>} - the prototype's regulators replace the Field sockets ({@code none} = the
 * sockets again);</li>
 * <li>{@code sim <seconds> [compare]} - run field seconds now; {@code compare} also runs {@code FieldSim} offline
 * from the same seed and settings and prints both hashes (equal when the Eye matches the prototype);</li>
 * <li>{@code set dial|feed|shock <v>} ({@code shock}: the next Core Collapse's bounce, 150 adopted, 90 for the golden
 * cases), {@code unlock <1-6>}, {@code milestones <n> done}, {@code fill};</li>
 * <li>{@code lose} - D-54: the live star is lost as a Breach loses it (T10: put out), and relights at its band's
 * base;</li>
 * <li>{@code transition [abort]} - the running transition's phase, or abort it as a wrench would; {@code event} - the
 * tier's next event now (its warning this second);</li>
 * <li>{@code sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog]} and {@code probe} - the sensor hatches;</li>
 * <li>{@code wrench} - what picking the controller up and placing it back keeps (item NBT only); {@code fix} - then
 * fix its maintenance and enable it, as the player does (GT keeps no maintenance in item NBT); {@code gt} - GT's view
 * (allowed to work, formed, maintenance, last shutdown);</li>
 * <li>{@code mefield} - swap a Field-socket hatch for an ME stocking input hatch and re-walk (R-15: walked, rejected,
 * never drained); {@code field <fluid> <L>} - pour a trim fluid into the Field-socket hatches (a player's
 * automation); {@code intake [setup [G]]} - the B-108 intake-cap rig (an ME stocking hatch in a gas socket on a network
 * of G gigalitres of each gas plus a second consumer) and its readout; {@code eu} - 10^18 wireless EU, buffers
 * untouched;</li>
 * <li>{@code render [on|off]} - the centre render: the cell, the block and the look the world holds (and the
 * per-Eye switch).</li>
 * <li>{@code bus <chart> [n]} - n copies of a star chart into a free plinth input bus slot (the automation
 * pickup).</li>
 * </ul>
 */
public final class EyeStarBench {

    private EyeStarBench() {}

    public static final String USAGE = "/btseye star <x> <y> <z> status | target <name> | lit [name] | seed <n> | "
        + "rig <off|bang|fine|none> | sim <s> [compare] | set dial|feed|shock <v> | unlock <1-6> | "
        + "milestones <n> done|reset | transition [abort] | lose | event | crossing | "
        + "fill | sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog] | sensors <channel> <gt|lt> <thr> "
        + "[analog] | probe | wrench | charge [n] | buffer <fluid> <L> | core <name> | escrow | "
        + "bus <chart> [n] | legacy [galactic] [run <key> <n>] | fix | gt | mefield | intake [setup [G]] | eu | "
        + "field <fluid> <L> | eu set <n> | mebus setup|stock|manual|power|status | damage | repair | walk | unload [all] | planet <key> | hatchwrench | "
        + "punch [owner] | "
        + "render [on|off] | "
        + "directive | autorig <s> [cut <k>] | liverig on|off|status | ocloop | compression <1-9> [all] | check | "
        + "gui [info|galaxy] | "
        + "P4: burst <Bu/t> [high] [halo] [quasar <mean>] | burst off | galaxy <s> [analog] [mass M] [fresh] | begin | "
        + "mode galaxy|planets | set mcshock|f8 <v> | slots <planet keys and chart names...> | slots clear | buses clear | "
        + "P5: p5 status|pellets <mag|erg|sup> [n] [mk <1-3>]|refill <grade|off>|accept <n|auto>|beam <c|auto> [k]|"
        + "deeplink [reset]|flow <n> [low|high]|uni|stdrig on [mass M]|off|status|count <counter> <n>";

    /** Runs {@code args} = {@code star x y z sub …}. Returns the lines to print. */
    public static List<String> run(ICommandSender sender, String[] args) {
        final List<String> out = new ArrayList<>();
        if (args.length < 5) {
            out.add(USAGE);
            return out;
        }
        final World world = sender.getEntityWorld();
        final int x = coord(sender, args[1], 0), y = coord(sender, args[2], 1), z = coord(sender, args[3], 2);
        final MTEEyeOfHarmonyBTS eye = eyeAt(world, x, y, z);
        if (eye == null) {
            out.add("No Eye of Harmony controller at (" + x + ", " + y + ", " + z + ").");
            return out;
        }
        eye.benchEnsureChecked(); // a chunk just reloaded: walk now so the hatch lists are there
        final EyeStarRuntime rt = eye.starRuntime();
        final String sub = args[4].toLowerCase(Locale.ROOT);
        final String a5 = args.length > 5 ? args[5].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "status":
                break;
            case "target": {
                if (!a5.isEmpty() && !"none".equals(a5) && chart(a5) == null) {
                    out.add(
                        "Unknown chart " + a5
                            + " (sol, siriusa, arcturus, neutron, siriusb, magnetar, triangulum, milkyway, none).");
                    return out;
                }
                eye.debugSetTarget(chart(a5) == null ? null : new ItemStack(chart(a5), 1, 0));
                out.add("Target: " + (chart(a5) == null ? "none" : a5));
                break;
            }
            case "lit": {
                if (!a5.isEmpty()) {
                    if (chart(a5) == null) {
                        out.add("Unknown chart " + a5 + ".");
                        return out;
                    }
                    eye.debugSetTarget(new ItemStack(chart(a5), 1, 0));
                }
                final String err = rt.debugLight(rt.wanted());
                out.add(err == null ? "LIT (bench ledger started, seed " + rt.seed + ")" : "lit refused: " + err);
                break;
            }
            case "seed":
                rt.debugSeed(Long.parseLong(a5));
                out.add("Seed " + a5 + ": fresh field" + (rt.burning() ? ", the star relit (ledger restarted)" : ""));
                break;
            case "rig": {
                final FieldRig.Mode mode = FieldRig.Mode.parse(a5);
                rt.setRig(mode);
                out.add("Rig: " + (mode == null ? "none (the Field sockets)" : mode.name()));
                break;
            }
            case "sim":
                sim(eye, rt, Long.parseLong(a5), args.length > 6 && "compare".equalsIgnoreCase(args[6]), out);
                break;
            case "set":
                if ("dial".equals(a5)) rt.setDial(Double.parseDouble(args[6]));
                else if ("feed".equals(a5)) rt.setFeed(Double.parseDouble(args[6]));
                else if ("shock".equals(a5)) rt.setBounceShock(Double.parseDouble(args[6]));
                else if ("mcshock".equals(a5)) rt.setCollapseShock(Double.parseDouble(args[6]));
                else if ("f8".equals(a5)) rt.setF8(Double.parseDouble(args[6]));
                else {
                    out.add("set dial|feed|shock|mcshock|f8 <v>");
                    return out;
                }
                out.add(
                    "dial " + EyeStarRuntime.f2(rt.dialSetpoint())
                        + ", feed "
                        + String.format(Locale.ROOT, "%.3f", rt.feedSetpoint())
                        + ", next Core Collapse bounce +"
                        + Math.round(rt.bounceShock()));
                break;
            case "unlock":
                rt.setUnlocked(Integer.parseInt(a5)); // 1..8
                out.add("Unlocked T" + rt.unlockedTier());
                break;
            case "milestones":
                if (args.length > 6 && "reset".equalsIgnoreCase(args[6])) {
                    rt.milestones.reset(Integer.parseInt(a5));
                    rt.galaxy.resetLive(); // the link windows and streaks too
                    if (eye.getBaseMetaTileEntity() != null) eye.getBaseMetaTileEntity()
                        .markDirty();
                } else {
                    rt.milestones.complete(Integer.parseInt(a5));
                }
                out.add(rt.milestones.progressLine(Integer.parseInt(a5)));
                break;
            case "fill": {
                eye.debugFillBuffers(1_000_000_000_000L);
                final UUID owner = rt.host.owner();
                if (owner != null) {
                    WirelessNetworkManager.strongCheckOrAddUser(owner);
                    WirelessNetworkManager.addEUToGlobalEnergyMap(owner, BigInteger.TEN.pow(18));
                }
                out.add("Buffers filled to 10^12 L" + (owner != null ? ", 10^18 EU granted" : " (no owner: no EU)"));
                break;
            }
            case "sensor":
                sensor(sender, world, args, out);
                return out;
            case "probe":
                for (MTEHatchStellarFieldSensor s : eye.sensorHatches()) {
                    out.add(s.statusLine() + " -> redstone " + s.currentOutput());
                }
                if (eye.sensorHatches()
                    .isEmpty()) out.add("No Stellar Field Sensor Hatches in the Sensor sockets.");
                return out;
            case "transition": {
                if ("abort".equals(a5)) {
                    rt.debugAbort();
                    out.add("Transition aborted (as a wrench would).");
                    break;
                }
                final EyeTransition tr = rt.transition();
                out.add(
                    tr == null ? "No transition running."
                        : tr.type.title + " running for " + tr.t + " s (began at field second " + tr.beginT + ")");
                break;
            }
            case "lose": {
                // D-54 (B-217): the live star is lost as a Breach loses it (T10: put out, since a T10 breach only
                // ends the universe); the dark Eye then relights it at the base of its band
                final String err = rt.debugLose();
                out.add(err == null ? "Lost: " + rt.lastFailure() : "lose refused: " + err);
                break;
            }
            case "event": {
                final String err = rt.debugEvent();
                out.add(err == null ? "The next event is due now (warning this second)." : "event refused: " + err);
                break;
            }
            case "crossing": {
                final String err = rt.debugCrossing();
                out.add(
                    err == null ? "The next satellite crossing is warned now (120 s)." : "crossing refused: " + err);
                break;
            }
            case "wrench":
                eye.debugWrench();
                out.add("Wrenched and placed back (item NBT only):");
                break;
            case "fix": {
                // what the player does after placing a controller back: GT keeps no maintenance in item NBT, so
                // a placed-back controller has every issue until fixed, and GT's NO_REPAIR stop disables it
                eye.fixAllIssues();
                final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
                if (base != null) base.enableWorking();
                out.add("Maintenance fixed and the machine enabled (as a player would after placing it back)");
                out.add(gtLine(eye));
                break;
            }
            case "gt":
                out.add(gtLine(eye));
                return out;
            case "render":
                // render [on|off]: the centre render block as the world has it (D-49 unit 6)
                if ("on".equals(a5) || "off".equals(a5)) eye.setRenderEnabled("on".equals(a5));
                eye.debugRenderTick();
                out.addAll(eye.debugRenderLines());
                return out;
            case "compression": {
                // compression <1-9> [all]: swap the first (or every) Spacetime Compression Field Generator to that
                // tier, as a player would, to test the 1:1 gate (B-71)
                final int tier = Integer.parseInt(a5);
                final boolean all = args.length > 6 && "all".equalsIgnoreCase(args[6]);
                final int n = com.nateyburr.beyondthestars.machines.eye.EyeKit
                    .setCompression(world, eye, Math.max(0, Math.min(8, tier - 1)), all);
                out.add(n + " Spacetime Compression Field Generator(s) set to tier " + tier + "; re-walked");
                break;
            }
            case "check": {
                // the recipe check GT would run now: the gate / lock / structure reason a target is refused with
                out.add("checkRecipe: " + eye.benchCheckRecipe());
                break;
            }
            case "gui":
                // controller GUI cleanup: the header and rows the GUI would show now (spec §5.1), as text
                // gui galaxy: the status rows, then the Galaxy panel's text (B-118's weak-quasar row and line)
                final com.nateyburr.beyondthestars.machines.eye.EyeGui gui = new com.nateyburr.beyondthestars.machines.eye.EyeGui(
                    eye);
                out.addAll(gui.benchDump("info".equals(a5)));
                if ("galaxy".equals(a5)) {
                    out.add("Galaxy panel:");
                    out.addAll(gui.benchGalaxyPanel());
                }
                return out;
            case "hatchgui":
                // controller GUI cleanup: what each Directive / Stellar Field Sensor Hatch's GUI shows (spec §5.10)
                out.addAll(
                    com.nateyburr.beyondthestars.machines.directive.DirectiveBench
                        .hatchGui(eye.directiveHatches(), eye.sensorHatches()));
                return out;
            case "mefield":
                out.add(meField(eye));
                break;
            case "intake":
                // B-108: intake setup [G L] | status - an ME stocking hatch on a network of far more gas than the cap,
                // with a second consumer (fluid export bus) on the same network (dev-only, rendertest.IntakeBench)
                out.addAll(com.nateyburr.beyondthestars.rendertest.IntakeBench.run(eye, args));
                return out;
            case "eu": {
                // eu: 10^18 wireless EU to the owner, buffers untouched (fill also fills the buffers);
                // eu set <n>: the owner's network holds exactly n EU (a network that runs short)
                final UUID owner = rt.host.owner();
                if (owner != null && "set".equals(a5) && args.length > 6) {
                    WirelessNetworkManager.strongCheckOrAddUser(owner);
                    final BigInteger want = new BigInteger(args[6]);
                    WirelessNetworkManager
                        .addEUToGlobalEnergyMap(owner, want.subtract(WirelessNetworkManager.getUserEU(owner)));
                    out.add("the owner's network now holds " + WirelessNetworkManager.getUserEU(owner) + " EU");
                    return out;
                }
                if (owner != null) {
                    WirelessNetworkManager.strongCheckOrAddUser(owner);
                    WirelessNetworkManager.addEUToGlobalEnergyMap(owner, BigInteger.TEN.pow(18));
                }
                out.add(owner != null ? "10^18 EU granted" : "no owner: no EU");
                return out;
            }
            case "mebus":
                // QA S-01 / QA-A07: an ME input bus in a plinth socket on a small network (dev-only,
                // rendertest.MeBusBench)
                out.addAll(com.nateyburr.beyondthestars.rendertest.MeBusBench.run(eye, args));
                return out;
            case "damage":
                // damage | repair: one core casing next to the controller out and back, as a player breaking and
                // replacing it (the GT power switch and the star's restart after a repair, QA S-05)
                out.add(damage(world, eye));
                break;
            case "repair":
                out.add(repair(world));
                break;
            case "unload": {
                // unload: queue every chunk of the Eye's T1-T3 footprint for unloading except the controller's (run
                // /btskerr release first; a player nearby keeps them): a partly loaded footprint (QA-A11)
                final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
                final int[] box = com.nateyburr.beyondthestars.machines.SLConsoleTest.bounds(
                    eye.getExtendedFacing(),
                    base.getXCoord(),
                    base.getYCoord(),
                    base.getZCoord(),
                    MTEEyeOfHarmonyBTS.PIECE_CORE,
                    MTEEyeOfHarmonyBTS.PIECE_BEAM,
                    MTEEyeOfHarmonyBTS.PIECE_JET);
                final int ccx = base.getXCoord() >> 4, ccz = base.getZCoord() >> 4;
                int n = 0;
                for (int cx = box[0] >> 4; cx <= box[3] >> 4; cx++) {
                    for (int cz = box[2] >> 4; cz <= box[5] >> 4; cz++) {
                        if (cx == ccx && cz == ccz && !"all".equals(a5)) continue;
                        ((net.minecraft.world.WorldServer) world).theChunkProviderServer
                            .unloadChunksIfNotNearSpawn(cx, cz);
                        n++;
                    }
                }
                out.add(n + " chunk(s) queued to unload (all but the controller's)");
                return out;
            }
            case "planet": {
                // planet <key>: a planet's display block in the controller slot (a GT planet run, QA-A06)
                final String key = a5.startsWith("planet:") ? args[5] : "planet:" + args[5];
                final com.nateyburr.beyondthestars.machines.eye.EyeRecipe r = com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry
                    .byKey(key);
                if (r == null) {
                    out.add("Unknown planet " + key);
                    return out;
                }
                eye.debugSetTarget(r.trigger.copy());
                out.add("Target: " + r.name);
                break;
            }
            case "hatchwrench": {
                // hatchwrench: every Directive and Sensor hatch's item NBT read back into a fresh hatch of its kind,
                // as a pick-up and placement does (QM-05)
                for (com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective h : eye.directiveHatches()) {
                    final net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                    h.setItemNBT(tag);
                    final com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective fresh = (com.nateyburr.beyondthestars.machines.directive.MTEHatchDirective) h
                        .newMetaEntity(h.getBaseMetaTileEntity());
                    fresh.loadNBTData(tag);
                    out.add("Directive: before " + h.settingsLine() + " | after a wrench " + fresh.settingsLine());
                }
                for (MTEHatchStellarFieldSensor h : eye.sensorHatches()) {
                    final net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                    h.setItemNBT(tag);
                    out.add("Sensor item NBT: " + tag);
                }
                return out;
            }
            case "walk": {
                // walk: GT's structure check now, whatever chunks are loaded (QA-A11: a walk across unloaded chunks)
                final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
                if (base != null) eye.checkStructure(true, base);
                out.add("Walked: formed " + eye.mMachine + ", structure T" + eye.structureTier());
                break;
            }
            case "punch": {
                // punch [owner]: a sneaking, empty-handed player punches the controller (the escrow return), by
                // default someone who is not the owner (QA-A08)
                final boolean asOwner = "owner".equals(a5);
                final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
                final com.mojang.authlib.GameProfile who = asOwner && base.getOwnerUuid() != null
                    ? new com.mojang.authlib.GameProfile(base.getOwnerUuid(), base.getOwnerName())
                    : new com.mojang.authlib.GameProfile(
                        UUID.nameUUIDFromBytes("BTS bench intruder".getBytes()),
                        "BenchIntruder");
                final net.minecraftforge.common.util.FakePlayer p = net.minecraftforge.common.util.FakePlayerFactory
                    .get((net.minecraft.world.WorldServer) world, who);
                p.setSneaking(true);
                p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
                eye.onLeftclick(base, p);
                int cores = 0;
                for (ItemStack s : p.inventory.mainInventory)
                    if (s != null && EyeStarRuntime.isCore(s.getItem())) cores += s.stackSize;
                p.inventory.clearInventory(null, -1);
                out.add(
                    "Punched by " + who.getName()
                        + (asOwner ? " (the owner)" : " (not the owner)")
                        + ": "
                        + cores
                        + " core(s) handed out");
                break;
            }
            case "directive":
                // AUTOMATION PASS: Trims / Status / Transition Directive Hatches in three Sensor sockets
                out.addAll(com.nateyburr.beyondthestars.machines.directive.DirectiveBench.eyeSetup(eye));
                return out;
            case "autorig":
                // AUTOMATION PASS: the scripted redstone gate rig on the Directive Hatches' codes (task 4)
                out.addAll(
                    com.nateyburr.beyondthestars.machines.directive.DirectiveBench.eyeAutorig(
                        eye,
                        a5.isEmpty() ? 600 : Long.parseLong(a5),
                        args.length > 7 && "cut".equalsIgnoreCase(args[6]) ? Long.parseLong(args[7]) : 0));
                break;
            case "liverig":
                // the same gate rig, live: one act a game second (the time warp included)
                out.addAll(
                    com.nateyburr.beyondthestars.machines.directive.EyeLiveRig
                        .set(world, x, y, z, a5.isEmpty() ? "on" : a5));
                return out;
            case "ocloop":
                // AUTOMATION PASS: the OpenComputers calls (BTSComputerApi), as a script makes them
                out.addAll(com.nateyburr.beyondthestars.machines.directive.DirectiveBench.eyeOcLoop(eye));
                return out;
            // ---- P4: the galaxy tiers
            case "burst":
                out.add(EyeGalaxyBench.burst(eye, args, 5));
                break;
            case "galaxy":
                out.addAll(EyeGalaxyBench.galaxy(eye, args, 5));
                break;
            case "begin":
                out.add(rt.requestBegin());
                break;
            case "inject": {
                // inject <qx> <qy> <qz> <hi> <lo> <halo> | inject off: real streams through the receptor hatches
                if ("off".equals(a5) || args.length < 11) {
                    eye.debugInject(null, 0, 0, 0);
                    out.add(
                        "Injection off" + (args.length < 11 && !"off".equals(a5)
                            ? " (inject <qx> <qy> <qz> <hi> <lo> <halo> | inject off)"
                            : ""));
                    break;
                }
                final com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource src = new com.nateyburr.beyondthestars.machines.quasar.burst.BurstSource(
                    world.provider.dimensionId,
                    coord(sender, args[5], 0),
                    coord(sender, args[6], 1),
                    coord(sender, args[7], 2));
                eye.debugInject(
                    src,
                    Double.parseDouble(args[8]),
                    Double.parseDouble(args[9]),
                    Double.parseDouble(args[10]));
                out.add("Injecting from " + src + " (status " + src.status() + ") into the Receptor Ports each second");
                break;
            }
            case "mode": {
                final EyeGalaxy.Mode m = EyeGalaxy.Mode.parse(a5);
                if (m == null) {
                    out.add("mode galaxy|planets");
                    return out;
                }
                rt.setMode(m);
                out.add("Mode: " + m.key);
                break;
            }
            case "slots":
                out.add(fillSlotBus(eye, args));
                break;
            case "buses": {
                // P4 hosts unit: empty the plinth input buses (the rig's item path) for a clean proof run
                int n = 0;
                for (gregtech.api.metatileentity.implementations.MTEHatchInputBus bus : eye.itemBuses()) {
                    if (bus == null || !bus.isValid()) continue;
                    for (int i = 0; i < bus.getSizeInventory(); i++) {
                        if (bus.getStackInSlot(i) == null) continue;
                        bus.getBaseMetaTileEntity()
                            .setInventorySlotContents(i, null);
                        n++;
                    }
                }
                out.add("Plinth input buses emptied: " + n + " stack(s) removed.");
                break;
            }
            case "field": {
                // field <fluid registry name> <L>: pour a trim fluid into the Field-socket hatches (what a player's
                // automation does), so the sockets, not a debug rig, regulate the star
                final Fluid fluid = args.length > 6 ? FluidRegistry.getFluid(args[5]) : null;
                if (fluid == null) {
                    out.add("field <fluid registry name> <litres>");
                    return out;
                }
                int left = Integer.parseInt(args[6]);
                for (MTEHatchInput h : eye.fieldHatches()) {
                    if (left <= 0) break;
                    if (h == null || !h.isValid()) continue;
                    left -= h
                        .fill(net.minecraftforge.common.util.ForgeDirection.UNKNOWN, new FluidStack(fluid, left), true);
                }
                out.add(
                    "Field sockets: " + (Integer.parseInt(args[6]) - left) + " L of " + fluid.getName() + " poured");
                return out;
            }
            case "charge": {
                final int n = a5.isEmpty() ? 1 : Integer.parseInt(a5);
                out.add(putCharges(eye, n));
                return out;
            }
            case "bus": {
                // bus <chart> [n]: n copies of a star chart into a free plinth input bus slot, as an export bus or a
                // conveyor would push them (B-72: duplicates, no swap during a transition)
                if (chart(a5) == null) {
                    out.add("bus <sol|siriusa|arcturus|neutron|siriusb|magnetar> [n]");
                    return out;
                }
                out.add(putInBus(eye, new ItemStack(chart(a5), args.length > 6 ? Integer.parseInt(args[6]) : 1, 0)));
                return out;
            }
            case "buffer": {
                final Fluid fluid = args.length > 6 ? FluidRegistry.getFluid(args[5]) : null;
                if (fluid == null) {
                    out.add("buffer <fluid registry name> <litres>");
                    return out;
                }
                eye.debugSetBuffer(fluid, Long.parseLong(args[6]));
                out.add("Buffer " + fluid.getName() + " = " + args[6] + " L");
                break;
            }
            case "sensors": {
                if (args.length < 8) {
                    out.add("sensors <channel> <gt|lt> <threshold> [analog]");
                    return out;
                }
                final FieldReadout.Channel ch = channel(args[5]);
                if (ch == null) {
                    out.add("Unknown channel " + args[5] + ".");
                    return out;
                }
                for (MTEHatchStellarFieldSensor s : eye.sensorHatches()) {
                    s.setChannelOrdinal(ch.ordinal());
                    s.setLessThan("lt".equalsIgnoreCase(args[6]));
                    s.setThreshold(Double.parseDouble(args[7]));
                    s.setAnalog(args.length > 8 && "analog".equalsIgnoreCase(args[8]));
                }
                out.add(
                    eye.sensorHatches()
                        .size() + " sensors set");
                return out;
            }
            case "p5": {
                out.addAll(EyeP5Bench.run(eye, args, 5));
                return out;
            }
            case "core": {
                final Item core = core(a5);
                if (core == null) {
                    out.add("core <stellarmass|shellburning|galactic|subspace|magnetar|intergalactic|supermassive>");
                    return out;
                }
                out.add(rt.offerCore(new ItemStack(core), new boolean[1]));
                return out;
            }
            case "escrow": {
                final ItemStack back = rt.takeEscrow();
                out.add("Escrow returned: " + (back == null ? "nothing" : back.getDisplayName()));
                break;
            }
            case "legacy": {
                // legacy [galactic] [run <key> <successes>]
                boolean galactic = false;
                String run = null;
                long successes = 0;
                for (int i = 5; i < args.length; i++) {
                    if ("galactic".equalsIgnoreCase(args[i])) galactic = true;
                    if ("run".equalsIgnoreCase(args[i]) && i + 2 < args.length) {
                        run = args[++i];
                        successes = Long.parseLong(args[++i]);
                    }
                }
                eye.debugLegacyLoad(galactic, run, successes);
                out.add(
                    "Loaded 0.24.x NBT (galactic " + galactic + ", run " + run + " x" + successes + "), re-walked:");
                break;
            }
            default:
                out.add(USAGE);
                return out;
        }
        out.addAll(eye.statusLines());
        return out;
    }

    private static void sim(MTEEyeOfHarmonyBTS eye, EyeStarRuntime rt, long secs, boolean compare, List<String> out) {
        final long start = System.nanoTime();
        for (long i = 0; i < secs; i++) eye.fieldSecond();
        final long us = (System.nanoTime() - start) / 1000L;
        out.add("sim " + secs + " s in " + us / 1000 + " ms (" + (secs > 0 ? us / secs : 0) + " us per field second)");
        final EyeStarRuntime.Ledger l = rt.ledger;
        if (l == null) {
            out.add("No bench ledger (it starts at 'lit' or 'seed').");
            return;
        }
        out.add(
            "ledger: " + l.steps
                + " s since lit, zones "
                + l.zoneT[0]
                + "/"
                + l.zoneT[1]
                + "/"
                + l.zoneT[2]
                + "/"
                + l.zoneT[3]
                + "/"
                + l.zoneT[4]
                + ", mean Y "
                + String.format(Locale.ROOT, "%.6f", l.yN > 0 ? l.ySum / l.yN : 0)
                + ", fails "
                + l.fails
                + ", cycles "
                + l.cycles
                + ", batches "
                + l.batches
                + ", product "
                + String.format(Locale.ROOT, "%.3f", l.product)
                + " L, HSM fed "
                + String.format(Locale.ROOT, "%.3f", l.hsm)
                + " L, hash "
                + Long.toHexString(l.hash & 0xFFFFFFFFL));
        if (!compare || rt.fs == null) return;
        final FieldSim.Cfg c = new FieldSim.Cfg();
        c.seed = l.seed;
        if (l.collapseBeginT >= 0) {
            // replay the Core Collapse: it began at that field second, with that bounce and SpaceTime buffer
            c.collapse = true;
            c.beginAt = l.collapseBeginT;
            c.shock = l.collapseShock;
            c.stBuf = l.collapseStBuf;
            c.tachRate = 100;
        }
        c.rig = rt.rig == null ? FieldRig.Mode.OFF : rt.rig;
        c.m = rt.dialSet;
        c.tt = rt.host.timeTier();
        c.ts = rt.host.stabilisationTier();
        c.r = 1;
        c.feed = rt.feedSet;
        final double stAtBegin = c.stBuf; // runCase spends the bounce's SpaceTime from c.stBuf: print the begin value
        final Map<String, Object> res = FieldSim.runCase(l.startTier, c, l.steps);
        final long hash = ((Number) res.get("hash")).longValue();
        out.add(
            "offline FieldSim T" + l.startTier
                + (c.collapse
                    ? " (Core Collapse at " + c.beginAt + ", shock " + c.shock + ", SpaceTime " + stAtBegin + ")"
                    : "")
                + " seed "
                + c.seed
                + " rig "
                + c.rig
                + " m "
                + c.m
                + " tt "
                + c.tt
                + " ts "
                + c.ts
                + " feed "
                + c.feed
                + ": zones "
                + res.get("zoneT")
                + ", mean Y "
                + String.format(Locale.ROOT, "%.6f", ((Number) res.get("meanY")).doubleValue())
                + ", fails "
                + res.get("fails")
                + ", prod "
                + res.get("prod")
                + ", hash "
                + Long.toHexString(hash));
        out.add(
            hash == (l.hash & 0xFFFFFFFFL) ? "COMPARE PASS: the Eye's field equals the prototype's, second for second"
                : "COMPARE DIFFERS (needs p = 1, upkeep r = 1, no failure, and the rig or no trims since 'lit')");
    }

    /** The core casing {@code damage} took out: {x, y, z, block, meta}; null when none is out. */
    private static Object[] damaged;

    /** Takes out the first plain block next to the controller (a core casing; never a tile), remembering it. */
    private static String damage(World world, MTEEyeOfHarmonyBTS eye) {
        if (damaged != null) return "A block is already out at " + damaged[0] + ", " + damaged[1] + ", " + damaged[2];
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        for (net.minecraftforge.common.util.ForgeDirection d : net.minecraftforge.common.util.ForgeDirection.VALID_DIRECTIONS) {
            final int bx = base.getXCoord() + d.offsetX, by = base.getYCoord() + d.offsetY,
                bz = base.getZCoord() + d.offsetZ;
            final net.minecraft.block.Block b = world.getBlock(bx, by, bz);
            if (b == null || b.isAir(world, bx, by, bz) || world.getTileEntity(bx, by, bz) != null) continue;
            damaged = new Object[] { bx, by, bz, b, world.getBlockMetadata(bx, by, bz) };
            world.setBlockToAir(bx, by, bz);
            return "Took out " + net.minecraft.block.Block.blockRegistry
                .getNameForObject(b) + " at " + bx + ", " + by + ", " + bz;
        }
        return "No plain block next to the controller to take out.";
    }

    private static String repair(World world) {
        if (damaged == null) return "Nothing to repair.";
        final int bx = (Integer) damaged[0], by = (Integer) damaged[1], bz = (Integer) damaged[2];
        world.setBlock(bx, by, bz, (net.minecraft.block.Block) damaged[3], (Integer) damaged[4], 3);
        damaged = null;
        return "Put the block back at " + bx + ", " + by + ", " + bz;
    }

    /** GT's view of the controller: allowed to work, formed, maintenance, the last shutdown reason. */
    private static String gtLine(MTEEyeOfHarmonyBTS eye) {
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        if (base == null) return "GT: no base tile";
        return "GT: allowed to work " + base.isAllowedToWork()
            + ", formed "
            + eye.mMachine
            + ", maintenance "
            + eye.getRepairStatus()
            + "/"
            + eye.getIdealStatus()
            + ", last shutdown "
            + (base.getLastShutDownReason() == null ? "none"
                : base.getLastShutDownReason()
                    .getKey());
    }

    /** R-15 bench: swaps the first live Field-socket hatch for an ME stocking input hatch and re-walks. */
    private static String meField(MTEEyeOfHarmonyBTS eye) {
        MTEHatchInput field = null;
        for (MTEHatchInput h : eye.fieldHatches()) {
            if (h != null && h.isValid()) {
                field = h;
                break;
            }
        }
        if (field == null) return "No live Field-socket hatch to swap.";
        final IGregTechTileEntity hb = field.getBaseMetaTileEntity();
        final int hx = hb.getXCoord(), hy = hb.getYCoord(), hz = hb.getZCoord();
        final ItemStack me = ItemList.Hatch_Input_ME.get(1L);
        if (me == null || !MTEReactorCoreMk1
            .placeGtMachine(hb.getWorld(), hx, hy, hz, (short) me.getItemDamage(), hb.getFrontFacing())) {
            return "Could not place an ME stocking input hatch at (" + hx + ", " + hy + ", " + hz + ").";
        }
        final IGregTechTileEntity base = eye.getBaseMetaTileEntity();
        if (base != null) eye.checkStructure(true, base);
        return "ME stocking input hatch placed in the Field socket at (" + hx
            + ", "
            + hy
            + ", "
            + hz
            + "), re-walked: formed "
            + eye.mMachine
            + ", Field hatches "
            + eye.fieldHatchCount()
            + ", rejected "
            + eye.rejectedFieldHatchCount();
    }

    private static void sensor(ICommandSender sender, World world, String[] args, List<String> out) {
        if (args.length < 11) {
            out.add("sensor <x> <y> <z> <channel> <gt|lt> <threshold> [analog]");
            return;
        }
        final TileEntity te = world
            .getTileEntity(coord(sender, args[5], 0), coord(sender, args[6], 1), coord(sender, args[7], 2));
        final IMetaTileEntity mte = te instanceof IGregTechTileEntity ? ((IGregTechTileEntity) te).getMetaTileEntity()
            : null;
        if (!(mte instanceof MTEHatchStellarFieldSensor)) {
            out.add("No Stellar Field Sensor Hatch there.");
            return;
        }
        final MTEHatchStellarFieldSensor s = (MTEHatchStellarFieldSensor) mte;
        final FieldReadout.Channel ch = channel(args[8]);
        if (ch == null) {
            out.add(
                "Channels: P, THETA, OMEGA, PI, SHIELD, INTEGRITY, STABILITY, ZONE, WARN_PLUS, WARN_MINUS, "
                    + "WARN_ANY, UPKEEP_OK, SETPOINT_ERROR");
            return;
        }
        s.setChannelOrdinal(ch.ordinal());
        s.setLessThan("lt".equalsIgnoreCase(args[9]));
        s.setThreshold(Double.parseDouble(args[10]));
        s.setAnalog(args.length > 11 && "analog".equalsIgnoreCase(args[11]));
        out.add("Sensor set: " + s.statusLine());
    }

    private static FieldReadout.Channel channel(String name) {
        final String n = name.toUpperCase(Locale.ROOT);
        switch (n) {
            case "TH":
                return FieldReadout.Channel.THETA;
            case "OM":
                return FieldReadout.Channel.OMEGA;
            case "A":
                return FieldReadout.Channel.SHIELD;
            case "I":
                return FieldReadout.Channel.INTEGRITY;
            default:
                for (FieldReadout.Channel c : FieldReadout.Channel.values()) if (c.name()
                    .equals(n)) return c;
                return null;
        }
    }

    private static Item core(String name) {
        switch (name) {
            case "stellarmass":
                return SLItems.stellarMassCore;
            case "shellburning":
                return SLItems.shellBurningCore;
            case "galactic":
                return SLItems.galacticUpgradeCore;
            case "subspace":
                return SLItems.subspaceCoolingCore;
            case "magnetar":
                return SLItems.magnetarSeedCore;
            case "intergalactic":
                return com.nateyburr.beyondthestars.items.SLQuasarItems.intergalacticUpgradeCore;
            case "supermassive":
                return com.nateyburr.beyondthestars.items.SLQuasarItems.supermassiveSeedCore;
            case "attractor":
                return com.nateyburr.beyondthestars.items.SLP5Items.attractorSeedCore;
            case "universal":
                return com.nateyburr.beyondthestars.items.SLMaxItems.universalUpgradeCore;
            default:
                return null;
        }
    }

    /**
     * P4 {@code slots <names...>}: puts planet items (an EyeRecipe key or its suffix, e.g. {@code planet:Ma} or
     * {@code Ma}) and star charts (sol .. magnetar) into the slot bus, one per slot in order; {@code slots clear}
     * empties it.
     */
    private static String fillSlotBus(MTEEyeOfHarmonyBTS eye, String[] args) {
        final MTEHatchInputBus bus = eye.firstSlotBus();
        if (bus == null) return "No slot bus (an input bus in the plinth's 'p' socket).";
        for (int i = 0; i < bus.getSizeInventory(); i++) bus.getBaseMetaTileEntity()
            .setInventorySlotContents(i, null);
        if (args.length > 5 && "clear".equalsIgnoreCase(args[5])) return "Slot bus cleared.";
        final StringBuilder sb = new StringBuilder("Slot bus:");
        int slot = 0;
        for (int i = 5; i < args.length && slot < bus.getSizeInventory(); i++) {
            ItemStack stack = null;
            final Item chart = chart(args[i].toLowerCase(Locale.ROOT));
            if (chart != null) stack = new ItemStack(chart, 1, 0);
            else {
                final String key = args[i].startsWith("planet:") ? args[i] : "planet:" + args[i];
                final com.nateyburr.beyondthestars.machines.eye.EyeRecipe r = com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry
                    .byKey(key);
                if (r != null) stack = r.trigger.copy();
            }
            if (stack == null) {
                sb.append(" [")
                    .append(args[i])
                    .append(": unknown]");
                continue;
            }
            bus.getBaseMetaTileEntity()
                .setInventorySlotContents(slot++, stack);
            sb.append(' ')
                .append(stack.getDisplayName())
                .append(';');
        }
        return sb.toString();
    }

    /** Puts {@code n} Stellar Ignition Charges into the first live plinth input bus. */
    private static String putCharges(MTEEyeOfHarmonyBTS eye, int n) {
        return putInBus(eye, new ItemStack(SLItems.stellarIgnitionCharge, n, 0));
    }

    private static String putInBus(MTEEyeOfHarmonyBTS eye, ItemStack stack) {
        for (MTEHatchInputBus bus : eye.itemBuses()) {
            if (bus == null || !bus.isValid()) continue;
            for (int i = 0; i < bus.getSizeInventory(); i++) {
                if (bus.getStackInSlot(i) != null) continue;
                bus.getBaseMetaTileEntity()
                    .setInventorySlotContents(i, stack);
                return stack.stackSize + " " + stack.getDisplayName() + " in a plinth input bus (slot " + i + ")";
            }
        }
        return "No plinth input bus with a free slot.";
    }

    private static Item chart(String name) {
        switch (name) {
            case "sol":
                return SLItems.starChartSol;
            case "siriusa":
                return SLItems.starChartSiriusA;
            case "arcturus":
                return SLItems.starChartArcturus;
            case "neutron":
            case "neutronstar":
                return SLItems.starChartNeutronStar;
            case "siriusb":
                return SLItems.starChartSiriusB;
            case "magnetar":
                return SLItems.starChartMagnetar;
            case "triangulum":
                return com.nateyburr.beyondthestars.items.SLQuasarItems.starChartTriangulum;
            case "milkyway":
                return com.nateyburr.beyondthestars.items.SLQuasarItems.starChartMilkyWay;
            case "attractor":
                return com.nateyburr.beyondthestars.items.SLP5Items.starChartGreatAttractor;
            case "universe":
                return com.nateyburr.beyondthestars.items.SLMaxItems.starChartUniverse;
            default:
                return null;
        }
    }

    private static MTEEyeOfHarmonyBTS eyeAt(World world, int x, int y, int z) {
        if (world == null) return null;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte instanceof MTEEyeOfHarmonyBTS ? (MTEEyeOfHarmonyBTS) mte : null;
    }

    /** An int, or "~" / "~n" relative to the sender's block position on {@code axis} (0 x, 1 y, 2 z). */
    private static int coord(ICommandSender sender, String text, int axis) {
        if (text.startsWith("~")) {
            final ChunkCoordinates at = sender.getPlayerCoordinates();
            final int base = axis == 0 ? at.posX : axis == 1 ? at.posY : at.posZ;
            return base + (text.length() > 1 ? Integer.parseInt(text.substring(1)) : 0);
        }
        return Integer.parseInt(text);
    }
}
