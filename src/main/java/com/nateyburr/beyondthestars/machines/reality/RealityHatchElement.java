package com.nateyburr.beyondthestars.machines.reality;

import java.util.Collections;
import java.util.List;

import net.minecraft.util.StatCollector;

import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.util.IGTHatchAdder;
import tectech.thing.metaTileEntity.hatch.bec.MTEHatchBEC;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): the Reality machines' private sockets.
 * <ul>
 * <li>{@link #Bec}: GT's BEC Hatch ({@link MTEHatchBEC}) on the base ring / foot ({@code b}); it joins our own list,
 * which is our neighbour set in the BEC grid ({@link MTERealityBase}).</li>
 * <li>{@link #AnchorI} .. {@link #AnchorVI}: Reality Diffraction's six Region Anchor sockets ({@code r s t u v w}),
 * any input hatch, ME stocking included. Each region has its own list, so the region identity is the socket, never
 * the fluid (trap: region identity comes from the socket). They never join GT's input list, so the medium's
 * processing never drinks an anchor.</li>
 * </ul>
 */
public final class RealityHatchElement {

    private RealityHatchElement() {}

    /** The BEC Hatch socket. */
    public static final IHatchElement<MTERealityBase<?>> Bec = new IHatchElement<MTERealityBase<?>>() {

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return Collections.singletonList(MTEHatchBEC.class);
        }

        @Override
        public IGTHatchAdder<? super MTERealityBase<?>> adder() {
            return (t, te, index) -> t.addBecHatch(te, index);
        }

        @Override
        public String name() {
            return "BEC Hatch";
        }

        /** B-249: GT's default key would be "hatch_type_bec hatch", which no lang file has. */
        @Override
        public String getDescriptionLangKey() {
            return "beyondthestars.hatch.reality.bec";
        }

        @Override
        public String getDisplayName() {
            return StatCollector.translateToLocal(getDescriptionLangKey());
        }

        @Override
        public long count(MTERealityBase<?> t) {
            return t.becHatchCount();
        }
    };

    /** Region Anchor I..VI: index = region - 1. */
    public static IHatchElement<MTERealityDiffraction> anchor(int region) {
        return ANCHORS[region];
    }

    private static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI" };

    /** B-249: the anchors' lang keys, written out so the build's lang check can read them. */
    private static final String[] ANCHOR_KEYS = { "beyondthestars.hatch.reality.anchor1",
        "beyondthestars.hatch.reality.anchor2", "beyondthestars.hatch.reality.anchor3",
        "beyondthestars.hatch.reality.anchor4", "beyondthestars.hatch.reality.anchor5",
        "beyondthestars.hatch.reality.anchor6" };

    @SuppressWarnings("unchecked")
    private static final IHatchElement<MTERealityDiffraction>[] ANCHORS = new IHatchElement[6];

    static {
        for (int i = 0; i < 6; i++) {
            final int region = i;
            ANCHORS[i] = new IHatchElement<MTERealityDiffraction>() {

                @Override
                public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
                    return Collections.singletonList(MTEHatchInput.class);
                }

                @Override
                public IGTHatchAdder<? super MTERealityDiffraction> adder() {
                    return (t, te, index) -> t.addAnchorHatch(region, te, index);
                }

                @Override
                public String name() {
                    return "Region Anchor " + ROMAN[region];
                }

                @Override
                public String getDescriptionLangKey() {
                    return ANCHOR_KEYS[region];
                }

                @Override
                public String getDisplayName() {
                    return StatCollector.translateToLocal(getDescriptionLangKey());
                }

                @Override
                public long count(MTERealityDiffraction t) {
                    return t.anchorHatchCount(region);
                }
            };
        }
    }
}
