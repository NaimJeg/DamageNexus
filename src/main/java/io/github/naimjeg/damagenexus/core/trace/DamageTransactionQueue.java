package io.github.naimjeg.damagenexus.core.trace;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DamageTransactionQueue {

    private final Deque<DamageNexusTransaction> entries =
            new ArrayDeque<>();

    public Deque<DamageNexusTransaction> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public void addLast(DamageNexusTransaction tx) {
        entries.addLast(tx);
    }

    public DamageNexusTransaction removeFirst() {
        return entries.removeFirst();
    }
}
