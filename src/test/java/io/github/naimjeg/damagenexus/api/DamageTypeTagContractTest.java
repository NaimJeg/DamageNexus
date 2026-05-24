package io.github.naimjeg.damagenexus.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DamageTypeTagContractTest {
    @Test
    void damageTypeTagsExposeOnlyCanonicalNames() throws Exception {
        assertFalse(Files.exists(Path.of(
                "src/main/resources/data/damagenexus/tags/damage_type/spear_charge.json"
        )));
        JsonArray canonical = values("is_spear_charge");
        assertFalse(canonical.toString().contains("damagenexus:spear_charge"));
        assertEquals("damagenexus:is_mace_smash",
                DamageNexusTags.DamageTypes.IS_MACE_SMASH.location().toString());
    }

    @Test
    void poisonAndMagicTagsDelegateToNeoForgeConventions() throws Exception {
        assertEquals("[\"#neoforge:is_poison\"]", values("is_poison").toString());
        assertEquals("[\"#neoforge:is_magic\"]", values("is_magic").toString());
    }

    private static JsonArray values(String name) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/damagenexus/tags/damage_type/"
                        + name + ".json"))).getAsJsonObject().getAsJsonArray("values");
    }
}
