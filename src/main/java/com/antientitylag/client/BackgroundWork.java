package com.antientitylag.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The only background executor in the mod. It exists for one job: moving
 * profiling-export and config file writes off the client thread.
 *
 * <p>Hard rules, enforced here rather than by convention:
 * <ul>
 *   <li>Exactly one worker thread, never one per entity.</li>
 *   <li>Bounded queue (capacity 4). Overflow drops the <em>oldest</em> pending
 *   export — exports are diagnostics, never authoritative state, so dropping
 *   is always safe and is counted.</li>
 *   <li>Only immutable snapshots ({@code String} payloads) cross the thread
 *   boundary. No world, entity, or render state is ever touched here.</li>
 *   <li>Tasks are fire-and-forget file writes with a synchronous fallback:
 *   callers can run the same {@code Runnable} inline when the queue is full.</li>
 * </ul>
 */
public final class BackgroundWork {
    public static final int QUEUE_CAPACITY = 4;

    private static final AtomicLong dropped = new AtomicLong();
    private static final BlockingQueue<Runnable> QUEUE = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            0, 1, 30L, TimeUnit.SECONDS,
            QUEUE,
            task -> {
                Thread thread = new Thread(task, "AntiEntityLag-Export");
                thread.setDaemon(true);
                return thread;
            },
            (task, executor) -> {
                // Queue full: drop the oldest pending export, count it, retry.
                if (executor.getQueue().poll() != null) {
                    dropped.incrementAndGet();
                }
                executor.execute(task);
            });

    private BackgroundWork() {
    }

    /**
     * Submits file-IO work. Returns false when the caller should run the task
     * inline instead (shutdown in progress); the task is never silently lost.
     */
    public static boolean submitOrRunInline(Runnable task) {
        try {
            EXECUTOR.execute(task);
            return true;
        } catch (Exception rejected) {
            task.run();
            return false;
        }
    }

    public static long queueDepth() {
        return QUEUE.size();
    }

    public static long droppedCount() {
        return dropped.get();
    }

    static void resetForTests() {
        QUEUE.clear();
        dropped.set(0);
    }
}
