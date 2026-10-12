package com.nateyburr.beyondthestars.machines.eye.star;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.fluids.SLFluids;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRig;
import com.nateyburr.beyondthestars.machines.eye.field.FieldRng;
import com.nateyburr.beyondthestars.machines.eye.field.FieldState;
import com.nateyburr.beyondthestars.machines.eye.field.TierParams;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;

/**
 * D-49: the Eye's star NBT, the one writer and reader (d49-build-plan.md §3.3, §13), and the save migration from
 * 0.24.x.
 * <p>
 * <b>Trap 2 (tile vs item NBT).</b> {@link #writePersistent} is everything that survives a wrench (schema, unlocked
 * tier, milestones, escrow, the dial and feed setpoints, migration debts); the controller calls it from both
 * {@code saveNBTData} and {@code setItemNBT}. {@link #writeStar} is the live star and goes into the tile only: a
 * wrenched controller comes back DARK. {@link #read} takes either.
 * <p>
 * <b>Migration</b> (schema absent = written by 0.24.x): {@code btsEyeGalactic} → unlocked T4; otherwise the bands are
 * grandfathered at the first successful walk ({@link EyeStarRuntime#grandfather}); a pre-D-49 star run in flight is
 * paid out from {@link #legacyOutput} once the Eye stands. A brand-new controller with no tag never reaches
 * {@link #read} (GT calls {@code loadNBTData} only for an item or tile that carries NBT); one with an unrelated tag
 * (an anvil name) is told apart by {@link #isLegacy}. Either way it starts at unlocked T1.
 */
public final class EyeNbt {

    /** 2 = D-49. Absent = 0.24.x (legacy). */
    public static final int SCHEMA = 2;

    public static final String SCHEMA_KEY = "btsEyeSchema";
    public static final String GALACTIC_KEY = "btsEyeGalactic";
    /** Item NBT only (a wrenched controller): the "Last failure" line it carries (B-71). */
    public static final String PICKUP_FAIL_KEY = "btsEyePickupFail";

    private EyeNbt() {}

    // ================================================================================== write

    /** The persistent part (item and tile NBT). */
    public static void writePersistent(EyeStarRuntime rt, NBTTagCompound nbt) {
        nbt.setInteger(SCHEMA_KEY, SCHEMA);
        nbt.setInteger("btsEyeUnlocked", rt.unlockedTier);
        nbt.setTag("btsEyeMilestones", rt.milestones.write());
        if (rt.escrow != null) nbt.setTag("btsEyeEscrow", rt.escrow.writeToNBT(new NBTTagCompound()));
        nbt.setDouble("btsEyeDial", rt.dialSet);
        nbt.setDouble("btsEyeFeed", rt.feedSet);
        if (rt.legacyPending) nbt.setBoolean("btsEyeLegacyPending", true);
        if (!rt.legacyPay.isEmpty()) {
            final NBTTagCompound pay = new NBTTagCompound();
            for (Map.Entry<String, Long> e : rt.legacyPay.entrySet()) pay.setLong(e.getKey(), e.getValue());
            nbt.setTag("btsEyeLegacyPay", pay);
        }
    }

