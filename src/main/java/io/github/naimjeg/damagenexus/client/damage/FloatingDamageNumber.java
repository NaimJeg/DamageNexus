package io.github.naimjeg.damagenexus.client.damage;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;

/**
 * One transient client-side damage number presentation instance.
 */
public final class FloatingDamageNumber {

    public static final int LIFETIME_TICKS = 24;
    public static final float BASE_WORLD_SCALE = 0.025F;
    public static final float CRITICAL_SCALE = 1.20F;

    private static final float RISE_BLOCKS = 0.45F;
    private static final float POP_START_TICKS = 3.0F;
    private static final float POP_SETTLE_TICKS = 6.0F;
    private static final float FADE_START_TICKS = 16.0F;
    private static final float FADE_DURATION_TICKS = 8.0F;

    private final long damageId;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    private final float damage;
    private final boolean critical;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    private int ageTicks;

    private FloatingDamageNumber(
            long damageId,
            double anchorX,
            double anchorY,
            double anchorZ,
            float damage,
            boolean critical
    ) {
        this.damageId = damageId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.damage = damage;
        this.critical = critical;

        long mixed = mix64(damageId);
        this.offsetX = unit(mixed) * 0.6F - 0.3F;
        this.offsetY = unit(mixed >>> 16) * 0.18F;
        this.offsetZ = unit(mixed >>> 32) * 0.3F - 0.15F;
    }

    public static FloatingDamageNumber from(DamageNumberPayload payload) {
        return new FloatingDamageNumber(
                payload.damageId(),
                payload.x(),
                payload.y(),
                payload.z(),
                payload.damage(),
                payload.critical()
        );
    }

    public long damageId() {
        return damageId;
    }

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public double anchorZ() {
        return anchorZ;
    }

    public float damage() {
        return damage;
    }

    public boolean critical() {
        return critical;
    }

    public int ageTicks() {
        return ageTicks;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public float offsetZ() {
        return offsetZ;
    }

    public void tick() {
        if (ageTicks < LIFETIME_TICKS) {
            ageTicks++;
        }
    }

    public boolean expired() {
        return ageTicks >= LIFETIME_TICKS;
    }

    public float normalized(float partialTick) {
        return clamp(
                (ageTicks + partialTick) / LIFETIME_TICKS,
                0.0F,
                1.0F
        );
    }

    public float popScale(float partialTick) {
        float age = ageTicks + partialTick;
        if (age <= POP_START_TICKS) {
            return lerp(
                    0.65F,
                    1.25F,
                    clamp(age / POP_START_TICKS, 0.0F, 1.0F)
            );
        }
        if (age <= POP_SETTLE_TICKS) {
            return lerp(
                    1.25F,
                    1.0F,
                    clamp(
                            (age - POP_START_TICKS)
                                    / (POP_SETTLE_TICKS - POP_START_TICKS),
                            0.0F,
                            1.0F
                    )
            );
        }
        return 1.0F;
    }

    public float alpha(float partialTick) {
        float age = ageTicks + partialTick;
        if (age <= FADE_START_TICKS) {
            return 1.0F;
        }
        return clamp(
                1.0F
                        - (age - FADE_START_TICKS)
                        / FADE_DURATION_TICKS,
                0.0F,
                1.0F
        );
    }

    public float worldScale(float partialTick) {
        float scale = BASE_WORLD_SCALE * popScale(partialTick);
        return critical ? scale * CRITICAL_SCALE : scale;
    }

    public double renderY(float partialTick) {
        return anchorY
                + offsetY
                + easeOutCubic(normalized(partialTick)) * RISE_BLOCKS;
    }

    public double renderX() {
        return anchorX + offsetX;
    }

    public double renderZ() {
        return anchorZ + offsetZ;
    }

    private static float easeOutCubic(float t) {
        float inverse = 1.0F - t;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float unit(long bits) {
        return (bits & 0xFFFFL) / 65535.0F;
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 33)) * 0xff51afd7ed558ccdL;
        mixed = (mixed ^ (mixed >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return mixed ^ (mixed >>> 33);
    }
}
