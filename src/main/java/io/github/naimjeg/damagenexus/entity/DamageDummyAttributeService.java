package io.github.naimjeg.damagenexus.entity;

import io.github.naimjeg.damagenexus.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * The single server/common service for resolving, snapshotting and editing a
 * pedestal's real {@link DamageDummyEntity} attribute map.
 */
public final class DamageDummyAttributeService {

    public enum ApplyResult {
        APPLIED,
        INVALID_BATCH
    }

    private record ValidatedEdit(
            AttributeInstance instance,
            double sanitizedValue
    ) {
    }

    private DamageDummyAttributeService() {
    }

    public static DamageDummyAttributeSnapshot snapshot(
            ServerLevel level,
            DamageDummyBlockEntity blockEntity
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(blockEntity, "blockEntity");
        BlockPos anchorPos = blockEntity.getBlockPos();
        return blockEntity.resolveManagedDummy(level)
                .map(dummy -> snapshot(anchorPos, dummy))
                .orElseGet(() ->
                        DamageDummyAttributeSnapshot.unavailable(anchorPos));
    }

    public static DamageDummyAttributeSnapshot snapshot(
            BlockPos anchorPos,
            DamageDummyEntity dummy
    ) {
        List<DamageDummyAttributeView> views = DamageDummyAttributes
                .availableAttributes(dummy)
                .stream()
                .map(entry -> new DamageDummyAttributeView(
                        entry.id(),
                        entry.attribute().value().getDescriptionId(),
                        entry.baseValue(),
                        entry.value(),
                        entry.attribute().value().getDefaultValue()
                ))
                .toList();
        return new DamageDummyAttributeSnapshot(anchorPos, true, views);
    }

    /**
     * Fully validates the batch before touching any instance. Unknown IDs,
     * missing instances, duplicates and non-finite values reject everything.
     */
    public static ApplyResult validateAndApply(
            DamageDummyEntity dummy,
            List<DamageDummyAttributeEdit> edits
    ) {
        Objects.requireNonNull(dummy, "dummy");
        if (edits == null
                || edits.size() > DamageDummyAttributeProtocol.MAX_ATTRIBUTES) {
            return ApplyResult.INVALID_BATCH;
        }

        Set<net.minecraft.resources.Identifier> seen = new HashSet<>();
        for (DamageDummyAttributeEdit edit : edits) {
            if (edit == null
                    || edit.attributeId() == null
                    || !seen.add(edit.attributeId())) {
                return ApplyResult.INVALID_BATCH;
            }
        }

        List<ValidatedEdit> validated = new ArrayList<>(edits.size());
        for (DamageDummyAttributeEdit edit : edits) {
            Holder.Reference<Attribute> holder = BuiltInRegistries.ATTRIBUTE
                    .get(edit.attributeId())
                    .orElse(null);
            if (holder == null) {
                return ApplyResult.INVALID_BATCH;
            }
            AttributeInstance instance = dummy.getAttribute(holder);
            if (instance == null) {
                return ApplyResult.INVALID_BATCH;
            }
            if (!Double.isFinite(edit.requestedBaseValue())) {
                return ApplyResult.INVALID_BATCH;
            }
            double sanitized = holder.value().sanitizeValue(
                    edit.requestedBaseValue()
            );
            if (!Double.isFinite(sanitized)) {
                return ApplyResult.INVALID_BATCH;
            }
            validated.add(new ValidatedEdit(instance, sanitized));
        }

        for (ValidatedEdit edit : validated) {
            edit.instance().setBaseValue(edit.sanitizedValue());
        }
        return ApplyResult.APPLIED;
    }

    /** Request identity check kept separate so network validation is testable. */
    public static boolean requestMatchesMenu(
            DamageDummyMenu menu,
            int containerId,
            BlockPos anchorPos
    ) {
        return menu != null
                && anchorPos != null
                && menu.containerId == containerId
                && menu.anchorPos().equals(anchorPos);
    }
}
