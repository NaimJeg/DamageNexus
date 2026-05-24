package io.github.naimjeg.damagenexus.client.damage;

import io.github.naimjeg.damagenexus.network.payload.DamageNumberPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientDamageNumberManager {

    public static final int MAX_ACTIVE = 128;

    private static final float MIN_HEALTH_DAMAGE = 0.001F;
    private static final List<FloatingDamageNumber> ACTIVE =
            new ArrayList<>();
    private static ClientLevel lastLevel;

    private ClientDamageNumberManager() {
    }

    public static void spawn(DamageNumberPayload payload) {
        syncLevel();
        if (!isValid(payload)) {
            return;
        }

        if (ACTIVE.size() >= MAX_ACTIVE) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(FloatingDamageNumber.from(payload));
    }

    public static void tick() {
        syncLevel();

        for (FloatingDamageNumber number : ACTIVE) {
            number.tick();
        }
        ACTIVE.removeIf(FloatingDamageNumber::expired);
    }

    public static List<FloatingDamageNumber> active() {
        return Collections.unmodifiableList(ACTIVE);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static void syncLevel() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel current = minecraft == null ? null : minecraft.level;
        if (current != lastLevel) {
            ACTIVE.clear();
            lastLevel = current;
        }
    }

    private static boolean isValid(DamageNumberPayload payload) {
        if (payload == null) {
            return false;
        }

        return Float.isFinite(payload.damage())
                && payload.damage() > MIN_HEALTH_DAMAGE
                && Double.isFinite(payload.x())
                && Double.isFinite(payload.y())
                && Double.isFinite(payload.z());
    }
}
