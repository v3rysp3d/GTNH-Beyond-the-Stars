package com.nateyburr.beyondthestars.machines.conduit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * P4: a conduit element's per-second books. Every stream that crosses an element is one record per key (its source,
 * jet port and throat, plus what the record is: in, passed, clipped, out), holding the value of the last second it
 * arrived. A record written again in the same tick adds (a stream that reached the element by two paths in one walk);
 * a record written in a later tick replaces it (the next second). Because every quasar sends exactly once every 20
 * ticks, summing the records younger than 20 ticks is "this second" and younger than {@link #SHOW} is "last second"
 * whatever the phase between quasars.
 */
public final class FlowBook {

    /** Display window: a record shows for two seconds' worth of ticks (the source's period plus jitter). */
    public static final long SHOW = 40;
    /** Throughput window: a record counts against a throat cap for one second. */
    public static final long SECOND = 20;

    private static final class Rec {

        long tick = Long.MIN_VALUE;
        double v;
    }

    private final Map<String, Rec> map = new HashMap<>();

    public void add(String key, double v, long now) {
        Rec r = map.get(key);
        if (r == null) {
            r = new Rec();
            map.put(key, r);
        }
        if (r.tick != now) {
            r.tick = now;
            r.v = 0;
        }
        r.v += v;
    }

    /** The sum of the records whose key starts with {@code prefix} and are younger than {@code window} ticks. */
    public double sum(String prefix, long now, long window) {
        double s = 0;
        for (Map.Entry<String, Rec> e : map.entrySet()) {
            if (now - e.getValue().tick < window && e.getKey()
                .startsWith(prefix)) s += e.getValue().v;
        }
        return s;
    }

    /** Drops records older than three seconds. */
    public void prune(long now) {
        for (Iterator<Rec> it = map.values()
            .iterator(); it.hasNext();) {
            if (now - it.next().tick > 60) it.remove();
        }
    }

    public void clear() {
        map.clear();
    }
}
