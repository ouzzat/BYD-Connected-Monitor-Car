package com.asmontec.bydavm.car.mqtt;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File d'attente en mémoire, bornée, pour les messages qui n'ont pas pu être
 * publiés faute de réseau. Les messages les plus anciens sont abandonnés en
 * premier si la file est pleine (conservation "temporaire", pas un journal
 * persistant illimité).
 */
public final class OfflineQueue {

    public static final class Pending {
        public final String topic;
        public final byte[] payload;

        public Pending(String topic, byte[] payload) {
            this.topic = topic;
            this.payload = payload;
        }
    }

    private final int maxSize;
    private final ArrayDeque<Pending> queue = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public OfflineQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public void offer(String topic, byte[] payload) {
        lock.lock();
        try {
            if (queue.size() >= maxSize) {
                queue.pollFirst();
            }
            queue.addLast(new Pending(topic, payload));
        } finally {
            lock.unlock();
        }
    }

    public Pending poll() {
        lock.lock();
        try {
            return queue.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}
