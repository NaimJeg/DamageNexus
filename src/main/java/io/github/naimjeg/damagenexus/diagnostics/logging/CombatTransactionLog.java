package io.github.naimjeg.damagenexus.diagnostics.logging;

public interface CombatTransactionLog {

    void begin(
            String attackerName,
            String victimName,
            String sourceId,
            String initialChannel,
            float eventOriginalAmount,
            float initialBaseAmount
    );

    void preNexus(float amount);

    void apply(
            float eventOriginalAmount,
            float initialBaseAmount,
            float offensiveTotal,
            float finalEventAmount
    );

    void end();
}

