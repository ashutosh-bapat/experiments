// It maintains a running average as well as a periodic average of time required to
// execute a given even event/method across multiple invocations/occurances of
// that event. It then periodically reports the average time of execution for
// the last period. The protocol to use this class is
// EventTimeAggregator eta = new EventTimeAggregator(name, period); The object
// created using this invocation can be used to track the execution time for
// multiple invocations of the given event/method. For every invocation one has
// to call start and end methods before and after the event or method like
// eta.start();
// ... event/method invocation goes here
// eta.end();
//
// After every period invocations, it will report the average time for given
// period. The average for a given period window or that for all the previous
// runs can be obtained by using eta.getPeriodicAverage() or eta.getAverage()
// resp.

package com.ashutosh.utils;

import org.apache.commons.lang3.time.StopWatch;

public class EventTimeAggregator
{
	private String eventName;
	private long period;
	private long prdNumEvents;
	private long totalNumEvents;
	private StopWatch prdStopWatch;
	private StopWatch totalStopWatch;
	private float prdAvgCorrection;
	private float totalAvgCorrection;
	private final int prdInitNumEvents = 10;
	private final int totalInitNumEvents = 10;

	public EventTimeAggregator(String eventNameIn, long periodIn)
	{
		eventName = eventNameIn;
		period = periodIn;
		prdStopWatch = new StopWatch();
		totalStopWatch = new StopWatch();
		prdNumEvents = 0;
		totalNumEvents = 0;

		// Start and suspend both the stopwatches. The time required for doing
		// that will be an overhead, we will incur with every measurement. This
		// might be useful for providing more accurate timings. Also it helps to
		// use resume and suspend methods later to accumulate timing for every
		// invocation of event/method. Do this a multiple times and take
		// an average for correction.
		prdAvgCorrection = calcAvgCorrection(prdStopWatch, prdInitNumEvents);
		totalAvgCorrection = calcAvgCorrection(totalStopWatch, totalInitNumEvents);
	}

	// Calculate the average time required to start and stop or resume and
	// suspend the given stopwatch. As a side effect the given stopwatch is in
	// suspended state.
	private float calcAvgCorrection(StopWatch sw, int numEvents)
	{
		int	cnt;

		sw.start();
		sw.suspend();

		for (cnt = 1; cnt < numEvents; cnt++)
		{
			sw.resume();
			sw.suspend();
		}

		// Assume that the time elapsed between start and suspend is same as that between resume and suspend.
		// TODO: revisit this assumption. Also it might be better to use nanosecond accuracy while calculating the
		// average, instead of millisecond, which may not capture the time between resume and suspend.
		return ((float) sw.getTime())/numEvents;
	}

	public void eventStart() throws IllegalStateException
	{
		// If this aggregator has been already started, the stopwatch would have
		// been already started. In that case, start() below will throw an
		// exception. The order is crucial here since we will need to apply
		// resume/suspend correction for one stopwatch to the other.
		prdStopWatch.resume();
		totalStopWatch.resume();
	}

	public void eventEnd() throws IllegalStateException
	{
		// The order is crucial here since we will need to apply resume/suspend
		// correction for one stopwatch to the other.
		totalStopWatch.suspend();
		prdStopWatch.suspend();
		prdNumEvents++;
		totalNumEvents++;

		if (prdNumEvents % period == 0)
		{
			prdStopWatch.reset();

			// We need the stopwatch to be in suspended state again, so 
			prdAvgCorrection = calcAvgCorrection(prdStopWatch, prdInitNumEvents);
			prdNumEvents = 0;
		}
	}

	public float getTotalTime() {
		// Adjust for starting, stopping and suspending the timer
		return totalStopWatch.getTime() - totalAvgCorrection * (totalInitNumEvents + totalNumEvents);
	}

	public float getTotalAverage() {
		return ((float) getTotalTime())/ totalNumEvents;
	}

	public float getPeriodicAverage()
	{
		float prdTotalTime = prdStopWatch.getTime();

		// Adjust the corrections for initial timing measurement as well as
		// correction for all the events.
		prdTotalTime = prdTotalTime - prdAvgCorrection * (prdInitNumEvents + prdNumEvents);

		// Adjust the correction for resuming and suspeding stopwatch measuring
		// all events.
		prdTotalTime = prdTotalTime - totalAvgCorrection * prdNumEvents;

		return prdTotalTime / prdNumEvents;
	}
}
