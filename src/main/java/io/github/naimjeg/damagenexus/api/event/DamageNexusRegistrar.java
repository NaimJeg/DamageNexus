package io.github.naimjeg.damagenexus.api.event;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionResolver;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionProvider;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSource;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import net.minecraft.resources.Identifier;

/**
 * Registration capability supplied only while
 * {@link DamageNexusRegisterEvent} is being dispatched.
 *
 * <p>Instances expire as soon as the event callback completes. Saving and
 * reusing one later is rejected even if the global lifecycle has not yet
 * finished freezing its internal registries.</p>
 */
public interface DamageNexusRegistrar {

    void registerCondition(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    );

    void registerOperation(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    );

    int registerPreMultiplierBucket(Identifier id);

    void registerRuleProvider(DamageRuleProvider provider);

    void registerGlobalRule(DamageRuleDefinition rule);

    void registerPhaseProcessor(DamagePhaseProcessor processor);

    /**
     * Registers a deterministic server-side attribution resolver. The ID is
     * retained verbatim and must use the registering mod's namespace.
     */
    void registerAttributionResolver(
            Identifier id,
            int priority,
            DamageAttributionResolver resolver
    );

    /**
     * Registers an external item-stack source without granting collector
     * access. The ID is retained verbatim and must use the registering mod's
     * namespace.
     */
    void registerEquippedItemRuleSource(
            Identifier id,
            int priority,
            EquippedItemRuleSource source
    );

    /**
     * Registers a deterministic server-side critical-decision provider.
     * Providers run once per transaction in descending priority order, then
     * by the full identifier. The callback-scoped collector is closed after
     * return and the registry freezes when this registration event ends.
     */
    void registerCriticalDecisionProvider(
            Identifier id,
            int priority,
            CriticalDecisionProvider provider
    );

    /**
     * Registers a server-side completed-damage callback. Callbacks are ordered
     * by descending priority, then by the full identifier, and receive child
     * authority only for their exact dynamic invocation. Priority must be in
     * the inclusive range {@code -10000..10000}.
     */
    void registerSettlementListener(
            Identifier id,
            int priority,
            DamageSettlementListener listener
    );

    /**
     * Registers a complete static entry template. The supplied ID must equal
     * {@link DamageEntryDefinition#id()} and retain the registering mod's
     * namespace.
     */
    void registerEntryTemplate(
            Identifier id,
            DamageEntryDefinition definition
    );

    /**
     * Registers a complete static affix template. The supplied ID must equal
     * {@link DamageAffixDefinition#id()} and retain the registering mod's
     * namespace.
     */
    void registerAffixTemplate(
            Identifier id,
            DamageAffixDefinition definition
    );
}
