package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.DamageNexusIds;
import net.minecraft.resources.Identifier;

/** Stable serialized identifiers for built-in DamageNexus rule operations. */
public final class DamageNexusOperationIds {
    public static final Identifier ADD_BASE_DAMAGE = id("add_base_damage");
    public static final Identifier ADD_CHANNEL_PRE_MULTIPLIER = id("add_channel_pre_multiplier");
    public static final Identifier ADD_CHANNEL_POST_MULTIPLIER = id("add_channel_post_multiplier");
    public static final Identifier ADD_GLOBAL_PRE_MULTIPLIER = id("add_global_pre_multiplier");
    public static final Identifier ADD_GLOBAL_POST_MULTIPLIER = id("add_global_post_multiplier");
    public static final Identifier OVERRIDE_FINAL_DAMAGE = id("override_final_damage");
    public static final Identifier CANCEL_DAMAGE = id("cancel_damage");
    public static final Identifier ADD_TEMPORARY_RESISTANCE = id("add_temporary_resistance");
    public static final Identifier CONVERT_DAMAGE = id("convert_damage");
    public static final Identifier GAIN_EXTRA_DAMAGE = id("gain_extra_damage");
    public static final Identifier ADD_CHANNEL_MITIGATION = id("add_channel_mitigation");
    public static final Identifier ADD_GLOBAL_MITIGATION = id("add_global_mitigation");
    public static final Identifier MULTIPLY_ARMOR_EFFECTIVENESS = id("multiply_armor_effectiveness");
    public static final Identifier ADD_TRUE_DAMAGE = id("add_true_damage");

    private DamageNexusOperationIds() {}
    private static Identifier id(String path) { return DamageNexusIds.id(path); }
}
