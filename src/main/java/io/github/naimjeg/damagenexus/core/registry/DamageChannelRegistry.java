package io.github.naimjeg.damagenexus.core.registry;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.api.enums.DamageChannel;
import io.github.naimjeg.damagenexus.core.lifecycle.DamageNexusReloadAccess;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusLifecycleLog;
import io.github.naimjeg.damagenexus.diagnostics.logging.DamageNexusDiagnosticState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.util.*;

@EventBusSubscriber(modid = DamageNexus.MODID)
public class DamageChannelRegistry extends SimpleJsonResourceReloadListener<DamageChannelRegistry.ChannelDefinition> {

    public static final int MAX_CUSTOM_CHANNELS = 128;
    public static final int MAX_TRIGGER_TAGS_PER_CHANNEL = 32;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ChannelData UNTYPED_DATA = new ChannelData(
            new DamageChannel(DamageChannel.UNTYPED_ID, 0),
            List.of(),
            null,
            true,
            Integer.MIN_VALUE
    );
    private static volatile RegistryState STATE = RegistryState.initial();

    public DamageChannelRegistry(DamageNexusReloadAccess access) {
        super(ChannelDefinition.CODEC, FileToIdConverter.json("damagenexus_channels"));
        Objects.requireNonNull(access, "access")
                .requireFrameworkOwner("DamageChannelRegistry");
    }

    public static DamageChannel getPhysical() {
        return getChannelOrUntyped(DamageChannel.PHYSICAL_ID);
    }

    public static DamageChannel getFire() {
        return getChannelOrUntyped(DamageChannel.FIRE_ID);
    }

    public static DamageChannel getMagic() {
        return getChannelOrUntyped(DamageChannel.MAGIC_ID);
    }

    public static int channelCount() {
        return STATE.byIndex.length;
    }

    /** Monotonic content revision; unchanged when an equal snapshot reloads. */
    public static long contentRevision() {
        return STATE.contentRevision;
    }

    public static DamageChannel getUntyped() {
        return STATE.byIndex[0].channel;
    }

    public static DamageChannel getChannelOrUntyped(Identifier id) {
        RegistryState state = STATE;

        if (id == null) {
            return state.byIndex[0].channel;
        }

        ChannelData data = state.byId.get(id);
        return data != null ? data.channel : state.byIndex[0].channel;
    }

    public static boolean containsChannel(Identifier id) {
        return id != null && STATE.byId.containsKey(id);
    }

    public static DamageChannel resolve(DamageChannel channel) {
        return resolveData(channel, STATE).channel;
    }

    public static DamageChannel determineInitialChannel(DamageSource source) {
        RegistryState state = STATE;

        if (source == null) {
            return state.byIndex[0].channel;
        }

        for (ChannelData data : state.matchOrder) {
            for (TagKey<DamageType> tag : data.triggerTags) {
                if (source.is(tag)) {
                    return data.channel;
                }
            }
        }

        return state.byIndex[0].channel;
    }

    public static Holder<Attribute> getResistanceAttribute(DamageChannel rawChannel) {
        return resolveData(rawChannel, STATE).resistanceAttribute;
    }

    public static ChannelData getData(DamageChannel rawChannel) {
        return resolveData(rawChannel, STATE);
    }

    private static ChannelData resolveData(
            DamageChannel channel,
            RegistryState state
    ) {
        if (channel == null) {
            return state.byIndex[0];
        }

        int index = channel.index();

        if (index >= 0 && index < state.byIndex.length) {
            ChannelData data = state.byIndex[index];

            if (data.channel.id().equals(channel.id())) {
                return data;
            }
        }

        ChannelData byId = state.byId.get(channel.id());
        return byId != null ? byId : state.byIndex[0];
    }

    public static boolean isKnownRuntimeChannel(DamageChannel channel) {
        if (channel == null) {
            return false;
        }

        RegistryState state = STATE;

        int index = channel.index();

        if (index >= 0 && index < state.byIndex.length) {
            ChannelData data = state.byIndex[index];

            if (data.channel.id().equals(channel.id())) {
                return true;
            }
        }

        return state.byId.containsKey(channel.id());
    }

