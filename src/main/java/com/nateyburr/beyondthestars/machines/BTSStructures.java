package com.nateyburr.beyondthestars.machines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;

import gregtech.api.util.HatchElementBuilder;

/**
 * D-26 AXIS 4: the shared structure-element guard for the four new production multiblocks.
 * <p>
 * {@link MTEReactorCoreMk1} already carries this rule as a private helper bound to its own type.
 * The four new machines of S5 need exactly the same rule and cannot borrow it, so it lives here
 * once, generically, rather than being pasted four more times.
 * <p>
 * <b>The rule, and why it exists.</b> A plain {@code ofBlock} / {@code isAir} element's
 * {@code placeBlock} writes into the cell unconditionally. If a player has an Energy Hatch, a
 * chest or another machine standing in a cell the projection wants as casing, the hologram build
 * PAVES OVER IT and drops nothing - the tile entity is simply gone. {@code check} and
 * {@code spawnHint} pass straight through, so the hologram still draws the block the cell wants
 * and the structure check still fails loudly and names it; only the destructive WRITE is refused.
 * <p>
 * Wrap every plain-material element of a new structure in this. Hatch adders built with GT's
 * {@code buildHatchAdder} do not need it - they already refuse to overwrite - but the casing they
 * chain to does, and {@code ofChain} returns at the FIRST fallback whose {@code placeBlock}
 * answers true, so the casing half is exactly the half that would do the damage.
 * <p>
 * <b>NEI.</b> The wrapper also forwards {@code getBlocksToPlace}: the NEI multiblock preview
 * (BlockRenderer6343) cannot see through an unknown element class and asks it for its blocks
 * directly, so without the forward every wrapped casing would be missing from NEI's "used in
 * multiblock" index.
 */
public final class BTSStructures {

    private BTSStructures() {}

    /** True when something with a tile entity is standing in this cell. */
    public static boolean standsOnTile(World world, int x, int y, int z) {
        return world.getTileEntity(x, y, z) != null;
    }

    /** Delegate everything, but never WRITE into a cell that holds a tile entity. */
    public static <T> IStructureElement<T> noOverwrite(IStructureElement<T> base) {
        return new IStructureElement<T>() {

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return base.check(t, world, x, y, z);
            }

            @Override
            public boolean couldBeValid(T t, World world, int x, int y, int z, ItemStack trigger) {
                return base.couldBeValid(t, world, x, y, z, trigger);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return base.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public List<String> getDescription(T t) {
                return base.getDescription(t);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (standsOnTile(world, x, y, z)) return true;
                return base.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                return base.getBlocksToPlace(t, world, x, y, z, trigger, env);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                IItemSource source, EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                return base.survivalPlaceBlock(t, world, x, y, z, trigger, source, actor, chatter);
            }
        };
    }

    /**
     * The StructureLib channel that turns the OPTIONAL hatch sockets on (set it on the Multiblock Structure Projector's
     * channel screen). Without it every optional socket is plain casing in the hologram, the creative build, survival
     * auto-place and the NEI preview, so nothing ever suggests the hatch is needed.
     */
    public static final String OPTIONAL_HATCH_CHANNEL = "bts_optional";

    /** Registers {@link #OPTIONAL_HATCH_CHANNEL}'s description for the projector (once, at init). */
    public static void registerChannels() {
        StructureLibAPI.registerChannelDescription(
            OPTIONAL_HATCH_CHANNEL,
            "beyondthestars",
            "channels.beyondthestars." + OPTIONAL_HATCH_CHANNEL);
    }

    /** True when the trigger asks for the optional hatch sockets ({@link #OPTIONAL_HATCH_CHANNEL} set). */
    public static boolean wantsOptionalHatches(ItemStack trigger) {
        return trigger != null && ChannelDataAccessor.hasSubChannel(trigger, OPTIONAL_HATCH_CHANNEL);
    }

