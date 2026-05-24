package io.github.naimjeg.damagenexus.externalapi;

import io.github.naimjeg.damagenexus.api.DamageNexusApi;
import io.github.naimjeg.damagenexus.api.damage.DamageRequest;
import io.github.naimjeg.damagenexus.api.damage.DamageFailure;
import io.github.naimjeg.damagenexus.api.damage.DamageRequestKind;
import io.github.naimjeg.damagenexus.api.damage.DamageResult;
import io.github.naimjeg.damagenexus.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.damagenexus.api.damage.DamageSubmissionStatus;
import io.github.naimjeg.damagenexus.api.event.DamageNexusRegisterEvent;
import io.github.naimjeg.damagenexus.api.event.DamageSettlementCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;

/** Compilation-only example; a content mod owns every gameplay decision. */
public final class ExternalSettlementProcExample {
    private static final Identifier ACTION =
            Identifier.fromNamespaceAndPath("examplemod", "example_proc");
    private static final Identifier AREA_DAMAGE =
            Identifier.fromNamespaceAndPath("examplemod", "area_damage");

    private ExternalSettlementProcExample() {}

    @SubscribeEvent
    public static void register(DamageNexusRegisterEvent event) {
        event.registerSettlementListener(
                Identifier.fromNamespaceAndPath(
                        "examplemod", "example_proc_listener"),
                0,
                ExternalSettlementProcExample::onDamageSettled
        );
    }

    public static void onDamageSettled(DamageSettlementCallback callback) {
        DamageSettlementSnapshot snapshot = callback.snapshot();
        var authority = callback.childAuthority();
        // Ignore every derived request, including requests created here.
        if (authority.isEmpty()
                || snapshot.requestKind() != DamageRequestKind.PRIMARY
                || !snapshot.triggerPolicy().procAllowed()
                || snapshot.logicalAttacker() == null
                || !ExampleContentEquipment.hasEffect(
                        snapshot.logicalAttacker())
                || !ExampleContentProbability.shouldTrigger(snapshot)) {
            return;
        }

        for (LivingEntity target :
                ExampleContentTargets.findTargets(snapshot)) {
            DamageRequest request = DamageRequest.builder(
                            snapshot.level(), target, snapshot.source())
                    .kind(DamageRequestKind.PROC)
                    .parent(authority.orElseThrow())
                    .logicalAttacker(snapshot.logicalAttacker())
                    .actionId(ACTION)
                    .sourceTag(AREA_DAMAGE)
                    .baseDamage(ExampleContentDamage.calculate(snapshot))
                    .build();

            DamageResult result = DamageNexusApi.submitDamage(request);
            ExampleContentDiagnostics.observe(
                    result.status(), result.failure());
        }
    }

    private static final class ExampleContentEquipment {
        static boolean hasEffect(LivingEntity attacker) {
            return false;
        }
    }

    private static final class ExampleContentProbability {
        static boolean shouldTrigger(DamageSettlementSnapshot snapshot) {
            return false;
        }
    }

    private static final class ExampleContentTargets {
        static List<LivingEntity> findTargets(
                DamageSettlementSnapshot snapshot
        ) {
            return List.of();
        }
    }

    private static final class ExampleContentDamage {
        static float calculate(DamageSettlementSnapshot snapshot) {
            return Math.max(0.0f, snapshot.healthDamage());
        }
    }

    private static final class ExampleContentDiagnostics {
        static void observe(
                DamageSubmissionStatus status,
                Optional<DamageFailure> failure
        ) {
            // The content mod decides how to record structured outcomes.
        }
    }
}
