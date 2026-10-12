package com.nateyburr.beyondthestars.machines.reality;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.StringSyncValue;

/**
 * HOMOGENY + REALITY MACHINES (H/R unit 2): the small MUI2 parts the three controllers' GUIs share (the Eye's look:
 * one coloured line per fact, a slim gradient progress bar, lamps drawn as filled squares).
 */
public final class HRGui {

    private HRGui() {}

    private static final int BAR_TRACK = 0xFF1E2026;
    private static final int BAR_EDGE = 0xFF4A4D55;
    private static final int PROGRESS_FROM = 0xFF2F6B8F;
    private static final int PROGRESS_TO = 0xFF7CD8FF;
    public static final int LAMP_ON = 0xFF2EBF4F;
    public static final int LAMP_OFF = 0xFFB0302A;
    public static final int LAMP_IDLE = 0xFF4A4D55;

    /** One terminal line, hidden while its text is empty. */
    public static IWidget row(StringSyncValue v) {
        return IKey.dynamic(v::getStringValue)
            .asWidget()
            .fullWidth()
            .marginBottom(2)
            .setEnabledIf(
                w -> !v.getStringValue()
                    .isEmpty());
    }

    /** One terminal line with a colour, hidden while {@code shown} is false. */
    public static IWidget row(Supplier<String> text, Supplier<Boolean> shown) {
        return IKey.dynamic(text::get)
            .asWidget()
            .fullWidth()
            .marginBottom(2)
            .setEnabledIf(w -> shown.get());
    }

    /** A percentage bar (0..1), hidden while the value is negative. */
    public static IWidget progress(DoubleSyncValue fraction) {
        return IKey.dynamic(() -> String.format("%.1f%%", Math.max(0, fraction.getDoubleValue()) * 100))
            .asWidget()
            .textAlign(Alignment.Center)
            .scale(0.8f)
            .widthRel(0.94f)
            .height(9)
            .background(bar(fraction))
            .marginBottom(3)
            .setEnabledIf(w -> fraction.getDoubleValue() >= 0);
    }

    private static IDrawable bar(DoubleSyncValue progress) {
        return (context, x, y, width, height, theme) -> {
            final double f = Math.max(0, Math.min(1, progress.getDoubleValue()));
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, BAR_TRACK);
            final float fill = (float) ((width - 2) * f);
            if (fill > 0) {
                GuiDraw.drawHorizontalGradientRect(x + 1, y + 1, fill, height - 2, PROGRESS_FROM, PROGRESS_TO);
            }
        };
    }

    /** A square lamp in {@code colour.get()} with a dark rim. */
    public static IDrawable lamp(Supplier<Integer> colour) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, BAR_EDGE);
            GuiDraw.drawRect(x + 1, y + 1, width - 2, height - 2, colour.get());
        };
    }

    /** The grey hint line style. */
    public static String grey(String s) {
        return EnumChatFormatting.GRAY + s;
    }

    public static final int TOOLTIP = TOOLTIP_DELAY;
}
