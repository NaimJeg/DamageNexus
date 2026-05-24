package io.github.naimjeg.damagenexus.builtin.rule.provider;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.*;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.rule.StackDamageEntryCollector;
import io.github.naimjeg.damagenexus.core.rule.ExternalItemRuleSnapshot;
import io.github.naimjeg.damagenexus.api.rule.source.EquippedItemRuleSourceDirection;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public final class ItemDamageRuleProvider implements DamageRuleProvider {

    @Override
    public void collect(
            DamageRuleContext context,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "item rule provider"
        );

        collectAttackerEquipmentRules(ctx, phase, out);
        collectVictimEquipmentRules(ctx, phase, out);
        collectExternalItemRules(ctx, phase, out);
    }

    private void collectAttackerEquipmentRules(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        LivingEntity equipmentOwner = ctx.equipmentOwner();
        if (!isValidEquipmentOwner(ctx, equipmentOwner)) {
            return;
        }

        VanillaDamageSourceProfile profile = ctx.vanillaSourceProfile();

        /*
         * Projectile attacks use the captured projectile source provider for
         * the weapon. Do not also collect the shooter's current hands, or
         * bow/crossbow rules could run twice or use hit-time item state.
         * Attacker armor remains a legitimate offensive item source.
         */
        if (profile == null || !profile.projectile()) {
            collectEquipmentSlot(
                    ctx,
                    phase,
                    out,
                    equipmentOwner,
                    EquipmentSlot.MAINHAND,
                    RuleSourceLocation.ATTACKER_MAINHAND,
                    DamageRuleRole.OFFENSIVE
            );

            collectEquipmentSlot(
                    ctx,
                    phase,
                    out,
                    equipmentOwner,
                    EquipmentSlot.OFFHAND,
                    RuleSourceLocation.ATTACKER_OFFHAND,
                    DamageRuleRole.OFFENSIVE
            );
        }

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.HEAD,
                RuleSourceLocation.ATTACKER_HEAD,
                DamageRuleRole.OFFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.CHEST,
                RuleSourceLocation.ATTACKER_CHEST,
                DamageRuleRole.OFFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.LEGS,
                RuleSourceLocation.ATTACKER_LEGS,
                DamageRuleRole.OFFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                equipmentOwner,
                EquipmentSlot.FEET,
                RuleSourceLocation.ATTACKER_FEET,
                DamageRuleRole.OFFENSIVE
        );
    }

    private void collectExternalItemRules(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        for (ExternalItemRuleSnapshot source : ctx.externalItemRuleSources()) {
            if (source.direction()
                    == EquippedItemRuleSourceDirection.OFFENSIVE
                    && !isValidEquipmentOwner(ctx, source.owner())) {
                continue;
            }
            DamageRuleRole role = source.direction()
                    == EquippedItemRuleSourceDirection.OFFENSIVE
                    ? DamageRuleRole.OFFENSIVE
                    : DamageRuleRole.DEFENSIVE;
            RuleExecutionContext execution =
                    RuleExecutionContext.externalItemSource(
                            role,
                            source.owner(),
                            source.stack(),
                            source.providerId(),
                            source.sourceKey(),
                            source.slotSemantic(),
                            source.category()
                    );
            StackDamageEntryCollector.collectStackEntries(
                    ctx,
                    phase,
                    out,
                    source.stack(),
                    execution,
                    "external_item_source/" + source.providerId()
                            + "/" + source.sourceKey(),
                    source.readEntries(),
                    source.readAffixes()
            );
        }
    }

    private static boolean isValidEquipmentOwner(
            DamageNexusContext ctx,
            LivingEntity owner
    ) {
        return owner != null
                && ctx.victim().level() instanceof ServerLevel level
                && owner.level() == level
                && !owner.isRemoved()
                && owner.isAddedToLevel();
    }

    private void collectVictimEquipmentRules(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        if (ctx.victim() == null) {
            return;
        }

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.MAINHAND,
                RuleSourceLocation.VICTIM_MAINHAND,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.OFFHAND,
                RuleSourceLocation.VICTIM_OFFHAND,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.HEAD,
                RuleSourceLocation.VICTIM_HEAD,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.CHEST,
                RuleSourceLocation.VICTIM_CHEST,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.LEGS,
                RuleSourceLocation.VICTIM_LEGS,
                DamageRuleRole.DEFENSIVE
        );

        collectEquipmentSlot(
                ctx,
                phase,
                out,
                ctx.victim(),
                EquipmentSlot.FEET,
                RuleSourceLocation.VICTIM_FEET,
                DamageRuleRole.DEFENSIVE
        );
    }

    private void collectEquipmentSlot(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            LivingEntity owner,
            EquipmentSlot slot,
            RuleSourceLocation location,
            DamageRuleRole role
    ) {
        ItemStack stack = owner.getItemBySlot(slot);

        RuleExecutionContext exec =
                RuleExecutionContext.itemEquipment(
                        location,
                        role,
                        owner,
                        stack,
                        slot
                );

        collectStackRules(
                ctx,
                phase,
                out,
                stack,
                exec
        );
    }

    private void collectStackRules(
            DamageNexusContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out,
            ItemStack stack,
            RuleExecutionContext exec
    ) {
        StackDamageEntryCollector.collectStackEntries(
                ctx,
                phase,
                out,
                stack,
                exec,
                "item_damage_entries/"
                        + exec.sourceLocation().name().toLowerCase()
        );
    }
}

