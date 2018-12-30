package com.ashutosh.utils;

import static org.junit.Assert.*;
import org.junit.Test;
import com.ashutosh.utils.EventTimeAggregator;

public class EventTimeAggregatorTest {

    @Test
    public void testUsingSleep() throws InterruptedException {
        testWithParams(5, 3, 20);
        testWithParams(1, 3, 20);
    }

    private void testWithParams(int period, int numPeriods, int sleepMS) throws InterruptedException {
        int i;
        EventTimeAggregator eta = new EventTimeAggregator("sleep tester", period);
        int totalExpectedTime = 0;

        for (i = 1; i <= period * numPeriods; i++) {
            eta.eventStart();
            Thread.sleep(sleepMS);
            eta.eventEnd();

            if (i % period == 0) {
                assertEquals(sleepMS, (int) eta.getPeriodicAverage());
            }
            totalExpectedTime = totalExpectedTime + sleepMS;
        }
        assertEquals(sleepMS, (int) eta.getTotalAverage());
        assertTrue("Error exceeded 1 millisecond accuracy.",
                totalExpectedTime <= (int) eta.getTotalTime() && totalExpectedTime + 1 >= eta.getTotalTime());
    }
}
