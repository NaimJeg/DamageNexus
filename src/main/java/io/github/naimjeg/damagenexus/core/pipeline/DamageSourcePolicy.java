package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.DamageNexusTags;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.jspecify.annotations.Nullable;

public final class DamageSourcePolicy {

    public static final Identifier VANILLA_THORNS_DAMAGE_TYPE =
            Identifier.withDefaultNamespace("thorns");

    private DamageSourcePolicy() {
    }

    public static boolean shouldManage(DamageSource source) {
        if (source == null) {
            return false;
        }

        /*
         * DamageNexus public origins require a stable registry identity.
         * Direct holders have no safe ID to persist or expose, so native
         * damage using one remains entirely on the vanilla/NeoForge path.
         */
        if (source.typeHolder().unwrapKey().isEmpty()) {
            return false;
        }

        if (source.is(DamageNexusTags.DamageTypes.BYPASSES_DAMAGENEXUS)) {
            return false;
        }

        /*
         * Keep hard-vanilla special damage outside DN by default.
         *
         * Examples usually include damage types intended to ignore ordinary
         * survival mechanics, such as out-of-world / kill-style damage.
         *
         * If you want DamageNexus to own even these sources, remove this check
         * and rely only on the custom BYPASSES_DAMAGENEXUS tag.
         */
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static boolean isVanillaThorns(DamageSource source) {
        if (source == null) {
            return false;
        }

        return source.is(DamageTypes.THORNS)
                || source.typeHolder()
                .unwrapKey()
                .map(key -> isVanillaThornsId(key.identifier()))
                .orElse(false);
    }

    public static boolean isVanillaThornsId(@Nullable Identifier id) {
        return VANILLA_THORNS_DAMAGE_TYPE.equals(id);
    }
}
