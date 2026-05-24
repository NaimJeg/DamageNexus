package io.github.naimjeg.damagenexus.core.lifecycle;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.damagenexus.api.DamagePhaseProcessor;
import io.github.naimjeg.damagenexus.api.damage.DamageAttributionResolver;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecisionProvider;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegistrar;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleValidator;
import io.github.naimjeg.damagenexus.api.rule.provider.StaticDamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSource;
import io.github.naimjeg.damagenexus.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.damagenexus.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.damagenexus.core.rule.ExternalItemRuleSources;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import io.github.naimjeg.damagenexus.core.settlement.DamageSettlementCallbacks;
import io.github.naimjeg.damagenexus.api.event.DamageSettlementListener;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.registry.DamagePhaseProcessorRegistry;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleConditionTypes;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleOperationTypes;
import io.github.naimjeg.damagenexus.registry.rule.DamageRuleProviders;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class DamageNexusRegistrationSession
        implements DamageNexusRegistrar {

    private final DamageNexusRegistrationAccess access;
    private final AtomicBoolean open = new AtomicBoolean(true);

    DamageNexusRegistrationSession(DamageNexusRegistrationAccess access) {
        this.access = Objects.requireNonNull(access, "access");
        DamageNexusLifecycle.requireRegistering(
                access,
                "createRegistrationSession"
        );
    }

    @Override
    public void registerCondition(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        requireOpen("registerCondition");
        DamageRuleConditionTypes.register(access, id, codec);
    }

    @Override
    public void registerOperation(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        requireOpen("registerOperation");
        DamageRuleOperationTypes.register(access, id, codec);
    }

    @Override
    public int registerPreMultiplierBucket(Identifier id) {
        requireOpen("registerPreMultiplierBucket");
        return PreMultiplierBucketRegistry.registerPreMultiplierBucket(
                access,
                id
        );
    }

    @Override
    public void registerRuleProvider(DamageRuleProvider provider) {
        requireOpen("registerRuleProvider");
        DamageRuleProviders.register(access, provider);
    }

    @Override
    public void registerGlobalRule(DamageRuleDefinition rule) {
        requireOpen("registerGlobalRule");
        DamageRuleValidator.requireValid(
                rule,
                "java_api/register_global_rule"
        );
        DamageRuleProviders.register(
                access,
                new StaticDamageRuleProvider(rule)
        );
    }

    @Override
    public void registerPhaseProcessor(DamagePhaseProcessor processor) {
        requireOpen("registerPhaseProcessor");
        DamagePhaseProcessorRegistry.registerExternal(access, processor);
    }

    @Override
    public void registerAttributionResolver(
            Identifier id,
            int priority,
            DamageAttributionResolver resolver
    ) {
        requireOpen("registerAttributionResolver");
        DamageAttributionResolvers.register(access, id, priority, resolver);
    }

    @Override
    public void registerEquippedItemRuleSource(
            Identifier id,
            int priority,
            EquippedItemRuleSource source
    ) {
        requireOpen("registerEquippedItemRuleSource");
        ExternalItemRuleSources.register(access, id, priority, source);
    }

    @Override
    public void registerCriticalDecisionProvider(
            Identifier id,
            int priority,
            CriticalDecisionProvider provider
    ) {
        requireOpen("registerCriticalDecisionProvider");
        CriticalDecisionProviders.register(access, id, priority, provider);
    }

    @Override
    public void registerSettlementListener(
            Identifier id,
            int priority,
            DamageSettlementListener listener
    ) {
        requireOpen("registerSettlementListener");
        DamageSettlementCallbacks.register(access, id, priority, listener);
    }

    @Override
    public void registerEntryTemplate(
            Identifier id,
            DamageEntryDefinition definition
    ) {
        requireOpen("registerEntryTemplate");
        DamageTemplateRegistry.registerEntry(access, id, definition);
    }

    @Override
    public void registerAffixTemplate(
            Identifier id,
            DamageAffixDefinition definition
    ) {
        requireOpen("registerAffixTemplate");
        DamageTemplateRegistry.registerAffix(access, id, definition);
    }

    void close() {
        open.set(false);
    }

    boolean isOpen() {
        return open.get();
    }

    private void requireOpen(String action) {
        if (!open.get()) {
            throw new IllegalStateException(
                    "DamageNexus registration event has ended; saved "
                            + "registrars cannot be reused. action="
                            + action
            );
        }

        DamageNexusLifecycle.requireRegistering(access, action);
    }
}
