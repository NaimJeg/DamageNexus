package io.github.naimjeg.damagenexus.builtin.rule.condition;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderReferenceTagFixture;
import net.minecraft.core.Registry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.effect.MobEffect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobEffectTagConditionReloadTest {

    @Test
    void sameConditionObservesReboundHolderTagsWithoutCachingMembers() {
        ResourceKey<Registry<MobEffect>> registryKey =
                ResourceKey.createRegistryKey(
                        id("test", "mob_effect_fixture")
                );
        HolderOwner<MobEffect> owner = new HolderOwner<>() {
        };
        Holder.Reference<MobEffect> first = Holder.Reference.createStandAlone(
                owner, effectKey(registryKey, "first")
        );
        Holder.Reference<MobEffect> second = Holder.Reference.createStandAlone(
                owner, effectKey(registryKey, "second")
        );
        TagKey<MobEffect> tag = TagKey.create(
                registryKey,
                id("contentmod", "reloadable_effects")
        );
        TagKey<MobEffect> undefined = TagKey.create(
                registryKey,
                id("contentmod", "undefined_effects")
        );

        HolderReferenceTagFixture.bind(first, List.of(tag));
        HolderReferenceTagFixture.bind(second, List.of());
        assertTrue(MobEffectTagConditionSupport.matchesHolder(
                first, tag
        ));
        assertFalse(MobEffectTagConditionSupport.matchesHolder(
                second, tag
        ));
        assertFalse(MobEffectTagConditionSupport.matchesHolder(
                first, undefined
        ));

        HolderReferenceTagFixture.bind(first, List.of());
        HolderReferenceTagFixture.bind(second, List.of(tag));
        assertFalse(MobEffectTagConditionSupport.matchesHolder(
                first, tag
        ));
        assertTrue(MobEffectTagConditionSupport.matchesHolder(
                second, tag
        ));

        HolderReferenceTagFixture.bind(second, List.of());
        assertFalse(MobEffectTagConditionSupport.matchesHolder(second, tag));
    }

    @Test
    void completeTagBindingsAndFrozenStateRestoreEvenAfterFailure() {
        ResourceKey<Registry<String>> registryKey =
                ResourceKey.createRegistryKey(id("test", "isolated_effects"));
        MappedRegistry<String> registry =
                new MappedRegistry<>(registryKey, Lifecycle.stable());
        Holder.Reference<String> first = registry.register(
                stringKey(registryKey, "first"), "first", RegistrationInfo.BUILT_IN);
        Holder.Reference<String> second = registry.register(
                stringKey(registryKey, "second"), "second", RegistrationInfo.BUILT_IN);
        TagKey<String> sentinel = TagKey.create(
                registryKey, id("existingmod", "sentinel"));
        TagKey<String> temporary = TagKey.create(
                registryKey, id("contentmod", "temporary"));
        registry.bindTags(Map.of(sentinel, List.of(first)));
        registry.freeze();
        Map<TagKey<String>, List<Holder<String>>> original = bindings(registry);

        assertThrows(IllegalStateException.class, () -> {
            try {
                Map<TagKey<String>, List<Holder<String>>> merged =
                        new LinkedHashMap<>(original);
                merged.put(temporary, List.of(second));
                apply(registry, merged);
                assertTrue(second.is(temporary));
                assertTrue(first.is(sentinel));
                throw new IllegalStateException("injected failure");
            } finally {
                apply(registry, original);
            }
        });

        assertEquals(List.of(first), bindings(registry).get(sentinel));
        assertFalse(second.is(temporary));
        assertThrows(IllegalStateException.class,
                () -> registry.bindTags(Map.of(temporary, List.of(second))));
    }

    private static <T> void apply(
            MappedRegistry<T> registry,
            Map<TagKey<T>, List<Holder<T>>> values
    ) {
        registry.prepareTagReload(
                new TagLoader.LoadResult<>(registry.key(), values)).apply();
    }

    private static <T> Map<TagKey<T>, List<Holder<T>>> bindings(
            Registry<T> registry
    ) {
        Map<TagKey<T>, List<Holder<T>>> copy =
                new LinkedHashMap<>();
        registry.listTags().forEach(tag ->
                copy.put(tag.key(), tag.stream().toList()));
        return Map.copyOf(copy);
    }

    private static ResourceKey<String> stringKey(
            ResourceKey<Registry<String>> registryKey,
            String path
    ) {
        return ResourceKey.create(registryKey, id("test", path));
    }

    private static ResourceKey<MobEffect> effectKey(
            ResourceKey<Registry<MobEffect>> registryKey,
            String path
    ) {
        return ResourceKey.create(
                registryKey,
                id("test", path)
        );
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
