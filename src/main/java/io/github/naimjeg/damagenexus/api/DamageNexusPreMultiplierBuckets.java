package io.github.naimjeg.damagenexus.api;

import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Stable persistent identifiers for built-in pre-multiplier buckets. */
public final class DamageNexusPreMultiplierBuckets {
    public static final Identifier CRIT_DAMAGE = id("crit_damage");
    public static final Identifier PHYSICAL_DAMAGE = id("physical_damage");
    public static final Identifier FIRE_DAMAGE = id("fire_damage");
    public static final Identifier COLD_DAMAGE = id("cold_damage");
    public static final Identifier LIGHTNING_DAMAGE = id("lightning_damage");
    public static final Identifier MAGIC_DAMAGE = id("magic_damage");
    public static final Identifier POISON_DAMAGE = id("poison_damage");
    public static final Identifier WITHER_DAMAGE = id("wither_damage");
    public static final Identifier KINETIC_DAMAGE = id("kinetic_damage");
    public static final Identifier GENERIC_DAMAGE = id("generic_damage");
    public static final Identifier VANILLA_DIFFICULTY = id("vanilla_difficulty");
    public static final Identifier VANILLA_SPECIAL_ATTACK = id("vanilla_special_attack");
    public static final Identifier VANILLA_SPEAR_STAB = id("vanilla_spear_stab");
    public static final Identifier VANILLA_SPEAR_CHARGE = id("vanilla_spear_charge");
    public static final Identifier VANILLA_SPEAR_ATTACK = id("vanilla_spear_attack");
    public static final Identifier VANILLA_PLAYER_ATTACK = id("vanilla_player_attack");
    public static final Identifier VANILLA_PROJECTILE = id("vanilla_projectile");

    private DamageNexusPreMultiplierBuckets() {}

    /**
     * Resolves a persistent bucket identifier after registration has frozen.
     * The returned integer is transaction-runtime state and must never be serialized.
     *
     * @throws IllegalStateException if called before registry freeze
     * @throws IllegalArgumentException if {@code id} is unknown
     */
    public static int runtimeIndex(Identifier id) {
        Objects.requireNonNull(id, "id");
        PreMultiplierBucketRegistry.requireFrozen();
        return PreMultiplierBucketRegistry.getPreMultiplierBucketId(id);
    }

    private static Identifier id(String path) { return DamageNexusIds.id(path); }
}
