package com.nateyburr.beyondthestars;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.google.gson.GsonBuilder;
import com.nateyburr.beyondthestars.machines.gui.BTSTip;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.ISecondaryDescribable;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/**
 * Bench: {@code /btstooltips [file]} writes every tooltip of the addon, and a few of GregTech's own for reference, to
 * a JSON file (default {@code tooltips.json} in the server folder) with the formatting codes kept, so tooltips can be
 * reviewed and rendered without a game client. Per entry: the name, the lines as shown, and the Shift lines (a
 * multiblock's structure section, a hatch's details).
 */
public class CommandBTSTooltips extends CommandBase {

    /** GregTech machines dumped for comparison (MTE class simple names). */
    private static final List<String> GT_REFERENCE = Arrays.asList(
        "MTEBlackHoleCompressor",
        "MTEForgeOfGods",
        "MTEPlasmaModule",
        "MTEEyeOfHarmony",
        "MTENanochipAssemblyComplex",
        "MTELargeHadronCollider",
        "MTEQuantumForceTransformer",
        "MTEBlackHoleUtility");

    @Override
    public String getCommandName() {
        return "btstooltips";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btstooltips [file]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final File file = new File(args.length > 0 ? args[0] : "tooltips.json");
        final Map<String, Object> all = new LinkedHashMap<>();
        final IMetaTileEntity[] mtes = GregTechAPI.METATILEENTITIES;
        for (int id = 1; id < mtes.length; id++) {
            final IMetaTileEntity m = mtes[id];
            if (m == null) continue;
            final String cls = m.getClass()
                .getName();
            final boolean ours = cls.startsWith("com.nateyburr.");
            final boolean ref = GT_REFERENCE.contains(
                m.getClass()
                    .getSimpleName());
            if (!ours && !ref) continue;
            try {
                all.put((ours ? "bts" : "gt") + ".mte." + id, machine(m));
            } catch (RuntimeException bad) {
                all.put((ours ? "bts" : "gt") + ".mte." + id, "FAILED: " + bad);
            }
        }
        for (Map.Entry<String, String> e : langTooltips().entrySet()) {
            final String base = e.getKey();
            final Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", StatCollector.translateToLocal(base + ".name"));
            final List<String> lines = new ArrayList<>();
            for (String line : e.getValue()
                .split("\\\\n")) lines.add(EnumChatFormatting.GRAY + line);
            o.put("lines", lines);
            all.put("bts." + base, o);
        }
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(all, w);
        } catch (Exception bad) {
            BTSChat.error(sender, "could not write " + file + ": " + bad);
            return;
        }
        BTSChat.ok(sender, all.size() + " tooltips written to " + file.getAbsolutePath());
    }

    private static Map<String, Object> machine(IMetaTileEntity m) {
        final Map<String, Object> o = new LinkedHashMap<>();
        final ItemStack stack = m.getStackForm(1L);
        o.put("name", stack != null ? stack.getDisplayName() : m.getLocalName());
        o.put(
            "class",
            m.getClass()
                .getSimpleName());
        if (m instanceof ISecondaryDescribable) {
            o.put("lines", Arrays.asList(((ISecondaryDescribable) m).getPrimaryDescription()));
            o.put("shift", Arrays.asList(((ISecondaryDescribable) m).getSecondaryDescription()));
        } else {
            try {
                BTSTip.forceShift = false;
                o.put("lines", Arrays.asList(nn(m.getDescription())));
                BTSTip.forceShift = true;
                final List<String> shift = Arrays.asList(nn(m.getDescription()));
                if (!shift.equals(o.get("lines"))) o.put("shift", shift);
            } finally {
                BTSTip.forceShift = null;
            }
        }
        return o;
    }

    private static String[] nn(String[] a) {
        return a == null ? new String[0] : a;
    }

    /** Every {@code item.*.tooltip} / {@code tile.*.tooltip} of our lang file: base key to raw text. */
    private static Map<String, String> langTooltips() {
        final Map<String, String> out = new LinkedHashMap<>();
        try (InputStream in = CommandBTSTooltips.class.getResourceAsStream("/assets/beyondthestars/lang/en_US.lang")) {
            if (in == null) return out;
            final BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                final int eq = line.indexOf('=');
                if (eq < 0 || line.startsWith("#")) continue;
                final String key = line.substring(0, eq);
                if ((key.startsWith("item.") || key.startsWith("tile.")) && key.endsWith(".tooltip")) {
                    out.put(key.substring(0, key.length() - ".tooltip".length()), line.substring(eq + 1));
                }
            }
        } catch (Exception ignored) {
            // nothing to add
        }
        return out;
    }
}
