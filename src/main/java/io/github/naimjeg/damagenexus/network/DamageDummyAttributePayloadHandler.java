package io.github.naimjeg.damagenexus.network;

import io.github.naimjeg.damagenexus.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeService;
import io.github.naimjeg.damagenexus.entity.DamageDummyAttributeSnapshot;
import io.github.naimjeg.damagenexus.entity.DamageDummyEntity;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import io.github.naimjeg.damagenexus.network.payload.DamageDummyApplyAttributesPayload;
import io.github.naimjeg.damagenexus.network.payload.DamageDummyAttributesPayload;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Main-thread, fail-closed server handler for attribute edit batches. */
public final class DamageDummyAttributePayloadHandler {

    private DamageDummyAttributePayloadHandler() {
    }

    public static void handleApply(
            DamageDummyApplyAttributesPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof DamageDummyMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId()) {
            sendCurrent(player, menu);
            return;
        }
        if (payload.anchorPos() == null
                || !menu.anchorPos().equals(payload.anchorPos())) {
            sendCurrent(player, menu);
            return;
        }
        if (!menu.stillValid(player)) {
            sendCurrent(player, menu);
            return;
        }

        ServerLevel level = player.level();
        if (!(level.getBlockEntity(menu.anchorPos())
                instanceof DamageDummyBlockEntity blockEntity)) {
            sendUnavailable(player, menu);
            return;
        }
        Optional<DamageDummyEntity> resolved =
                blockEntity.resolveManagedDummy(level);
        if (resolved.isEmpty()) {
            sendUnavailable(player, menu);
            return;
        }

        DamageDummyAttributeService.ApplyResult result =
                DamageDummyAttributeService.validateAndApply(
                        resolved.get(),
                        payload.edits()
                );
        if (result != DamageDummyAttributeService.ApplyResult.APPLIED) {
            sendCurrent(player, menu);
            return;
        }

        DamageDummyAttributeSnapshot snapshot =
                DamageDummyAttributeService.snapshot(level, blockEntity);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.containerMenu instanceof DamageDummyMenu viewerMenu
                    && viewerMenu.anchorPos().equals(menu.anchorPos())
                    && viewerMenu.stillValid(viewer)) {
                viewerMenu.replaceSnapshot(snapshot);
                PacketDistributor.sendToPlayer(
                        viewer,
                        new DamageDummyAttributesPayload(
                                viewerMenu.containerId,
                                snapshot
                        )
                );
            }
        }
    }

    private static void sendCurrent(
            ServerPlayer player,
            DamageDummyMenu menu
    ) {
        ServerLevel level = player.level();
        DamageDummyAttributeSnapshot snapshot;
        if (level.getBlockEntity(menu.anchorPos())
                instanceof DamageDummyBlockEntity blockEntity) {
            snapshot = DamageDummyAttributeService.snapshot(level, blockEntity);
        } else {
            snapshot = DamageDummyAttributeSnapshot.unavailable(
                    menu.anchorPos()
            );
        }
        send(player, menu, snapshot);
    }

    private static void sendUnavailable(
            ServerPlayer player,
            DamageDummyMenu menu
    ) {
        send(
                player,
                menu,
                DamageDummyAttributeSnapshot.unavailable(menu.anchorPos())
        );
    }

    private static void send(
            ServerPlayer player,
            DamageDummyMenu menu,
            DamageDummyAttributeSnapshot snapshot
    ) {
        menu.replaceSnapshot(snapshot);
        PacketDistributor.sendToPlayer(
                player,
                new DamageDummyAttributesPayload(menu.containerId, snapshot)
        );
    }
}
