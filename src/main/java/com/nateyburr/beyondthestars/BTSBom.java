// GENERATED FILE - DO NOT EDIT.
//
// Rendered by questbook/gen-bom.ps1 out of the structure shape code of
// MTEReactorCoreMk1 and MTEModuleCoolingTower, and out of nothing else. The very
// same in-memory tally is what gen-questbook.ps1 interpolates into the five
// Blueprint quests, so the questbook and /btskit cannot quote different numbers.
//
// gen-bom.ps1 re-derives this file on every run and throws if the copy on disk
// disagrees; gen-questbook.ps1 dot-sources gen-bom.ps1, so the questbook cannot be
// rebuilt while this file is stale. Whitespace and comments are ignored by that
// check, so spotlessApply is free to reformat what is below.
//
// The reverse is sealed the same way: gen-bom.ps1 NEVER rewrites this file on its
// own. The one script run that rewrites it also runs gen-questbook.ps1 and rewrites
// questbook/DefaultQuests from the same tally, and rolls this file back if that
// throws - so the pair is either written together or left in the stale state every
// gate in that script refuses. It cannot come to rest split and silent.
//
// To change a count, change the geometry in the Java, then run ONE command:
// powershell -NoProfile -ExecutionPolicy Bypass -File questbook/gen-bom.ps1
// gradlew spotlessApply compileJava
package com.nateyburr.beyondthestars;

/**
 * Bills of materials for every piece the Reactor Core Mk1 projection raises, derived
 * cell by cell from the shape functions. Consumed by CommandBTSKit; mirrored count
 * for count by the Blueprint quests.
 */
public final class BTSBom {

    private BTSBom() {}

    // ------------------------------------------------------------- item names
    // Display names exactly as GregTech registers them (assets/gregtech/lang).
    // Name a material through these constants and nowhere else.
    public static final String CLEAN_STAINLESS_STEEL_MACHINE_CASING = "Clean Stainless Steel Machine Casing";
    public static final String DYNAMO_HATCH_IV = "Dynamo Hatch (IV)";
    public static final String ENERGY_HATCH_IV = "Energy Hatch (IV)";
    public static final String EUROPIUM_REINFORCED_RADIATION_PROOF_MACHINE_CASING = "Europium Reinforced Radiation Proof Machine Casing";
    public static final String FILTER_MACHINE_CASING = "Filter Machine Casing";
    public static final String INPUT_BUS_IV = "Input Bus (IV)";
    public static final String INPUT_HATCH_IV = "Input Hatch (IV)";
    public static final String IRIDIUM_REINFORCED_BLOCK = "Iridium Reinforced Block";
    public static final String LASER_SOURCE_HATCH = "Laser Source Hatch";
    public static final String LIGHT_CONCRETE = "Light Concrete";
    public static final String MAINTENANCE_HATCH = "Maintenance Hatch";
    public static final String OUTPUT_BUS_IV = "Output Bus (IV)";
    public static final String OUTPUT_HATCH_IV = "Output Hatch (IV)";
    public static final String PRECISION_INJECTOR = "Precision Injector";
    public static final String RADIANT_NAQUADAH_ALLOY_CASING = "Radiant Naquadah Alloy Casing";
    public static final String RADIATION_PROOF_MACHINE_CASING = "Radiation Proof Machine Casing";
    public static final String REACTOR_FUEL_PILLAR_FRAME = "Reactor Fuel Pillar Frame";
    public static final String ROD_LOADING_PORT = "Rod Loading Port";
    public static final String SILO_CONTROL_BLOCK = "Silo Control Block";
    public static final String SOLID_STEEL_MACHINE_CASING = "Solid Steel Machine Casing";
    public static final String SPIRAL_CYCLE_SENSOR = "Spiral Cycle Sensor";
    public static final String STEEL_FRAME_BOX = "Steel Frame Box";
    public static final String STEEL_PIPE_CASING = "Steel Pipe Casing";
    public static final String TUNGSTENSTEEL_REINFORCED_BLOCK = "Tungstensteel Reinforced Block";
    public static final String TUNGSTENSTEEL_TURBINE_CASING = "Tungstensteel Turbine Casing";
    public static final String TURBINE_CONTROL_BLOCK = "Turbine Control Block";
    public static final String TURBINE_HOUSING = "Turbine Housing";

