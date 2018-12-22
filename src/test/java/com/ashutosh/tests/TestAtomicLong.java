package com.ashutosh.tests;

import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;
import org.junit.Test;

public class TestAtomicLong {
    @Test
    public void testSimpleOperation() {
        AtomicLong cnt = new AtomicLong();
        assertEquals(cnt.get(), 0);
        assertEquals(cnt.getAndIncrement(), 0);
        assertEquals(cnt.get(), 1);
    }

    @Test
    public void testThreadedOperation() throws InterruptedException {
       Thread[] threads = new Thread[3];
       AtomicLong sharedCounter = new AtomicLong();

       threads[0] = new Thread(new TestThread(sharedCounter), "thread1");
       threads[1] = new Thread(new TestThread(sharedCounter), "thread2");
       threads[2] = new Thread(new TestThread(sharedCounter), "thread3");

       threads[0].start();
       threads[1].start();
       threads[2].start();

       threads[0].join();
       threads[1].join();
       threads[2].join();

       assertEquals(sharedCounter.get(), 6);
    }

    private class TestThread implements Runnable {
        private AtomicLong sharedCounter;

        public TestThread(AtomicLong sharedCounter) {
            this.sharedCounter = sharedCounter;
        }

        @Override
        public void run() {
            try {
                sharedCounter.getAndIncrement();
                Thread.sleep(10);
                sharedCounter.getAndIncrement();
                Thread.sleep(10);
            } catch (Exception e) {
                // Do nothing, assert would fail in case the counter wasn't incremented.
            }
        }
    }
}
