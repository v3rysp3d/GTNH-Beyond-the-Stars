package com.nateyburr.beyondthestars;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nateyburr.beyondthestars.fluids.SLRealityFluids;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipe;
import com.nateyburr.beyondthestars.machines.eye.EyeRecipeRegistry;
import com.nateyburr.beyondthestars.machines.eye.star.StarDef;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.NaniteTier;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import tectech.recipe.EyeOfHarmonyRecipe;
import tectech.recipe.TecTechRecipeMaps;
import tectech.util.FluidStackLong;
import tectech.util.ItemStackLong;

/**
 * {@code /btsrecipes [status | dump [file]]} (console or op): every recipe this mod registers, changes or removes, read
 * from the LIVE recipe maps after load-complete, written as JSON for the playtest guide's "All new recipes" page.
 * <p>
 * The live maps are the truth: the dump walks every GT recipe map, the crafting table and the Eye's own registry, and
 * asks {@link BTSRecipeLedger} which of those objects a guarded registration added (by identity). A recipe GT dropped
 * or a load-complete pass removed is therefore not listed as new, and ids that registered but left nothing live are
 * listed separately. What we did to the pack's own recipes comes from the code that did it: "changed-GT" (rewritten in
 * place, with the recipe as it was before) and "removed" (with what replaces it). Read-only.
 */
public class CommandBTSRecipes extends CommandBase {

    /** The lines ("group" in the JSON). Public so the code that changes or removes a pack recipe can name its line. */
    public static final String KERR = "Kerr", EYE = "Eye", BEC = "BEC / Condensate", HR = "Homogeny / Reality",
        QUASAR = "Quasar / Conduit", P5 = "Great Attractor / MAX", AUTOMATION = "Automation", OLD_PLANT = "Old plant",
        UPGRADE = "Upgrade path", CIRCUITS = "Circuits", OTHER = "Other";

    static final String NEW = "new", CHANGED = "changed-GT", REMOVED = "removed";

    private static final Pattern FORMATTING = Pattern.compile("§.");

