package io.github.naimjeg.damagenexus.api.item.template;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Stable, payload-free reference to a complete entry template. */
public record DamageEntryTemplateReference(Identifier id) {
    public static final int MAX_ID_CODE_POINTS = 256;
    public static final Codec<DamageEntryTemplateReference> CODEC =
            Identifier.CODEC.flatXmap(
                    DamageEntryTemplateReference::decode,
                    reference -> DataResult.success(reference.id())
            );

    public DamageEntryTemplateReference {
        Objects.requireNonNull(id, "entry template id");
        if (id.toString().codePointCount(0, id.toString().length())
                > MAX_ID_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "Entry template ID exceeds maximum length: " + id);
        }
    }

    private static DataResult<DamageEntryTemplateReference> decode(
            Identifier id
    ) {
        try {
            return DataResult.success(new DamageEntryTemplateReference(id));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }
}
