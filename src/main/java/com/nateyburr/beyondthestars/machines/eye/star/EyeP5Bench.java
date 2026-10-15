package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.nateyburr.beyondthestars.items.SLP5Items;
import com.nateyburr.beyondthestars.machines.eye.MTEEyeOfHarmonyBTS;
import com.nateyburr.beyondthestars.machines.eye.MTEHatchPelletInjector;
import com.nateyburr.beyondthestars.machines.eye.field.AttractorField;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.StellarField;
import com.nateyburr.beyondthestars.machines.eye.field.UniverseField;

/**
 * P5 test bench ({@code /btseye star <x> <y> <z> p5 …}): the Eye's T9 / T10 state on a dev server.
 * <ul>
 * <li>{@code status}: the beam, the flow, the Deep Link, the universe, the counters;</li>
 * <li>{@code pellets <mag|erg|sup> [n] [mk <1-3>]}: n pellets (default 64) into the first Pellet Injector Hatch, and a
 * barrel of that Mk;</li>
 * <li>{@code refill <mag|erg|sup|off>}: keep the injector stocked every field second (a player's ME interface);</li>
 * <li>{@code accept <0-12|auto>}: the flow accept limit; {@code beam <c|auto> [k]}: the injector's centre and
 * gain;</li>
 * <li>{@code deeplink [reset]}: the Deep Link's window and streak; {@code flow <n> [low|high]}: warn a group of n now
 * (180 s ahead), as the next Galactic Flow;</li>
 * <li>{@code uni}: the universe's age, events and charge;</li>
 * <li>{@code count <galaxies|flows|universes|dawns|sdsec|wide|flat> <n>}: sets a P5 counter (B-218, the quest bridge's
 * test: the next field second credits the owner's quests).</li>
 * </ul>
 */
public final class EyeP5Bench {

    private EyeP5Bench() {}

    /** Eyes kept stocked by {@code refill}: their position key to the grade. Not saved. */
    private static final Map<Integer, Integer> REFILL = new HashMap<>();

    public static List<String> run(MTEEyeOfHarmonyBTS eye, String[] args, int at) {
        final List<String> out = new ArrayList<>();
        final EyeStarRuntime rt = eye.starRuntime();
        final String sub = args.length > at ? args[at].toLowerCase(Locale.ROOT) : "status";
        final String a1 = args.length > at + 1 ? args[at + 1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "pellets": {
                final int g = grade(a1);
                long n = 64;
                int mk = 0;
                for (int i = at + 2; i < args.length; i++) {
                    if ("mk".equalsIgnoreCase(args[i]) && i + 1 < args.length) mk = Integer.parseInt(args[++i]);
                    else n = Long.parseLong(args[i]);
                }
                out.add(stock(eye, g, n, mk));
                break;
            }
            case "refill": {
                if ("off".equals(a1)) REFILL.remove(key(eye));
                else REFILL.put(key(eye), grade(a1));
                out.add(
                    "refill: " + ("off".equals(a1) ? "off"
                        : "the injector is kept stocked with " + EyeAttractor.gradeName(grade(a1)) + " pellets"));
                break;
            }
            case "accept":
                rt.setAcceptLimit("auto".equals(a1) ? -1 : Integer.parseInt(a1));
                out.add("accept limit: " + (rt.attractor.acceptSet < 0 ? "the hold rating" : rt.attractor.acceptSet));
                break;
            case "beam": {
                final MTEHatchPelletInjector inj = firstInjector(eye);
                if (inj == null) {
                    out.add("No Pellet Injector Hatch on the top collar.");
                    return out;
                }
                inj.setBeam(
                    "auto".equals(a1) ? Double.NaN : Double.parseDouble(a1),
                    args.length > at + 2 ? Double.parseDouble(args[at + 2]) : Double.NaN);
                out.add("injector: " + inj.statusLine());
                break;
            }
            case "deeplink":
                if ("reset".equals(a1)) {
                    rt.attractor.deep.restore(new double[3 * 60], 0);
                    rt.attractor.deep.link.streak = 0;
                    rt.attractor.deep.link.done = false;
                    rt.milestones.linkDone[8] = false;
                    rt.milestones.linkBest[8] = 0;
                }
                break;
            case "flow": {
                final FieldState fs = rt.fs;
                if (fs == null || !fs.tier.attractor) {
                    out.add("No Great Attractor burning.");
                    return out;
                }
                final FieldState.Flow f = fs.flow != null ? fs.flow : (fs.flow = new FieldState.Flow());
                if (f.left > 0 || f.warned) {
                    out.add("A flow is already warned or surging.");
                    return out;
                }
                // the next group is due now: the field draws its sign and size at the warning (the prototype's)
                f.next = fs.t + AttractorField.FLOW_WARN;
                out.add("the next Galactic Flow is warned on the next field second (arrives in 180 s)");
                break;
            }
            case "count": {
                final long n = args.length > at + 2 ? Long.parseLong(args[at + 2]) : 0;
                final EyeMilestones ms = rt.milestones;
                switch (a1) {
                    case "galaxies":
                        ms.galaxies = n;
                        break;
                    case "flows":
                        ms.flowsHeld = (int) n;
                        break;
                    case "universes":
                        ms.universes = n;
                        break;
                    case "dawns":
                        ms.dawns = n;
                        break;
                    case "sdsec":
                        ms.superdenseSeconds = n;
                        break;
                    case "wide":
                        ms.widePulls = (int) n;
                        break;
                    case "flat":
                        ms.flatUniverses = n;
                        break;
                    default:
                        out.add("p5 count <galaxies|flows|universes|dawns|sdsec|wide|flat> <n>");
                        return out;
                }
                if (eye.getBaseMetaTileEntity() != null) eye.getBaseMetaTileEntity()
                    .markDirty();
                out.add("counter " + a1 + " = " + n);
                break;
            }
            case "uni":
            case "status":
                break;
            default:
                out.add(
                    "p5 status | pellets <mag|erg|sup> [n] [mk <1-3>] | refill <grade|off> | accept <n|auto> |"
                        + " beam <c|auto> [k] | deeplink [reset] | flow | uni | count <counter> <n>");
                return out;
        }
        out.addAll(lines(rt, eye));
        return out;
    }

