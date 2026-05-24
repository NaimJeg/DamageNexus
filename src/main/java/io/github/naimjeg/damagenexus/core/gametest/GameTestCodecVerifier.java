package io.github.naimjeg.damagenexus.core.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.RegistryOps;

/** Runtime-only verification against the active GameTest registries. */
public final class GameTestCodecVerifier {

    private GameTestCodecVerifier() {
    }

    public static void verifyFunctionInstance(GameTestHelper helper) {
        GameTestInstance instance = helper.testInfo.getTest();
        if (!(instance instanceof FunctionGameTestInstance)
                || instance.codec() != FunctionGameTestInstance.CODEC) {
            throw new AssertionError(
                    "Registered GameTest does not use its runtime codec: "
                            + instance.getClass().getName()
            );
        }

        RegistryOps<JsonElement> ops = RegistryOps.create(
                JsonOps.INSTANCE,
                helper.getLevel().registryAccess()
        );
        JsonElement encoded = GameTestInstance.DIRECT_CODEC
                .encodeStart(ops, instance)
                .getOrThrow(message -> new AssertionError(
                        "Unable to encode registered GameTest: " + message
                ));
        GameTestInstance decoded = GameTestInstance.DIRECT_CODEC
                .parse(ops, encoded)
                .getOrThrow(message -> new AssertionError(
                        "Unable to decode registered GameTest: " + message
                ));

        if (!(decoded instanceof FunctionGameTestInstance)
                || decoded.codec() != FunctionGameTestInstance.CODEC) {
            throw new AssertionError(
                    "GameTest codec round trip changed the runtime type"
            );
        }
        if (!decoded.structure().equals(instance.structure())
                || decoded.maxTicks() != instance.maxTicks()
                || decoded.setupTicks() != instance.setupTicks()
                || decoded.required() != instance.required()
                || decoded.rotation() != instance.rotation()) {
            throw new AssertionError(
                    "GameTest codec round trip changed TestData"
            );
        }
    }
}
