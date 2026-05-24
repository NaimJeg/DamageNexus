package io.github.naimjeg.damagenexus.core.rule;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleDefinition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.AllOfCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.RequestKindIsCondition;
import io.github.naimjeg.damagenexus.builtin.rule.condition.SourceActionIsCondition;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Phase6DatapackReloadTest {

    @AfterEach
    void reset() {
        DatapackDamageRuleStore.replace(List.of());
    }

    @Test
    void realJsonCodecPublishesNewConditionsAndFailureKeepsSnapshot() {
        String validJson = """
                {
                  "id": "contentmod:phase6_rule",
                  "phase": "final_override",
                  "conditions": [{
                    "type": "damagenexus:all_of",
                    "conditions": [
                      {"type":"damagenexus:source_action_is","action":"contentmod:action"},
                      {"type":"damagenexus:source_tag","tag":"contentmod:tag"},
                      {"type":"damagenexus:request_kind_is","kind":"proc"},
                      {"type":"damagenexus:has_parent_damage"}
                    ]
                  }],
                  "operations": [{"type":"damagenexus:cancel_damage"}]
                }
                """;
        DamageRuleDefinition decoded = DamageRuleDefinition.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString(validJson)
        ).getOrThrow();
        assertTrue(DatapackDamageRuleReloadListener.applyPreparedForTesting(
                Map.of(id("contentmod", "phase6_file"), decoded)
        ));
        assertEquals(List.of(decoded), DatapackDamageRuleStore.rules());
        AllOfCondition all = (AllOfCondition) decoded.conditions().getFirst();
        assertInstanceOf(SourceActionIsCondition.class, all.conditions().get(0));
        assertInstanceOf(RequestKindIsCondition.class, all.conditions().get(2));

        String invalidJson = validJson.replace("\"proc\"", "\"future_kind\"");
        assertTrue(DamageRuleDefinition.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString(invalidJson)
        ).error().isPresent());
        assertEquals(
                List.of(decoded),
                DatapackDamageRuleStore.rules(),
                "failed decode must not replace the prior published snapshot"
        );
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
