package com.ashutosh;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        System.out.println("Hello World");
        stopwatch.stop();
        System.out.println("Time elapsed in printing a string\n" + stopwatch.toString());
    }
}
