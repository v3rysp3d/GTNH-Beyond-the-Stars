package com.nateyburr.beyondthestars.machines.reality;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.nateyburr.beyondthestars.machines.gui.BTSGuiKit;

/**
 * HOMOGENY + REALITY MACHINES: the gap-row region lamps the two Reality machines share. Their terminals are on the
 * controller GUI kit (controller-gui-spec.md §5.7); the lamps stay in the gap row as before, 12 px squares with the
 * region's numeral at 0.7 (it has to fit inside the lamp), the "Regions" caption and the tooltips from lang keys.
 */
public final class HRGui {

    private HRGui() {}

    public static final int LAMP_ON = BTSGuiKit.LAMP_ON;
    public static final int LAMP_OFF = BTSGuiKit.LAMP_FAULT;
    public static final int LAMP_IDLE = 0xFF4A4D55;

    private static final String[] NUMERALS = { "I", "II", "III", "IV", "V", "VI" };

    /** A square lamp in {@code colour.get()} with a dark rim. */
    public static IDrawable lamp(Supplier<Integer> colour) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, BTSGuiKit.EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, colour.get());
        };
    }

    /**
     * Appends "Regions" and the six lamps I..VI to a gap-row flow. {@code colour} gives region k's lamp colour;
     * {@code legendKey} names the lang key (under {@code beyondthestars.gui.reality.}) of the lamp tooltip's legend
     * lines ({@code .1}, {@code .2}, {@code .3}; a missing line is skipped).
     */
    public static Flow regionLamps(Flow row, IntUnaryOperator colour, String legendKey) {
        final String r = "beyondthestars.gui.reality.";
        row.childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                IKey.lang(r + "regions")
                    .color(BTSGuiKit.PANEL_TEXT)
                    .asWidget()
                    .marginRight(1)
                    .heightRel(1f));
        for (int k = 0; k < NUMERALS.length; k++) {
            final int region = k;
            row.child(
                IKey.str(NUMERALS[k])
                    .asWidget()
                    .size(12, 12)
                    .textAlign(Alignment.Center)
                    .scale(0.7f)
                    .background(lamp(() -> colour.applyAsInt(region)))
                    .tooltip(t -> {
                        t.addLine(IKey.lang(r + "lamp.title", StatCollector.translateToLocal(r + "region." + region)));
                        for (int i = 1; i <= 3; i++) {
                            final String key = r + legendKey + "." + i;
                            if (StatCollector.canTranslate(key)) {
                                t.addLine(EnumChatFormatting.GRAY + StatCollector.translateToLocal(key));
                            }
                        }
                    })
                    .tooltipShowUpTimer(TOOLTIP_DELAY));
        }
        return row;
    }
}
