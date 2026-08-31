package compass;

import java.util.concurrent.atomic.AtomicLong;

/** Rejects Swing work queued before a newer account/UI refresh. */
final class UiGenerationGuard
{
    private final AtomicLong generation = new AtomicLong();

    long next() { return generation.incrementAndGet(); }
    void invalidate() { generation.incrementAndGet(); }
    boolean isCurrent(long candidate) { return generation.get() == candidate; }
}
