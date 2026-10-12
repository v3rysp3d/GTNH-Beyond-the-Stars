package com.nateyburr.beyondthestars;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * One chat style for the addon's commands and right-click readouts, so every reply reads the
 * same way: a heading in AQUA, a success in GREEN, a caution in YELLOW, a refusal in RED and
 * a side note in GRAY. Bad arguments are not a chat line at all - commands throw
 * {@code WrongUsageException} like vanilla and GT do.
 */
public final class BTSChat {

    private BTSChat() {}

    /** Sends one line as it is (it may carry its own colour codes). */
    public static void send(ICommandSender to, String text) {
        to.addChatMessage(new ChatComponentText(text));
    }

    public static void header(ICommandSender to, String text) {
        send(to, EnumChatFormatting.AQUA + text);
    }

    public static void ok(ICommandSender to, String text) {
        send(to, EnumChatFormatting.GREEN + text);
    }

    public static void warn(ICommandSender to, String text) {
        send(to, EnumChatFormatting.YELLOW + text);
    }

    public static void error(ICommandSender to, String text) {
        send(to, EnumChatFormatting.RED + text);
    }

    public static void note(ICommandSender to, String text) {
        send(to, EnumChatFormatting.GRAY + text);
    }
}
