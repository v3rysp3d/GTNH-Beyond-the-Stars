package com.nateyburr.beyondthestars.items;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

import com.nateyburr.beyondthestars.BeyondTheStars;
import com.nateyburr.beyondthestars.Config;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/**
 * CIRCUITS v2 items (design repo {@code designs/docs/circuits-v2.md} §1, revision 3): the Stellar Boule and Wafer, the
 * five Stellar SMDs and the Sidereal (UMV), Empyrean (UXV) and Omega (MAX) circuit families, four members each, with
 * their wafers, chips and boards. A separate holder, so the other SL holders' fields stay as they are; called from
 * {@code CommonProxy.preInit} right after {@link SLQuasarItems#register()}, then {@link #registerOreDict()}.
 * <p>
 * Same rule as SLItems: one plain item per registry name, meta 0, the field names ARE the registry names (frozen once
 * shipped), unlocalized name {@code item.beyondthestars.<name>}, texture {@code beyondthestars:<name>}
 * (tools/gen-sl-textures.js), tooltip {@code item.beyondthestars.<name>.tooltip} (rule 14).
 * <p>
 * Items always register, so a world's item registry is the same whatever the config says. With
 * {@code stellarCircuits=false} they get no ore-dict entry, no recipe, a "disabled" tooltip and are hidden from NEI
 * ({@code NEIBTSConfig}).
 * <p>
 * Revision 3 (decided 2026-10-13: "Any of the tier such as the (any uxv) forming oress recipe"): the ONLY way our
 * circuits enter any recipe is GT's ore-dict any-of-tier name ({@link #registerOreDict()}); no card names them.
 */
public final class SLCircuitItems {

    // ---- shared Stellar items (plasma orange). Field names are registry names: frozen once shipped.
    /** Optically Enriched Boule regrown in Raw Star Matter (SL-X01, Eye T1). */
    public static Item stellarBoule;
    /** Cut from the boule (SL-X02/X03): base wafer of the three families and of the Stellar SMDs. */
    public static Item stellarWafer;
    /** SMD tier 4 (above GT's Optical SMDs), doped with ESPM (Eye T2 or the Homogeny). */
    public static Item stellarSmdTransistor;
    public static Item stellarSmdResistor;
    public static Item stellarSmdCapacitor;
    public static Item stellarSmdDiode;
    public static Item stellarSmdInductor;

    // ---- Sidereal (gold, Raw Star Matter, Eye T1)
    public static Item siderealWafer;
    public static Item siderealChip;
    public static Item siderealCircuitBoard;
    /** circuitInfinite (UHV). */
    public static Item siderealProcessor;
    /** circuitBio (UEV). */
    public static Item siderealAssembly;
    /** circuitOptical (UIV). */
    public static Item siderealSupercomputer;
    /** circuitExotic (UMV). */
    public static Item siderealMainframe;

    // ---- Empyrean (white-blue, White Dwarf Matter, Eye T4)
    public static Item empyreanWafer;
    public static Item empyreanChip;
    public static Item empyreanCircuitBoard;
    /** circuitBio (UEV). */
    public static Item empyreanProcessor;
    /** circuitOptical (UIV). */
    public static Item empyreanAssembly;
    /** circuitExotic (UMV). */
    public static Item empyreanSupercomputer;
    /** circuitCosmic (UXV). */
    public static Item empyreanMainframe;

    // ---- Omega (violet-black, Black Dwarf Matter, Eye T5)
    public static Item omegaWafer;
    public static Item omegaChip;
    public static Item omegaCircuitBoard;
    /** circuitOptical (UIV). */
    public static Item omegaProcessor;
    /** circuitExotic (UMV). */
    public static Item omegaAssembly;
    /** circuitCosmic (UXV). */
    public static Item omegaSupercomputer;
    /** circuitTranscendent (MAX). */
    public static Item omegaMainframe;

    /** The circuit tiers GT names its circuit ores by, lowest first (spec §1.3). */
    public static final String[] TIER_NAMES = { "UHV", "UEV", "UIV", "UMV", "UXV", "MAX" };

