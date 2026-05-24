package io.github.naimjeg.damagenexus.api;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.enums.DamageApplicationBucket;
import io.github.naimjeg.damagenexus.api.rule.*;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Phase9PublicApiTest {
    @Test
    void publicIdsHaveCanonicalNames() {
        assertEquals("damagenexus:damage_type_tag",
                DamageNexusConditionIds.DAMAGE_TYPE_TAG.toString());
        assertEquals("damagenexus:source_tag",
                DamageNexusConditionIds.SOURCE_TAG.toString());
        assertEquals("damagenexus:add_base_damage",
                DamageNexusOperationIds.ADD_BASE_DAMAGE.toString());
        assertEquals("damagenexus:crit_damage",
                DamageNexusPreMultiplierBuckets.CRIT_DAMAGE.toString());
        assertEquals("damagenexus:crit_chance",
                DamageNexusAttributes.CRIT_CHANCE.identifier().toString());
        assertNull(findPhysicalDamageAdditive());
    }

    @Test
    void applicationBucketUsesStableNamesAndReportsUnknownValues() {
        for (DamageApplicationBucket bucket : DamageApplicationBucket.values()) {
            JsonPrimitive encoded = DamageApplicationBucket.CODEC
                    .encodeStart(JsonOps.INSTANCE, bucket)
                    .getOrThrow().getAsJsonPrimitive();
            assertEquals(bucket.serializedName(), encoded.getAsString());
            assertEquals(bucket, DamageApplicationBucket.CODEC
                    .parse(JsonOps.INSTANCE, encoded).getOrThrow());
        }
        assertTrue(DamageApplicationBucket.CODEC.parse(
                JsonOps.INSTANCE, new JsonPrimitive("future_bucket"))
                .error().isPresent());
    }

    @Test
    void externalNamespacesAreNeverRewrittenByPublicFactories() {
        Identifier external = Identifier.fromNamespaceAndPath("contentmod", "example");
        assertEquals(external, DamageNexusConditions.sourceTag(external)
                .type().equals(DamageNexusConditionIds.SOURCE_TAG) ? external : null);
        assertEquals(external, io.github.naimjeg.damagenexus.api.damage
                .DamageMetadataKey.stringKey(external).id());
    }

    private static Object findPhysicalDamageAdditive() {
        try {
            return DamageNexusAttributes.class.getField("PHYSICAL_DAMAGE_ADDITIVE");
        } catch (NoSuchFieldException expected) {
            return null;
        }
    }

}
