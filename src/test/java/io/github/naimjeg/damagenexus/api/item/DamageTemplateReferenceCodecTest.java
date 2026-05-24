package io.github.naimjeg.damagenexus.api.item;

import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.item.template.DamageAffixTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.damagenexus.api.item.template.DamageItemTemplateReferences;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DamageTemplateReferenceCodecTest {
    @Test
    void unresolvedReferencesRoundTripStorageAndNetworkInDeclarationOrder() {
        DamageItemTemplateReferences value = new DamageItemTemplateReferences(
                List.of(entry("first"), entry("first"), entry("second")),
                List.of(affix("unknown"))
        );
        var json = DamageItemTemplateReferences.CODEC
                .encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        assertEquals(value, DamageItemTemplateReferences.CODEC
                .parse(JsonOps.INSTANCE, json).getOrThrow());
        var nbt = DamageItemTemplateReferences.CODEC
                .encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        assertEquals(value, DamageItemTemplateReferences.CODEC
                .parse(NbtOps.INSTANCE, nbt).getOrThrow());

        ByteBuf buffer = Unpooled.buffer();
        try {
            DamageItemTemplateReferences.NETWORK_CODEC.encode(buffer, value);
            assertEquals(value,
                    DamageItemTemplateReferences.NETWORK_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void listsAreImmutableRejectNullsAndEnforceBounds() {
        var mutable = new java.util.ArrayList<>(List.of(entry("one")));
        DamageItemTemplateReferences value =
                new DamageItemTemplateReferences(mutable, List.of());
        mutable.clear();
        assertEquals(1, value.entries().size());
        assertThrows(UnsupportedOperationException.class,
                () -> value.entries().add(entry("two")));
        assertThrows(NullPointerException.class,
                () -> new DamageItemTemplateReferences(
                        Collections.singletonList(null), List.of()));
        assertThrows(NullPointerException.class,
                () -> new DamageEntryTemplateReference(null));
        assertThrows(NullPointerException.class,
                () -> new DamageAffixTemplateReference(null));
        assertThrows(IllegalArgumentException.class,
                () -> new DamageItemTemplateReferences(
                        java.util.stream.IntStream.rangeClosed(
                                        0,
                                        DamageItemTemplateReferences
                                                .MAX_ENTRY_REFERENCES)
                                .mapToObj(index -> entry("v" + index))
                                .toList(),
                        List.of()));
    }

    @Test
    void materializedRecordHasTwoComponents() {
        assertArrayEquals(
                new String[]{"entries", "affixes"},
                java.util.Arrays.stream(
                                DamageNexusItemEntries.class
                                        .getRecordComponents())
                        .map(java.lang.reflect.RecordComponent::getName)
                        .toArray(String[]::new));
    }

    @Test
    void productionResourcesContainNoStaticTemplates() throws Exception {
        for (String directory : List.of(
                "damagenexus_entry_templates",
                "damagenexus_affix_templates")) {
            Path root = Path.of("src/main/resources/data");
            try (var files = Files.walk(root)) {
                assertTrue(files.noneMatch(path -> path.toString()
                                .replace('\\', '/').contains("/" + directory + "/")
                                && path.toString().endsWith(".json")),
                        "Production static template resource found in " + directory);
            }
        }
    }

    @Test
    void itemApiExposesExplicitMaterializedAccessorNames() throws Exception {
        assertEquals(List.class, DamageNexusItemApi.class
                .getMethod("getMaterializedEntries",
                        net.minecraft.world.item.ItemStack.class)
                .getReturnType());
        assertEquals(List.class, DamageNexusItemApi.class
                .getMethod("getResolvedMaterializedEntries",
                        net.minecraft.world.item.ItemStack.class)
                .getReturnType());
        assertEquals(List.class, DamageNexusItemApi.class
                .getMethod("getMaterializedAffixes",
                        net.minecraft.world.item.ItemStack.class)
                .getReturnType());
        assertEquals(List.class, DamageNexusItemApi.class
                .getMethod("getResolvedMaterializedAffixes",
                        net.minecraft.world.item.ItemStack.class)
                .getReturnType());
        for (String removed : List.of(
                "getEntries",
                "getResolvedEntries",
                "getAffixes",
                "getResolvedAffixes"
        )) {
            assertThrows(NoSuchMethodException.class,
                    () -> DamageNexusItemApi.class.getMethod(
                            removed,
                            net.minecraft.world.item.ItemStack.class
                    ));
        }
    }

    private static DamageEntryTemplateReference entry(String path) {
        return new DamageEntryTemplateReference(id(path));
    }

    private static DamageAffixTemplateReference affix(String path) {
        return new DamageAffixTemplateReference(id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("contentmod", path);
    }
}
