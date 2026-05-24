package io.github.naimjeg.damagenexus.core.rule;

import io.github.naimjeg.damagenexus.api.rule.source.*;
import io.github.naimjeg.damagenexus.core.config.DamageNexusSettings;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;
import io.github.naimjeg.damagenexus.core.pipeline.DamageNexusContext;
import io.github.naimjeg.damagenexus.core.util.JvmFatalErrors;
import io.github.naimjeg.damagenexus.core.util.StrictCallbackFailure;
import io.github.naimjeg.damagenexus.diagnostics.logging.ExternalItemRuleSourceDiagnosticsLog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/** Frozen external-stack source registry and transaction snapshot builder. */
public final class ExternalItemRuleSources {

    public static final int MIN_PRIORITY = -10_000;
    public static final int MAX_PRIORITY = 10_000;
    private static final Comparator<Entry> ENTRY_ORDER = Comparator
            .comparingInt(Entry::priority).reversed()
            .thenComparing(entry -> entry.id().toString());
    private static final Comparator<Candidate> CANDIDATE_ORDER = Comparator
            .comparingInt((Candidate c) -> c.entry().priority()).reversed()
            .thenComparing(
                    Comparator.comparingInt(
                            (Candidate c) -> c.contribution().sourcePriority()
                    ).reversed()
            )
            .thenComparing(c -> c.entry().id().toString())
            .thenComparing(c -> c.contribution().sourceKey().toString())
            .thenComparing(c -> c.contribution().slotSemantic().toString());
    private static final Map<Identifier, Entry> MUTABLE = new LinkedHashMap<>();
    private static volatile List<Entry> entries = List.of();
    private static boolean frozen;

    private ExternalItemRuleSources() {
    }

