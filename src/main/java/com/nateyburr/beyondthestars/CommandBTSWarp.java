package com.nateyburr.beyondthestars;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * {@code /btswarp <N> | off | status} - TIME WARP, a cheat and testing aid ({@link TimeWarp}; docs/automation.md
 * §4.2): the whole server runs up to N ticks for every real one, through the same code as normal play, so a long
 * process (a star lighting, a cooling batch, a merger, an install) plays out N times faster. Op only (permission
 * level 2: in single player, cheats on), off by default and off again when the server stops.
 */
public class CommandBTSWarp extends CommandBase {

    @Override
    public String getCommandName() {
        return "btswarp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btswarp <2-" + TimeWarp.MAX + "> | off | status  (cheat: the whole server runs N ticks per real tick)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) throw new WrongUsageException(getCommandUsage(sender));
        final String a = args[0].toLowerCase(java.util.Locale.ROOT);
        final String line;
        if (a.equals("off") || a.equals("0") || a.equals("1")) {
            line = TimeWarp.off(sender.getCommandSenderName());
        } else if (a.equals("status")) {
            line = TimeWarp.on() ? "Time warp " + TimeWarp.status() : "Time warp is off.";
        } else {
            final int n = parseIntBounded(sender, a.replace("x", ""), 2, TimeWarp.MAX);
            line = TimeWarp.set(n, sender.getCommandSenderName());
        }
        sender.addChatMessage(
            new ChatComponentText((TimeWarp.on() ? EnumChatFormatting.GOLD : EnumChatFormatting.YELLOW) + line));
    }
}
