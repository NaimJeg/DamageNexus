package io.github.naimjeg.damagenexus.registry.rule;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.builtin.rule.operation.*;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Runtime codec registry for damage-rule operations. */
public final class DamageRuleOperationTypes {
    private static final Map<Identifier, MapCodec<? extends DamageRuleOperation>> CODECS =
            new HashMap<>();

    static {
        registerBuiltin(
                DamageNexusOperationIds.ADD_BASE_DAMAGE,
                AddBaseDamageOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_CHANNEL_PRE_MULTIPLIER,
                AddChannelPreMultiplierOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_CHANNEL_POST_MULTIPLIER,
                AddChannelPostMultiplierOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_GLOBAL_POST_MULTIPLIER,
                AddGlobalPostMultiplierOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.OVERRIDE_FINAL_DAMAGE,
                OverrideFinalDamageOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.CANCEL_DAMAGE,
                CancelDamageOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_TEMPORARY_RESISTANCE,
                AddTemporaryResistanceOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_GLOBAL_PRE_MULTIPLIER,
                AddGlobalPreMultiplierOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.CONVERT_DAMAGE,
                ConvertDamageOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.GAIN_EXTRA_DAMAGE,
                GainExtraDamageOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_CHANNEL_MITIGATION,
                AddChannelMitigationOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_TRUE_DAMAGE,
                AddTrueDamageOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.ADD_GLOBAL_MITIGATION,
                AddGlobalMitigationOperation.CODEC);
        registerBuiltin(
                DamageNexusOperationIds.MULTIPLY_ARMOR_EFFECTIVENESS,
                MultiplyArmorEffectivenessOperation.CODEC);
    }

    private DamageRuleOperationTypes() {
    }

    public static synchronized void register(
            DamageNexusRegistrationAccess access,
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "DamageRuleOperationTypes.register"
        );
        registerChecked(id, codec);
    }

    private static void registerBuiltin(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        registerChecked(id, codec);
    }

    private static void registerChecked(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(codec, "codec");

        if (CODECS.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate DamageNexus rule operation type: " + id
            );
        }

        CODECS.put(id, codec);
    }

    public static synchronized MapCodec<? extends DamageRuleOperation> codec(Identifier id) {
        MapCodec<? extends DamageRuleOperation> codec = CODECS.get(id);

        if (codec == null) {
            throw new IllegalArgumentException(
                    "Unknown DamageNexus rule operation type: " + id
            );
        }

        return codec;
    }

    public static synchronized Set<Identifier> registeredTypes() {
        return Set.copyOf(CODECS.keySet());
    }
}
