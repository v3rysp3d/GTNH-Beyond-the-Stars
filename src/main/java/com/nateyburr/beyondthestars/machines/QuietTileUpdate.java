package com.nateyburr.beyondthestars.machines;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.joml.Vector3f;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;

/**
 * Render QA round 3 (P-1): GT's {@link MTEEnhancedMultiBlockBase#onStructureCheckFinished} asks for a tile update
 * after EVERY structure check while the machine is formed, to send the structure's centre (its sound position) to the
 * clients. A controller that re-checks its structure on a timer (the Kerr Ergosphere Engine, every 10 s) therefore sent
 * a full description packet each time, and every client near it rebuilt the controller's chunk section (GT's
 * description reader marks the block five times per packet). This keeps one tile update per real change: the first
 * formed check (after a load, a placement or a re-form) and any check that moves the centre or the radius.
 * <p>
 * Use: in the controller's {@code onStructureCheckFinished}, call
 * {@code super.onStructureCheckFinished(quiet.wrap(igte))} and then {@code quiet.after(this, igte)}.
 */
public final class QuietTileUpdate {

    private boolean asked;
    private boolean synced;
    private float x, y, z;
    private int radius;

    /** The tile with {@code issueTileUpdate()} recorded instead of done; everything else goes straight through. */
    public IGregTechTileEntity wrap(IGregTechTileEntity igte) {
        asked = false;
        return (IGregTechTileEntity) Proxy.newProxyInstance(
            IGregTechTileEntity.class.getClassLoader(),
            new Class<?>[] { IGregTechTileEntity.class },
            (proxy, method, args) -> {
                if (method.getParameterCount() == 0 && "issueTileUpdate".equals(method.getName())) {
                    asked = true;
                    return null;
                }
                try {
                    return method.invoke(igte, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
    }

    /** After GT's check: does the tile update GT asked for only if the centre data the clients hold is stale. */
    public void after(MTEEnhancedMultiBlockBase<?> mte, IGregTechTileEntity igte) {
        if (!mte.mMachine) {
            synced = false;
            return;
        }
        if (!asked) return;
        final Vector3f c = mte.getCenter();
        final float cx = c == null ? 0 : c.x, cy = c == null ? 0 : c.y, cz = c == null ? 0 : c.z;
        final int r = mte.getApproximateRadius();
        if (synced && cx == x && cy == y && cz == z && r == radius) return;
        synced = true;
        x = cx;
        y = cy;
        z = cz;
        radius = r;
        igte.issueTileUpdate();
    }
}