    /** One field second's top-up of the Eye's injector (the bench's stand-in for an ME interface). */
    static void refill(MTEEyeOfHarmonyBTS eye) {
        final Integer g = REFILL.get(key(eye));
        if (g == null) return;
        final MTEHatchPelletInjector inj = firstInjector(eye);
        if (inj == null || inj.pellets(g) >= 64 * 4) return;
        stock(eye, g, 64 * 8, 0);
    }

    private static String stock(MTEEyeOfHarmonyBTS eye, int g, long n, int mk) {
        final MTEHatchPelletInjector inj = firstInjector(eye);
        if (inj == null) return "No Pellet Injector Hatch on the top collar.";
        final Item pellet = g == 1 ? SLP5Items.gatedPelletMagnetar
            : g == 2 ? SLP5Items.gatedPelletErgomatter : SLP5Items.gatedPelletSuperdense;
        final Item barrel = mk == 1 ? SLP5Items.injectorBarrelMk1
            : mk == 2 ? SLP5Items.injectorBarrelMk2 : mk == 3 ? SLP5Items.injectorBarrelMk3 : null;
        final gregtech.api.interfaces.tileentity.IGregTechTileEntity base = inj.getBaseMetaTileEntity();
        if (barrel != null) {
            for (int i = 0; i < MTEHatchPelletInjector.SLOTS; i++) {
                if (SLP5Items.barrelTier(base.getStackInSlot(i)) > 0) base.setInventorySlotContents(i, null);
            }
            for (int i = 0; i < MTEHatchPelletInjector.SLOTS; i++) {
                if (base.getStackInSlot(i) == null) {
                    base.setInventorySlotContents(i, new ItemStack(barrel, 1, 0));
                    break;
                }
            }
        }
        long left = n;
        for (int i = 0; i < MTEHatchPelletInjector.SLOTS && left > 0; i++) {
            final ItemStack s = base.getStackInSlot(i);
            if (s == null) {
                final int put = (int) Math.min(64, left);
                base.setInventorySlotContents(i, new ItemStack(pellet, put, 0));
                left -= put;
            } else if (s.getItem() == pellet && s.stackSize < 64) {
                final int put = (int) Math.min(64 - s.stackSize, left);
                s.stackSize += put;
                left -= put;
            }
        }
        return "injector: " + inj.statusLine();
    }

    private static MTEHatchPelletInjector firstInjector(MTEEyeOfHarmonyBTS eye) {
        for (MTEHatchPelletInjector inj : eye.injectorHatches()) {
            if (inj != null && inj.getBaseMetaTileEntity() != null
                && inj.getBaseMetaTileEntity()
                    .isServerSide())
                return inj;
        }
        return null;
    }

    private static int grade(String s) {
        return s.startsWith("mag") ? 1 : s.startsWith("erg") ? 2 : 3;
    }

