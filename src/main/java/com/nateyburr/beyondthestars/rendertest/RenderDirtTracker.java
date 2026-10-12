package com.nateyburr.beyondthestars.rendertest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * DEV-ONLY render QA (render QA E10): a client world listener that counts what makes the client re-render chunk
 * sections, by cause and by block (a GT machine by its MetaTileEntity class), so each shot's log line can say what kept
 * the chunk-update counter busy. A block update ({@code markBlockForUpdate}: a block change or a tile asking for one)
 * and a render update ({@code markBlockForRenderUpdate}: a light change, a tile's {@code markBlockForRenderUpdate})
 * each rebuild the sections around the block; a range update comes from a chunk arriving. Only registered by
 * {@link RenderTestClient} (client world) and {@link RenderTest} (server world, dev harness only).
 */
final class RenderDirtTracker implements IWorldAccess {

    private final World world;
    private final Map<String, Integer> counts = new HashMap<>();
    /** The call path of the first block update per GT machine class since the last reset (render QA P-1). */
    private final Map<String, String> stacks = new HashMap<>();
    private int total;

    RenderDirtTracker(World world) {
        this.world = world;
    }

    World world() {
        return world;
    }

    void reset() {
        counts.clear();
        stacks.clear();
        total = 0;
    }

    /** "class <- frame <- frame ..." for the first block update of each GT machine since the reset; "" if none. */
    String stacks() {
        final StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> e : stacks.entrySet()) {
            if (out.length() > 0) out.append(" | ");
            out.append(e.getKey())
                .append(" <- ")
                .append(e.getValue());
        }
        return out.toString();
    }

    /** The busiest causes since the last reset, "n x cause block", most first; "none" if nothing. */
    String top(int n) {
        if (total == 0) return "none";
        final List<Map.Entry<String, Integer>> list = new ArrayList<>(counts.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        final StringBuilder out = new StringBuilder().append(total)
            .append(" total: ");
        for (int i = 0; i < Math.min(n, list.size()); i++) {
            if (i > 0) out.append(", ");
            out.append(
                list.get(i)
                    .getValue())
                .append(" x ")
                .append(
                    list.get(i)
                        .getKey());
        }
        return out.toString();
    }

    private void count(String cause, int x, int y, int z) {
        total++;
        counts.merge(cause + " " + what(x, y, z), 1, Integer::sum);
    }

    private String what(int x, int y, int z) {
        if (y < 0 || y > 255 || !world.blockExists(x, y, z)) return "(unloaded)";
        final TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IGregTechTileEntity) {
            final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
            if (mte != null) return mte.getClass()
                .getSimpleName();
        }
        final Block block = world.getBlock(x, y, z);
        final String name = Block.blockRegistry.getNameForObject(block);
        return (name == null ? String.valueOf(block) : name) + ":" + world.getBlockMetadata(x, y, z);
    }

    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        count("block-update", x, y, z);
        if (y < 0 || y > 255 || !world.blockExists(x, y, z)) return;
        if (!(world.getTileEntity(x, y, z) instanceof IGregTechTileEntity)) return;
        final String key = what(x, y, z);
        if (stacks.containsKey(key)) return;
        final StringBuilder path = new StringBuilder();
        int kept = 0;
        for (StackTraceElement f : new Throwable().getStackTrace()) {
            final String cls = f.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("sun.")
                || cls.equals(RenderDirtTracker.class.getName())
                || "markBlockForUpdate".equals(f.getMethodName()) && kept == 0) continue;
            if (kept > 0) path.append(" <- ");
            path.append(cls.substring(cls.lastIndexOf('.') + 1))
                .append('.')
                .append(f.getMethodName())
                .append(':')
                .append(f.getLineNumber());
            if (++kept >= 9) break;
        }
        stacks.put(key, path.toString());
    }

    @Override
    public void markBlockForRenderUpdate(int x, int y, int z) {
        count("render-update", x, y, z);
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x0, int y0, int z0, int x1, int y1, int z1) {
        total++;
        counts.merge("range-update", 1, Integer::sum);
    }

    @Override
    public void playSound(String sound, double x, double y, double z, float volume, float pitch) {}

    @Override
    public void playSoundToNearExcept(EntityPlayer player, String sound, double x, double y, double z, float volume,
        float pitch) {}

    @Override
    public void spawnParticle(String particle, double x, double y, double z, double vx, double vy, double vz) {}

    @Override
    public void onEntityCreate(Entity entity) {}

    @Override
    public void onEntityDestroy(Entity entity) {}

    @Override
    public void playRecord(String record, int x, int y, int z) {}

    @Override
    public void broadcastSound(int id, int x, int y, int z, int data) {}

    @Override
    public void playAuxSFX(EntityPlayer player, int id, int x, int y, int z, int data) {}

    @Override
    public void destroyBlockPartially(int breaker, int x, int y, int z, int progress) {}

    @Override
    public void onStaticEntitiesChanged() {}
}
