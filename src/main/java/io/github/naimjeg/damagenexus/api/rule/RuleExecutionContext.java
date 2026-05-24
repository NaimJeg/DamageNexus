package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record RuleExecutionContext(
        DamageRuleProviderType providerType,
        RuleSourceLocation sourceLocation,
        DamageRuleRole role,
        @Nullable LivingEntity owner,
        ItemStack sourceStack,
        @Nullable EquipmentSlot equipmentSlot,
        @Nullable Entity sourceEntity,
        @Nullable Identifier externalProviderId,
        @Nullable Identifier externalSourceKey,
        @Nullable Identifier slotSemantic,
        @Nullable EquippedItemRuleSourceCategory externalCategory
) {
    public RuleExecutionContext {
        providerType = Objects.requireNonNull(
                providerType,
                "providerType"
        );
        sourceLocation = Objects.requireNonNull(
                sourceLocation,
                "sourceLocation"
        );
        role = Objects.requireNonNull(role, "role");
        sourceStack = sourceStack == null ? ItemStack.EMPTY : sourceStack;
        boolean external = providerType
                == DamageRuleProviderType.EXTERNAL_ITEM_SOURCE;
        if (external != (sourceLocation == RuleSourceLocation.EXTERNAL)
                || external != (externalProviderId != null)
                || external != (externalSourceKey != null)
                || external != (slotSemantic != null)
                || external != (externalCategory != null)) {
            throw new IllegalArgumentException(
                    "External item execution contexts require complete external source identity"
            );
        }
    }

    public static RuleExecutionContext itemEquipment(
            RuleSourceLocation sourceLocation,
            DamageRuleRole role,
            @Nullable LivingEntity owner,
            ItemStack stack,
            @Nullable EquipmentSlot slot
    ) {
        return builtIn(
                DamageRuleProviderType.ITEM_EQUIPMENT,
                sourceLocation,
                role,
                owner,
                stack == null ? ItemStack.EMPTY : stack,
                slot,
                owner
        );
    }

    public static RuleExecutionContext projectileSource(
            DamageRuleRole role,
            @Nullable LivingEntity owner,
            ItemStack stack,
            @Nullable Entity projectile
    ) {
        return builtIn(
                DamageRuleProviderType.PROJECTILE_SOURCE,
                RuleSourceLocation.PROJECTILE,
                role,
                owner,
                stack == null ? ItemStack.EMPTY : stack,
                null,
                projectile
        );
    }

    public static RuleExecutionContext entitySource(
            RuleSourceLocation sourceLocation,
            DamageRuleRole role,
            @Nullable LivingEntity owner,
            @Nullable Entity sourceEntity
    ) {
        return builtIn(
                DamageRuleProviderType.ENTITY,
                sourceLocation,
                role,
                owner,
                ItemStack.EMPTY,
                null,
                sourceEntity
        );
    }

    public static RuleExecutionContext vanillaEnchantment(
            DamageRuleRole role,
            @Nullable LivingEntity owner,
            ItemStack stack,
            @Nullable EquipmentSlot slot
    ) {
        return builtIn(
                DamageRuleProviderType.VANILLA_ENCHANTMENT,
                RuleSourceLocation.VANILLA,
                role,
                owner,
                stack == null ? ItemStack.EMPTY : stack,
                slot,
                owner
        );
    }

    public static RuleExecutionContext vanillaMobEffect(
            DamageRuleRole role,
            @Nullable LivingEntity owner
    ) {
        return builtIn(
                DamageRuleProviderType.VANILLA_MOB_EFFECT,
                RuleSourceLocation.VANILLA,
                role,
                owner,
                ItemStack.EMPTY,
                null,
                owner
        );
    }

    public static RuleExecutionContext datapackRule(DamageRuleRole role) {
        return builtIn(
                DamageRuleProviderType.DATAPACK_RULE,
                RuleSourceLocation.DATAPACK,
                role,
                null,
                ItemStack.EMPTY,
                null,
                null
        );
    }

    public static RuleExecutionContext javaApiRule(DamageRuleRole role) {
        return builtIn(
                DamageRuleProviderType.JAVA_API,
                RuleSourceLocation.JAVA_API,
                role,
                null,
                ItemStack.EMPTY,
                null,
                null
        );
    }

    public static RuleExecutionContext externalItemSource(
            DamageRuleRole role,
            @Nullable LivingEntity owner,
            ItemStack stack,
            Identifier providerId,
            Identifier sourceKey,
            Identifier slotSemantic,
            EquippedItemRuleSourceCategory category
    ) {
        return new RuleExecutionContext(
                DamageRuleProviderType.EXTERNAL_ITEM_SOURCE,
                RuleSourceLocation.EXTERNAL,
                role,
                owner,
                stack == null ? ItemStack.EMPTY : stack,
                null,
                owner,
                Objects.requireNonNull(providerId, "providerId"),
                Objects.requireNonNull(sourceKey, "sourceKey"),
                Objects.requireNonNull(slotSemantic, "slotSemantic"),
                Objects.requireNonNull(category, "category")
        );
    }

    private static RuleExecutionContext builtIn(
            DamageRuleProviderType providerType,
            RuleSourceLocation sourceLocation,
            DamageRuleRole role,
            @Nullable LivingEntity owner,
            ItemStack sourceStack,
            @Nullable EquipmentSlot equipmentSlot,
            @Nullable Entity sourceEntity
    ) {
        return new RuleExecutionContext(
                providerType,
                sourceLocation,
                role,
                owner,
                sourceStack,
                equipmentSlot,
                sourceEntity,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Returns whether the provider kind and source location describe the same
     * built-in source family.
     *
     * <p>Third-party providers may still construct execution contexts directly,
     * but contradictory combinations never satisfy built-in entry/affix slot
     * matching.</p>
     */
    public boolean hasConsistentBuiltInSource() {
        return switch (providerType) {
            case ITEM_EQUIPMENT -> isEquipmentLocation(sourceLocation);
            case PROJECTILE_SOURCE ->
                    sourceLocation == RuleSourceLocation.PROJECTILE;
            case EXTERNAL_ITEM_SOURCE ->
                    sourceLocation == RuleSourceLocation.EXTERNAL
                            && externalProviderId != null
                            && externalSourceKey != null
                            && slotSemantic != null
                            && externalCategory != null;
            case ENTITY -> isEntityLocation(sourceLocation);
            case DAMAGE_TYPE ->
                    sourceLocation == RuleSourceLocation.DAMAGE_TYPE;
            case DATAPACK_RULE ->
                    sourceLocation == RuleSourceLocation.DATAPACK;
            case JAVA_API ->
                    sourceLocation == RuleSourceLocation.JAVA_API;
            case VANILLA_ENCHANTMENT,
                 VANILLA_MOB_EFFECT ->
                    sourceLocation == RuleSourceLocation.VANILLA;
            case CUSTOM_MOD_EFFECT -> true;
        };
    }

    public boolean matches(DamageEntrySlot slot) {
        if (slot == null) {
            return false;
        }

        return matchesSupportedSlot(switch (slot) {
            case ITEM -> SupportedSlot.ITEM;
            case WEAPON -> SupportedSlot.WEAPON;
            case ARMOR -> SupportedSlot.ARMOR;
            case PROJECTILE -> SupportedSlot.PROJECTILE;
        });
    }

    public boolean matches(DamageAffixSlot slot) {
        if (slot == null) {
            return false;
        }

        return matchesSupportedSlot(switch (slot) {
            case ITEM -> SupportedSlot.ITEM;
            case WEAPON -> SupportedSlot.WEAPON;
            case ARMOR -> SupportedSlot.ARMOR;
            case PROJECTILE -> SupportedSlot.PROJECTILE;
        });
    }

    private boolean matchesSupportedSlot(SupportedSlot slot) {
        if (!hasConsistentBuiltInSource()) {
            return false;
        }

        return switch (slot) {
            case ITEM ->
                    providerType == DamageRuleProviderType.ITEM_EQUIPMENT
                            || providerType
                            == DamageRuleProviderType.PROJECTILE_SOURCE
                            || providerType
                            == DamageRuleProviderType.EXTERNAL_ITEM_SOURCE;
            case WEAPON ->
                    (providerType == DamageRuleProviderType.ITEM_EQUIPMENT
                            && isHandLocation(sourceLocation))
                            || (providerType
                            == DamageRuleProviderType.PROJECTILE_SOURCE
                            && sourceLocation
                            == RuleSourceLocation.PROJECTILE)
                            || (providerType
                            == DamageRuleProviderType.EXTERNAL_ITEM_SOURCE
                            && (externalCategory
                            == EquippedItemRuleSourceCategory.WEAPON
                            || externalCategory
                            == EquippedItemRuleSourceCategory.PROJECTILE));
            case ARMOR ->
                    providerType == DamageRuleProviderType.ITEM_EQUIPMENT
                            && isArmorLocation(sourceLocation)
                            || providerType
                            == DamageRuleProviderType.EXTERNAL_ITEM_SOURCE
                            && externalCategory
                            == EquippedItemRuleSourceCategory.ARMOR;
            case PROJECTILE ->
                    providerType
                            == DamageRuleProviderType.PROJECTILE_SOURCE
                            && sourceLocation
                            == RuleSourceLocation.PROJECTILE
                            || providerType
                            == DamageRuleProviderType.EXTERNAL_ITEM_SOURCE
                            && externalCategory
                            == EquippedItemRuleSourceCategory.PROJECTILE;
        };
    }

    private static boolean isEquipmentLocation(
            RuleSourceLocation location
    ) {
        return isHandLocation(location) || isArmorLocation(location);
    }

    private static boolean isHandLocation(RuleSourceLocation location) {
        return location == RuleSourceLocation.ATTACKER_MAINHAND
                || location == RuleSourceLocation.ATTACKER_OFFHAND
                || location == RuleSourceLocation.VICTIM_MAINHAND
                || location == RuleSourceLocation.VICTIM_OFFHAND;
    }

    private static boolean isArmorLocation(RuleSourceLocation location) {
        return location == RuleSourceLocation.ATTACKER_HEAD
                || location == RuleSourceLocation.ATTACKER_CHEST
                || location == RuleSourceLocation.ATTACKER_LEGS
                || location == RuleSourceLocation.ATTACKER_FEET
                || location == RuleSourceLocation.VICTIM_HEAD
                || location == RuleSourceLocation.VICTIM_CHEST
                || location == RuleSourceLocation.VICTIM_LEGS
                || location == RuleSourceLocation.VICTIM_FEET;
    }

    private static boolean isEntityLocation(RuleSourceLocation location) {
        return location == RuleSourceLocation.ATTACKER_ENTITY
                || location == RuleSourceLocation.VICTIM_ENTITY;
    }

    private enum SupportedSlot {
        ITEM,
        WEAPON,
        ARMOR,
        PROJECTILE
    }
}