    /** Family order (spec §1.2): each starts one tier above the previous one. */
    public static final String[] FAMILIES = { "sidereal", "empyrean", "omega" };

    /** Member order inside a family. */
    public static final String[] MEMBERS = { "Processor", "Assembly", "Supercomputer", "Mainframe" };

    /** Every item registered here, in registration order (for NEI hiding and the check verb). */
    private static final List<Item> ALL = new ArrayList<>();

    /** True once {@link #registerOreDict()} put the twelve circuits into GT's ore names. */
    private static boolean oreDictDone;

    private SLCircuitItems() {}

    public static void register() {
        stellarBoule = define("stellarBoule");
        stellarWafer = define("stellarWafer");
        stellarSmdTransistor = define("stellarSmdTransistor");
        stellarSmdResistor = define("stellarSmdResistor");
        stellarSmdCapacitor = define("stellarSmdCapacitor");
        stellarSmdDiode = define("stellarSmdDiode");
        stellarSmdInductor = define("stellarSmdInductor");

        siderealWafer = define("siderealWafer");
        siderealChip = define("siderealChip");
        siderealCircuitBoard = define("siderealCircuitBoard");
        siderealProcessor = define("siderealProcessor");
        siderealAssembly = define("siderealAssembly");
        siderealSupercomputer = define("siderealSupercomputer");
        siderealMainframe = define("siderealMainframe");

        empyreanWafer = define("empyreanWafer");
        empyreanChip = define("empyreanChip");
        empyreanCircuitBoard = define("empyreanCircuitBoard");
        empyreanProcessor = define("empyreanProcessor");
        empyreanAssembly = define("empyreanAssembly");
        empyreanSupercomputer = define("empyreanSupercomputer");
        empyreanMainframe = define("empyreanMainframe");

        omegaWafer = define("omegaWafer");
        omegaChip = define("omegaChip");
        omegaCircuitBoard = define("omegaCircuitBoard");
        omegaProcessor = define("omegaProcessor");
        omegaAssembly = define("omegaAssembly");
        omegaSupercomputer = define("omegaSupercomputer");
        omegaMainframe = define("omegaMainframe");
    }

    /**
     * Spec §1.3: each circuit joins GT's ore name for its tier ({@code OrePrefixes.circuit} + {@code Materials.<tier>},
     * e.g. {@code circuitExotic} for UMV) and is blacklisted from unification, the coremod's Piko / Quantum / Planck
     * pattern. Runs in preInit, before GT and TecTech register their recipes in postInit, so every stock ore-dict
     * circuit slot (research assembly lines included, which snapshot ore members when they register) accepts ours.
     * Skipped with {@code stellarCircuits=false}.
     */
    public static void registerOreDict() {
        if (!Config.stellarCircuits) {
            BeyondTheStars.LOG.info("[BTS] Circuits v2: stellarCircuits=false, no ore-dict entries for our circuits.");
            return;
        }
        int registered = 0;
        for (int f = 0; f < FAMILIES.length; f++) {
            for (int m = 0; m < MEMBERS.length; m++) {
                final ItemStack stack = new ItemStack(circuit(f, m), 1, 0);
                GTOreDictUnificator.registerOre(OrePrefixes.circuit, tierMaterial(f + m), stack);
                GTOreDictUnificator.addToBlacklist(stack);
                registered++;
            }
        }
        oreDictDone = true;
        BeyondTheStars.LOG.info("[BTS] Circuits v2: " + registered + " circuits in GT's circuit ore names.");
    }

    /** The circuit item of family {@code f} (0 Sidereal, 1 Empyrean, 2 Omega) and member {@code m} (0-3). */
    public static Item circuit(int f, int m) {
        final Item[][] table = { { siderealProcessor, siderealAssembly, siderealSupercomputer, siderealMainframe },
            { empyreanProcessor, empyreanAssembly, empyreanSupercomputer, empyreanMainframe },
            { omegaProcessor, omegaAssembly, omegaSupercomputer, omegaMainframe } };
        return table[f][m];
    }