    // ------------------------------------------------------------- dimensions
    public static final int MAX_TIER = 5;

    /** Cooling towers this controller raises (nw + sw). */
    public static final int TOWERS = 2;

    /** Silo octagon edge, indexed by tier; slot 0 is unused. */
    public static final int[] EDGE = { 0, 9, 11, 13, 15, 17 };

    /** Fuel pillars the silo interior holds, indexed by tier. */
    public static final int[] PILLARS = { 0, 45, 77, 109, 157, 201 };

    /** Spine duct length, indexed by tier. */
    public static final int[] DUCT_LENGTH = { 0, 12, 11, 10, 9, 8 };

    // ------------------------------------------------------------------ bills

    /** One piece: item names paired with the cell count the projection places. */
    public static final class Bill {

        private final String piece;
        private final String[] items;
        private final int[] counts;

        Bill(String piece, String[] items, int[] counts) {
            this.piece = piece;
            this.items = items;
            this.counts = counts;
        }

        public String piece() {
            return piece;
        }

        /** Entry count. Entries are ordered biggest tally first. */
        public int size() {
            return items.length;
        }

        public String item(int i) {
            return items[i];
        }

        public int count(int i) {
            return counts[i];
        }

        /** Tally of one item, 0 when this piece does not use it. */
        public int of(String item) {
            for (int i = 0; i < items.length; i++) {
                if (items[i].equals(item)) return counts[i];
            }
            return 0;
        }
    }

    public static final Bill PLATFORM = new Bill(
        "Starter platform",
        new String[] { LIGHT_CONCRETE, TUNGSTENSTEEL_REINFORCED_BLOCK },
        new int[] { 2251, 249 });

    public static final Bill SPIRAL = new Bill(
        "Spiral chamber",
        new String[] { TUNGSTENSTEEL_TURBINE_CASING, LIGHT_CONCRETE, PRECISION_INJECTOR, INPUT_HATCH_IV,
            MAINTENANCE_HATCH, SPIRAL_CYCLE_SENSOR },
        new int[] { 326, 50, 8, 2, 1, 1 });

    public static final Bill GALLERY = new Bill(
        "Rotor Assembly",
        new String[] { TUNGSTENSTEEL_TURBINE_CASING, TURBINE_HOUSING, DYNAMO_HATCH_IV, ENERGY_HATCH_IV,
            LASER_SOURCE_HATCH, MAINTENANCE_HATCH, TURBINE_CONTROL_BLOCK },
        new int[] { 408, 8, 4, 4, 4, 1, 1 });

    public static final Bill TOWER = new Bill(
        "Cooling tower (each)",
        new String[] { LIGHT_CONCRETE, SOLID_STEEL_MACHINE_CASING },
        new int[] { 736, 52 });

    public static final Bill VENT = new Bill(
        "Vent Stack",
        new String[] { CLEAN_STAINLESS_STEEL_MACHINE_CASING, FILTER_MACHINE_CASING },
        new int[] { 161, 152 });

    public static final Bill SWITCHYARD = new Bill(
        "Switchyard",
        new String[] { SOLID_STEEL_MACHINE_CASING, STEEL_FRAME_BOX },
        new int[] { 261, 90 });

    public static final Bill DERRICK = new Bill(
        "Extraction Derrick",
        new String[] { SOLID_STEEL_MACHINE_CASING, STEEL_FRAME_BOX, STEEL_PIPE_CASING, OUTPUT_BUS_IV },
        new int[] { 144, 80, 19, 1 });