    public static boolean isCurrentRuntimeChannel(DamageChannel channel) {
        if (channel == null) {
            return false;
        }

        RegistryState state = STATE;
        int index = channel.index();

        if (index < 0 || index >= state.byIndex.length) {
            return false;
        }

        return state.byIndex[index].channel.id().equals(channel.id());
    }

    @Override
    protected void apply(
            Map<Identifier, ChannelDefinition> prepared,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        DamageNexusDiagnosticState.clearAll();
        RegistryState nextState = buildState(prepared);
        publish(nextState);

        DamageNexusLifecycleLog.channelsLoaded(
                STATE.byIndex.length, STATE.contentRevision);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    static void replaceStateForTesting(
            Map<Identifier, ChannelDefinition> prepared
    ) {
        publish(buildState(prepared));
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    static void resetStateForTesting() {
        STATE = RegistryState.initial();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        publish(RegistryState.initial());
    }

    static List<Identifier> matchOrderIdsForTesting() {
        return Arrays.stream(STATE.matchOrder)
                .map(data -> data.channel().id())
                .toList();
    }

    private static RegistryState buildState(
            Map<Identifier, ChannelDefinition> prepared
    ) {
        if (prepared == null) {
            throw new IllegalArgumentException(
                    "Damage channel definitions must not be null"
            );
        }

        if (prepared.size() > MAX_CUSTOM_CHANNELS) {
            throw new IllegalArgumentException(
                    "Damage channel reload exceeds maximum definition count: "
                            + prepared.size()
                            + " > "
                            + MAX_CUSTOM_CHANNELS
            );
        }

        for (Map.Entry<Identifier, ChannelDefinition> entry
                : prepared.entrySet()) {
            ChannelDefinition definition = Objects.requireNonNull(
                    entry.getValue(),
                    "Null channel definition from " + entry.getKey()
            );

            if (definition.triggerTags().size()
                    > MAX_TRIGGER_TAGS_PER_CHANNEL) {
                throw new IllegalArgumentException(
                        "Damage channel "
                                + definition.channel()
                                + " from "
                                + entry.getKey()
                                + " has "
                                + definition.triggerTags().size()
                                + " trigger tags; maximum="
                                + MAX_TRIGGER_TAGS_PER_CHANNEL
                );
            }
        }

        Map<Identifier, ChannelData> nextById = new HashMap<>();
        List<ChannelData> nextByIndex = new ArrayList<>();

        nextById.put(DamageChannel.UNTYPED_ID, UNTYPED_DATA);
        nextByIndex.add(UNTYPED_DATA);

        List<Map.Entry<Identifier, ChannelDefinition>> ordered =
                new ArrayList<>(prepared.entrySet());
        ordered.sort(
                Comparator
                        .comparing((Map.Entry<Identifier, ChannelDefinition> entry) ->
                                entry.getValue().channel().toString())
                        .thenComparing(entry -> entry.getKey().toString())
        );

        Map<Identifier, Identifier> sourceFileByChannel = new HashMap<>();

        for (Map.Entry<Identifier, ChannelDefinition> entry : ordered) {
            Identifier fileId = entry.getKey();
            ChannelDefinition def = entry.getValue();
            Identifier channelId = def.channel();

            if (channelId.equals(DamageChannel.UNTYPED_ID)) {
                LOGGER.warn(
                        "Ignoring datapack definition for reserved channel {} from {}",
                        channelId,
                        fileId
                );
                continue;
            }

            if (nextById.containsKey(channelId)) {
                LOGGER.warn(
                        "Duplicate DamageNexus channel definition {}: keeping file {}, skipping file {}.",
                        channelId,
                        sourceFileByChannel.get(channelId),
                        fileId
                );
                continue;
            }

            Holder<Attribute> resistanceAttr = null;

            if (def.resistanceAttribute().isPresent()) {
                Identifier attrId = def.resistanceAttribute().get();

                resistanceAttr = BuiltInRegistries.ATTRIBUTE
                        .get(attrId)
                        .orElse(null);

                if (resistanceAttr == null) {
                    LOGGER.warn(
                            "DamageNexus channel {} references missing resistance attribute {}",
                            channelId,
                            attrId
                    );
                }
            }

            int denseIndex = nextByIndex.size();
            DamageChannel channel = new DamageChannel(channelId, denseIndex);
            ChannelData data = new ChannelData(
                    channel,
                    List.copyOf(def.triggerTags()),
                    resistanceAttr,
                    def.affectedByArmor(),
                    def.priority()
            );

            nextById.put(channelId, data);
            sourceFileByChannel.put(channelId, fileId);
            nextByIndex.add(data);

            LOGGER.debug(
                    "Loaded DamageNexus channel {} with dense index {}",
                    channelId,
                    denseIndex
            );
        }

        ChannelData[] indexedArray = nextByIndex.toArray(ChannelData[]::new);

        ChannelData[] matchArray = nextByIndex.stream()
                .filter(data -> !data.channel.id().equals(DamageChannel.UNTYPED_ID))
                .sorted(
                        Comparator
                                .comparingInt(ChannelData::priority)
                                .reversed()
                                .thenComparing(data -> data.channel.id().toString())
                )
                .toArray(ChannelData[]::new);

        RegistryState nextState = new RegistryState(
                0L,
                Map.copyOf(nextById),
                indexedArray,
                matchArray
        );

        return nextState;
    }

    private static synchronized void publish(RegistryState candidate) {
        RegistryState previous = STATE;
        long revision = previous.sameContent(candidate)
                ? previous.contentRevision
                : Math.addExact(previous.contentRevision, 1L);
        STATE = candidate.withRevision(revision);
    }

    private record RegistryState(
            long contentRevision,
            Map<Identifier, ChannelData> byId,
            ChannelData[] byIndex,
            ChannelData[] matchOrder
    ) {
        static RegistryState initial() {
            return new RegistryState(
                    0L,
                    Map.of(DamageChannel.UNTYPED_ID, UNTYPED_DATA),
                    new ChannelData[]{UNTYPED_DATA},
                    new ChannelData[0]
            );
        }

        RegistryState withRevision(long revision) {
            return new RegistryState(revision, byId, byIndex, matchOrder);
        }

        boolean sameContent(RegistryState other) {
            return byId.equals(other.byId)
                    && Arrays.equals(matchOrder, other.matchOrder);
        }
    }

    public record ChannelDefinition(
            Identifier channel,
            List<TagKey<DamageType>> triggerTags,
            Optional<Identifier> resistanceAttribute,
            boolean affectedByArmor,
            int priority
    ) {
        public static final Codec<ChannelDefinition> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Identifier.CODEC
                                .fieldOf("channel")
                                .forGetter(ChannelDefinition::channel),

                        io.github.naimjeg.damagenexus.api.rule.DamageRuleLimits
                                .boundedList(
                                        TagKey.codec(
                                                Registries.DAMAGE_TYPE
                                        ),
                                        MAX_TRIGGER_TAGS_PER_CHANNEL,
                                        "damage channel trigger_tags"
                                )
                                .optionalFieldOf("trigger_tags", List.of())
                                .forGetter(ChannelDefinition::triggerTags),

                        Identifier.CODEC
                                .optionalFieldOf("resistance_attribute")
                                .forGetter(ChannelDefinition::resistanceAttribute),

                        Codec.BOOL
                                .optionalFieldOf("affected_by_armor", true)
                                .forGetter(ChannelDefinition::affectedByArmor),

                        Codec.INT
                                .optionalFieldOf("priority", 0)
                                .forGetter(ChannelDefinition::priority)
                ).apply(instance, ChannelDefinition::new));
    }

    public record ChannelData(
            DamageChannel channel,
            List<TagKey<DamageType>> triggerTags,
            Holder<Attribute> resistanceAttribute,
            boolean affectedByArmor,
            int priority
    ) {
    }
}