    private static int key(MTEEyeOfHarmonyBTS eye) {
        final gregtech.api.interfaces.tileentity.IGregTechTileEntity b = eye.getBaseMetaTileEntity();
        return b == null ? 0 : (b.getXCoord() * 31 + b.getYCoord()) * 31 + b.getZCoord();
    }

    /** The P5 bench lines. */
    public static List<String> lines(EyeStarRuntime rt, MTEEyeOfHarmonyBTS eye) {
        final List<String> out = new ArrayList<>();
        final EyeAttractor a = rt.attractor;
        final FieldState fs = rt.fs;
        final EyeMilestones ms = rt.milestones;
        out.add(
            String.format(
                Locale.ROOT,
                "P5: tier %s, unlocked T%d, state %s%s; galaxies held %,d, flows held %d, universes %d, dawns %d;"
                    + " quest counts: Superdense %d s, three-companion cycles %d, flat universes %d",
                fs == null ? "-" : "T" + fs.tier.n + (fs.tier.planets() ? " planets" : ""),
                rt.unlockedTier(),
                rt.state(),
                rt.transition() == null ? "" : ", " + rt.transitionLine(),
                ms.galaxies,
                ms.flowsHeld,
                ms.universes,
                ms.dawns,
                ms.superdenseSeconds,
                ms.widePulls,
                ms.flatUniverses));
        out.add(
            String.format(
                Locale.ROOT,
                "beam: %d injector(s), barrel Mk %d, grade %s (%s), stock %,d, rate %.2f/s, rho %.3f of rho_max %.2f,"
                    + " hold rating %d, accept limit %d, level %s%s",
                a.injectors,
                a.barrelMk,
                EyeAttractor.gradeName(a.grade),
                EyeAttractor.gevText(a.grade),
                a.stock,
                a.rate,
                fs == null || fs.beam == null ? 0 : fs.beam.rho,
                a.rhoMax,
                a.holdRating,
                a.acceptLimit(),
                a.level < 0 ? "none (8)" : String.valueOf(a.level),
                rt.ledger == null ? "" : String.format(Locale.ROOT, ", pellets drawn %,.0f", rt.ledger.pellets)));
        if (fs != null) {
            final FieldState.Flow f = fs.flow;
            out.add(
                String.format(
                    Locale.ROOT,
                    "field: P %s W %s Λ %s V %s H %s, I %.0f, zone %d; flow %s; last flow: %s",
                    StellarField.fmtSigned(fs.x[StellarField.P]),
                    StellarField.fmtSigned(fs.x[StellarField.W]),
                    StellarField.fmtSigned(fs.x[StellarField.L]),
                    StellarField.fmtSigned(fs.x[StellarField.V]),
                    StellarField.fmtSigned(fs.x[StellarField.H]),
                    fs.I,
                    fs.zone,
                    f == null ? "none"
                        : f.left > 0
                            ? String.format(
                                Locale.ROOT,
                                "SURGING %d of %d (tau %d, %d s Stable, side %s)",
                                f.size,
                                f.size,
                                f.tau,
                                f.ok,
                                f.sign > 0 ? "high" : "low")
                            : f.warned ? String.format(
                                Locale.ROOT,
                                "WARNED %d galaxies, side %s, %s, arrives in %.0f s",
                                f.size,
                                f.sign > 0 ? "high" : "low",
                                f.accepted ? "accepted" : "declined",
                                f.next - fs.t) : String.format(Locale.ROOT, "next in %.0f s", f.next - fs.t),
                    a.lastFlow.isEmpty() ? "none yet" : a.lastFlow));
            final FieldState.Universe u = fs.uni;
            if (u != null) out.add(
                String.format(
                    Locale.ROOT,
                    "universe: age %d / %d s (phase %d), Recombination at %d, Cosmic Dawn at %d (%s), mean Y %.3f,"
                        + " charge owed %,d L",
                    u.t,
                    UniverseField.BATCH,
                    UniverseField.phase(u),
                    UniverseField.REC_AT,
                    u.dawn,
                    u.dawnOk ? "on track" : "lost",
                    u.n > 0 ? u.ySum / u.n : 0,
                    a.uniOwed));
        }
        out.add(
            String.format(
                Locale.ROOT,
                "Deep Link: %s, streak %d / 1800 s, 60 s mean %.2f Bu/t (collar %.0f %%), beam rho %.2f; %s",
                ms.linkDone[8] ? "HELD" : "not held",
                a.deep.link.streak,
                a.deep.link.mean,
                100 * a.deep.link.frac,
                a.deep.rho,
                ms.progressLine(rt.unlockedTier() <= 8 ? 8 : 9)));
        return out;
    }
}
