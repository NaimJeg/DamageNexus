package io.github.naimjeg.damagenexus.api.client.phrase;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * Fired on the NeoForge event bus during DamageNexus client initialization.
 * Registration ends when the event returns; the registry is then frozen.
 */
public final class RegisterRulePhrasesEvent extends Event {
    private final RulePhraseRegistry registry;

    public RegisterRulePhrasesEvent(RulePhraseRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void registerSchema(RulePhraseSchema schema) {
        registry.registerSchema(schema);
    }

    public <T extends DamageRuleCondition> void registerCondition(
            Identifier type,
            Class<T> valueClass,
            RulePhraseProvider<? super T> provider
    ) {
        registry.registerCondition(type, valueClass, provider);
    }

    public <T extends DamageRuleOperation> void registerOperation(
            Identifier type,
            Class<T> valueClass,
            RulePhraseProvider<? super T> provider
    ) {
        registry.registerOperation(type, valueClass, provider);
    }
}
