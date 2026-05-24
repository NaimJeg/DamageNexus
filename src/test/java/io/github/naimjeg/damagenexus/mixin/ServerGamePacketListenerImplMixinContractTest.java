package io.github.naimjeg.damagenexus.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerGamePacketListenerImplMixinContractTest {

    private static final String TARGET_OWNER =
            "net/minecraft/network/protocol/PacketUtils";
    private static final String TARGET_NAME =
            "ensureRunningOnSameThread";
    private static final String TARGET_DESCRIPTOR =
            "(Lnet/minecraft/network/protocol/Packet;"
                    + "Lnet/minecraft/network/PacketListener;"
                    + "Lnet/minecraft/server/level/ServerLevel;)V";
    private static final String HANDLER_DESCRIPTOR =
            "(Lnet/minecraft/network/protocol/game/"
                    + "ServerboundSetCreativeModeSlotPacket;)V";

    @Test
    void currentTargetContainsExactlyOnePostThreadSwitchAnchor()
            throws Exception {
        AtomicInteger matches = new AtomicInteger();
        AtomicInteger handlers = new AtomicInteger();
        AtomicInteger instruction = new AtomicInteger();
        AtomicInteger threadSwitch = new AtomicInteger(-1);
        AtomicInteger itemRead = new AtomicInteger(-1);
        AtomicInteger inventoryWrite = new AtomicInteger(-1);

        try (InputStream input = resource(
                "net/minecraft/server/network/"
                        + "ServerGamePacketListenerImpl.class"
        )) {
            new ClassReader(input).accept(
                    new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(
                                int access,
                                String name,
                                String descriptor,
                                String signature,
                                String[] exceptions
                        ) {
                            if (!"handleSetCreativeModeSlot".equals(name)
                                    || !HANDLER_DESCRIPTOR.equals(
                                    descriptor
                            )) {
                                return null;
                            }

                            handlers.incrementAndGet();

                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitMethodInsn(
                                        int opcode,
                                        String owner,
                                        String name,
                                        String descriptor,
                                        boolean isInterface
                                ) {
                                    int index = instruction.getAndIncrement();

                                    if (opcode == Opcodes.INVOKESTATIC
                                            && TARGET_OWNER.equals(owner)
                                            && TARGET_NAME.equals(name)
                                            && TARGET_DESCRIPTOR.equals(
                                            descriptor
                                    )) {
                                        matches.incrementAndGet();
                                        threadSwitch.set(index);
                                    }

                                    if ("net/minecraft/network/protocol/game/"
                                            .concat("ServerboundSetCreativeModeSlotPacket")
                                            .equals(owner)
                                            && "itemStack".equals(name)) {
                                        itemRead.compareAndSet(-1, index);
                                    }

                                    if ("net/minecraft/world/inventory/Slot"
                                            .equals(owner)
                                            && "setByPlayer".equals(name)) {
                                        inventoryWrite.compareAndSet(-1, index);
                                    }
                                }
                            };
                        }
                    },
                    ClassReader.SKIP_DEBUG
                            | ClassReader.SKIP_FRAMES
            );
        }

        assertEquals(
                1,
                matches.get(),
                "security injection anchor must match exactly once"
        );
        assertEquals(1, handlers.get(), "handler descriptor changed");
        assertTrue(threadSwitch.get() >= 0);
        assertTrue(threadSwitch.get() < itemRead.get());
        assertTrue(itemRead.get() < inventoryWrite.get());
    }

    @Test
    void mixinIsFailClosedAndExplicitlyRequiresItsInjection()
            throws Exception {
        Class<?> mixinClass = Class.forName(
                "io.github.naimjeg.damagenexus.mixin."
                        + "ServerGamePacketListenerImplMixin"
        );
        Method injector = mixinClass.getDeclaredMethod(
                "damageNexus$sanitizeCreativeItem",
                Class.forName(
                        "net.minecraft.network.protocol.game."
                                + "ServerboundSetCreativeModeSlotPacket"
                ),
                Class.forName(
                        "org.spongepowered.asm.mixin.injection.callback."
                                + "CallbackInfo"
                )
        );
        Inject inject = injector.getAnnotation(Inject.class);

        assertEquals(1, inject.require());
        assertEquals(1, inject.allow());
        assertEquals(1, inject.expect());
        assertEquals(1, inject.method().length);
        assertEquals("handleSetCreativeModeSlot", inject.method()[0]);
        assertEquals(1, inject.at().length);

        At at = inject.at()[0];
        assertEquals("INVOKE", at.value());
        assertEquals(At.Shift.AFTER, at.shift());
        assertEquals(
                "L" + TARGET_OWNER + ";" + TARGET_NAME
                        + TARGET_DESCRIPTOR,
                at.target()
        );

        var json = JsonParser.parseReader(new InputStreamReader(
                resource("damagenexus.mixins.json"),
                StandardCharsets.UTF_8
        )).getAsJsonObject();

        assertTrue(json.get("required").getAsBoolean());
        assertTrue(json.getAsJsonArray("mixins")
                .asList()
                .stream()
                .anyMatch(element ->
                        "ServerGamePacketListenerImplMixin".equals(
                                element.getAsString()
                        )));
        assertEquals(
                1,
                json.getAsJsonObject("injectors")
                        .get("defaultRequire")
                        .getAsInt()
        );

        String modMetadata;

        try (InputStream input = resource(
                "META-INF/neoforge.mods.toml"
        )) {
            modMetadata = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        assertTrue(modMetadata.contains("[[mixins]]"));
        assertTrue(modMetadata.contains(
                "config=\"damagenexus.mixins.json\""
        ));

        try (InputStream ignored = resource(
                "io/github/naimjeg/damagenexus/mixin/"
                        + "ServerGamePacketListenerImplMixin.class"
        )) {
            assertTrue(ignored.read() >= 0);
        }
    }

    private static InputStream resource(String path) {
        InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path);

        if (input == null) {
            throw new AssertionError(
                    "Missing target class resource " + path
            );
        }

        return input;
    }
}
