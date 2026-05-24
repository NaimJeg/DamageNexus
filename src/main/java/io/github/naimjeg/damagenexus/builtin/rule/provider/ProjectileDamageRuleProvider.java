package io.github.naimjeg.damagenexus.builtin.rule.provider;

import io.github.naimjeg.damagenexus.api.context.DamageRuleContext;
import io.github.naimjeg.damagenexus.api.enums.DamagePhase;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleRole;
import io.github.naimjeg.damagenexus.api.rule.RuleExecutionContext;
import io.github.naimjeg.damagenexus.api.rule.RuntimeDamageRule;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageSourceProfile;
import io.github.naimjeg.damagenexus.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.rule.StackDamageEntryCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ProjectileDamageRuleProvider implements DamageRuleProvider {

    @Override
    public void collect(
            DamageRuleContext context,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageNexusContext ctx = DamageInternalContexts.require(
                context,
                "projectile rule provider"
        );

        VanillaDamageSourceProfile profile = ctx.vanillaSourceProfile();

        if (profile == null || !profile.projectile()) {
            return;
        }

        Entity projectileEntity = profile.directAttacker();

        if (!(projectileEntity instanceof Projectile)) {
            return;
        }

        VanillaDamageCapture.OffensiveSnapshot snapshot =
                ctx.getVanillaSnapshot();

        if (snapshot == null) {
            return;
        }

        ItemStack projectileSourceStack = snapshot.weapon();

        if (projectileSourceStack == null || projectileSourceStack.isEmpty()) {
            return;
        }

        LivingEntity owner = ctx.equipmentOwner();

        RuleExecutionContext exec =
                RuleExecutionContext.projectileSource(
                        DamageRuleRole.OFFENSIVE,
                        owner,
                        projectileSourceStack,
                        projectileEntity
                );

        collectStackRules(
                ctx,
                phase,
                out,
                projectileSourceStack,
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
                "projectile_damage_entries"
        );
    }
}

