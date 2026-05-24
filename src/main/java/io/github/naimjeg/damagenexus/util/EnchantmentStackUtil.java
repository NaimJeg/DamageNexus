package io.github.naimjeg.damagenexus.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantmentStackUtil {

    private EnchantmentStackUtil() {
    }

    public static void forEachEnchantment(
            ItemStack stack,
            StackEnchantmentConsumer action
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemEnchantments enchantments =
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        if (enchantments.isEmpty()) {
            return;
        }

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            int level = entry.getIntValue();

            if (level > 0) {
                action.accept(stack, entry.getKey(), level);
            }
        }
    }

    public static void forEachArmorEnchantment(
            LivingEntity entity,
            StackEnchantmentConsumer action
    ) {
        if (entity == null) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            forEachEnchantment(stack, action);
        }
    }

    public static void forEachWeaponEnchantment(
            LivingEntity entity,
            StackEnchantmentConsumer action
    ) {
        if (entity == null) {
            return;
        }

        ItemStack weapon = entity.getWeaponItem();
        forEachEnchantment(weapon, action);
    }

    @FunctionalInterface
    public interface StackEnchantmentConsumer {
        void accept(ItemStack stack, Holder<Enchantment> enchantment, int level);
    }
}
