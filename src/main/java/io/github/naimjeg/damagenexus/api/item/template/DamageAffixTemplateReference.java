package io.github.naimjeg.damagenexus.api.item.template;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Stable, payload-free reference to a complete affix template. */
public record DamageAffixTemplateReference(Identifier id) {
    public static final int MAX_ID_CODE_POINTS = 256;
    public static final Codec<DamageAffixTemplateReference> CODEC =
            Identifier.CODEC.flatXmap(
                    DamageAffixTemplateReference::decode,
                    reference -> DataResult.success(reference.id())
            );

    public DamageAffixTemplateReference {
        Objects.requireNonNull(id, "affix template id");
        if (id.toString().codePointCount(0, id.toString().length())
                > MAX_ID_CODE_POINTS) {
            throw new IllegalArgumentException(
                    "Affix template ID exceeds maximum length: " + id);
        }
    }

    private static DataResult<DamageAffixTemplateReference> decode(
            Identifier id
    ) {
        try {
            return DataResult.success(new DamageAffixTemplateReference(id));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }
}
