package io.github.naimjeg.damagenexus.core.pipeline;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageSourcePolicyTest {

    @Test
    void vanillaThornsIdMatchesOnlyMinecraftThorns() {
        assertTrue(DamageSourcePolicy.isVanillaThornsId(
                Identifier.withDefaultNamespace("thorns")
        ));

        assertFalse(DamageSourcePolicy.isVanillaThornsId(
                Identifier.withDefaultNamespace("cactus")
        ));

        assertFalse(DamageSourcePolicy.isVanillaThornsId(
                Identifier.fromNamespaceAndPath("other_mod", "thorns")
        ));

        assertFalse(DamageSourcePolicy.isVanillaThornsId(null));
    }
}
