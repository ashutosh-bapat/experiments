package com.ashutosh.utils;

import static org.junit.Assert.*;
import org.junit.Test;
import com.ashutosh.utils.EventTimeAggregator;

public class EventTimeAggregatorTest {

    @Test
    public void testUsingSleep() throws InterruptedException {
        testWithParams(5, 3, 100);
        testWithParams(1, 3, 100);
    }

    private void testWithParams(int period, int numPeriods, int sleepMS) throws InterruptedException {
        int i;
        EventTimeAggregator eta = new EventTimeAggregator("sleep tester", period);
        float totalExpectedTime = 0;
        float permissibleErrorPct = 5;

        for (i = 1; i <= period * numPeriods; i++) {
            eta.eventStart();
            Thread.sleep(sleepMS);
            eta.eventEnd();

            if (i % period == 0) {
                verifyExpectedValueWithError(eta.getPeriodicAverage(), sleepMS, permissibleErrorPct);
                totalExpectedTime = totalExpectedTime + eta.getPeriodicAverage() * period;
            }
        }
        verifyExpectedValueWithError(eta.getTotalAverage(), sleepMS, permissibleErrorPct);
        verifyExpectedValueWithError(eta.getTotalTime(), totalExpectedTime, permissibleErrorPct);
    }

    private void verifyExpectedValueWithError(float actualAvg, float expectedAvg, float errorPct) {
        float error = expectedAvg * errorPct / 100;
        assertTrue("Actual value " + actualAvg + " less than expected value " + expectedAvg + " by more than permissible error " + error,
                actualAvg >= expectedAvg - error);
        assertTrue("Actual value " + actualAvg + " more than expected value " + expectedAvg + " by more than permissible error " + error,
                actualAvg <= expectedAvg + error);
    }
}
