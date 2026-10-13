package com.nateyburr.beyondthestars.client.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import com.nateyburr.beyondthestars.BeyondTheStars;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders v2 (§2.1): serves our GLSL to GT's {@code ShaderRecipe} with the shared includes expanded.
 * <p>
 * GT bakes a program from {@code <domain>:shaders/<name>.<vert|frag>[.330].glsl} through the resource manager, and
 * GLSL itself has no {@code #include}. Our programs therefore use the resource domain {@value #DOMAIN}, which this
 * pack answers: it reads {@code assets/beyondthestars/shaders/<path>} from the mod jar and
 * <ul>
 * <li>replaces every {@code #include "lib/x.glsl"} line by that file (each file once per program, recursively),
 * followed by a {@code #line} directive so a compile error still names the right line of the main file;</li>
 * <li>right after the {@code #version} line, defines the profile: {@code BTS_CORE} for a {@code .330.glsl} file (GT's
 * Angelica core profile) and {@code BTS_TEX} as {@code texture} (core) or {@code texture2D} (legacy {@code 120}), so
 * an include samples a texture the same way in both.</li>
 * </ul>
 * The pack is added to the live resource manager on first use ({@link #ensureRegistered()}); a full resource reload
 * drops it again, and GT's reload hook makes every program re-bake, which registers it again. A missing file throws
 * the usual {@link FileNotFoundException}, so GT logs the failed bake and the object draws its fixed-function path.
 */
@SideOnly(Side.CLIENT)
public final class GlslSourcePack implements IResourcePack {

    /** The resource domain GT's recipes for our programs name. */
    public static final String DOMAIN = "beyondthestars_glsl";

    private static final String ROOT = "/assets/beyondthestars/";
    private static final GlslSourcePack INSTANCE = new GlslSourcePack();
    private static final Set<String> DOMAINS = Collections.singleton(DOMAIN);

    private GlslSourcePack() {}

    /** Makes the live resource manager answer {@value #DOMAIN} (cheap when it already does). */
    @SuppressWarnings("unchecked")
    public static void ensureRegistered() {
        final IResourceManager manager = Minecraft.getMinecraft()
            .getResourceManager();
        if (manager.getResourceDomains()
            .contains(DOMAIN)) return;
        if (manager instanceof SimpleReloadableResourceManager) {
            ((SimpleReloadableResourceManager) manager).reloadResourcePack(INSTANCE);
        } else {
            BeyondTheStars.LOG.warn(
                "[BTS render] the resource manager is a {}: our shaders cannot be served",
                manager.getClass()
                    .getName());
        }
    }

    /** The expanded source of {@code path} (relative to the mod's assets root), or null if it does not exist. */
    public static String expanded(String path) {
        final String main = read(path);
        if (main == null) return null;
        final boolean core = path.endsWith(".330.glsl");
        final StringBuilder out = new StringBuilder(main.length() + 4096);
        final Set<String> seen = new HashSet<>();
        final String[] lines = main.split("\n", -1);
        boolean defined = false;
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            final String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            final String t = line.trim();
            if (!defined && t.startsWith("#version")) {
                out.append(line)
                    .append('\n');
                defines(out, core);
                out.append("#line ")
                    .append(lineNo + 1)
                    .append('\n');
                defined = true;
                continue;
            }
            if (!defined && !t.isEmpty() && !t.startsWith("//")) {
                // no #version line (a legacy 120 file may omit it): the defines go first
                defines(out, core);
                out.append("#line ")
                    .append(lineNo)
                    .append('\n');
                defined = true;
            }
            if (t.startsWith("#include")) {
                include(out, includePath(t, path), seen, 0);
                out.append("#line ")
                    .append(lineNo + 1)
                    .append('\n');
                continue;
            }
            out.append(line)
                .append('\n');
        }
        return out.toString();
    }

    private static void defines(StringBuilder out, boolean core) {
        if (core) {
            out.append("#define BTS_CORE 1\n#define BTS_TEX texture\n");
        } else {
            out.append("#define BTS_TEX texture2D\n");
        }
    }

    private static void include(StringBuilder out, String path, Set<String> seen, int depth) {
        if (depth > 8) throw new IllegalStateException("[BTS render] #include nested too deep at " + path);
        if (!seen.add(path)) return;
        final String text = read(path);
        if (text == null) throw new IllegalStateException("[BTS render] missing GLSL include " + path);
        for (String raw : text.split("\n", -1)) {
            final String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            final String t = line.trim();
            if (t.startsWith("#include")) {
                include(out, includePath(t, path), seen, depth + 1);
            } else {
                out.append(line)
                    .append('\n');
            }
        }
    }

    /** {@code #include "lib/noise.glsl"} -> {@code shaders/lib/noise.glsl} (always relative to shaders/). */
    private static String includePath(String directive, String from) {
        final int a = directive.indexOf('"');
        final int b = directive.lastIndexOf('"');
        if (a < 0 || b <= a) throw new IllegalStateException("[BTS render] bad #include in " + from + ": " + directive);
        return "shaders/" + directive.substring(a + 1, b);
    }

    private static String read(String path) {
        try (InputStream in = GlslSourcePack.class.getResourceAsStream(ROOT + path)) {
            if (in == null) return null;
            final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            final byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException bad) {
            return null;
        }
    }

    // ============================================================================ IResourcePack

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!DOMAIN.equals(location.getResourceDomain())) throw new FileNotFoundException(location.toString());
        final String text;
        try {
            text = expanded(location.getResourcePath());
        } catch (IllegalStateException bad) {
            throw new IOException(bad.getMessage(), bad);
        }
        if (text == null) throw new FileNotFoundException(location.toString());
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!DOMAIN.equals(location.getResourceDomain())) return false;
        return GlslSourcePack.class.getResource(ROOT + location.getResourcePath()) != null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set getResourceDomains() {
        return DOMAINS;
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return "Beyond the Stars GLSL";
    }
}
