package com.ashutosh.DynaThreading;

import com.ashutosh.utils.EventTimeAggregator;

import java.util.concurrent.atomic.AtomicLong;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import org.apache.commons.lang3.StringUtils;

// Imports for RDBMS connection
import java.sql.*;

// In the initial implementation, this class just implements dynamic threading for insert into a database, but in long
// run this is supposed to be library which accepts runnable class and schedules multiple threads dynamically.
public class DynamicThreading implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(DynamicThreading.class.getName());;
    private static final String DBUrl = "jdbc:postgresql://localhost/perfdb";;
    private static final String User = "perfusr";
    private static final String Password = "perfusr";

    private long numRows;
    private AtomicLong rowCnt;
    private EventTimeAggregator eta;

    static
    {
        try
        {
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException cnfex)
        {
            throw new ExceptionInInitializerError(cnfex);
        }
    }

    public DynamicThreading(long numRows, AtomicLong rowCnt, EventTimeAggregator eta) {
        this.numRows = numRows;
        this.rowCnt = rowCnt;
        this.eta = eta;
    }

    private static void createTablesAndIndexes(Connection conn) throws SQLException {
        String createStrTableCmd = "CREATE TABLE str_table (id BIGINT PRIMARY KEY, str VARCHAR NOT NULL UNIQUE)";
        LOGGER.info("creating table str_table using command " + createStrTableCmd + ".");
        Statement crStmt = conn.createStatement();
        crStmt.executeUpdate(createStrTableCmd);
        conn.commit();
    }

    private static void dropTablesAndIndexes(Connection conn) throws SQLException {
        String dropStrTableCmd = "DROP TABLE str_table";
        LOGGER.info("dropping table str_table.");
        Statement crStmt = conn.createStatement();
        crStmt.executeUpdate(dropStrTableCmd);
        conn.commit();
    }

    private static void testDynamicThreading(long numRows) throws InterruptedException {
        List<Thread> threads = new LinkedList<Thread>();
        EventTimeAggregator eta = new EventTimeAggregator("Leader", 1);
        EventTimeAggregator etaPerfMeasurer = new EventTimeAggregator("PerfMeasurer", 1);
        List<EventTimeAggregator> threadEtas = new LinkedList<EventTimeAggregator>();
        AtomicLong rowCnt = new AtomicLong();
        boolean needMoreThreads = true;
        int napTime = 100; // Sleep for 100ms during two breaks
        float workDoneInPreviousCycle = ((float) 1)/napTime;

        LOGGER.info("Testing insert of " + numRows);
        etaPerfMeasurer.eventStart();

        // Main loop creating and scheduling threads
        while (true) {
            long countBeforeThread;
            float workDoneInThisCyle;

            // Quit if we have inserted requested number of rows
            if (rowCnt.get() >= numRows) {
                break;
            }

            // Schedule thread, if we need more threads and we think that it will get work done faster.
            if (needMoreThreads) {
                String threadName = "InsertionThread#" + (threads.size() + 1);
                // Take averages for a total of 100 intervals.
                EventTimeAggregator threadEta = new EventTimeAggregator(threadName, numRows/100);
                Thread newThread = new Thread(new DynamicThreading(numRows, rowCnt, threadEta));
                threadEtas.add(threadEta);
                threads.add(newThread);

                // Take the stock, start the timer and the thread
                LOGGER.info("Starting new thread " + threadName);
                newThread.start();
            }

            countBeforeThread = rowCnt.get();
            eta.eventStart();
            Thread.sleep(100);
            eta.eventEnd();

            long numRowsInserted = rowCnt.get() - countBeforeThread;
            float cycleDuration = eta.getPeriodicAverage();
            LOGGER.info("Number of rows inserted in this cycle " + numRowsInserted + " in " + cycleDuration + "ms.");
            workDoneInThisCyle = numRowsInserted/cycleDuration;
            // If the improvement was more than 10% consider increasing the number of threads
            float percWorkDoneChange = (workDoneInThisCyle - workDoneInPreviousCycle)/workDoneInPreviousCycle;
            LOGGER.info("Work done in the last cycle: " + workDoneInPreviousCycle);
            LOGGER.info("Work done in this cycle: " + workDoneInThisCyle);
            LOGGER.info("Rate of work done changed by " + (percWorkDoneChange * 100) +"%.");
            if (percWorkDoneChange > 0.05) {
                needMoreThreads = true;
                workDoneInPreviousCycle = workDoneInThisCyle;
            } else {
                LOGGER.info("Do not need any more new threads. Right now " + threads.size() + " number of threads are running.");
                needMoreThreads = false;
            }
        }

        // Wait for all the threads to finish
        for (Thread thread : threads) {
            thread.join();
        }

        etaPerfMeasurer.eventEnd();
        LOGGER.info("Finished inserting " + numRows + " rows using " + threads.size() + " threads in " + etaPerfMeasurer.getTotalTime() + "ms.");
    }

    public static void main(String args[]) throws SQLException, InterruptedException {
        Long numRows = Long.valueOf(args[0]);

        Handler consoleHandler = new ConsoleHandler();

        LOGGER.setLevel(Level.INFO);
        consoleHandler.setLevel(Level.INFO);
        LOGGER.addHandler(consoleHandler);

        try (Connection conn = DriverManager.getConnection(DBUrl, User, Password)) {
            try {
                conn.setAutoCommit(false);

                createTablesAndIndexes(conn);
                testDynamicThreading(numRows);
            } finally {
                // Rollback anything we were trying to do. And drop the tables.
                conn.rollback();
                dropTablesAndIndexes(conn);
            }
        }
    }

    // Each insertion thread inserts nRows with length len
    @Override
    public void run()
    {
        String threadName = Thread.currentThread().getName();

        try (Connection conn = DriverManager.getConnection(DBUrl, User, Password)) {
            String insertCmd;
            PreparedStatement insertStmt;
            insertCmd = "INSERT INTO str_table(str, id) VALUES (?, ?)";
            insertStmt = conn.prepareStatement(insertCmd);
            String pad = threadName + StringUtils.repeat('a', 1000);

            conn.setAutoCommit(false);
            while(true) {
                long cnt = rowCnt.getAndIncrement();

                if (cnt >= numRows) {
                    break;
                }

                String value = cnt + pad;
                value.substring(0, 1000);

                eta.eventStart();
                insertStmt.setString(1, value);
                insertStmt.setLong(2, rowCnt.getAndIncrement());
                insertStmt.executeUpdate();
                conn.commit();
                eta.eventEnd();
            }
        } catch (Exception e) {
            LOGGER.info("Got exception " + e + " in thread " + threadName);
        }
        return;
    }
}