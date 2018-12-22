package com.ashutosh.utils;

import static org.junit.Assert.*;
import org.junit.Test;
import com.ashutosh.utils.EventTimeAggregator;

public class EventTimeAggregatorTest {

    @Test
    public void testUsingSleep() throws InterruptedException {
        int period = 5;
        int numPeriods = 3;
        int sleepMS = 20;
        int i;
        EventTimeAggregator eta = new EventTimeAggregator("sleep tester", period);
        int totalExpectedTime = 0;

        for (i = 0; i < period * numPeriods; i++) {
            eta.eventStart();
            Thread.sleep(sleepMS);
            eta.eventEnd();

            if (i % period == 0) {
                assertEquals((int) eta.getPeriodicAverage(), sleepMS);
            }
            totalExpectedTime = totalExpectedTime + sleepMS;
        }
        assertEquals((int) eta.getTotalAverage(), sleepMS);
        assertTrue("Error exceeded 1 millisecond accuracy.",
                totalExpectedTime <= (int) eta.getTotalTime() && totalExpectedTime + 1 >= eta.getTotalTime());
    }
}
