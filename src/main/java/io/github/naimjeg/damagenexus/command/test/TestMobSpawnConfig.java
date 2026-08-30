package io.github.naimjeg.damagenexus.command.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable spawn configuration for a test mob: the orthogonal spawn option
 * plus any pre-spawn mutations. The existing {@code TestMobSpawnOptions} enum
 * stays orthogonal; mutations are a separate, extensible abstraction.
 */
public record TestMobSpawnConfig(
        TestMobSpawnOptions options,
        List<TestMobMutation> mutations
) {

    public TestMobSpawnConfig {
        options = Objects.requireNonNull(
                options,
                "options must not be null"
        );
        List<TestMobMutation> copy = new ArrayList<>(
                Objects.requireNonNull(
                        mutations,
                        "mutations must not be null"
                )
        );
        for (TestMobMutation mutation : copy) {
            Objects.requireNonNull(
                    mutation,
                    "mutations must not contain null elements"
            );
        }
        mutations = List.copyOf(copy);
    }

    public static TestMobSpawnConfig defaults() {
        return new TestMobSpawnConfig(TestMobSpawnOptions.DEFAULT, List.of());
    }

    public TestMobSpawnConfig withOptions(TestMobSpawnOptions options) {
        return new TestMobSpawnConfig(options, mutations);
    }

    public TestMobSpawnConfig withMutation(TestMobMutation mutation) {
        Objects.requireNonNull(
                mutation,
                "mutation must not be null"
        );
        List<TestMobMutation> next = new ArrayList<>(mutations);
        next.add(mutation);
        return new TestMobSpawnConfig(options, next);
    }
}
