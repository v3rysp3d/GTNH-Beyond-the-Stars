package com.nateyburr.beyondthestars.rendertest;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * DEV-ONLY render QA (round 3, P-1): the harness player's server net handler, counting the tile description packets
 * (S35) the server sends it for GT machines and keeping the server call path of the first one per machine class, to
 * find what makes a client rebuild a machine's chunk section. Only installed by {@link RenderTest} when the render
 * test harness runs.
 */
final class PacketSpy extends NetHandlerPlayServer {

    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, String> paths = new HashMap<>();

    private PacketSpy(MinecraftServer server, EntityPlayerMP player) {
        super(server, player.playerNetServerHandler.netManager, player);
    }

    /** Puts a spy on the player's connection (once). */
    static PacketSpy install(EntityPlayerMP player) {
        if (player.playerNetServerHandler instanceof PacketSpy) return (PacketSpy) player.playerNetServerHandler;
        return new PacketSpy(MinecraftServer.getServer(), player);
    }

    @Override
    public void sendPacket(Packet packet) {
        if (packet instanceof S35PacketUpdateTileEntity) record((S35PacketUpdateTileEntity) packet);
        if (packet instanceof S24PacketBlockAction) {
            final S24PacketBlockAction a = (S24PacketBlockAction) packet;
            final String key = machine(a.func_148867_d(), a.func_148866_e(), a.func_148865_f());
            if (key != null)
                counts.merge("block event " + a.func_148869_g() + "=" + a.func_148864_h() + " " + key, 1, Integer::sum);
        }
        super.sendPacket(packet);
    }

    private void record(S35PacketUpdateTileEntity p) {
        final World world = playerEntity.worldObj;
        final int x = p.func_148856_c(), y = p.func_148855_d(), z = p.func_148854_e();
        if (world == null || y < 0 || y > 255 || !world.blockExists(x, y, z)) return;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        final String key = mte == null ? "GT tile"
            : mte.getClass()
                .getSimpleName();
        counts.merge(key, 1, Integer::sum);
        if (paths.containsKey(key)) return;
        final StringBuilder path = new StringBuilder();
        int kept = 0;
        for (StackTraceElement f : new Throwable().getStackTrace()) {
            final String cls = f.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("sun.") || cls.equals(PacketSpy.class.getName())) continue;
            if (kept > 0) path.append(" <- ");
            path.append(cls.substring(cls.lastIndexOf('.') + 1))
                .append('.')
                .append(f.getMethodName())
                .append(':')
                .append(f.getLineNumber());
            if (++kept >= 10) break;
        }
        paths.put(key, path.toString());
    }

    /** The GT machine class at a cell, or null. */
    private String machine(int x, int y, int z) {
        final World world = playerEntity.worldObj;
        if (world == null || y < 0 || y > 255 || !world.blockExists(x, y, z)) return null;
        final TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof IGregTechTileEntity)) return null;
        final IMetaTileEntity mte = ((IGregTechTileEntity) te).getMetaTileEntity();
        return mte == null ? "GT tile"
            : mte.getClass()
                .getSimpleName();
    }

    /** "n x class (path)" per machine since the last call, then resets; "none" if nothing. */
    String drain() {
        if (counts.isEmpty()) return "none";
        final StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (out.length() > 0) out.append(" | ");
            out.append(e.getValue())
                .append(" x ")
                .append(e.getKey())
                .append(paths.containsKey(e.getKey()) ? " <- " + paths.get(e.getKey()) : "");
        }
        counts.clear();
        paths.clear();
        return out.toString();
    }
}
