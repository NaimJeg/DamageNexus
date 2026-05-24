package io.github.naimjeg.damagenexus.api.item.template;

import io.github.naimjeg.damagenexus.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.damagenexus.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.damagenexus.core.template.DamageTemplateRegistry;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only public lookup facade for complete static templates.
 *
 * <p>The server registry is authoritative. Clients may only observe templates
 * available from their own Java registrations; a client miss says nothing
 * about whether the server can execute the reference.</p>
 */
public final class DamageNexusTemplates {
    private DamageNexusTemplates() {}

    /** Returns the immutable definition in the currently published revision. */
    public static Optional<DamageEntryDefinition> entry(Identifier id) {
        return DamageTemplateRegistry.entry(Objects.requireNonNull(id, "id"));
    }

    /** Returns the immutable definition in the currently published revision. */
    public static Optional<DamageAffixDefinition> affix(Identifier id) {
        return DamageTemplateRegistry.affix(Objects.requireNonNull(id, "id"));
    }

    /** Monotonic runtime revision. It is not a persistent or network ID. */
    public static long revision() {
        return DamageTemplateRegistry.revision();
    }

    /**
     * Reports whether the published definitions have completed strict
     * server-side validation against the current damage-channel content.
     * Lookup availability alone does not imply execution readiness.
     */
    public static boolean serverExecutionReady() {
        return DamageTemplateRegistry.serverExecutionReady();
    }

    /**
     * Channel content revision validated by the current template snapshot,
     * or {@code -1} for the Java-only pre-reload snapshot.
     */
    public static long validatedChannelRevision() {
        return DamageTemplateRegistry.validatedChannelRevision();
    }
}
