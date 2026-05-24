package io.github.naimjeg.damagenexus.api.rule;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Implemented by operations that reference serialized pre-multiplier bucket ids.
 */
public interface PreMultiplierBucketReferencingOperation {

    List<Identifier> referencedPreMultiplierBuckets();
}
