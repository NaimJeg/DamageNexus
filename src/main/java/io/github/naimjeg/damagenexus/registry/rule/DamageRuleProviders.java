package io.github.naimjeg.damagenexus.registry.rule;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleProvider;
import io.github.naimjeg.damagenexus.builtin.rule.provider.DatapackDamageRuleProvider;
import io.github.naimjeg.damagenexus.builtin.rule.provider.ItemDamageRuleProvider;
import io.github.naimjeg.damagenexus.builtin.rule.provider.ProjectileDamageRuleProvider;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusLifecycle;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusRegistrationAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DamageRuleProviders {

    private static final List<DamageRuleProvider> PROVIDERS =
            new ArrayList<>();

    private static final Set<DamageRuleProvider> BUILTIN_PROVIDERS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        registerBuiltin(new ItemDamageRuleProvider());
        registerBuiltin(new ProjectileDamageRuleProvider());
        registerBuiltin(new DatapackDamageRuleProvider());
    }

    private DamageRuleProviders() {
    }

    public static synchronized void register(
            DamageNexusRegistrationAccess access,
            DamageRuleProvider provider
    ) {
        DamageNexusLifecycle.requireRegistering(
                access,
                "DamageRuleProviders.register"
        );
        Objects.requireNonNull(provider, "provider");

        if (PROVIDERS.contains(provider)) {
            return;
        }

        PROVIDERS.add(provider);
    }

    public static synchronized List<DamageRuleProvider> all() {
        return List.copyOf(PROVIDERS);
    }

    public static synchronized boolean isBuiltin(
            DamageRuleProvider provider
    ) {
        return BUILTIN_PROVIDERS.contains(provider);
    }

    private static synchronized void registerBuiltin(DamageRuleProvider provider) {
        Class<?> providerClass = provider.getClass();

        for (DamageRuleProvider existing : PROVIDERS) {
            if (existing.getClass() == providerClass) {
                return;
            }
        }

        PROVIDERS.add(provider);
        BUILTIN_PROVIDERS.add(provider);
    }
}
