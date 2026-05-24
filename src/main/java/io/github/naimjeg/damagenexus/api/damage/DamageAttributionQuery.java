package io.github.naimjeg.damagenexus.api.damage;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Read-only server-side input supplied to attribution resolvers. */
public record DamageAttributionQuery(
        ServerLevel level,
        LivingEntity target,
        DamageSourceDescriptor source,
        Optional<DamageSource> actualSource,
        DamageRequestKind requestKind,
        DamageAttribution candidate,
        Optional<Identifier> actionId,
        Set<Identifier> sourceTags,
        DamageMetadata metadata,
        DamageAttributionEntryPoint entryPoint
) {
    public DamageAttributionQuery {
        level = Objects.requireNonNull(level, "level");
        target = Objects.requireNonNull(target, "target");
        source = Objects.requireNonNull(source, "source");
        actualSource = actualSource == null ? Optional.empty() : actualSource;
        requestKind = Objects.requireNonNull(requestKind, "requestKind");
        candidate = Objects.requireNonNull(candidate, "candidate");
        actionId = actionId == null ? Optional.empty() : actionId;
        Set<Identifier> tags = sourceTags == null
                ? Set.of()
                : new LinkedHashSet<>(sourceTags);
        if (tags.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Source tags cannot contain null");
        }
        sourceTags = Collections.unmodifiableSet(tags);
        metadata = Objects.requireNonNull(metadata, "metadata");
        entryPoint = Objects.requireNonNull(entryPoint, "entryPoint");
    }
}
