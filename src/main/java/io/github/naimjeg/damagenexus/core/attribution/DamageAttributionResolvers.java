package io.github.naimjeg.damagenexus.core.attribution;

import io.github.naimjeg.damagenexus.api.damage.*;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;
import io.github.naimjeg.damagenexus.core.util.JvmFatalErrors;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageAttributionDiagnosticsLog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Framework-owned, frozen registry and execution boundary for resolvers. */
public final class DamageAttributionResolvers {

    public static final int MIN_PRIORITY = -10_000;
    public static final int MAX_PRIORITY = 10_000;
    private static final Comparator<Entry> ORDER = Comparator
            .comparingInt(Entry::priority)
            .reversed()
            .thenComparing(entry -> entry.id().toString());
    private static final Map<Identifier, Entry> MUTABLE =
            new LinkedHashMap<>();
    private static volatile List<Entry> entries = List.of();
    private static boolean frozen;

    private DamageAttributionResolvers() {
    }

    public static synchronized void register(
            DamageNexusRegistrationAccess access,
            Identifier id,
            int priority,
            DamageAttributionResolver resolver
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "registerAttributionResolver"
        );
        if (frozen) {
            throw new IllegalStateException(
                    "Attribution resolver registry is frozen"
            );
        }
        Identifier safeId = Objects.requireNonNull(id, "id");
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "Resolver priority must be between " + MIN_PRIORITY
                            + " and " + MAX_PRIORITY
            );
        }
        Entry entry = new Entry(
                safeId,
                priority,
                Objects.requireNonNull(resolver, "resolver")
        );
        if (MUTABLE.putIfAbsent(safeId, entry) != null) {
            throw new IllegalArgumentException(
                    "Duplicate attribution resolver ID: " + safeId
            );
        }
    }

    public static synchronized void freeze(
            DamageNexusRegistrationAccess access
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "freezeAttributionResolvers"
        );
        if (frozen) {
            throw new IllegalStateException(
                    "Attribution resolver registry already frozen"
            );
        }
        ArrayList<Entry> sorted = new ArrayList<>(MUTABLE.values());
        sorted.sort(ORDER);
        entries = List.copyOf(sorted);
        frozen = true;
    }

    /** Resolves once and returns a registry-authored authoritative origin. */
    public static DamageOrigin resolve(DamageAttributionQuery query,
                                       DamageOrigin baseOrigin) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(baseOrigin, "baseOrigin");
        requireServerThread(query.level());

        List<Claim> claims = new ArrayList<>();
        for (Entry entry : entries) {
            try {
                Optional<DamageAttributionResolution> resolution =
                        entry.resolver().resolve(query);
                if (resolution == null) {
                    DamageAttributionDiagnosticsLog.invalid(
                            entry.id(),
                            query.entryPoint(),
                            "resolver returned null instead of Optional"
                    );
                    continue;
                }
                if (resolution.isEmpty()) {
                    continue;
                }
                DamageAttribution attribution = resolution.orElseThrow()
                        .attribution();
                String invalid = invalidAttribution(query.level(), attribution);
                if (invalid != null) {
                    DamageAttributionDiagnosticsLog.invalid(
                            entry.id(), query.entryPoint(), invalid
                    );
                    continue;
                }
                claims.add(new Claim(entry, attribution));
            } catch (Throwable throwable) {
                JvmFatalErrors.rethrowIfFatal(throwable);
                DamageAttributionDiagnosticsLog.failure(
                        entry.id(), query.entryPoint(), throwable
                );
            }
        }

        if (claims.isEmpty()) {
            return baseOrigin;
        }
        Claim selected = claims.getFirst();
        if (claims.size() > 1) {
            DamageAttributionDiagnosticsLog.ambiguous(
                    selected.entry().id(),
                    claims.stream().map(claim -> claim.entry().id()).toList(),
                    query.entryPoint()
            );
        }
        return baseOrigin.withResolvedAttribution(
                selected.attribution(),
                DamageAttributionProvenance.registeredResolver(
                        selected.entry().id()
                )
        );
    }

    public static @Nullable String invalidAttribution(
            ServerLevel level,
            DamageAttribution attribution
    ) {
        if (attribution == null) {
            return "null attribution";
        }
        String invalid = invalidEntity(level, attribution.directEntity());
        if (invalid != null) return "directEntity: " + invalid;
        invalid = invalidEntity(level, attribution.logicalAttacker());
        if (invalid != null) return "logicalAttacker: " + invalid;
        invalid = invalidEntity(level, attribution.effectOwner());
        if (invalid != null) return "effectOwner: " + invalid;
        invalid = invalidEntity(level, attribution.equipmentOwner());
        return invalid == null ? null : "equipmentOwner: " + invalid;
    }

    /**
     * Normalizes untrusted entity references carried by a native
     * {@code DamageSource}. Native damage remains valid; only invalid role
     * references are discarded before resolver queries and rule collection.
     */
    public static DamageAttribution normalizeVanillaDefault(
            ServerLevel level,
            DamageAttribution attribution
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(attribution, "attribution");
        return new DamageAttribution(
                normalizeNativeEntity(
                        level, attribution.directEntity(), "directEntity"
                ),
                normalizeNativeLiving(
                        level, attribution.logicalAttacker(),
                        "logicalAttacker"
                ),
                normalizeNativeEntity(
                        level, attribution.effectOwner(), "effectOwner"
                ),
                normalizeNativeLiving(
                        level, attribution.equipmentOwner(),
                        "equipmentOwner"
                )
        );
    }

    private static @Nullable Entity normalizeNativeEntity(
            ServerLevel level,
            @Nullable Entity entity,
            String role
    ) {
        String invalid = invalidEntity(level, entity);
        if (invalid == null) {
            return entity;
        }
        DamageAttributionDiagnosticsLog.nativeNormalized(role, invalid);
        return null;
    }

    private static @Nullable LivingEntity normalizeNativeLiving(
            ServerLevel level,
            @Nullable LivingEntity entity,
            String role
    ) {
        return (LivingEntity)
                normalizeNativeEntity(level, entity, role);
    }

    private static @Nullable String invalidEntity(
            ServerLevel level,
            @Nullable Entity entity
    ) {
        if (entity == null) return null;
        if (entity.isRemoved()) return "entity is removed";
        if (entity.level() != level) return "entity belongs to another level";
        return entity.isAddedToLevel()
                ? null
                : "entity is not added to the server level";
    }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException(
                    "Attribution resolution must run on the server thread"
            );
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
            DamageAttributionResolver resolver
    ) {
    }

    private record Claim(Entry entry, DamageAttribution attribution) {
    }
}
