package com.benenv.rty813.cleanship.util;

import android.support.annotation.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author doufu
 * @date 2018-03-24
 */

public class WriteSerialThreadFactory implements ThreadFactory {
    private final AtomicInteger mNumber = new AtomicInteger();

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        return new Thread(runnable, "WriteSerialThread" + "-" + mNumber.getAndIncrement()) {
            @Override
            public void run() {
                super.run();
            }
        };
    }
}
