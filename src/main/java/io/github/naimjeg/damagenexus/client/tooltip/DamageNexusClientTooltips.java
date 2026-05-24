package io.github.naimjeg.damagenexus.client.tooltip;

import io.github.naimjeg.damagenexus.api.client.phrase.RegisterRulePhrasesEvent;
import io.github.naimjeg.damagenexus.api.client.phrase.RulePhraseRegistry;
import net.neoforged.neoforge.common.NeoForge;

public final class DamageNexusClientTooltips {

    private static volatile RulePhraseRegistry registry;

    private DamageNexusClientTooltips() {
    }

    public static synchronized void register() {
        if (registry != null) {
            return;
        }

        RulePhraseRegistry building = new RulePhraseRegistry();
        DamageNexusRulePhraseBootstrap.register(building);
        NeoForge.EVENT_BUS.post(new RegisterRulePhrasesEvent(building));
        building.freeze();
        registry = building;
    }

    public static RulePhraseRegistry registry() {
        register();
        return registry;
    }
}
