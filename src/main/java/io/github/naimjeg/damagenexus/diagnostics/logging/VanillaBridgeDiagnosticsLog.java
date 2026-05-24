package io.github.naimjeg.damagenexus.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaBridgePlan;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.damagenexus.bridge.vanilla.VanillaMobEffectBridge;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class VanillaBridgeDiagnosticsLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VanillaBridgeDiagnosticsLog() {
    }

    public static void incomingCaught(String sourceMsgId) {
        if (!acceptsDetail()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-Incoming] Caught Damage: {}",
                sourceMsgId
        );
    }

    public static void bridgePlan(
            float eventOriginalAmount,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            VanillaMobEffectBridge.OffensiveMobEffectBreakdown mobEffectBreakdown,
            VanillaBridgePlan bridgePlan
    ) {
        if (!acceptsDetail()) {
            return;
        }

        String preEventInfo = vanillaSnapshot == null
                ? "no_snapshot"
                : "kind=" + vanillaSnapshot.preEventDelta().kind()
                  + " postEnchant=" + vanillaSnapshot.preEventDelta().postEnchantDamage()
                  + " eventOriginal=" + vanillaSnapshot.preEventDelta().eventOriginalDamage()
                  + " ratio=" + vanillaSnapshot.preEventDelta().ratio()
                  + " delta=" + vanillaSnapshot.preEventDelta().delta()
                  + " reason=" + vanillaSnapshot.preEventDelta().reason();

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-Incoming] Vanilla bridge plan. eventOriginal={} initialBase={} rebuildMobEffect={} rebuildEnchant={} rebuildPreEvent={} strengthDelta={} observedWeaknessDelta={} observedMobEffectDelta={} enabledMobEffectDelta={} preEvent=[{}] reason={}",
                eventOriginalAmount,
                bridgePlan.initialBaseAmount(),
                bridgePlan.rebuildOffensiveMobEffects(),
                bridgePlan.rebuildOffensiveEnchantment(),
                bridgePlan.rebuildPreEventDelta(),
                mobEffectBreakdown.strengthDelta(),
                mobEffectBreakdown.weaknessDelta(),
                mobEffectBreakdown.observedDelta(),
                bridgePlan.offensiveMobEffectDelta(),
                preEventInfo,
                bridgePlan.reason()
        );
    }

    public static void offensiveEnchantSnapshot(
            VanillaDamageCapture.OffensiveSnapshot snapshot
    ) {
        if (!acceptsDetail()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-Bridge] offensive_enchant attacker={} victim={} source={} weapon={} preEnchant={} postEnchant={} enchantDelta={} eventOriginal={}",
                entityName(snapshot.attacker()),
                entityName(snapshot.victim()),
                sourceId(snapshot.source()),
                snapshot.weapon().getHoverName().getString(),
                snapshot.preEnchantDamage(),
                snapshot.postEnchantDamage(),
                snapshot.enchantDelta(),
                snapshot.eventOriginalDamage()
        );
    }

    public static void preEventDelta(
            VanillaDamageCapture.OffensiveSnapshot snapshot,
            VanillaDamageCapture.PreEventDelta delta
    ) {
        if (!acceptsDetail()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-Bridge] pre_event kind={} reason={} source_id={} postEnchant={} eventOriginal={} ratio={} delta={}",
                delta.kind(),
                delta.reason(),
                sourceId(snapshot.source()),
                delta.postEnchantDamage(),
                delta.eventOriginalDamage(),
                delta.ratio(),
                delta.delta()
        );
    }

    public static void unknownPreEventDelta(
            VanillaDamageCapture.OffensiveSnapshot snapshot,
            VanillaDamageCapture.PreEventDelta delta
    ) {
        if (!DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.VANILLA_BRIDGE,
                sourceId(snapshot.source()),
                "incoming/pre_event_delta",
                delta.kind().name()
        )) {
            return;
        }
        warn(
                "[DN-Bridge][UNKNOWN_PRE_EVENT_DELTA] attacker={} victim={} source={} weapon={} postEnchant={} eventOriginal={} ratio={} delta={} reason={}",
                entityName(snapshot.attacker()),
                entityName(snapshot.victim()),
                sourceId(snapshot.source()),
                snapshot.weapon().getHoverName().getString(),
                delta.postEnchantDamage(),
                delta.eventOriginalDamage(),
                delta.ratio(),
                delta.delta(),
                delta.reason()
        );
    }

    public static void projectileCriticalBonus(
            VanillaDamageCapture.OffensiveSnapshot snapshot
    ) {
        if (!acceptsDetail()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-Bridge] projectile_crit_bonus attacker={} victim={} source={} weapon={} bonus={} eventOriginal={}",
                entityName(snapshot.attacker()),
                entityName(snapshot.victim()),
                sourceId(snapshot.source()),
                snapshot.weapon().getHoverName().getString(),
                snapshot.projectileCriticalBonus(),
                snapshot.eventOriginalDamage()
        );
    }

    public static void projectileDamage(
            ItemStack weapon,
            float preEnchantDamage,
            float postEnchantDamage,
            int preCritDamage,
            int postCritDamage,
            boolean critical,
            DamageSource source
    ) {
        if (!acceptsDetail()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-VanillaCapture] projectile_damage weapon={} preEnchant={} postEnchant={} preCrit={} postCrit={} critical={} critBonus={} source={}",
                weapon == null || weapon.isEmpty()
                        ? "<empty>"
                        : weapon.getHoverName().getString(),
                preEnchantDamage,
                postEnchantDamage,
                preCritDamage,
                postCritDamage,
                critical,
                Math.max(0, postCritDamage - preCritDamage),
                sourceId(source)
        );
    }

    public static void modifyDamage(
            ItemStack weapon,
            float inputDamage,
            float outputDamage,
            float delta,
            DamageSource source
    ) {
        if (!acceptsDetail()) {
            return;
        }

        DamageNexusLogSink.info(
                DamageNexusLogKind.TRACE_DETAIL,
                LOGGER,
                "[DN-VanillaCapture] modifyDamage weapon={} input={} output={} delta={} source={}",
                weapon == null || weapon.isEmpty()
                        ? "<empty>"
                        : weapon.getHoverName().getString(),
                inputDamage,
                outputDamage,
                delta,
                sourceId(source)
        );
    }

    public static void staleOffensiveEnchantFrame(
            Entity oldAttacker,
            Entity oldVictim,
            DamageSource oldSource,
            ItemStack oldWeapon,
            float oldInput,
            float oldOutput,
            Entity newVictim,
            DamageSource newSource,
            ItemStack newWeapon
    ) {
        String oldSourceId = sourceId(oldSource);
        String newSourceId = sourceId(newSource);
        if (!DamageNexusDiagnosticState.shouldLog(
                DamageNexusDiagnosticState.Domain.VANILLA_BRIDGE,
                oldSourceId,
                "capture/stale_frame",
                newSourceId
        )) {
            return;
        }
        warn(
                "[DN-VanillaCapture] stale offensive enchant frame overwritten. old_attacker={} old_victim={} old_source={} old_weapon={} old_input={} old_output={} new_victim={} new_source={} new_weapon={}",
                entityName(oldAttacker),
                entityName(oldVictim),
                oldSourceId,
                itemName(oldWeapon),
                oldInput,
                oldOutput,
                entityName(newVictim),
                newSourceId,
                itemName(newWeapon)
        );
    }

    private static String entityName(Entity entity) {
        return entity == null
                ? "<null>"
                : entity.getName().getString();
    }

    private static String itemName(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? "<empty>"
                : stack.getHoverName().getString();
    }

    private static String sourceId(DamageSource source) {
        if (source == null) {
            return "null";
        }

        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(source.type().msgId());
    }

    private static boolean acceptsDetail() {
        return DamageNexusLogSink.shouldAccept(DamageNexusLogKind.TRACE_DETAIL);
    }

    private static void warn(String template, Object... args) {
        LOGGER.warn(
                DiagnosticTextSanitizer.sanitizeLine(template),
                DiagnosticTextSanitizer.sanitizeArguments(args)
        );
    }
}


