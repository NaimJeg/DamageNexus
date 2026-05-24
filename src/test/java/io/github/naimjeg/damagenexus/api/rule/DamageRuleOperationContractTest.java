package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DamageRuleOperationContractTest {

    private static final Identifier RULE_ID =
            Identifier.fromNamespaceAndPath("test", "cancel_rule");

    @Test
    void cancelDamageDeclaresFinalOverrideOnly() {
        assertEquals(
                Set.of(DamagePhase.FINAL_OVERRIDE),
                DamageNexusOperations.cancelDamage().supportedPhases()
        );
    }

    @Test
    void builderRejectsCancelDamageOutsideFinalOverride() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> DamageRuleBuilder
                        .offensive(RULE_ID)
                        .cancelDamage()
                        .build()
        );

        assertTrue(error.getMessage().contains("cancel_damage"));
        assertTrue(error.getMessage().contains("BASE_MODIFICATION"));
        assertTrue(error.getMessage().contains("FINAL_OVERRIDE"));
    }

    @Test
    void builderAcceptsCancelDamageInFinalOverride() {
        DamageRuleDefinition rule = DamageRuleBuilder
                .offensive(RULE_ID)
                .phase(DamagePhase.FINAL_OVERRIDE)
                .cancelDamage()
                .build();

        assertEquals(DamagePhase.FINAL_OVERRIDE, rule.phase());
    }
}