    /** {@code /btsrecipes dump <file>}: a bare file name, nothing that can leave config/beyondthestars/. */
    private static final Pattern SAFE_DUMP_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    @Override
    public String getCommandName() {
        return "btsrecipes";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/btsrecipes [status | dump [file name, written to config/beyondthestars/] | circuits | nac <build|run <piko|SL-Nxx>|status> x y z [facing]"
            + " | chain <build|plan <family>|make <family>|gt <umv|uxv|max>|status|stop> x y z]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, "status", "dump", "circuits", "nac", "chain");
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final String sub = args.length > 0 ? args[0].toLowerCase() : "status";
        if ("status".equals(sub)) {
            status(sender, collect());
            return;
        }
        if ("circuits".equals(sub)) {
            // Circuits v2 checks (circuits-v2.md §7.2): C1 ore names; C2 chain, slot fills, lookups, research, rack;
            // C4 the Piko raise.
            for (String line : BTSCircuitChecks.run()) {
                sender.addChatMessage(new ChatComponentText(line));
                BeyondTheStars.LOG.info("[BTS] circuits: " + line);
            }
            return;
        }
        if ("nac".equals(sub)) {
            // Circuits v2 C5 bench: a real Nanochip Assembly Complex + Assembly Matrix in the world (NacBench).
            final com.nateyburr.beyondthestars.machines.SLConsoleTest.Placement at = com.nateyburr.beyondthestars.machines.SLConsoleTest
                .parsePlacement(sender, args);
            if (at == null) throw new WrongUsageException(com.nateyburr.beyondthestars.nac.NacBench.USAGE);
            for (String line : com.nateyburr.beyondthestars.nac.NacBench.run(sender.getEntityWorld(), at, args)) {
                sender.addChatMessage(new ChatComponentText(line));
                BeyondTheStars.LOG.info("[BTS] nac: " + line);
            }
            return;
        }
        if ("chain".equals(sub)) {
            // Circuits v2 C7 bench: the SL-X chain run end to end in real stock GT machines (CircuitChainBench).
            final com.nateyburr.beyondthestars.machines.SLConsoleTest.Placement at = com.nateyburr.beyondthestars.machines.SLConsoleTest
                .parsePlacement(sender, args);
            if (at == null) throw new WrongUsageException(CircuitChainBench.USAGE);
            for (String line : CircuitChainBench.run(sender.getEntityWorld(), at, args)) {
                sender.addChatMessage(new ChatComponentText(line));
                BeyondTheStars.LOG.info("[BTS] chain: " + line);
            }
            return;
        }
        if (!"dump".equals(sub) || args.length > 2) throw new WrongUsageException(getCommandUsage(sender));
        final String name = args.length > 1 ? args[1] : "bts-recipes.json";
        // A plain file name only, written into config/beyondthestars/: no absolute path, no folders, no "..".
        if (!SAFE_DUMP_NAME.matcher(name)
            .matches() || name.contains("..")) {
            BTSChat.error(
                sender,
                "Use a plain file name (letters, digits, - _ .), for example bts-recipes.json. "
                    + "It is written to config/beyondthestars/.");
            return;
        }
        final MinecraftServer server = MinecraftServer.getServer();
        final File folder = server == null ? new File("config", "beyondthestars")
            : server.getFile("config" + File.separator + "beyondthestars");
        final File file = new File(folder, name);
        final Map<String, Object> dump = collect();
        try {
            final File parent = file.getAbsoluteFile()
                .getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("cannot create " + parent);
            }
            final Gson gson = new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
            try (Writer out = new OutputStreamWriter(new java.io.FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(dump, out);
            }
        } catch (IOException | RuntimeException failed) {
            BTSChat.error(sender, "Could not write " + file.getAbsolutePath() + ": " + failed);
            return;
        }
        status(sender, dump);
        BTSChat.ok(sender, "Wrote " + file.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    private static void status(ICommandSender sender, Map<String, Object> dump) {
        final Map<String, Object> counts = (Map<String, Object>) dump.get("counts");
        BTSChat.header(sender, "== Beyond the Stars recipes (live) ==");
        BTSChat.send(sender, "Total " + counts.get("total") + " | by status " + counts.get("byStatus"));
        BTSChat.send(sender, "By line " + counts.get("byGroup"));
        final List<String> idle = (List<String>) dump.get("registeredWithoutLiveRecipe");
        if (!idle.isEmpty()) BTSChat.note(sender, "Registered but nothing live: " + idle);
        BTSChat.note(sender, "Config " + dump.get("config"));
    }

    // =========================================================================================== collect

    static Map<String, Object> collect() {
        final List<Map<String, Object>> recipes = new ArrayList<>();
        final Map<String, Integer> liveById = new LinkedHashMap<>();

        // Research data of our assembly lines, by output, for the visual NEI recipe that shows them.
        final List<GTRecipe.RecipeAssemblyLine> ownLines = new ArrayList<>();
        for (BTSRecipeLedger.Added a : BTSRecipeLedger.allAdded()) {
            if (a.assemblyLine != null && GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.contains(a.assemblyLine)) {
                ownLines.add(a.assemblyLine);
            }
        }
        final Map<GTRecipe.RecipeAssemblyLine, Boolean> lineShown = new IdentityHashMap<>();

        final Map<Object, BTSRecipeLedger.Changed> changedLive = new IdentityHashMap<>();
        final Map<String, RecipeMap<?>> maps = new TreeMap<>(RecipeMap.ALL_RECIPE_MAPS);
        for (Map.Entry<String, RecipeMap<?>> e : maps.entrySet()) {
            final RecipeMap<?> map = e.getValue();
            if (map == RecipeMaps.scannerFakeRecipes || map == TecTechRecipeMaps.researchStationFakeRecipes) continue;
            final Collection<GTRecipe> all;
            try {
                all = map.getAllRecipes();
            } catch (RuntimeException odd) {
                continue;
            }
            for (GTRecipe recipe : all) {
                if (recipe == null) continue;
                final BTSRecipeLedger.Added added = BTSRecipeLedger.added(recipe);
                if (added != null) {
                    final Map<String, Object> entry = base(added.id, group(added.id, added.line), NEW);
                    gt(entry, map, recipe);
                    if (map == RecipeMaps.assemblylineVisualRecipes) {
                        for (GTRecipe.RecipeAssemblyLine line : ownLines) {
                            if (sameOutput(line.mOutput, recipe.mOutputs)) {
                                entry.put("research", research(line));
                                lineShown.put(line, Boolean.TRUE);
                            }
                        }
                    }
                    marks(entry, added.id, added.flag, recipe);
                    recipes.add(entry);
                    liveById.merge(added.id, 1, Integer::sum);
                }
            }
            for (BTSRecipeLedger.Changed c : BTSRecipeLedger.allChanged()) {
                if (c.map == map && all.contains(c.recipe)) changedLive.put(c, c);
            }
        }
        for (BTSRecipeLedger.Changed c : BTSRecipeLedger.allChanged()) {
            final Map<String, Object> entry = base(c.id, c.group, CHANGED);
            gt(entry, c.map, c.recipe);
            entry.put("before", c.before);
            if (c.note != null) entry.put("note", c.note);
            if (!changedLive.containsKey(c)) entry.put("live", false);
            marks(entry, c.id, c.flag, c.recipe);
            recipes.add(entry);
        }

        // Assembly lines whose NEI page we did not see (should not happen; listed so nothing hides).
        for (GTRecipe.RecipeAssemblyLine line : ownLines) {
            if (lineShown.containsKey(line)) continue;
            final BTSRecipeLedger.Added added = BTSRecipeLedger.added(line);
            final Map<String, Object> entry = base(added.id, group(added.id, added.line), NEW);
            entry.put("machine", "Assembly Line (data only)");
            entry.put("map", "sAssemblylineRecipes");
            entry.put("inputs", items(line.mInputs, null));
            entry.put("fluidInputs", fluids(line.mFluidInputs, false));
            entry.put("outputs", items(new ItemStack[] { line.mOutput }, null));
            energy(entry, line.mEUt, line.mDuration);
            entry.put("research", research(line));
            marks(entry, added.id, added.flag, null);
            recipes.add(entry);
            liveById.merge(added.id, 1, Integer::sum);
        }

        // The crafting table.
        for (Object o : CraftingManager.getInstance()
            .getRecipeList()) {
            final BTSRecipeLedger.Added added = BTSRecipeLedger.added(o);
            if (added == null || !(o instanceof IRecipe)) continue;
            final Map<String, Object> entry = base(added.id, group(added.id, added.line), NEW);
            crafting(entry, (IRecipe) o);
            marks(entry, added.id, added.flag, null);
            recipes.add(entry);
            liveById.merge(added.id, 1, Integer::sum);
        }

        eye(recipes);

        for (BTSRecipeLedger.Removed r : BTSRecipeLedger.allRemoved()) {
            final Map<String, Object> entry = base(r.id, r.group, REMOVED);
            gt(entry, r.map, r.recipe);
            entry.put("replacement", r.replacement);
            if (r.note != null) entry.put("note", r.note);
            marks(entry, r.id, r.flag, null);
            recipes.add(entry);
        }

        // Several live recipes from one id (GT's emitters, loops): number them.
        final Map<String, Integer> seen = new LinkedHashMap<>();
        for (Map<String, Object> entry : recipes) {
            final String id = (String) entry.get("id");
            final Integer n = liveById.get(id);
            if (n != null && n > 1 && NEW.equals(entry.get("status"))) {
                final int k = seen.merge(id, 1, Integer::sum);
                entry.put("part", k + "/" + n);
            }
        }

        final List<String> idle = new ArrayList<>();
        final java.util.Set<String> changedIds = new java.util.HashSet<>();
        for (BTSRecipeLedger.Changed c : BTSRecipeLedger.allChanged()) changedIds.add(c.id);
        for (String id : BTSRecipeLedger.guardedIds()) {
            if (!liveById.containsKey(id) && !changedIds.contains(id) && !idle.contains(id)) idle.add(id);
        }

        final Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("generator", "/btsrecipes dump");
        dump.put("mod", Tags.VERSION);
        dump.put("time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put("replaceTecTechEyeOfHarmony", Config.replaceTecTechEyeOfHarmony);
        config.put("removeBlackHoleCompressorRecipe", Config.removeBlackHoleCompressorRecipe);
        config.put("stellarCircuits", Config.stellarCircuits);
        config.put("nacCircuitComponents", Config.nacCircuitComponents);
        config.put("raiseStockSpecialCircuits", Config.raiseStockSpecialCircuits);
        if (Config.raiseStockSpecialCircuits) {
            config.put("stockSpecialCircuitEnergyFactor", Config.stockSpecialCircuitEnergyFactor);
            config.put("stockSpecialCircuitMaterialFactor", Config.stockSpecialCircuitMaterialFactor);
        }
        dump.put("config", config);
        final Map<String, Object> guard = new LinkedHashMap<>();
        guard.put("registered", BTSRecipeGuard.registeredCount());
        guard.put("skipped", BTSRecipeGuard.skippedCount());
        guard.put("skips", BTSRecipeGuard.skippedRecipes());
        dump.put("guard", guard);
        dump.put("counts", counts(recipes));
        dump.put("registeredWithoutLiveRecipe", idle);
        dump.put("recipes", recipes);
        return dump;
    }

    private static Map<String, Object> counts(List<Map<String, Object>> recipes) {
        final Map<String, Integer> byStatus = new LinkedHashMap<>(), byGroup = new LinkedHashMap<>();
        for (String s : new String[] { NEW, CHANGED, REMOVED }) byStatus.put(s, 0);
        for (String g : new String[] { KERR, EYE, BEC, HR, QUASAR, P5, CIRCUITS, AUTOMATION, OLD_PLANT, UPGRADE,
            OTHER }) {
            byGroup.put(g, 0);
        }
        int display = 0, fallback = 0, placeholder = 0;
        for (Map<String, Object> r : recipes) {
            byStatus.merge((String) r.get("status"), 1, Integer::sum);
            byGroup.merge((String) r.get("group"), 1, Integer::sum);
            if (Boolean.TRUE.equals(r.get("displayOnly"))) display++;
            if (Boolean.TRUE.equals(r.get("fallback"))) fallback++;
            if (Boolean.TRUE.equals(r.get("placeholder"))) placeholder++;
        }
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", recipes.size());
        out.put("byStatus", byStatus);
        out.put("byGroup", byGroup);
        out.put("displayOnly", display);
        out.put("fallback", fallback);
        out.put("placeholder", placeholder);
        return out;
    }

    /**
     * The line a recipe belongs to: the {@code BTSRecipeGuard.line} it registered in, or (load-complete) its id.
     * SL-P02..P14 in the "singularity" line are the Kerr's structure parts, the rest of that line the Eye's.
     */
    static String group(String id, String line) {
        final String i = id == null ? "" : id;
        if (i.startsWith("SL-U")) return UPGRADE;
        if (line != null) {
            switch (line) {
                case "fuel":
                case "materials":
                case "propellant":
                case "coolant":
                case "machines":
                case "display":
                    return OLD_PLANT;
                case "kerr":
                    return KERR;
                case "eye":
                case "stellar":
                    return EYE;
                case "bec":
                    return BEC;
                case "automation":
                    return AUTOMATION;
                case "reality":
                case "reality-process":
                    return HR;
                case "quasar":
                    // P4: an SL-P id here (SL-P40 was the Resonance Chamber IV, now SL-G03) sits with the compression
                    // gate parts
                    return i.startsWith("SL-P") ? EYE : QUASAR;
                case "p5":
                    // P5 (BTSRecipesP5): the T9 parts SL-F, the MAX parts SL-M, the UXV leftovers SL-V
                    return P5;
                case "singularity":
                    return i.startsWith("SL-P") && number(i.substring(4)) <= 14 ? KERR : EYE;
                case "circuits":
                case "nac":
                    // the circuit chain SL-X and its NAC route SL-N (BTSRecipes lines "circuits" and "nac")
                    return CIRCUITS;
                default:
                    return OTHER;
            }
        }
        if (i.startsWith("SL-B") || i.startsWith("SL-P27") || i.startsWith("SL-P28")) return BEC;
        if (i.startsWith("SL-G1")) return CIRCUITS;
        // SL-G01..G03 (the chambers in place) and their load-complete fallbacks SL-P39 / SL-P33 / SL-P40
        if (i.startsWith("SL-G") || i.startsWith("SL-P39") || i.startsWith("SL-P33") || i.startsWith("SL-P40")) {
            return EYE;
        }
        if (i.startsWith("SL-Q")) return QUASAR;
        if (i.startsWith("SL-X") || i.startsWith("SL-N")) return CIRCUITS;
        return OTHER;
    }

    private static int number(String s) {
        int n = 0, i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) n = n * 10 + (s.charAt(i++) - '0');
        return i == 0 ? Integer.MAX_VALUE : n;
    }

    private static Map<String, Object> base(String id, String group, String status) {
        final Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("group", group);
        entry.put("status", status);
        return entry;
    }

    /** Flag, fallback and placeholder marks. */
    private static void marks(Map<String, Object> entry, String id, String flag, GTRecipe recipe) {
        entry.put("flag", flag);
        final String fallback = BTSRecipeLedger.fallbacks()
            .get(id);
        entry.put("fallback", fallback != null);
        if (fallback != null) entry.put("fallbackReason", fallback);
        final List<String> why = new ArrayList<>();
        for (Map.Entry<String, String> p : BTSRecipeLedger.placeholders()
            .entrySet()) {
            final String key = p.getKey();
            if (id.equals(key) || (id.startsWith(key) && !Character.isDigit(id.charAt(key.length())))) {
                why.add(p.getValue());
            }
        }
        if (recipe != null) {
            for (FluidStack fs : allFluidInputs(recipe)) {
                if (fs != null && SLRealityFluids.isPlaceholder(fs.getFluid()) && why.isEmpty()) {
                    why.add(fluidName(fs) + " has no maker yet (placeholder fluid)");
                }
            }
        }
        entry.put("placeholder", !why.isEmpty());
        if (!why.isEmpty()) entry.put("placeholderReason", String.join("; ", why));
    }

    // ============================================================================================ GT

    /** Machine, IO, energy and special values of a GT recipe. */
    private static void gt(Map<String, Object> entry, RecipeMap<?> map, GTRecipe recipe) {
        // An assembly line's NEI page is a fake recipe, but it stands for the real (data-stick) recipe.
        final boolean assemblyLine = map == RecipeMaps.assemblylineVisualRecipes;
        entry.put("machine", assemblyLine ? "Assembly Line" : machine(map));
        entry.put("map", map == null ? null : map.unlocalizedName);
        entry.put("displayOnly", recipe.mFakeRecipe && !assemblyLine);
        entry.putAll(snapshot(recipe));
        final Map<String, Object> special = special(map, recipe);
        if (!special.isEmpty()) entry.put("special", special);
    }

    /** The recipe's IO and energy as it is NOW; call before an in-place edit to keep the "before". */
    public static Map<String, Object> snapshot(GTRecipe recipe) {
        final Map<String, Object> out = new LinkedHashMap<>();
        try {
            NaniteTier[] nanites = null;
            try {
                nanites = recipe.getMetadata(GTRecipeConstants.NANITE_TIERS);
            } catch (RuntimeException | LinkageError ignored) {
                // a GT without nanite tiers
            }
            final ItemStack[][] alt = recipe instanceof GTRecipe.GTRecipe_WithAlt
                ? ((GTRecipe.GTRecipe_WithAlt) recipe).mOreDictAlt
                : null;
            final List<Map<String, Object>> in = items(recipe.mInputs, alt);
            if (nanites != null) {
                for (int i = 0, k = 0; recipe.mInputs != null && i < recipe.mInputs.length && k < in.size(); i++) {
                    if (recipe.mInputs[i] == null) continue;
                    if (i < nanites.length && nanites[i] != null) in.get(k)
                        .put("nanite", nanites[i].name());
                    k++;
                }
            }
            out.put("inputs", in);
            final List<Map<String, Object>> fluidsIn = fluids(recipe.mFluidInputs, false);
            fluidsIn.addAll(fluids(condensates(recipe), true));
            out.put("fluidInputs", fluidsIn);
            final List<Map<String, Object>> outs = items(recipe.mOutputs, null);
            if (recipe.mOutputChances != null) {
                for (int i = 0, k = 0; recipe.mOutputs != null && i < recipe.mOutputs.length && k < outs.size(); i++) {
                    if (recipe.mOutputs[i] == null) continue;
                    if (i < recipe.mOutputChances.length && recipe.mOutputChances[i] < 10_000) {
                        outs.get(k)
                            .put("chance", recipe.mOutputChances[i] / 100.0);
                    }
                    k++;
                }
            }
            out.put("outputs", outs);
            out.put("fluidOutputs", fluids(recipe.mFluidOutputs, false));
            energy(out, recipe.mEUt, recipe.mDuration);
        } catch (RuntimeException | LinkageError odd) {
            out.put("error", odd.toString());
        }
        return out;
    }

    private static FluidStack[] condensates(GTRecipe recipe) {
        try {
            return recipe.getMetadata(GTRecipeConstants.CONDENSATE_INPUT);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static List<FluidStack> allFluidInputs(GTRecipe recipe) {
        final List<FluidStack> out = new ArrayList<>();
        if (recipe.mFluidInputs != null) java.util.Collections.addAll(out, recipe.mFluidInputs);
        final FluidStack[] c = condensates(recipe);
        if (c != null) java.util.Collections.addAll(out, c);
        return out;
    }

    private static void energy(Map<String, Object> out, long eut, long ticks) {
        out.put("eut", eut);
        out.put("tier", tierName(eut));
        out.put("ticks", ticks);
        out.put("seconds", ticks / 20.0);
    }

    static String tierName(long eut) {
        if (eut <= 0) return "-";
        final int tier = Math.max(0, Math.min(GTValues.VN.length - 1, GTUtility.getTier(eut)));
        return GTValues.VN[tier];
    }

    /** Special value, special item and every metadata entry other than the nanites and condensates shown on IO. */
    private static Map<String, Object> special(RecipeMap<?> map, GTRecipe recipe) {
        final Map<String, Object> out = new LinkedHashMap<>();
        if (recipe.mSpecialValue != 0) {
            out.put(
                map == com.nateyburr.beyondthestars.machines.reality.RealityRecipeMaps.HOMOGENY ? "homogenyTier"
                    : "specialValue",
                recipe.mSpecialValue);
        }
        if (recipe.mSpecialItems instanceof ItemStack) out.put("specialItem", name((ItemStack) recipe.mSpecialItems));
        try {
            for (Map.Entry<RecipeMetadataKey<?>, Object> e : recipe.getMetadataStorage()
                .getEntries()) {
                final RecipeMetadataKey<?> key = e.getKey();
                if (key == GTRecipeConstants.NANITE_TIERS || key == GTRecipeConstants.CONDENSATE_INPUT) continue;
                out.put(keyName(key), value(e.getValue()));
            }
        } catch (RuntimeException | LinkageError ignored) {
            // no metadata API
        }
        return out;
    }

    private static Field identifier;

    private static String keyName(RecipeMetadataKey<?> key) {
        try {
            if (identifier == null) {
                identifier = RecipeMetadataKey.class.getDeclaredField("identifier");
                identifier.setAccessible(true);
            }
            return String.valueOf(identifier.get(key));
        } catch (ReflectiveOperationException | RuntimeException moved) {
            return key.toString();
        }
    }

    private static Object value(Object v) {
        if (v == null || v instanceof Number || v instanceof Boolean || v instanceof String) return v;
        if (v instanceof ItemStack) return name((ItemStack) v);
        if (v instanceof FluidStack) return fluidName((FluidStack) v) + " " + ((FluidStack) v).amount + " L";
        if (v instanceof Scanning) {
            final Scanning s = (Scanning) v;
            return "scan " + s.time / 20 + " s at " + tierName(s.voltage);
        }
        if (v instanceof Enum) return ((Enum<?>) v).name();
        if (v instanceof Object[]) {
            final List<Object> list = new ArrayList<>();
            for (Object o : (Object[]) v) list.add(value(o));
            return list;
        }
        if (v instanceof int[]) return java.util.Arrays.toString((int[]) v);
        return String.valueOf(v);
    }

    private static String machine(RecipeMap<?> map) {
        if (map == null) return null;
        final String key = map.unlocalizedName;
        final String local = translate(key);
        if (local != null) return local;
        // Last resort: "gt.recipe.assemble-condensate" -> "Assemble Condensate".
        String tail = key.substring(key.lastIndexOf('.') + 1)
            .replace('-', ' ')
            .replace('_', ' ');
        final StringBuilder b = new StringBuilder();
        for (String w : tail.split(" ")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0)))
                .append(w.substring(1));
        }
        return b.toString();
    }

    private static String translate(String key) {
        try {
            if (StatCollector.canTranslate(key)) return strip(StatCollector.translateToLocal(key));
        } catch (RuntimeException | LinkageError ignored) {
            // server without that lang entry
        }
        return null;
    }

    private static Map<String, Object> research(GTRecipe.RecipeAssemblyLine line) {
        final Map<String, Object> r = new LinkedHashMap<>();
        // A research Assembly Line (TecTech's Research Station, Circuits v2 §2.4): GT's list holds a placeholder
        // research item; the real one and the computation live in TecTech's twin of the recipe.
        final TecTechRecipeMaps.TTResearchStationALRecipe station = stationTwin(line);
        if (station != null) {
            r.put("machine", "Research Station");
            r.put("item", station.mResearchItem == null ? null : name(station.mResearchItem));
            r.put("computation", station.mComputation);
            r.put("computationPerSecond", station.mComputationRequiredPerSec);
            // TecTech keeps the research EU/t in the twin's mEUt (the assembly line's own EU/t is GT's recipe's).
            if (station.mEUt > 0) r.put("voltage", tierName(station.mEUt));
            r.put("amperage", station.mAmperage);
            return r;
        }
        r.put("item", line.mResearchItem == null ? null : name(line.mResearchItem));
        r.put("time", line.mResearchTime);
        if (line.mResearchVoltage > 0) r.put("voltage", tierName(line.mResearchVoltage));
        return r;
    }

    /** TecTech's Research Station twin of an assembly-line recipe (same persistent hash and output), or null. */
    static TecTechRecipeMaps.TTResearchStationALRecipe stationTwin(GTRecipe.RecipeAssemblyLine line) {
        if (line == null || line instanceof TecTechRecipeMaps.TTResearchStationALRecipe) return null;
        for (TecTechRecipeMaps.TTResearchStationALRecipe tt : TecTechRecipeMaps.researchableALRecipeList) {
            if (tt != null && tt.getPersistentHash() == line.getPersistentHash()
                && tt.mOutput != null
                && line.mOutput != null
                && tt.mOutput.isItemEqual(line.mOutput)) return tt;
        }
        return null;
    }

    private static boolean sameOutput(ItemStack out, ItemStack[] outputs) {
        if (out == null || outputs == null) return false;
        for (ItemStack o : outputs) if (o != null && o.isItemEqual(out)) return true;
        return false;
    }

    // ===================================================================================== crafting

    private static void crafting(Map<String, Object> entry, IRecipe recipe) {
        final List<Object> raw = new ArrayList<>();
        String kind = "Crafting Table";
        if (recipe instanceof ShapedRecipes) {
            java.util.Collections.addAll(raw, (Object[]) ((ShapedRecipes) recipe).recipeItems);
            kind += " (shaped)";
        } else if (recipe instanceof ShapelessRecipes) {
            raw.addAll(((ShapelessRecipes) recipe).recipeItems);
            kind += " (shapeless)";
        } else if (recipe instanceof ShapedOreRecipe) {
            java.util.Collections.addAll(raw, ((ShapedOreRecipe) recipe).getInput());
            kind += " (shaped)";
        } else if (recipe instanceof ShapelessOreRecipe) {
            raw.addAll(((ShapelessOreRecipe) recipe).getInput());
            kind += " (shapeless)";
        }
        // Merge equal inputs into counts.
        final Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Object o : raw) {
            ItemStack stack = null;
            if (o instanceof ItemStack) stack = (ItemStack) o;
            else if (o instanceof List && !((List<?>) o).isEmpty() && ((List<?>) o).get(0) instanceof ItemStack)
                stack = (ItemStack) ((List<?>) o).get(0);
            if (stack == null) continue;
            final Map<String, Object> item = item(stack);
            item.put("count", 1);
            final String k = item.get("id") + "|" + item.get("name");
            final Map<String, Object> have = merged.get(k);
            if (have == null) merged.put(k, item);
            else have.put("count", (Integer) have.get("count") + 1);
        }
        entry.put("machine", kind);
        entry.put("map", "crafting");
        entry.put("displayOnly", false);
        entry.put("inputs", new ArrayList<>(merged.values()));
        entry.put("fluidInputs", new ArrayList<>());
        entry.put("outputs", items(new ItemStack[] { recipe.getRecipeOutput() }, null));
        entry.put("fluidOutputs", new ArrayList<>());
        energy(entry, 0, 0);
    }

    // ========================================================================================== eye

    /** The Eye's own registry: the six live stars and the planets (converted from TecTech's Eye). */
    private static void eye(List<Map<String, Object>> recipes) {
        for (StarDef def : StarDef.all()) {
            final Map<String, Object> entry = base(def.key, EYE, NEW);
            entry.put("name", def.name);
            entry.put("machine", "Eye of Harmony (Beyond the Stars): star, per 60 s cycle");
            entry.put("map", "beyondthestars.eye.stars");
            entry.put("displayOnly", false);
            final List<Map<String, Object>> in = new ArrayList<>();
            final Map<String, Object> chart = item(def.chart);
            chart.put("count", 1);
            chart.put("notConsumed", true);
            in.add(chart);
            entry.put("inputs", in);
            entry.put("fluidInputs", amounts(def.upkeep));
            final List<Map<String, Object>> outFluids = new ArrayList<>();
            final Map<String, Object> product = fluid(def.product, (long) def.productBase());
            if (def.productBase() <= 0) product.put("note", "rate depends on the feed / batch, see special");
            outFluids.add(product);
            entry.put("outputs", new ArrayList<>());
            entry.put("fluidOutputs", outFluids);
            energy(entry, def.euPerCycle / StarDef.CYCLE_TICKS, StarDef.CYCLE_TICKS);
            final Map<String, Object> special = new LinkedHashMap<>();
            special.put("eyeTier", def.n());
            special.put("euPerCycle", def.euPerCycle);
            special.put("power", "wireless EU");
            if (def.feed != null)
                special.put("feed", fluidName(new FluidStack(def.feed, 1)) + " (rate set by the player)");
            if (!def.seed.isEmpty()) special.put("ignitionSeed", text(def.seed));
            if (!def.charge.isEmpty()) special.put("batchCharge", text(def.charge));
            entry.put("special", special);
            marks(entry, def.key, null, null);
            recipes.add(entry);
        }
        final boolean replaced = EyeRecipeRegistry.tecTechReplaced();
        final Map<String, EyeOfHarmonyRecipe> sources = BTSRecipeLedger.tecTechPlanets();
        for (EyeRecipe r : EyeRecipeRegistry.all()) {
            if (!r.isPlanet()) continue;
            final EyeOfHarmonyRecipe source = sources.get(r.key);
            final Map<String, Object> entry = base(r.key, EYE, replaced && source != null ? CHANGED : NEW);
            entry.put("name", r.name);
            entry.put("machine", "Eye of Harmony (Beyond the Stars): planet run");
            entry.put("map", "beyondthestars.eye.planets");
            entry.put("displayOnly", false);
            final List<Map<String, Object>> in = new ArrayList<>();
            final Map<String, Object> trigger = item(r.trigger);
            trigger.put("count", 1);
            trigger.put("notConsumed", true);
            in.add(trigger);
            entry.put("inputs", in);
            entry.put("fluidInputs", amounts(r.inputs));
            final List<Map<String, Object>> outs = new ArrayList<>();
            for (EyeRecipe.ItemAmount ia : r.itemOutputs) {
                final Map<String, Object> m = item(ia.stack);
                m.put("count", ia.amount);
                outs.add(m);
            }
            entry.put("outputs", outs);
            entry.put("fluidOutputs", amounts(r.fluidOutputs));
            energy(entry, r.baseTicks > 0 ? r.euPerRun / r.baseTicks : 0, r.baseTicks);
            final Map<String, Object> special = new LinkedHashMap<>();
            special.put("eyeTier", r.tier);
            special.put("euPerRun", r.euPerRun);
            special.put("power", "wireless EU");
            special.put("baseSuccess", r.baseSuccess);
            special.put("compressionBaseline", r.compressionRequired);
            entry.put("special", special);
            if (source != null) {
                entry.put("before", tecTech(source));
                entry.put(
                    "note",
                    replaced
                        ? "TecTech's Eye of Harmony recipe, replaced by this conversion (its NEI page and storage are cleared)"
                        : "converted copy; TecTech's own Eye of Harmony keeps its recipe (flag off)");
            }
            marks(entry, r.key, replaced && source != null ? "replaceTecTechEyeOfHarmony=true" : null, null);
            recipes.add(entry);
        }
    }

    private static Map<String, Object> tecTech(EyeOfHarmonyRecipe source) {
        final Map<String, Object> out = new LinkedHashMap<>();
        try {
            final List<Map<String, Object>> in = new ArrayList<>();
            final Map<String, Object> trigger = item(source.getRecipeTriggerItem());
            trigger.put("count", 1);
            trigger.put("notConsumed", true);
            in.add(trigger);
            out.put("inputs", in);
            final List<Map<String, Object>> fin = new ArrayList<>();
            fin.add(fluid(gas("hydrogen"), source.getHydrogenRequirement()));
            fin.add(fluid(gas("helium"), source.getHeliumRequirement()));
            out.put("fluidInputs", fin);
            final List<Map<String, Object>> items = new ArrayList<>();
            for (ItemStackLong s : source.getOutputItems()) {
                if (s == null || s.itemStack == null) continue;
                final Map<String, Object> m = item(s.itemStack);
                m.put("count", s.stackSize);
                items.add(m);
            }
            out.put("outputs", items);
            final List<Map<String, Object>> fl = new ArrayList<>();
            for (FluidStackLong s : source.getOutputFluids()) {
                if (s == null || s.fluidStack == null) continue;
                fl.add(fluid(s.fluidStack.getFluid(), s.amount));
            }
            out.put("fluidOutputs", fl);
            final long ticks = source.getRecipeTimeInTicks();
            energy(out, 0, ticks);
            out.put("euStartCost", source.getEUStartCost());
            out.put("euOutput", source.getEUOutput());
            out.put("baseSuccess", source.getBaseRecipeSuccessChance());
            out.put("spacetimeCasingTier", source.getSpacetimeCasingTierRequired());
            out.put("rocketTier", source.getRocketTier());
        } catch (RuntimeException | LinkageError odd) {
            out.put("error", odd.toString());
        }
        return out;
    }

    private static Fluid gas(String name) {
        return net.minecraftforge.fluids.FluidRegistry.getFluid(name);
    }

    private static List<Map<String, Object>> amounts(List<EyeRecipe.FluidAmount> list) {
        final List<Map<String, Object>> out = new ArrayList<>();
        for (EyeRecipe.FluidAmount fa : list) out.add(fluid(fa.fluid, fa.amount));
        return out;
    }

    private static String text(List<EyeRecipe.FluidAmount> list) {
        final StringBuilder b = new StringBuilder();
        for (EyeRecipe.FluidAmount fa : list) {
            if (b.length() > 0) b.append(", ");
            b.append(String.format("%,d", fa.amount))
                .append(" L ")
                .append(fa.fluid == null ? "?" : fluidName(new FluidStack(fa.fluid, 1)));
        }
        return b.toString();
    }

    // ============================================================================= items and fluids

    private static List<Map<String, Object>> items(ItemStack[] stacks, ItemStack[][] alt) {
        final List<Map<String, Object>> out = new ArrayList<>();
        if (stacks == null) return out;
        for (int i = 0; i < stacks.length; i++) {
            final ItemStack s = stacks[i];
            if (s == null || s.getItem() == null) continue;
            final Map<String, Object> m = item(s);
            m.put("count", s.stackSize);
            if (s.stackSize == 0) m.put("notConsumed", true);
            if (alt != null && i < alt.length && alt[i] != null && alt[i].length > 1) {
                final List<String> or = new ArrayList<>();
                for (ItemStack a : alt[i]) {
                    if (a == null || a.getItem() == null) continue;
                    final String n = name(a);
                    if (!n.equals(m.get("name")) && !or.contains(n)) or.add(n);
                    if (or.size() >= 6) break;
                }
                if (!or.isEmpty()) m.put("or", or);
            }
            out.add(m);
        }
        return out;
    }

    static Map<String, Object> item(ItemStack s) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", s == null ? "?" : name(s));
        m.put("id", s == null ? "?" : registry(s));
        return m;
    }

