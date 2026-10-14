package com.nateyburr.beyondthestars.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * FIELD GENERATOR RETROFIT KITS (decided 2026-10-13, B-30: "Add an upgrade path"; question B-83). One item, three
 * kinds by metadata:
 * <ul>
 * <li>{@code 0} Field Generator Retrofit Frame: the blank a kit is built on (Assembler, SL-U00).</li>
 * <li>{@code 1..8} Spacetime Compression Retrofit Kit for tier t (TecTech tier index; 1 = Primitive .. 8 =
 * Gallifreyan): the parts a tier t-1 compression generator lacks for tier t (SL-U31..U38).</li>
 * <li>{@code 11..18} Time Dilation Retrofit Kit for tier t (SL-U41..U48).</li>
 * </ul>
 * A Stabilisation Field Generator holds a compression and a time generator, so its in-place upgrade (SL-U21..U28)
 * takes one kit of each instead of their 23 loose parts, which would not fit the Condensate Assembler's 16 item
 * slots. The compression and time generators themselves upgrade from loose parts (SL-U01..U08, SL-U11..U18). The
 * frame keeps a kit recipe from being a subset of GT's own generator recipe (the Condensate Assembler looks recipes
 * up by items only).
 * <p>
 * Registry name {@code fieldRetrofitKit}; metadata values are frozen once shipped. Display names and tooltips come
 * from the lang file ({@code item.beyondthestars.<kind>.name / .tooltip}, the kit names formatted with the tier name
 * {@code beyondthestars.eohTier.<t>}).
 */
public class ItemFieldRetrofitKit extends BTSTooltipItem {

    public static final int FRAME = 0;
    /** Compression kit for tier t is meta {@code COMPRESSION + t - 1}, t = 1..8. */
    public static final int COMPRESSION = 1;
    /** Time Dilation kit for tier t is meta {@code TIME + t - 1}, t = 1..8. */
    public static final int TIME = 11;
    public static final int TIERS = 8;

    private static final String[] KINDS = { "fieldRetrofitFrame", "compressionRetrofitKit", "timeRetrofitKit" };
    private final IIcon[] icons = new IIcon[KINDS.length];

    public ItemFieldRetrofitKit() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setUnlocalizedName("beyondthestars.fieldRetrofitKit");
        setCreativeTab(BTSItems.TAB);
    }

    /** The kit for {@code family} (COMPRESSION or TIME) at tier {@code t} (1..8). */
    public static ItemStack kit(Item item, int family, int t, int count) {
        if (t < 1 || t > TIERS) throw new IllegalArgumentException("retrofit kit tier " + t);
        return new ItemStack(item, count, family + t - 1);
    }

    /** 0 frame, 1 compression kit, 2 time kit, -1 not a valid meta. */
    static int kind(int meta) {
        if (meta == FRAME) return 0;
        if (meta >= COMPRESSION && meta < COMPRESSION + TIERS) return 1;
        if (meta >= TIME && meta < TIME + TIERS) return 2;
        return -1;
    }

    /** The kit's tier (1..8), 0 for the frame. */
    static int tier(int meta) {
        final int kind = kind(meta);
        if (kind == 1) return meta - COMPRESSION + 1;
        if (kind == 2) return meta - TIME + 1;
        return 0;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        final int kind = kind(stack.getItemDamage());
        return "item.beyondthestars." + KINDS[Math.max(0, kind)];
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        final int meta = stack.getItemDamage();
        final String key = getUnlocalizedName(stack) + ".name";
        final int tier = tier(meta);
        if (tier == 0) return StatCollector.translateToLocal(key);
        return StatCollector
            .translateToLocalFormatted(key, StatCollector.translateToLocal("beyondthestars.eohTier." + tier));
    }

    @Override
    public IIcon getIconFromDamage(int meta) {
        final IIcon icon = icons[Math.max(0, kind(meta))];
        return icon != null ? icon : itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        for (int i = 0; i < KINDS.length; i++) {
            icons[i] = register.registerIcon("beyondthestars:" + KINDS[i]);
        }
        itemIcon = icons[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, FRAME));
        for (int t = 1; t <= TIERS; t++) list.add(new ItemStack(item, 1, COMPRESSION + t - 1));
        for (int t = 1; t <= TIERS; t++) list.add(new ItemStack(item, 1, TIME + t - 1));
    }
}