    /** The live star (tile NBT only). */
    public static void writeStar(EyeStarRuntime rt, NBTTagCompound nbt) {
        final FieldState fs = rt.fs;
        if (fs == null) return;
        final NBTTagCompound s = new NBTTagCompound();
        s.setByte("state", (byte) rt.state.ordinal());
        s.setByte("tier", (byte) fs.tier.n);
        if (rt.star != null) s.setString("key", rt.star.key);
        s.setDouble("P", fs.x[0]);
        s.setDouble("Th", fs.x[1]);
        s.setDouble("Om", fs.x[2]);
        s.setDouble("Pi", fs.x[3]);
        s.setDouble("A", fs.A);
        s.setDouble("I", fs.I);
        s.setInteger("lock", fs.lock);
        s.setInteger("rng", fs.rng.state());
        s.setLong("seed", rt.seed);
        s.setLong("t", fs.t);
        s.setDouble("eyeT", fs.eyeT);
        s.setDouble("cycR", rt.cycR);
        s.setInteger("cycP", rt.cycP); // the parallel the cycle / batch paid for (D-49 review)
        s.setInteger("batchP", rt.batchP);
        for (int k = 0; k < 4; k++) s.setDouble("prevE" + k, fs.prevE[k]);
        s.setByte("prevWarn", (byte) fs.prevWarn);
        if (fs.ev != null) {
            final NBTTagCompound ev = new NBTTagCompound();
            ev.setDouble("next", fs.ev.next); // fractional (B-34 item 3)
            ev.setInteger("left", fs.ev.left);
            ev.setBoolean("warned", fs.ev.warned);
            ev.setByte("sign", (byte) fs.ev.sign);
            s.setTag("ev", ev);
        }
        final NBTTagCompound cyc = new NBTTagCompound();
        cyc.setDouble("eyeT", fs.cyc.eyeT);
        cyc.setDouble("ySum", fs.cyc.ySum);
        cyc.setInteger("n", fs.cyc.n);
        cyc.setDouble("rMin", fs.cyc.rMin);
        cyc.setDouble("hsm", fs.cyc.hsm);
        s.setTag("cyc", cyc);
        if (fs.batch != null) {
            final NBTTagCompound b = new NBTTagCompound();
            b.setDouble("eyeT", fs.batch.eyeT);
            b.setDouble("ySum", fs.batch.ySum);
            b.setInteger("n", fs.batch.n);
            b.setDouble("rMin", fs.batch.rMin);
            b.setBoolean("warned", fs.batch.warned);
            s.setTag("batch", b);
        }
        s.setDouble("tengam", rt.tengam);
        s.setDouble("hsmOwed", rt.hsmOwed);
        s.setDouble("prodCarry", rt.prodCarry);
        s.setInteger("cycWorst", rt.cycWorst);
        s.setInteger("flashWatch", rt.flashState);
        s.setInteger("flashTail", rt.flashTail);
        s.setBoolean("flashOk", rt.flashOk);
        if (rt.rig != null) s.setString("rig", rt.rig.name());
        if (!rt.lastEvent.isEmpty()) s.setString("lastEvent", rt.lastEvent);
        if (!rt.lastFailure.isEmpty()) s.setString("lastFail", rt.lastFailure);
        if (rt.ledger != null) s.setTag("ledger", writeLedger(rt.ledger));
        s.setDouble("shock", rt.bounceShock);
        s.setDouble("dwarfReserve", rt.dwarfReserve);
        s.setLong("topUps", rt.topUps);
        s.setLong("tengamOut", rt.tengamOut);
        nbt.setTag("btsEyeStar", s);
        // D-49 unit 5: the transition running (tile only: a wrench aborts it, plan Q-10)
        if (rt.trans != null) nbt.setTag("btsEyeTrans", EyeTransition.write(rt.trans));
    }

    private static NBTTagCompound writeLedger(EyeStarRuntime.Ledger l) {
        final NBTTagCompound out = new NBTTagCompound();
        out.setLong("seed", l.seed);
        out.setInteger("startTier", l.startTier);
        out.setLong("batches", l.batches);
        out.setLong("collapseBeginT", l.collapseBeginT);
        out.setDouble("collapseShock", l.collapseShock);
        out.setDouble("collapseStBuf", l.collapseStBuf);
        out.setDouble("stAtBounce", l.stAtBounce);
        for (int z = 0; z < 5; z++) out.setLong("z" + z, l.zoneT[z]);
        out.setDouble("ySum", l.ySum);
        out.setLong("yN", l.yN);
        out.setLong("fails", l.fails);
        out.setLong("cycles", l.cycles);
        out.setLong("steps", l.steps);
        out.setDouble("product", l.product);
        out.setDouble("hsm", l.hsm);
        out.setInteger("hash", l.hash);
        return out;
    }

    private static EyeStarRuntime.Ledger readLedger(NBTTagCompound in) {
        final EyeStarRuntime.Ledger l = new EyeStarRuntime.Ledger(
            in.getLong("seed"),
            in.hasKey("startTier") ? in.getInteger("startTier") : 1);
        l.batches = in.getLong("batches");
        l.collapseBeginT = in.hasKey("collapseBeginT") ? in.getLong("collapseBeginT") : -1;
        l.collapseShock = in.getDouble("collapseShock");
        l.collapseStBuf = in.getDouble("collapseStBuf");
        l.stAtBounce = in.getDouble("stAtBounce");
        for (int z = 0; z < 5; z++) l.zoneT[z] = in.getLong("z" + z);
        l.ySum = in.getDouble("ySum");
        l.yN = in.getLong("yN");
        l.fails = in.getLong("fails");
        l.cycles = in.getLong("cycles");
        l.steps = in.getLong("steps");
        l.product = in.getDouble("product");
        l.hsm = in.getDouble("hsm");
        l.hash = in.getInteger("hash");
        return l;
    }

    // =================================================================================== read