    /** The tier index (0 UHV ... 5 MAX) of family {@code f}, member {@code m}: the family's first tier plus m. */
    public static int tierIndex(int f, int m) {
        return f + m;
    }

    /** GT's tier material for a tier index (0 UHV ... 5 MAX); its name makes the ore name (Exotic, Cosmic, ...). */
    public static Materials tierMaterial(int tierIndex) {
        switch (tierIndex) {
            case 0:
                return Materials.UHV;
            case 1:
                return Materials.UEV;
            case 2:
                return Materials.UIV;
            case 3:
                return Materials.UMV;
            case 4:
                return Materials.UXV;
            default:
                return Materials.MAX;
        }
    }

    /** The ore name GT asks for at a tier index, e.g. {@code circuitExotic} for UMV. */
    public static String oreName(int tierIndex) {
        return String.valueOf(OrePrefixes.circuit.get(tierMaterial(tierIndex)));
    }

    /** Every item of this holder, in registration order. */
    public static List<Item> all() {
        return ALL;
    }

    /**
     * The C1 check (spec §7.2, "circuits" verb of {@code /btsrecipes}): one line per circuit with the ore names it has
     * and the one it should have, then a summary line starting {@code PASS} or {@code FAIL}. Read-only.
     */
    public static List<String> oreReport() {
        final List<String> lines = new ArrayList<>();
        if (!Config.stellarCircuits) {
            lines.add("stellarCircuits=false: no ore-dict entries expected.");
        }
        int bad = 0;
        for (int f = 0; f < FAMILIES.length; f++) {
            for (int m = 0; m < MEMBERS.length; m++) {
                final Item item = circuit(f, m);
                final ItemStack stack = new ItemStack(item, 1, 0);
                final List<String> names = new ArrayList<>();
                for (int id : OreDictionary.getOreIDs(stack)) names.add(OreDictionary.getOreName(id));
                final String want = oreName(tierIndex(f, m));
                final boolean ok = Config.stellarCircuits ? names.size() == 1 && names.contains(want) : names.isEmpty();
                if (!ok) bad++;
                lines.add(
                    (ok ? "ok   " : "BAD  ") + Item.itemRegistry.getNameForObject(item)
                        + " ["
                        + TIER_NAMES[tierIndex(f, m)]
                        + "] ores="
                        + names
                        + " expected="
                        + (Config.stellarCircuits ? "[" + want + "]" : "[]"));
            }
        }
        int others = 0;
        for (Item item : ALL) {
            if (isCircuit(item)) continue;
            if (OreDictionary.getOreIDs(new ItemStack(item, 1, 0)).length > 0) others++;
        }
        if (others > 0) {
            bad++;
            lines.add("BAD  " + others + " non-circuit item(s) carry an ore name (none should).");
        }
        lines.add(
            (bad == 0 ? "PASS" : "FAIL") + " circuits ore-dict: "
                + ALL.size()
                + " items registered, 12 circuits checked, "
                + bad
                + " problem(s); registerOreDict ran="
                + oreDictDone);
        return lines;
    }

    /** True for the twelve circuit items. */
    public static boolean isCircuit(Item item) {
        for (int f = 0; f < FAMILIES.length; f++) {
            for (int m = 0; m < MEMBERS.length; m++) if (circuit(f, m) == item) return true;
        }
        return false;
    }

    private static Item define(String name) {
        final Item item = new CircuitItem().setUnlocalizedName("beyondthestars." + name)
            .setTextureName("beyondthestars:" + name)
            .setCreativeTab(BTSItems.TAB);
        GameRegistry.registerItem(item, name);
        ALL.add(item);
        return item;
    }

    /** A lang-tooltip item that says "Disabled by config" instead when {@code stellarCircuits=false} (spec §6.2). */
    private static final class CircuitItem extends BTSTooltipItem {

        @Override
        @SideOnly(Side.CLIENT)
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
            if (!Config.stellarCircuits) {
                lines.add(
                    EnumChatFormatting.RED + "Disabled by config"
                        + EnumChatFormatting.GRAY
                        + " (stellarCircuits=false)");
                return;
            }
            super.addInformation(stack, player, lines, advanced);
        }
    }
}
