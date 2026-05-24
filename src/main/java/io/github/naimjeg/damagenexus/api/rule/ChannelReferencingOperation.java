package io.github.naimjeg.damagenexus.api.rule;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Implemented by built-in operations that reference serialized DamageNexus
 * channel ids.
 * <p>
 * These references are validated during datapack rule reload so typoed channel
 * ids do not silently degrade to untyped at runtime.
 */
public interface ChannelReferencingOperation {

    List<Identifier> referencedChannels();
}
