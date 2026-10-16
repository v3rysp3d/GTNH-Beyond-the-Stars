package com.nateyburr.beyondthestars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

/**
 * B-227: the server tells every player at login which recipe settings it runs ({@link Config#recipeFlags()}), and a
 * client whose own config differs says so in chat (client half: {@code client.BTSConfigSyncClient}).
 * <p>
 * <b>Why a warning and not a reload.</b> In 1.7.10 neither GT nor NEI syncs recipes: each side builds its own recipe
 * maps from its own config at startup, and NEI reads the client's. Ours are not reversible at runtime. The BHC swap,
 * the TecTech Eye removal, the Resonance Chamber rewrites and the Piko / Quantum / Planck raise change GT's own recipes
 * in place at load-complete; {@code stellarCircuits} also sets GT's circuit ore names in preInit, which recipes
 * snapshot as they are built. Undoing that on a connected client would mean rebuilding GT's recipe maps, which only a
 * restart does. So the client keeps its own recipes and is told exactly which keys differ, so the player can copy the
 * server's values into {@code config/beyondthestars.cfg} and restart. Recipes are always run on the server, so
 * crafting is never wrong, only what NEI shows.
 * <p>
 * The mod had no network channel before this (its tile sync rides on vanilla description packets), so this registers
 * the first one: {@code beyondthestars}, discriminator 0. The packet is a key / value list, so a newer server's extra
 * keys are skipped by an older client and the other way round.
 */
public final class BTSConfigSync {

    /** The mod's network channel (FML's SimpleNetworkWrapper). */
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(BeyondTheStars.MODID);

    /** Packet layout version (first byte). */
    static final int VERSION = 1;

    /** The server's values from the last login packet, or null; set on the network thread, read on the client's. */
    public static volatile Map<String, String> received;

    /** Registers the packet (both sides, preInit) and the login listener. */
    public static void register() {
        CHANNEL.registerMessage(Handler.class, Message.class, 0, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new BTSConfigSync());
    }

    /** Server: send the recipe settings to each player as they log in (single player too, where they always match). */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            CHANNEL.sendTo(new Message(Config.recipeFlags()), (EntityPlayerMP) event.player);
        }
    }

    /**
     * Keys both sides know whose values differ, as {@code key (server X, yours Y)}. Keys only one side knows (another
     * jar version) are left out.
     */
    public static List<String> differences(Map<String, String> server, Map<String, String> local) {
        final List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : server.entrySet()) {
            final String mine = local.get(e.getKey());
            if (mine != null && !mine.equals(e.getValue())) {
                out.add(e.getKey() + " (server " + e.getValue() + ", yours " + mine + ")");
            }
        }
        return out;
    }

    /** The chat lines a client shows for these differences (none when they match). */
    public static List<String> chatLines(List<String> diffs) {
        if (diffs.isEmpty()) return Collections.emptyList();
        final List<String> out = new ArrayList<>();
        out.add(
            EnumChatFormatting.YELLOW + "[Beyond the Stars] This server's recipe settings differ from your config: "
                + String.join(", ", diffs)
                + ".");
        out.add(
            EnumChatFormatting.GRAY + "NEI shows the recipes of your own config, so it may be wrong for this server"
                + " until config/beyondthestars.cfg has the server's values and you restart. Crafting itself follows"
                + " the server.");
        return out;
    }

    /** The login packet: the server's {@link Config#recipeFlags()}. Public, with a no-argument constructor, for FML. */
    public static final class Message implements IMessage {

        public Map<String, String> flags = new LinkedHashMap<>();

        public Message() {}

        public Message(Map<String, String> flags) {
            this.flags = flags;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeByte(VERSION);
            buf.writeShort(flags.size());
            for (Map.Entry<String, String> e : flags.entrySet()) {
                ByteBufUtils.writeUTF8String(buf, e.getKey());
                ByteBufUtils.writeUTF8String(buf, e.getValue());
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            flags = new LinkedHashMap<>();
            if (buf.readUnsignedByte() != VERSION) return;
            final int n = buf.readUnsignedShort();
            for (int i = 0; i < n; i++) {
                final String key = ByteBufUtils.readUTF8String(buf);
                flags.put(key, ByteBufUtils.readUTF8String(buf));
            }
        }
    }

    /** Client: keep the values for the client tick to compare (no client class is touched here). */
    public static final class Handler implements IMessageHandler<Message, IMessage> {

        @Override
        public IMessage onMessage(Message message, MessageContext ctx) {
            received = message.flags;
            return null;
        }
    }
}