    /**
     * A socket for an OPTIONAL hatch: the walk accepts the hatch or the casing, but every build path treats the cell as
     * the casing unless {@link #OPTIONAL_HATCH_CHANNEL} is set.
     * <p>
     * Why: GT's {@code buildHatchAdder(...).atLeast(a, b, c).buildAndChain(casing)} asks for at least one of EACH listed
     * hatch. The NEI multiblock preview runs survival auto-place with GT's hatch channel on and a creative item source,
     * so it placed one of every optional hatch and listed them all in its required items; the hologram drew the
     * hatch's hint dot; and a projector with the hatch channel on pulled the hatches from the player's inventory. For an
     * optional late-tier part (the GRB Receptor Port on a T1 Kerr) that read as "required".
     * <p>
     * With the channel set the socket behaves exactly like {@code buildAndChain}: hint dot, hatch first, casing as the
     * fallback. It stays an {@link IStructureElementChain}, so NEI's usage index and hatch-dot overlay still see the
     * hatch.
     */
    public static <T> IStructureElement<T> optionalHatch(HatchElementBuilder<T> hatch, IStructureElement<T> casing) {
        final IStructureElement<T>[] chain = hatch.buildAndChain(casing)
            .fallbacks();
        return new IStructureElementChain<T>() {

            @Override
            public IStructureElement<T>[] fallbacks() {
                return chain;
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (wantsOptionalHatches(trigger)) return IStructureElementChain.super.spawnHint(t, world, x, y, z, trigger);
                return casing.spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                if (wantsOptionalHatches(trigger)) return IStructureElementChain.super.placeBlock(t, world, x, y, z, trigger);
                return casing.placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (wantsOptionalHatches(trigger)) {
                    return IStructureElementChain.super.getBlocksToPlace(t, world, x, y, z, trigger, env);
                }
                return casing.getBlocksToPlace(t, world, x, y, z, trigger, env);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (wantsOptionalHatches(trigger)) {
                    return IStructureElementChain.super.survivalPlaceBlock(t, world, x, y, z, trigger, env);
                }
                return casing.survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }

            @Override
            @SuppressWarnings("deprecation")
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                IItemSource source, EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
                if (wantsOptionalHatches(trigger)) {
                    return IStructureElementChain.super
                        .survivalPlaceBlock(t, world, x, y, z, trigger, source, actor, chatter);
                }
                return casing.survivalPlaceBlock(t, world, x, y, z, trigger, source, actor, chatter);
            }
        };
    }

    /**
     * A tiered block that the WALK accepts at any tier (the caller's {@code walk}, typically an
     * {@code ofChain} of {@code onElementPass} elements that records the lowest tier it meets),
     * while every BUILD path places one wanted tier and never downgrades:
     * <ul>
     * <li>{@code wanted} maps the trigger stack to an index into {@code metas} (low to high). It must
     * read the trigger only, never server state: holograms run client-side (trap 1).</li>
     * <li><b>Hologram hints</b> show the wanted tier.</li>
     * <li><b>Creative build</b> places the wanted tier, but keeps a cell that already holds this
     * block at the wanted tier or higher (so re-projecting over upgraded parts never downgrades
     * them) and never writes into a tile entity.</li>
     * <li><b>Survival auto-place</b> skips a cell that already holds the wanted tier or higher, and
     * otherwise places the wanted tier or, if the player has none, the next higher tier they do
     * have. A cell holding a LOWER tier is left alone (auto-place never breaks blocks); the player
     * swaps it by hand.</li>
     * </ul>
     *
     * @param block  the tiered block
     * @param metas  its metas, lowest tier first
     * @param walk   the element the structure check uses
     * @param wanted trigger stack to the wanted index into {@code metas}, clamped to the range
     */
    @SuppressWarnings("unchecked")
    public static <T> IStructureElement<T> tieredBlock(Block block, int[] metas, IStructureElement<T> walk,
        ToIntFunction<ItemStack> wanted) {
        final int n = metas.length;
        final IStructureElement<T>[] place = new IStructureElement[n];
        for (int i = 0; i < n; i++) {
            place[i] = noOverwrite(ofBlock(block, metas[i]));
        }
        // chains[i] = place[i], place[i + 1], ..., place[n - 1]: survival takes the first tier the
        // player can supply, and the chain only forwards "missing block" chat if all of them fail.
        final IStructureElement<T>[] chains = new IStructureElement[n];
        for (int i = 0; i < n; i++) {
            chains[i] = ofChain(Arrays.copyOfRange(place, i, n));
        }
        return new IStructureElement<T>() {

            private int want(ItemStack trigger) {
                return Math.max(0, Math.min(n - 1, wanted.applyAsInt(trigger)));
            }

            /** The tier index standing in the cell, or -1 if the cell holds something else. */
            private int present(World world, int x, int y, int z) {
                if (world.getBlock(x, y, z) != block) return -1;
                final int meta = world.getBlockMetadata(x, y, z);
                for (int i = 0; i < n; i++) {
                    if (metas[i] == meta) return i;
                }
                return -1;
            }

            @Override
            public boolean check(T t, World world, int x, int y, int z) {
                return walk.check(t, world, x, y, z);
            }

            @Override
            public boolean couldBeValid(T t, World world, int x, int y, int z, ItemStack trigger) {
                return walk.couldBeValid(t, world, x, y, z, trigger);
            }

            @Override
            public List<String> getDescription(T t) {
                return walk.getDescription(t);
            }

            @Override
            public boolean spawnHint(T t, World world, int x, int y, int z, ItemStack trigger) {
                return place[want(trigger)].spawnHint(t, world, x, y, z, trigger);
            }

            @Override
            public boolean placeBlock(T t, World world, int x, int y, int z, ItemStack trigger) {
                final int w = want(trigger);
                if (present(world, x, y, z) >= w) return true;
                return place[w].placeBlock(t, world, x, y, z, trigger);
            }

            @Override
            public BlocksToPlace getBlocksToPlace(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                return place[want(trigger)].getBlocksToPlace(t, world, x, y, z, trigger, env);
            }

            @Override
            public PlaceResult survivalPlaceBlock(T t, World world, int x, int y, int z, ItemStack trigger,
                AutoPlaceEnvironment env) {
                if (standsOnTile(world, x, y, z)) return PlaceResult.SKIP;
                final int w = want(trigger);
                if (present(world, x, y, z) >= w) return PlaceResult.SKIP;
                return chains[w].survivalPlaceBlock(t, world, x, y, z, trigger, env);
            }
        };
    }

    /**
     * A structure error's display text on one line, for status lines and chat: GT's "Any of:" lists
     * come as "- item" lines, which turn into "item / item".
     */
    public static String oneLine(String text) {
        if (text == null || text.indexOf('\n') < 0) return text;
        final StringBuilder sb = new StringBuilder();
        boolean lastItem = false;
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            final boolean item = line.startsWith("- ");
            if (item) line = line.substring(2);
            if (sb.length() > 0) sb.append(item && lastItem ? " / " : " ");
            sb.append(line);
            lastItem = item;
        }
        return sb.toString();
    }

    /**
     * A StructureLib channel's value on the trigger as a 0-based tier index (channel value 1 = index
     * 0), or {@code fallback} when the trigger does not set the channel. Channels are set on the
     * Multiblock Structure Projector (its channel screen) and by the NEI preview.
     */
    public static int channelIndex(ItemStack trigger, String channel, int fallback) {
        if (trigger == null || !ChannelDataAccessor.hasSubChannel(trigger, channel)) return fallback;
        return ChannelDataAccessor.getChannelData(trigger, channel) - 1;
    }

    /**
     * The per-round survival auto-place budget for a structure of tens of thousands of blocks.
     * StructureLib's default round is 25 elements every 300 ms, which would take minutes per
     * thousand blocks; GT's largest multis (TecTech's Eye of Harmony among them) scale a small
     * budget up, and this is the same rule with a factor sized for our structures: 25 becomes 200.
     * A budget already at 200 or more (a raised config, or NEI's preview, which passes
     * {@code Integer.MAX_VALUE}) is used as is.
     */
    public static int survivalBudget(int elementBudget) {
        return elementBudget >= 200 ? elementBudget : Math.min(200, elementBudget * 8);
    }
}
