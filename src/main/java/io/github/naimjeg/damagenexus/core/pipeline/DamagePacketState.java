// src/main/java/io/github/naimjeg/damagenexus/core/pipeline/DamagePacketState.java
package io.github.naimjeg.damagenexus.core.pipeline;

import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.core.DamageComponent;
import io.github.naimjeg.damagenexus.core.PreMultiplierSet;
import io.github.naimjeg.damagenexus.core.registry.DamageChannelRegistry;
import io.github.naimjeg.damagenexus.core.registry.PreMultiplierBucketRegistry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

public final class DamagePacketState {

    private final DamageComponent[] componentsByChannelIndex =
            new DamageComponent[DamageChannelRegistry.channelCount()];

    private final int[] activeChannelIndexes =
            new int[componentsByChannelIndex.length];

    private int activeChannelCount = 0;

    private PreMultiplierSet globalPreMultipliers = null;
    private FloatArrayList globalPostMultipliers = null;
    private FloatArrayList globalMitigations = null;

    @ApiStatus.Internal
    public Checkpoint checkpoint() {
        DamageComponent[] componentIdentities = Arrays.copyOf(
                componentsByChannelIndex,
                componentsByChannelIndex.length
        );
        DamageComponent.Checkpoint[] componentStates =
                new DamageComponent.Checkpoint[componentIdentities.length];

        for (int index = 0; index < componentIdentities.length; index++) {
            DamageComponent component = componentIdentities[index];
            if (component != null) {
                componentStates[index] = component.checkpoint();
            }
        }

        return new Checkpoint(
                componentIdentities,
                componentStates,
                Arrays.copyOf(activeChannelIndexes, activeChannelIndexes.length),
                activeChannelCount,
                copy(globalPreMultipliers),
                copy(globalPostMultipliers),
                copy(globalMitigations)
        );
    }

    @ApiStatus.Internal
    public void restore(Checkpoint checkpoint) {
        if (checkpoint == null) {
            throw new IllegalArgumentException(
                    "Damage packet checkpoint must not be null"
            );
        }
        if (checkpoint.componentIdentities.length
                != componentsByChannelIndex.length) {
            throw new IllegalArgumentException(
                    "Damage packet checkpoint channel count changed"
            );
        }

        Arrays.fill(componentsByChannelIndex, null);
        for (int index = 0;
             index < checkpoint.componentIdentities.length;
             index++) {
            DamageComponent component =
                    checkpoint.componentIdentities[index];
            if (component != null) {
                component.restore(checkpoint.componentStates[index]);
                componentsByChannelIndex[index] = component;
            }
        }

        System.arraycopy(
                checkpoint.activeChannelIndexes,
                0,
                activeChannelIndexes,
                0,
                activeChannelIndexes.length
        );
        activeChannelCount = checkpoint.activeChannelCount;
        globalPreMultipliers = copy(checkpoint.globalPreMultipliers);
        globalPostMultipliers = copy(checkpoint.globalPostMultipliers);
        globalMitigations = copy(checkpoint.globalMitigations);
    }

    private static PreMultiplierSet copy(PreMultiplierSet source) {
        return source == null ? null : source.copy();
    }

    private static FloatArrayList copy(FloatArrayList source) {
        return source == null ? null : new FloatArrayList(source);
    }

    @ApiStatus.Internal
    public static final class Checkpoint {

        private final DamageComponent[] componentIdentities;
        private final DamageComponent.Checkpoint[] componentStates;
        private final int[] activeChannelIndexes;
        private final int activeChannelCount;
        private final PreMultiplierSet globalPreMultipliers;
        private final FloatArrayList globalPostMultipliers;
        private final FloatArrayList globalMitigations;

        private Checkpoint(
                DamageComponent[] componentIdentities,
                DamageComponent.Checkpoint[] componentStates,
                int[] activeChannelIndexes,
                int activeChannelCount,
                PreMultiplierSet globalPreMultipliers,
                FloatArrayList globalPostMultipliers,
                FloatArrayList globalMitigations
        ) {
            this.componentIdentities = componentIdentities;
            this.componentStates = componentStates;
            this.activeChannelIndexes = activeChannelIndexes;
            this.activeChannelCount = activeChannelCount;
            this.globalPreMultipliers = globalPreMultipliers;
            this.globalPostMultipliers = globalPostMultipliers;
            this.globalMitigations = globalMitigations;
        }
    }

    public DamageComponent getOrCreateComponent(DamageChannel rawChannel) {
        DamageChannel channel = DamageChannelRegistry.resolve(rawChannel);
        int index = channel.index();

        if (index < 0 || index >= componentsByChannelIndex.length) {
            channel = DamageChannelRegistry.getUntyped();
            index = channel.index();
        }

        DamageComponent component = componentsByChannelIndex[index];

        if (component == null) {
            component = new DamageComponent(channel);
            componentsByChannelIndex[index] = component;
            activeChannelIndexes[activeChannelCount++] = index;
        }

        return component;
    }

    public DamageComponent findActiveComponent(DamageChannel rawChannel) {
        DamageChannel channel = DamageChannelRegistry.resolve(rawChannel);

        for (int i = 0; i < activeChannelCount; i++) {
            DamageComponent component =
                    componentsByChannelIndex[activeChannelIndexes[i]];

            if (component.channel.equals(channel)) {
                return component;
            }
        }

        return null;
    }

    public int activeComponentCount() {
        return activeChannelCount;
    }

    public DamageComponent activeComponent(int activeIndex) {
        if (activeIndex < 0 || activeIndex >= activeChannelCount) {
            throw new IndexOutOfBoundsException(
                    "Invalid active component index: " + activeIndex
            );
        }

        return componentsByChannelIndex[activeChannelIndexes[activeIndex]];
    }

    public void addGlobalPreMultiplier(int modifierId, float value) {
        PreMultiplierBucketRegistry.requireFrozen();

        if (globalPreMultipliers == null) {
            globalPreMultipliers = new PreMultiplierSet();
        }

        globalPreMultipliers.add(modifierId, value);
    }

    public void addGlobalPostMultiplier(float value) {
        if (globalPostMultipliers == null) {
            globalPostMultipliers = new FloatArrayList(4);
        }

        globalPostMultipliers.add(value);
    }

    public void addGlobalMitigation(float reductionPercent) {
        if (globalMitigations == null) {
            globalMitigations = new FloatArrayList(4);
        }

        globalMitigations.add(reductionPercent);
    }

    public PreMultiplierSet globalPreMultipliers() {
        return globalPreMultipliers;
    }

    public FloatArrayList globalPostMultipliers() {
        return globalPostMultipliers;
    }

    public FloatArrayList globalMitigations() {
        return globalMitigations;
    }
}
