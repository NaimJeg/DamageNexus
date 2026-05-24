package io.github.naimjeg.damagenexus.externalapi;

import io.github.naimjeg.damagenexus.api.DamageNexusAttributes;
import io.github.naimjeg.damagenexus.api.DamageNexusPreMultiplierBuckets;
import io.github.naimjeg.damagenexus.api.critical.CriticalDecision;
import io.github.naimjeg.damagenexus.api.damage.DamageMetadataKey;
import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageSourceDescriptor;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegistrar;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditionIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusConditions;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperationIds;
import io.github.naimjeg.damagenexus.api.rule.DamageNexusOperations;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import io.github.naimjeg.damagenexus.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageAffixTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageNexusTemplates;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.damagenexus.api.item.DamageNexusItemApi;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Compilation-only fixture representing a third-party mod using API imports. */
public final class ExternalModApiFixture {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("contentmod", "example");

    private ExternalModApiFixture() {}

    public static int frozenRuntimeBucketIndex() {
        return DamageNexusPreMultiplierBuckets.runtimeIndex(
                DamageNexusPreMultiplierBuckets.GENERIC_DAMAGE);
    }

    /** Stores a payload-free ID; the server decides readiness and execution. */
    public static boolean attachTemplateReference(ItemStack stack) {
        DamageNexusTemplates.serverExecutionReady();
        return DamageNexusItemApi.addEntryTemplateReference(
                stack, new DamageEntryTemplateReference(ID));
    }

    public static DamageRequest exercise(
            DamageNexusRegistrar registrar,
            ServerLevel level,
            LivingEntity target,
            ResourceKey<DamageType> damageType
    ) {
        registrar.registerCondition(ID,
                DamageRuleCondition.CODEC.fieldOf("delegate"));
        registrar.registerOperation(ID,
                DamageRuleOperation.CODEC.fieldOf("delegate"));
        registrar.registerPreMultiplierBucket(ID);
        registrar.registerAttributionResolver(ID, 0, query -> Optional.empty());
        registrar.registerEquippedItemRuleSource(ID, 0, query -> List.of());
        registrar.registerCriticalDecisionProvider(ID, 0,
                (context, collector) -> collector.contribute(
                        CriticalDecision.FORCE_CRITICAL));
        registrar.registerSettlementListener(ID, 0, callback ->
                callback.childAuthority());

        var templateRule = DamageRuleBuilder.offensive(ID)
                .operation(DamageNexusOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID, 1.0f))
                .build();
        var entryTemplate = new DamageEntryDefinition(
                ID, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(templateRule), DamageEntryStacking.STACK,
                Optional.empty());
        var affixId = Identifier.fromNamespaceAndPath(
                "contentmod", "example_affix");
        var affixTemplate = new DamageAffixDefinition(
                affixId, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON, List.of(entryTemplate),
                DamageAffixStacking.STACK, Optional.empty());
        registrar.registerEntryTemplate(ID, entryTemplate);
        registrar.registerAffixTemplate(affixId, affixTemplate);
        new DamageItemTemplateReferences(
                List.of(new DamageEntryTemplateReference(ID)),
                List.of(new DamageAffixTemplateReference(affixId)));
        DamageNexusTemplates.entry(ID);
        DamageNexusTemplates.affix(affixId);

        DamageRuleBuilder.offensive(ID)
                .when(DamageNexusConditions.sourceTag(ID))
                .operation(DamageNexusOperations.addGlobalPreMultiplier(ID, 0.25f))
                .build();
        DamageNexusConditionIds.ALWAYS.toString();
        DamageNexusOperationIds.ADD_BASE_DAMAGE.toString();
        DamageNexusPreMultiplierBuckets.GENERIC_DAMAGE.toString();
        DamageNexusAttributes.CRIT_CHANCE.identifier();
        DamageChannel.FIRE_ID.toString();
        DamageApplicationBucket.DN_RULE_BASE.serializedName();

        return DamageRequest.builder(level, target,
                        DamageSourceDescriptor.of(damageType), 1.0f)
                .actionId(ID)
                .sourceTag(ID)
                .metadata(DamageMetadataKey.stringKey(ID), "value")
                .build();
    }
}
