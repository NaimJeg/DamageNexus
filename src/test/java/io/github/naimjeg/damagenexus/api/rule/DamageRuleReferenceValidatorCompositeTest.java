package io.github.naimjeg.damagenexus.api.rule;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageRuleReferenceValidatorCompositeTest {

    @Test
    void strictReferencesTraverseThirdPartyCompositeChildren() {
        Identifier unknown = Identifier.fromNamespaceAndPath(
                "examplemod", "missing_channel"
        );
        CompositeDamageRuleCondition wrapper =
                new CompositeDamageRuleCondition() {
                    @Override
                    public List<DamageRuleCondition> childConditions() {
                        return List.of(
                                DamageNexusConditions.damageChannelIs(unknown)
                        );
                    }

                    @Override
                    public Identifier type() {
                        return Identifier.fromNamespaceAndPath(
                                "examplemod", "wrapper"
                        );
                    }

                    @Override
                    public boolean test(DamageRuleContext ctx) {
                        return true;
                    }
                };
        DamageRuleDefinition rule = new DamageRuleDefinition(
                Identifier.fromNamespaceAndPath("examplemod", "nested_rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(wrapper),
                List.of(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID,
                        1.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );

        assertFalse(DamageRuleReferenceValidator.validateDatapackReferences(
                rule,
                "third_party_composite_test",
                DamageRuleValidator.Policy.WARN
        ));
    }

    @Test
    void invalidThirdPartyCompositeChildCallbacksFailClosed() {
        CompositeDamageRuleCondition nullChildren = wrapper(null);
        CompositeDamageRuleCondition nullChild = wrapper(
                java.util.Collections.singletonList(null));
        CompositeDamageRuleCondition throwing =
                new CompositeDamageRuleCondition() {
                    @Override
                    public List<DamageRuleCondition> childConditions() {
                        throw new IllegalStateException("synthetic");
                    }

                    @Override
                    public Identifier type() {
                        return Identifier.fromNamespaceAndPath(
                                "examplemod", "throwing_wrapper");
                    }

                    @Override
                    public boolean test(DamageRuleContext ctx) {
                        return true;
                    }
                };

        assertFalse(validate(rule(nullChildren)));
        assertFalse(validate(rule(nullChild)));
        assertFalse(validate(rule(throwing)));
    }

    @Test
    void nonCompositeThirdPartyConditionRemainsATrustedLeaf() {
        DamageRuleCondition leaf = new DamageRuleCondition() {
            @Override
            public Identifier type() {
                return Identifier.fromNamespaceAndPath(
                        "examplemod", "trusted_leaf");
            }

            @Override
            public boolean test(DamageRuleContext ctx) {
                return true;
            }
        };

        assertTrue(validate(rule(leaf)));
    }

    private static boolean validate(DamageRuleDefinition rule) {
        return DamageRuleReferenceValidator.validateDatapackReferences(
                rule,
                "third_party_composite_test",
                DamageRuleValidator.Policy.WARN
        );
    }

    private static DamageRuleDefinition rule(
            DamageRuleCondition condition
    ) {
        return new DamageRuleDefinition(
                Identifier.fromNamespaceAndPath("examplemod", "fixture_rule"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(condition),
                List.of(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID,
                        1.0f
                )),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty()
        );
    }

    private static CompositeDamageRuleCondition wrapper(
            List<DamageRuleCondition> children
    ) {
        return new CompositeDamageRuleCondition() {
            @Override
            public List<DamageRuleCondition> childConditions() {
                return children;
            }

            @Override
            public Identifier type() {
                return Identifier.fromNamespaceAndPath(
                        "examplemod", "wrapper_fixture");
            }

            @Override
            public boolean test(DamageRuleContext ctx) {
                return true;
            }
        };
    }
}
