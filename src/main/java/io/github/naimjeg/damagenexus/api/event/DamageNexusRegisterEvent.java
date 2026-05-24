package io.github.naimjeg.damagenexus.api.event;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionResolver;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionProvider;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSource;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * Fired on the NeoForge event bus during DamageNexus common setup,
 * after built-in pre-multiplier buckets are registered and before
 * the pre-multiplier bucket registry is frozen.
 * <p>
 * Listen on the NeoForge/GAME bus, not the mod event bus.
 * Registration methods delegate to an expiring {@link DamageNexusRegistrar};
 * neither this event nor its registrar may be reused after event dispatch.
 */
public final class DamageNexusRegisterEvent extends Event {

    private final DamageNexusRegistrar registrar;

    public DamageNexusRegisterEvent(DamageNexusRegistrar registrar) {
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    public DamageNexusRegistrar registrar() {
        return registrar;
    }

    public void registerCondition(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        registrar.registerCondition(id, codec);
    }

    public void registerOperation(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        registrar.registerOperation(id, codec);
    }

    public int registerPreMultiplierBucket(Identifier id) {
        return registrar.registerPreMultiplierBucket(id);
    }

    public void registerRuleProvider(DamageRuleProvider provider) {
        registrar.registerRuleProvider(provider);
    }

    public void registerGlobalRule(DamageRuleDefinition rule) {
        registrar.registerGlobalRule(rule);
    }

    public void registerPhaseProcessor(DamagePhaseProcessor processor) {
        registrar.registerPhaseProcessor(processor);
    }

    public void registerAttributionResolver(
            Identifier id,
            int priority,
            DamageAttributionResolver resolver
    ) {
        registrar.registerAttributionResolver(id, priority, resolver);
    }

    public void registerEquippedItemRuleSource(
            Identifier id,
            int priority,
            EquippedItemRuleSource source
    ) {
        registrar.registerEquippedItemRuleSource(id, priority, source);
    }

    public void registerCriticalDecisionProvider(
            Identifier id,
            int priority,
            CriticalDecisionProvider provider
    ) {
        registrar.registerCriticalDecisionProvider(id, priority, provider);
    }

    /**
     * Registers an authority-bearing server settlement callback with priority
     * in the inclusive range {@code -10000..10000}.
     */
    public void registerSettlementListener(
            Identifier id,
            int priority,
            DamageSettlementListener listener
    ) {
        registrar.registerSettlementListener(id, priority, listener);
    }

    public void registerEntryTemplate(
            Identifier id,
            DamageEntryDefinition definition
    ) {
        registrar.registerEntryTemplate(id, definition);
    }

    public void registerAffixTemplate(
            Identifier id,
            DamageAffixDefinition definition
    ) {
        registrar.registerAffixTemplate(id, definition);
    }
}