    public static final Bill ANNEX = new Bill(
        "Enrichment Annex",
        new String[] { CLEAN_STAINLESS_STEEL_MACHINE_CASING, STEEL_PIPE_CASING, INPUT_BUS_IV, INPUT_HATCH_IV,
            OUTPUT_BUS_IV },
        new int[] { 291, 13, 1, 1, 1 });

    /** Indexed by tier; slot 0 is null. */
    public static final Bill[] SILO = { null,
        new Bill(
            "Silo tier 1",
            new String[] { RADIATION_PROOF_MACHINE_CASING, REACTOR_FUEL_PILLAR_FRAME, TUNGSTENSTEEL_REINFORCED_BLOCK,
                ROD_LOADING_PORT, INPUT_BUS_IV, INPUT_HATCH_IV, MAINTENANCE_HATCH, OUTPUT_BUS_IV, OUTPUT_HATCH_IV,
                SILO_CONTROL_BLOCK },
            new int[] { 520, 360, 113, 45, 1, 1, 1, 1, 1, 1 }),
        new Bill(
            "Silo tier 2",
            new String[] { RADIATION_PROOF_MACHINE_CASING, REACTOR_FUEL_PILLAR_FRAME, TUNGSTENSTEEL_REINFORCED_BLOCK,
                ROD_LOADING_PORT, INPUT_BUS_IV, INPUT_HATCH_IV, MAINTENANCE_HATCH, OUTPUT_BUS_IV, OUTPUT_HATCH_IV,
                SILO_CONTROL_BLOCK },
            new int[] { 812, 616, 193, 77, 1, 1, 1, 1, 1, 1 }),
        new Bill(
            "Silo tier 3",
            new String[] { EUROPIUM_REINFORCED_RADIATION_PROOF_MACHINE_CASING, REACTOR_FUEL_PILLAR_FRAME,
                IRIDIUM_REINFORCED_BLOCK, ROD_LOADING_PORT, RADIATION_PROOF_MACHINE_CASING, INPUT_BUS_IV,
                INPUT_HATCH_IV, MAINTENANCE_HATCH, OUTPUT_BUS_IV, OUTPUT_HATCH_IV, SILO_CONTROL_BLOCK },
            new int[] { 963, 872, 257, 109, 105, 1, 1, 1, 1, 1, 1 }),
        new Bill(
            "Silo tier 4",
            new String[] { EUROPIUM_REINFORCED_RADIATION_PROOF_MACHINE_CASING, REACTOR_FUEL_PILLAR_FRAME,
                IRIDIUM_REINFORCED_BLOCK, ROD_LOADING_PORT, RADIATION_PROOF_MACHINE_CASING, INPUT_BUS_IV,
                INPUT_HATCH_IV, MAINTENANCE_HATCH, OUTPUT_BUS_IV, OUTPUT_HATCH_IV, SILO_CONTROL_BLOCK },
            new int[] { 1343, 1256, 381, 157, 129, 1, 1, 1, 1, 1, 1 }),
        new Bill(
            "Silo tier 5",
            new String[] { RADIANT_NAQUADAH_ALLOY_CASING, REACTOR_FUEL_PILLAR_FRAME, IRIDIUM_REINFORCED_BLOCK,
                ROD_LOADING_PORT, RADIATION_PROOF_MACHINE_CASING, INPUT_BUS_IV, INPUT_HATCH_IV, MAINTENANCE_HATCH,
                OUTPUT_BUS_IV, OUTPUT_HATCH_IV, SILO_CONTROL_BLOCK },
            new int[] { 1667, 1608, 497, 201, 141, 1, 1, 1, 1, 1, 1 }) };