    public static synchronized void register(
            DamageNexusRegistrationAccess access,
            Identifier id,
            int priority,
            EquippedItemRuleSource source
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "registerEquippedItemRuleSource"
        );
        if (frozen) {
            throw new IllegalStateException(
                    "External item source registry is frozen"
            );
        }
        Identifier safeId = Objects.requireNonNull(id, "id");
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "Provider priority must be between " + MIN_PRIORITY
                            + " and " + MAX_PRIORITY
            );
        }
        Entry entry = new Entry(
                safeId,
                priority,
                Objects.requireNonNull(source, "source")
        );
        if (MUTABLE.putIfAbsent(safeId, entry) != null) {
            throw new IllegalArgumentException(
                    "Duplicate external item source ID: " + safeId
            );
        }
    }

    public static synchronized void freeze(
            DamageNexusRegistrationAccess access
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "freezeExternalItemRuleSources"
        );
        if (frozen) {
            throw new IllegalStateException(
                    "External item source registry already frozen"
            );
        }
        ArrayList<Entry> sorted = new ArrayList<>(MUTABLE.values());
        sorted.sort(ENTRY_ORDER);
        entries = List.copyOf(sorted);
        frozen = true;
    }

    /** Called once per context; callbacks are never re-run for later phases. */
    public static List<ExternalItemRuleSnapshot> snapshot(DamageNexusContext ctx) {
        ServerLevel level = (ServerLevel) ctx.victim().level();
        ArrayList<Candidate> candidates = new ArrayList<>();
        LivingEntity offensive = ctx.equipmentOwner();
        if (isValidOwner(level, offensive)) {
            collectDirection(
                    candidates, ctx, level, offensive,
                    EquippedItemRuleSourceDirection.OFFENSIVE
            );
        }
        collectDirection(
                candidates, ctx, level, ctx.victim(),
                EquippedItemRuleSourceDirection.DEFENSIVE
        );

        candidates.sort(CANDIDATE_ORDER);
        ArrayList<ExternalItemRuleSnapshot> snapshots = new ArrayList<>();
        Set<ProviderSourceKey> providerSources = new HashSet<>();
        Set<PhysicalKey> physical = new HashSet<>();
        IdentityHashMap<
                ItemStack,
                EnumSet<EquippedItemRuleSourceDirection>
                > identities = new IdentityHashMap<>();
        for (Candidate candidate : candidates) {
            EquippedItemRuleContribution contribution =
                    candidate.contribution();
            ProviderSourceKey providerSourceKey = new ProviderSourceKey(
                    candidate.owner(), candidate.direction(),
                    candidate.entry().id(), contribution.sourceKey()
            );
            PhysicalKey key = new PhysicalKey(
                    candidate.owner(), candidate.direction(),
                    contribution.sourceKey()
            );
            boolean duplicateIdentity = identities
                    .computeIfAbsent(
                            candidate.originalStack(),
                            ignored -> EnumSet.noneOf(
                                    EquippedItemRuleSourceDirection.class
                            )
                    )
                    .contains(candidate.direction());
            if (!providerSources.add(providerSourceKey)
                    || !physical.add(key)
                    || duplicateIdentity) {
                ExternalItemRuleSourceDiagnosticsLog.duplicate(
                        candidate.entry().id(), contribution.sourceKey(),
                        candidate.direction()
                );
                continue;
            }
            identities.get(candidate.originalStack()).add(candidate.direction());
            snapshots.add(new ExternalItemRuleSnapshot(
                    candidate.entry().id(),
                    candidate.entry().priority(),
                    contribution.sourceKey(),
                    contribution.slotSemantic(),
                    contribution.category(),
                    contribution.sourcePriority(),
                    candidate.direction(),
                    candidate.owner(),
                    candidate.stackSnapshot(),
                    contribution.readEntries(),
                    contribution.readAffixes()
            ));
        }
        return List.copyOf(snapshots);
    }

    private static boolean isValidOwner(
            ServerLevel level,
            LivingEntity owner
    ) {
        return owner != null
                && owner.level() == level
                && !owner.isRemoved()
                && owner.isAddedToLevel();
    }

    private static void collectDirection(
            List<Candidate> output,
            DamageNexusContext ctx,
            ServerLevel level,
            LivingEntity owner,
            EquippedItemRuleSourceDirection direction
    ) {
        EquippedItemRuleSourceQuery query = new EquippedItemRuleSourceQuery(
                level, owner, ctx.victim(), direction, ctx.origin(),
                ctx.vanillaSourceProfile() != null
                        && ctx.vanillaSourceProfile().projectile()
        );
        for (Entry entry : entries) {
            ArrayList<Candidate> local = new ArrayList<>();
            try {
                List<EquippedItemRuleContribution> contributed =
                        entry.source().collect(query);
                if (contributed == null) {
                    throw new IllegalArgumentException(
                            "Source returned null list"
                    );
                }
                for (EquippedItemRuleContribution contribution : contributed) {
                    if (contribution == null) {
                        throw new IllegalArgumentException(
                                "Source returned null contribution"
                        );
                    }
                    ItemStack stack = contribution.stack();
                    if (stack.isEmpty()) continue;
                    if (direction
                            == EquippedItemRuleSourceDirection.OFFENSIVE
                            && contribution.category()
                            == EquippedItemRuleSourceCategory.PROJECTILE
                            && ctx.getVanillaSnapshot() != null
                            && ctx.getVanillaSnapshot().weapon() != null
                            && !ctx.getVanillaSnapshot().weapon().isEmpty()) {
                        ExternalItemRuleSourceDiagnosticsLog.duplicate(
                                entry.id(),
                                contribution.sourceKey(),
                                direction
                        );
                        continue;
                    }
                    local.add(new Candidate(
                            entry,
                            direction,
                            owner,
                            contribution,
                            stack,
                            stack.copy()
                    ));
                }
                output.addAll(local);
            } catch (Throwable throwable) {
                JvmFatalErrors.rethrowIfFatal(throwable);
                if (DamageNexusSettings.strictRuleErrors()) {
                    throw new StrictCallbackFailure(
                            "External item source failed: " + entry.id(),
                            throwable
                    );
                }
                ExternalItemRuleSourceDiagnosticsLog.failure(
                        entry.id(), direction, throwable
                );
            }
        }
    }

    public static List<Identifier> orderedIds() {
        return entries.stream().map(Entry::id).toList();
    }

    @ApiStatus.Internal
    public static synchronized void resetForTesting() {
        MUTABLE.clear();
        entries = List.of();
        frozen = false;
    }

    private record Entry(
            Identifier id,
            int priority,
            EquippedItemRuleSource source
    ) {
    }

    private record Candidate(
            Entry entry,
            EquippedItemRuleSourceDirection direction,
            LivingEntity owner,
            EquippedItemRuleContribution contribution,
            ItemStack originalStack,
            ItemStack stackSnapshot
    ) {
    }

    private record PhysicalKey(
            LivingEntity owner,
            EquippedItemRuleSourceDirection direction,
            Identifier sourceKey
    ) {
        @Override
        public boolean equals(Object other) {
            return other instanceof PhysicalKey key
                    && owner == key.owner
                    && direction == key.direction
                    && sourceKey.equals(key.sourceKey);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(owner);
            result = 31 * result + direction.hashCode();
            return 31 * result + sourceKey.hashCode();
        }
    }

    private record ProviderSourceKey(
            LivingEntity owner,
            EquippedItemRuleSourceDirection direction,
            Identifier providerId,
            Identifier sourceKey
    ) {
        @Override
        public boolean equals(Object other) {
            return other instanceof ProviderSourceKey key
                    && owner == key.owner
                    && direction == key.direction
                    && providerId.equals(key.providerId)
                    && sourceKey.equals(key.sourceKey);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(owner);
            result = 31 * result + direction.hashCode();
            result = 31 * result + providerId.hashCode();
            return 31 * result + sourceKey.hashCode();
        }
    }
}
