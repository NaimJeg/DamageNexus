package io.github.naimjeg.damagenexus.client.tooltip;

public final class DamageNexusClientTooltips {

    private static boolean registered = false;

    private DamageNexusClientTooltips() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        DefaultConditionTooltips.register();
        DefaultOperationTooltips.register();

        registered = true;
    }
}
