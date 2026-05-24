package io.github.naimjeg.damagenexus.api.rule;

import net.minecraft.resources.Identifier;

import java.util.List;

public interface ChannelReferencingCondition {

    List<Identifier> referencedChannels();
}