    /** Indexed by tier; slot 0 is null. */
    public static final Bill[] DUCT = { null,
        new Bill(
            "Spine duct tier 1",
            new String[] { TUNGSTENSTEEL_TURBINE_CASING, TURBINE_HOUSING },
            new int[] { 96, 4 }),
        new Bill(
            "Spine duct tier 2",
            new String[] { TUNGSTENSTEEL_TURBINE_CASING, TURBINE_HOUSING },
            new int[] { 88, 4 }),
        new Bill(
            "Spine duct tier 3",
            new String[] { TUNGSTENSTEEL_TURBINE_CASING, TURBINE_HOUSING },
            new int[] { 80, 4 }),
        new Bill(
            "Spine duct tier 4",
            new String[] { TUNGSTENSTEEL_TURBINE_CASING, TURBINE_HOUSING },
            new int[] { 72, 4 }),
        new Bill(
            "Spine duct tier 5",
            new String[] { TUNGSTENSTEEL_TURBINE_CASING, TURBINE_HOUSING },
            new int[] { 64, 4 }) };

    /** Platform + silo + duct + spiral + gallery + TOWERS towers, by tier. */
    public static final Bill[] TOTAL = { null,
        new Bill(
            "Whole plant tier 1",
            new String[] { LIGHT_CONCRETE, TUNGSTENSTEEL_TURBINE_CASING, RADIATION_PROOF_MACHINE_CASING,
                SOLID_STEEL_MACHINE_CASING, CLEAN_STAINLESS_STEEL_MACHINE_CASING, TUNGSTENSTEEL_REINFORCED_BLOCK,
                REACTOR_FUEL_PILLAR_FRAME, STEEL_FRAME_BOX, FILTER_MACHINE_CASING, ROD_LOADING_PORT, STEEL_PIPE_CASING,
                TURBINE_HOUSING, PRECISION_INJECTOR, DYNAMO_HATCH_IV, ENERGY_HATCH_IV, INPUT_HATCH_IV,
                LASER_SOURCE_HATCH, MAINTENANCE_HATCH, OUTPUT_BUS_IV, INPUT_BUS_IV, OUTPUT_HATCH_IV, SILO_CONTROL_BLOCK,
                SPIRAL_CYCLE_SENSOR, TURBINE_CONTROL_BLOCK },
            new int[] { 3773, 830, 520, 509, 452, 362, 360, 170, 152, 45, 32, 12, 8, 4, 4, 4, 4, 3, 3, 2, 1, 1, 1, 1 }),
        new Bill(
            "Whole plant tier 2",
            new String[] { LIGHT_CONCRETE, TUNGSTENSTEEL_TURBINE_CASING, RADIATION_PROOF_MACHINE_CASING,
                REACTOR_FUEL_PILLAR_FRAME, SOLID_STEEL_MACHINE_CASING, CLEAN_STAINLESS_STEEL_MACHINE_CASING,
                TUNGSTENSTEEL_REINFORCED_BLOCK, STEEL_FRAME_BOX, FILTER_MACHINE_CASING, ROD_LOADING_PORT,
                STEEL_PIPE_CASING, TURBINE_HOUSING, PRECISION_INJECTOR, DYNAMO_HATCH_IV, ENERGY_HATCH_IV,
                INPUT_HATCH_IV, LASER_SOURCE_HATCH, MAINTENANCE_HATCH, OUTPUT_BUS_IV, INPUT_BUS_IV, OUTPUT_HATCH_IV,
                SILO_CONTROL_BLOCK, SPIRAL_CYCLE_SENSOR, TURBINE_CONTROL_BLOCK },
            new int[] { 3773, 822, 812, 616, 509, 452, 442, 170, 152, 77, 32, 12, 8, 4, 4, 4, 4, 3, 3, 2, 1, 1, 1, 1 }),
        new Bill(
            "Whole plant tier 3",
            new String[] { LIGHT_CONCRETE, EUROPIUM_REINFORCED_RADIATION_PROOF_MACHINE_CASING,
                REACTOR_FUEL_PILLAR_FRAME, TUNGSTENSTEEL_TURBINE_CASING, SOLID_STEEL_MACHINE_CASING,
                CLEAN_STAINLESS_STEEL_MACHINE_CASING, IRIDIUM_REINFORCED_BLOCK, TUNGSTENSTEEL_REINFORCED_BLOCK,
                STEEL_FRAME_BOX, FILTER_MACHINE_CASING, ROD_LOADING_PORT, RADIATION_PROOF_MACHINE_CASING,
                STEEL_PIPE_CASING, TURBINE_HOUSING, PRECISION_INJECTOR, DYNAMO_HATCH_IV, ENERGY_HATCH_IV,
                INPUT_HATCH_IV, LASER_SOURCE_HATCH, MAINTENANCE_HATCH, OUTPUT_BUS_IV, INPUT_BUS_IV, OUTPUT_HATCH_IV,
                SILO_CONTROL_BLOCK, SPIRAL_CYCLE_SENSOR, TURBINE_CONTROL_BLOCK },
            new int[] { 3773, 963, 872, 814, 509, 452, 257, 249, 170, 152, 109, 105, 32, 12, 8, 4, 4, 4, 4, 3, 3, 2, 1,
                1, 1, 1 }),
        new Bill(
            "Whole plant tier 4",
            new String[] { LIGHT_CONCRETE, EUROPIUM_REINFORCED_RADIATION_PROOF_MACHINE_CASING,
                REACTOR_FUEL_PILLAR_FRAME, TUNGSTENSTEEL_TURBINE_CASING, SOLID_STEEL_MACHINE_CASING,
                CLEAN_STAINLESS_STEEL_MACHINE_CASING, IRIDIUM_REINFORCED_BLOCK, TUNGSTENSTEEL_REINFORCED_BLOCK,
                STEEL_FRAME_BOX, ROD_LOADING_PORT, FILTER_MACHINE_CASING, RADIATION_PROOF_MACHINE_CASING,
                STEEL_PIPE_CASING, TURBINE_HOUSING, PRECISION_INJECTOR, DYNAMO_HATCH_IV, ENERGY_HATCH_IV,
                INPUT_HATCH_IV, LASER_SOURCE_HATCH, MAINTENANCE_HATCH, OUTPUT_BUS_IV, INPUT_BUS_IV, OUTPUT_HATCH_IV,
                SILO_CONTROL_BLOCK, SPIRAL_CYCLE_SENSOR, TURBINE_CONTROL_BLOCK },
            new int[] { 3773, 1343, 1256, 806, 509, 452, 381, 249, 170, 157, 152, 129, 32, 12, 8, 4, 4, 4, 4, 3, 3, 2,
                1, 1, 1, 1 }),
        new Bill(
            "Whole plant tier 5",
            new String[] { LIGHT_CONCRETE, RADIANT_NAQUADAH_ALLOY_CASING, REACTOR_FUEL_PILLAR_FRAME,
                TUNGSTENSTEEL_TURBINE_CASING, SOLID_STEEL_MACHINE_CASING, IRIDIUM_REINFORCED_BLOCK,
                CLEAN_STAINLESS_STEEL_MACHINE_CASING, TUNGSTENSTEEL_REINFORCED_BLOCK, ROD_LOADING_PORT, STEEL_FRAME_BOX,
                FILTER_MACHINE_CASING, RADIATION_PROOF_MACHINE_CASING, STEEL_PIPE_CASING, TURBINE_HOUSING,
                PRECISION_INJECTOR, DYNAMO_HATCH_IV, ENERGY_HATCH_IV, INPUT_HATCH_IV, LASER_SOURCE_HATCH,
                MAINTENANCE_HATCH, OUTPUT_BUS_IV, INPUT_BUS_IV, OUTPUT_HATCH_IV, SILO_CONTROL_BLOCK,
                SPIRAL_CYCLE_SENSOR, TURBINE_CONTROL_BLOCK },
            new int[] { 3773, 1667, 1608, 798, 509, 497, 452, 249, 201, 170, 152, 141, 32, 12, 8, 4, 4, 4, 4, 3, 3, 2,
                1, 1, 1, 1 }) };
}
