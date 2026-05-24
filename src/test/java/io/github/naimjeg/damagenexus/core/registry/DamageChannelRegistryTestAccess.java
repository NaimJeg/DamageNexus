package io.github.naimjeg.damagenexus.core.registry;

import net.minecraft.resources.Identifier;

import java.util.Map;

/** Test-source-only bridge for package-private registry mutation hooks. */
public final class DamageChannelRegistryTestAccess {

    private DamageChannelRegistryTestAccess() {
    }

    public static void replace(
            Map<Identifier, DamageChannelRegistry.ChannelDefinition>
                    definitions
    ) {
        DamageChannelRegistry.replaceStateForTesting(definitions);
    }

    public static void reset() {
        DamageChannelRegistry.resetStateForTesting();
    }
}