    /**
     * Reads item or tile NBT into a runtime reset to defaults. Returns true when the NBT was written by 0.24.x (no
     * schema): the controller then migrates its run in flight too.
     */
    public static boolean read(EyeStarRuntime rt, NBTTagCompound nbt) {
        if (!nbt.hasKey(SCHEMA_KEY)) {
            // A tag with no schema and no sign of a placed 0.24.x machine (an anvil name, any other item-only tag
            // on a freshly crafted controller) is a NEW controller: defaults, unlocked T1, nothing grandfathered.
            if (!isLegacy(nbt)) return false;
            if (nbt.getBoolean(GALACTIC_KEY)) {
                rt.unlockedTier = 4; // they paid for a Galactic core; the neutron star still needs a Core Collapse
                rt.legacyPending = false;
            } else {
                rt.unlockedTier = 1;
                rt.legacyPending = true; // grandfathered at the first successful walk
            }
            return true;
        }
        rt.unlockedTier = Math.max(1, Math.min(6, nbt.getInteger("btsEyeUnlocked")));
        rt.milestones.read(nbt.hasKey("btsEyeMilestones") ? nbt.getCompoundTag("btsEyeMilestones") : null);
        rt.escrow = nbt.hasKey("btsEyeEscrow") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("btsEyeEscrow"))
            : null;
        if (nbt.hasKey("btsEyeDial")) rt.setDialQuiet(nbt.getDouble("btsEyeDial"));
        if (nbt.hasKey("btsEyeFeed")) rt.setFeedQuiet(nbt.getDouble("btsEyeFeed"));
        rt.legacyPending = nbt.getBoolean("btsEyeLegacyPending");
        rt.legacyPay.clear();
        if (nbt.hasKey("btsEyeLegacyPay")) {
            final NBTTagCompound pay = nbt.getCompoundTag("btsEyeLegacyPay");
            for (Object key : pay.func_150296_c()) rt.legacyPay.put((String) key, pay.getLong((String) key));
        }
        if (nbt.hasKey("btsEyeStar")) readStar(rt, nbt.getCompoundTag("btsEyeStar"));
        rt.trans = nbt.hasKey("btsEyeTrans") && rt.live() ? EyeTransition.read(nbt.getCompoundTag("btsEyeTrans"))
            : null;
        if (nbt.hasKey(PICKUP_FAIL_KEY)) rt.restorePickupFailure(nbt.getString(PICKUP_FAIL_KEY));
        return false;
    }

    /**
     * True when schema-less NBT really comes from a 0.24.x Eye (D-49 review): tile NBT (GT's {@code mID}), any of the
     * 0.24.x Eye's own keys ({@code btsEyeGalactic}, {@code btsEyeBuffers}, {@code btsEyeRun*}), or the keys GT's
     * {@code getDrops} puts on a dropped machine (upgrades, colour, covers, strong redstone). A crafted controller
     * carries none of these, so renaming it in an anvil no longer grandfathers it up to T3. A 0.24.x controller
     * wrenched with nothing to save has no tag at all and was already treated as new (B-37 item 7).
     */
    static boolean isLegacy(NBTTagCompound nbt) {
        if (nbt.hasKey("mID") || nbt.hasKey("mLockUpgrade")
            || nbt.hasKey("mColor")
            || nbt.hasKey("mOtherUpgrades")
            || nbt.hasKey("mStrongRedstone")
            || nbt.hasKey(GTValues.NBT.COVERS)) return true;
        for (Object key : nbt.func_150296_c()) {
            if (key instanceof String && ((String) key).startsWith("btsEye")) return true;
        }
        return false;
    }

    private static void readStar(EyeStarRuntime rt, NBTTagCompound s) {
        final TierParams tier;
        try {
            tier = TierParams.of(Math.max(1, s.getByte("tier")));
        } catch (IllegalArgumentException bad) {
            return;
        }
        final FieldState fs = new FieldState(tier, FieldRng.ofState(s.getInteger("rng")));
        fs.setX(s.getDouble("P"), s.getDouble("Th"), s.getDouble("Om"), s.getDouble("Pi"));
        fs.A = s.getDouble("A");
        fs.I = s.getDouble("I");
        fs.lock = s.getInteger("lock");
        fs.t = s.getLong("t");
        fs.eyeT = s.getDouble("eyeT");
        for (int k = 0; k < 4; k++) fs.prevE[k] = s.getDouble("prevE" + k);
        fs.prevWarn = s.getByte("prevWarn");
        if (s.hasKey("ev")) {
            final NBTTagCompound ev = s.getCompoundTag("ev");
            fs.ev = new FieldState.EventState(ev.getDouble("next"));
            fs.ev.left = ev.getInteger("left");
            fs.ev.warned = ev.getBoolean("warned");
            fs.ev.sign = ev.getByte("sign") < 0 ? -1 : 1;
        } else {
            fs.ev = null;
        }
        final NBTTagCompound cyc = s.getCompoundTag("cyc");
        fs.cyc = new FieldState.Cycle(cyc.getDouble("eyeT"));
        fs.cyc.ySum = cyc.getDouble("ySum");
        fs.cyc.n = cyc.getInteger("n");
        fs.cyc.rMin = cyc.hasKey("rMin") ? cyc.getDouble("rMin") : 1;
        fs.cyc.hsm = cyc.getDouble("hsm");
        if (s.hasKey("batch")) {
            final NBTTagCompound b = s.getCompoundTag("batch");
            fs.batch = new FieldState.Batch();
            fs.batch.eyeT = b.getDouble("eyeT");
            fs.batch.ySum = b.getDouble("ySum");
            fs.batch.n = b.getInteger("n");
            fs.batch.rMin = b.getDouble("rMin");
            fs.batch.warned = b.getBoolean("warned");
        }
        rt.fs = fs;
        rt.seed = s.getLong("seed");
        rt.star = s.hasKey("key") ? StarDef.byKey(s.getString("key")) : null;
        final int st = s.getByte("state");
        rt.state = st >= 0 && st < EyeStarRuntime.StarState.values().length ? EyeStarRuntime.StarState.values()[st]
            : EyeStarRuntime.StarState.DARK;
        if (rt.star == null || rt.star.tier.n != tier.n && rt.state != EyeStarRuntime.StarState.DARK) {
            // the star's definition is gone (or disagrees with the field): it goes out, the field clock stays
            if (rt.state != EyeStarRuntime.StarState.DARK) {
                BeyondTheStars.LOG
                    .warn("[BTS] Eye: the saved star {} is no longer registered; it goes out.", s.getString("key"));
            }
            rt.state = EyeStarRuntime.StarState.DARK;
        }
        rt.cycR = s.hasKey("cycR") ? s.getDouble("cycR") : 1;
        rt.cycP = s.getInteger("cycP"); // 0 (an older save): no cap until the next charge
        rt.batchP = s.getInteger("batchP");
        rt.tengam = s.getDouble("tengam");
        rt.hsmOwed = s.getDouble("hsmOwed");
        rt.prodCarry = s.getDouble("prodCarry");
        rt.cycWorst = s.getInteger("cycWorst");
        rt.flashState = s.getInteger("flashWatch");
        rt.flashTail = s.getInteger("flashTail");
        rt.flashOk = s.getBoolean("flashOk");
        rt.rig = s.hasKey("rig") ? FieldRig.Mode.parse(s.getString("rig")) : null;
        rt.lastEvent = s.getString("lastEvent");
        rt.lastFailure = s.getString("lastFail");
        rt.ledger = s.hasKey("ledger") ? readLedger(s.getCompoundTag("ledger")) : null;
        if (s.hasKey("shock")) rt.bounceShock = s.getDouble("shock");
        rt.dwarfReserve = s.getDouble("dwarfReserve");
        rt.topUps = s.getLong("topUps");
        rt.tengamOut = s.getLong("tengamOut");
    }

    // ============================================================================== migration

    /**
     * Plan §13 item 3: what one success of a pre-D-49 star run made (its inputs and EU were paid at its start), or
     * null for planets and unknown keys.
     */
    public static FluidStack legacyOutput(String key) {
        if (key == null) return null;
        switch (key) {
            case "star:sol":
                return stack(Materials.RawStarMatter.getFluid(1L), 1_000_000);
            case "star:siriusa":
                return stack(SLFluids.excitedStellarPlasma, 100_000);
            case "star:arcturus":
                return stack(SLFluids.rawHyperdensePlasma, 50_000);
            case "star:siriusb":
                return stack(Materials.WhiteDwarfMatter.getMolten(1L), 18_432);
            case "star:siriusbcooled":
                return stack(Materials.BlackDwarfMatter.getMolten(1L), 4_608);
            default:
                return null;
        }
    }

    private static FluidStack stack(FluidStack one, int amount) {
        return one == null || one.getFluid() == null ? null : new FluidStack(one.getFluid(), amount);
    }

    private static FluidStack stack(Fluid fluid, int amount) {
        return fluid == null ? null : new FluidStack(fluid, amount);
    }

    /** Records a legacy run's payout (litres = per-success output × successes × yield) on the runtime. */
    public static void owe(EyeStarRuntime rt, String key, long successes, double yield) {
        final FluidStack per = legacyOutput(key);
        if (per == null) return;
        final long litres = (long) Math.floor(per.amount * (double) successes * yield);
        if (litres <= 0) return;
        rt.legacyPay.merge(
            per.getFluid()
                .getName(),
            litres,
            Long::sum);
        BeyondTheStars.LOG.info(
            "[BTS] Eye migration: the pre-D-49 run {} in flight ({} successes) is owed {} L of {}.",
            key,
            successes,
            litres,
            per.getFluid()
                .getName());
    }
}
