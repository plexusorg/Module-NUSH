package dev.plex.nush;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

final class TrafficWindow
{
    private static final int HISTORY_SIZE = 300;
    private static final int LEARNING_DELAY_SECONDS = 60;

    private final int seconds;
    private final int minimum;
    private final long[] bucketTimes;
    private final int[] buckets;
    private final double[] history = new double[HISTORY_SIZE];
    private final Deque<Sample> pending = new ArrayDeque<>();
    private int historySize;
    private int historyIndex;
    private double baseline;
    private double noise = 1;

    TrafficWindow(int seconds, int minimum)
    {
        this.seconds = seconds;
        this.minimum = minimum;
        // Retain one extra bucket to sample the last complete window while new events arrive.
        bucketTimes = new long[seconds + 1];
        Arrays.fill(bucketTimes, -1);
        buckets = new int[seconds + 1];
    }

    void record(long now)
    {
        int index = (int) (now % buckets.length);
        if (bucketTimes[index] != now)
        {
            bucketTimes[index] = now;
            buckets[index] = 0;
        }
        if (buckets[index] < Integer.MAX_VALUE)
        {
            buckets[index]++;
        }
    }

    long count(long now)
    {
        long count = 0;
        for (int index = 0; index < buckets.length; index++)
        {
            if (bucketTimes[index] > now - seconds && bucketTimes[index] <= now)
            {
                count += buckets[index];
            }
        }
        return count;
    }

    long trigger()
    {
        return (long) Math.ceil(Math.max(minimum, Math.max(5 * baseline, baseline + 6 * noise)));
    }

    long recovery()
    {
        return (long) Math.ceil(Math.max(minimum / 2.0, Math.max(2 * baseline, baseline + 3 * noise)));
    }

    double baseline()
    {
        return baseline;
    }

    void freeze()
    {
        // Discard the lead-up to a detected burst, not the pre-raid history.
        pending.clear();
    }

    void learn(long now)
    {
        pending.addLast(new Sample(now, count(now - 1)));
        boolean changed = false;
        while (!pending.isEmpty() && pending.peekFirst().time() <= now - LEARNING_DELAY_SECONDS)
        {
            history[historyIndex] = pending.removeFirst().count();
            historyIndex = (historyIndex + 1) % HISTORY_SIZE;
            historySize = Math.min(historySize + 1, HISTORY_SIZE);
            changed = true;
        }
        if (changed)
        {
            double[] sorted = Arrays.copyOf(history, historySize);
            Arrays.sort(sorted);
            baseline = median(sorted);
            for (int index = 0; index < sorted.length; index++)
            {
                sorted[index] = Math.abs(sorted[index] - baseline);
            }
            Arrays.sort(sorted);
            // MAD resists isolated outliers. The count-noise floor also covers sparse, constant samples.
            noise = Math.max(1, Math.max(Math.sqrt(baseline), 1.4826 * median(sorted)));
        }
    }

    private static double median(double[] sorted)
    {
        int middle = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2 : sorted[middle];
    }

    private record Sample(long time, long count)
    {
    }
}
