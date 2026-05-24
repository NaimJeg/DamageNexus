package io.github.naimjeg.damagenexus.mixin;

import io.github.naimjeg.damagenexus.core.security.DamageNexusItemSecurity;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.1.2 / NeoForge 26.1.2.75 has no cancellable server event for
 * creative slot packets. Vanilla accepts packet.itemStack() directly in
 * handleSetCreativeModeSlot, so this is the narrowest authoritative ingress
 * hook. The injection is immediately after PacketUtils' thread handoff:
 * network-thread invocations schedule and abort inside that call, while the
 * server-thread re-entry reaches this filter before packet.itemStack() is
 * read or written to the inventory.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleSetCreativeModeSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;"
                            + "ensureRunningOnSameThread("
                            + "Lnet/minecraft/network/protocol/Packet;"
                            + "Lnet/minecraft/network/PacketListener;"
                            + "Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1,
            expect = 1,
            allow = 1
    )
    private void damageNexus$sanitizeCreativeItem(
            ServerboundSetCreativeModeSlotPacket packet,
            CallbackInfo callbackInfo
    ) {
        DamageNexusItemSecurity.sanitizeCreativeInbound(
                player,
                packet.itemStack()
        );
    }
}
