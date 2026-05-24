package io.github.naimjeg.damagenexus.registry.rule;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.*;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Runtime codec registry for damage-rule conditions. */
public final class DamageRuleConditionTypes {
    private static final Map<Identifier, MapCodec<? extends DamageRuleCondition>> CODECS =
            new HashMap<>();

    static {
        registerBuiltin(DamageNexusConditionIds.ALWAYS, AlwaysCondition.CODEC);
        registerBuiltin(DamageNexusConditionIds.ALL_OF, AllOfCondition.CODEC);
        registerBuiltin(DamageNexusConditionIds.ANY_OF, AnyOfCondition.CODEC);
        registerBuiltin(DamageNexusConditionIds.NOT, NotCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.IS_CRITICAL,
                IsCriticalCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.TARGET_ON_FIRE,
                TargetOnFireCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.DAMAGE_TYPE_TAG,
                DamageTypeTagCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.DAMAGE_TYPE_IS,
                DamageTypeIsCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_HEALTH_BELOW,
                AttackerHealthBelowCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_HEALTH_ABOVE,
                AttackerHealthAboveCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.TARGET_HEALTH_BELOW,
                TargetHealthBelowCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.TARGET_HEALTH_ABOVE,
                TargetHealthAboveCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_HAS_EFFECT,
                AttackerHasEffectCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.TARGET_HAS_EFFECT,
                TargetHasEffectCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_EFFECT_TAG,
                AttackerEffectTagCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.TARGET_EFFECT_TAG,
                TargetEffectTagCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.SOURCE_ACTION_IS,
                SourceActionIsCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.SOURCE_TAG,
                SourceTagCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.REQUEST_KIND_IS,
                RequestKindIsCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.IS_PRIMARY_DAMAGE,
                IsPrimaryDamageCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.IS_PROC_DAMAGE,
                IsProcDamageCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.HAS_PARENT_DAMAGE,
                HasParentDamageCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.PROC_ALLOWED,
                ProcAllowedCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.DAMAGE_CHANNEL_IS,
                DamageChannelIsCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.TARGET_ENTITY_TYPE_IS,
                TargetEntityTypeIsCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_IS,
                AttackerEntityTypeIsCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.TARGET_MOB_CATEGORY_IS,
                TargetMobCategoryIsCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_MOB_CATEGORY_IS,
                AttackerMobCategoryIsCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.TARGET_IS_BOSS,
                TargetIsBossCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_IS_BOSS,
                AttackerIsBossCondition.CODEC);

        registerBuiltin(
                DamageNexusConditionIds.TARGET_ENTITY_TYPE_TAG,
                TargetEntityTypeTagCondition.CODEC);
        registerBuiltin(
                DamageNexusConditionIds.ATTACKER_ENTITY_TYPE_TAG,
                AttackerEntityTypeTagCondition.CODEC);
    }

    private DamageRuleConditionTypes() {
    }

    public static synchronized void register(
            DamageNexusRegistrationAccess access,
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "DamageRuleConditionTypes.register"
        );
        registerChecked(id, codec);
    }

    private static void registerBuiltin(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        registerChecked(id, codec);
    }

    private static void registerChecked(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");

        if (CODECS.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate DamageNexus rule condition type: " + id
            );
        }

        CODECS.put(id, codec);
    }

    public static synchronized MapCodec<? extends DamageRuleCondition> codec(Identifier id) {
        MapCodec<? extends DamageRuleCondition> codec = CODECS.get(id);

        if (codec == null) {
            throw new IllegalArgumentException(
                    "Unknown DamageNexus rule condition type: " + id
            );
        }

        return codec;
    }

    public static synchronized Set<Identifier> registeredTypes() {
        return Set.copyOf(CODECS.keySet());
    }
}