    private static String registry(ItemStack s) {
        final Item item = s.getItem();
        final Object name = item == null ? null : Item.itemRegistry.getNameForObject(item);
        final int meta = s.getItemDamage();
        return (name == null ? "?" : name) + ":" + (meta == GTValues.W ? "*" : String.valueOf(meta));
    }

    static String name(ItemStack s) {
        if (s == null || s.getItem() == null) return "?";
        try {
            final ItemStack one = s.copy();
            if (one.getItemDamage() == GTValues.W) one.setItemDamage(0);
            return strip(one.getDisplayName());
        } catch (RuntimeException | LinkageError unnamed) {
            return s.getUnlocalizedName();
        }
    }

    private static List<Map<String, Object>> fluids(FluidStack[] stacks, boolean condensate) {
        final List<Map<String, Object>> out = new ArrayList<>();
        if (stacks == null) return out;
        for (FluidStack fs : stacks) {
            if (fs == null || fs.getFluid() == null) continue;
            final Map<String, Object> m = fluid(fs.getFluid(), fs.amount);
            if (condensate) m.put("condensate", true);
            out.add(m);
        }
        return out;
    }

    private static Map<String, Object> fluid(Fluid fluid, long amount) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", fluid == null ? "?" : fluidName(new FluidStack(fluid, 1)));
        m.put("id", fluid == null ? "?" : fluid.getName());
        m.put("amount", amount);
        return m;
    }

    private static String fluidName(FluidStack fs) {
        try {
            return strip(fs.getLocalizedName());
        } catch (RuntimeException | LinkageError unnamed) {
            return fs.getFluid() == null ? "?"
                : fs.getFluid()
                    .getName();
        }
    }

    private static String strip(String s) {
        return s == null ? "?"
            : FORMATTING.matcher(s)
                .replaceAll("");
    }
}
