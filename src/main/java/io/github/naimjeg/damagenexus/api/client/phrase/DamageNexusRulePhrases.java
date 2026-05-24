package io.github.naimjeg.damagenexus.api.client.phrase;

import io.github.naimjeg.damagenexus.DamageNexus;

/** Stable built-in phrase types and slots reusable by client extensions. */
public final class DamageNexusRulePhrases {
    public static final RulePhraseType ADD_FLAT_DAMAGE = type("add_flat_damage");
    public static final RulePhraseType ADD_TRUE_DAMAGE = type("add_true_damage");
    public static final RulePhraseType CHANGE_CHANNEL_DAMAGE = type("change_channel_damage");
    public static final RulePhraseType CHANGE_GLOBAL_DAMAGE = type("change_global_damage");
    public static final RulePhraseType ADD_RESISTANCE = type("add_resistance");
    public static final RulePhraseType ADD_CHANNEL_MITIGATION = type("add_channel_mitigation");
    public static final RulePhraseType ADD_GLOBAL_MITIGATION = type("add_global_mitigation");
    public static final RulePhraseType MULTIPLY_ARMOR_EFFECTIVENESS = type("multiply_armor_effectiveness");
    public static final RulePhraseType CONVERT_DAMAGE = type("convert_damage");
    public static final RulePhraseType GAIN_EXTRA_DAMAGE = type("gain_extra_damage");
    public static final RulePhraseType OVERRIDE_FINAL_DAMAGE = type("override_final_damage");
    public static final RulePhraseType CANCEL_DAMAGE = type("cancel_damage");

    public static final RulePhraseType ALWAYS = type("always");
    public static final RulePhraseType ALL_OF = type("all_of");
    public static final RulePhraseType ANY_OF = type("any_of");
    public static final RulePhraseType NOT = type("not");
    public static final RulePhraseType HAS_EFFECT = type("has_effect");
    public static final RulePhraseType MATCHES_EFFECT_TAG = type("matches_effect_tag");
    public static final RulePhraseType HEALTH_THRESHOLD = type("health_threshold");
    public static final RulePhraseType ENTITY_TYPE_IS = type("entity_type_is");
    public static final RulePhraseType ENTITY_TYPE_TAG = type("entity_type_tag");
    public static final RulePhraseType MOB_CATEGORY_IS = type("mob_category_is");
    public static final RulePhraseType DAMAGE_CHANNEL_IS = type("damage_channel_is");
    public static final RulePhraseType DAMAGE_TYPE_IS = type("damage_type_is");
    public static final RulePhraseType DAMAGE_TYPE_TAG = type("damage_type_tag");
    public static final RulePhraseType REQUEST_KIND_IS = type("request_kind_is");
    public static final RulePhraseType SOURCE_ACTION_IS = type("source_action_is");
    public static final RulePhraseType SOURCE_TAG = type("source_tag");
    public static final RulePhraseType IS_BOSS = type("is_boss");
    public static final RulePhraseType IS_BURNING = type("is_burning");
    public static final RulePhraseType IS_CRITICAL = type("is_critical");
    public static final RulePhraseType HAS_PARENT_DAMAGE = type("has_parent_damage");
    public static final RulePhraseType PROC_ALLOWED = type("proc_allowed");
    public static final RulePhraseType UNKNOWN_CONDITION = type("unknown_condition");
    public static final RulePhraseType UNKNOWN_EFFECT = type("unknown_effect");

    public static final PhraseSlot<NumberValue> AMOUNT = PhraseSlot.required("amount", NumberValue.class);
    public static final PhraseSlot<PercentValue> PERCENT = PhraseSlot.required("percent", PercentValue.class);
    public static final PhraseSlot<ChannelValue> CHANNEL = PhraseSlot.required("channel", ChannelValue.class);
    public static final PhraseSlot<ChannelValue> FROM_CHANNEL = PhraseSlot.required("from_channel", ChannelValue.class);
    public static final PhraseSlot<ChannelValue> TO_CHANNEL = PhraseSlot.required("to_channel", ChannelValue.class);
    public static final PhraseSlot<EntityRoleValue> ENTITY_ROLE = PhraseSlot.required("entity_role", EntityRoleValue.class);
    public static final PhraseSlot<EffectValue> EFFECT = PhraseSlot.required("effect", EffectValue.class);
    public static final PhraseSlot<EntityTypeValue> ENTITY_TYPE = PhraseSlot.required("entity_type", EntityTypeValue.class);
    public static final PhraseSlot<TagValue> TAG = PhraseSlot.required("tag", TagValue.class);
    public static final PhraseSlot<RequestKindValue> REQUEST_KIND = PhraseSlot.required("request_kind", RequestKindValue.class);
    public static final PhraseSlot<IdentifierValue> IDENTIFIER = PhraseSlot.required("identifier", IdentifierValue.class);
    public static final PhraseSlot<MobCategoryValue> MOB_CATEGORY = PhraseSlot.required("mob_category", MobCategoryValue.class);

    private DamageNexusRulePhrases() {
    }

    private static RulePhraseType type(String path) {
        return RulePhraseType.of(DamageNexus.MODID, path);
    }
}
