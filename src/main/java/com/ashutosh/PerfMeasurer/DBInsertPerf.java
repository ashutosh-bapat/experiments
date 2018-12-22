package com.ashutosh.PerfMeasurer;

// Imports for logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.ConsoleHandler;


// Imports for RDBMS connection
import java.sql.*;

// Other imports
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.Thread;
import org.apache.commons.math3.util.ArithmeticUtils;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.util.concurrent.atomic.AtomicLong;
import com.google.common.base.Joiner;

import com.ashutosh.utils.EventTimeAggregator;
import org.apache.commons.math3.util.MathUtils;

public class DBInsertPerf implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(DBInsertPerf.class.getName());;
    private static final String DBUrl = "jdbc:postgresql://localhost/perfdb";;
    private static final String User = "perfusr";
    private static final String Password = "perfusr";
    private static final int[] numThreadsArray = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private static final int[] lenArray = {100, 1000, 10000};
    private static final List<PerfRecord> perfRecords = new ArrayList<PerfRecord>();

    private long nRows;
    private int len;
    private EventTimeAggregator eta;
    private Connection conn;
    private boolean useSerial;
    private AtomicLong rowCnt;

    public DBInsertPerf(Long nRows, int len, EventTimeAggregator eta, AtomicLong rowCnt)
            throws InterruptedException, SQLException
    {
        this.nRows = nRows;
        this.len = len;
        this.eta = eta;
        this.useSerial = (rowCnt == null);
        this.rowCnt = rowCnt;
    }

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

    private static void createTablesAndIndexes(Connection conn, boolean withIndex, boolean useSerial) throws SQLException {
        // Unique and primary key constraints will create indexes on respective columns, so no need to
        // create indexes explicitly.
        String createStrTableCmd = "CREATE TABLE str_table (" +
                " id " + (useSerial ? "BIGSERIAL" : "BIGINT") + (withIndex ? " PRIMARY KEY" : "") +
                ", str VARCHAR NOT NULL " + (withIndex ? " UNIQUE " : "") +
                ")";

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

    private static void testVarcharInsertsWithLength(int len, Long numRows, int numThreads, boolean withIndexes,
                                                     boolean useSerial) throws InterruptedException, SQLException {
        int cnt;
        LOGGER.info("Testing insert of " + numRows + " with column length " + len + " using " + numThreads + " threads.");
        Thread[] insertionThreads = new Thread[numThreads];
        EventTimeAggregator eta = new EventTimeAggregator("Leader", 1);

        EventTimeAggregator[] threadEtas = new EventTimeAggregator[numThreads];
        long rowsPerThread = numRows/numThreads;
        AtomicLong rowCnt = null;

        if (!useSerial) {
            rowCnt = new AtomicLong();
        }

        // Initialize and start insertion threads
        for (cnt = 0; cnt < numThreads; cnt++)
        {
            String threadName = "InsertionThread#" + cnt;
            threadEtas[cnt] = new EventTimeAggregator(threadName + "_" + len + "_" + numRows, numRows);
            insertionThreads[cnt] = new Thread(new DBInsertPerf(rowsPerThread, len, threadEtas[cnt], rowCnt),
                                                threadName);
        }

        LOGGER.info("Starting " + numThreads + " threads.");
        eta.eventStart();
        for (cnt = 0; cnt < numThreads; cnt++) {
            insertionThreads[cnt].start();
        }

        for (cnt = 0; cnt < numThreads; cnt++) {
           insertionThreads[cnt].join();
        }
        eta.eventEnd();
        LOGGER.info("All threads finished.");
        float sum = 0;
        float min = eta.getTotalTime();
        float max = 0;
        for (EventTimeAggregator thEta : threadEtas) {
            float avg = thEta.getTotalAverage();
            sum = sum + avg;
            min = Math.min(min, avg);
            max = Math.max(max, avg);
        }
        perfRecords.add(new PerfRecord(numThreads, numRows, len,withIndexes, useSerial, (long) eta.getTotalAverage(),
                (long) (sum/numThreads), (long) min, (long) max));
    }

    private static void testWithIndexesAndLen(int len, Long numRows, int numThreads, boolean withIndex, boolean useSerial) throws InterruptedException, SQLException {
        // Setup connection with auto commit off
        Connection conn = DriverManager.getConnection(DBUrl, User, Password);
        conn.setAutoCommit(false);

        createTablesAndIndexes(conn, withIndex, useSerial);
        testVarcharInsertsWithLength(len, numRows, numThreads, withIndex, useSerial);
        dropTablesAndIndexes(conn);

        conn.close();
    }

    private static void runPerfTest(Long numRows, boolean withIndex, boolean useSerial) throws InterruptedException, SQLException {
        for (int numThreads : numThreadsArray) {
            for (int len : lenArray) {
                testWithIndexesAndLen(len, numRows, numThreads, withIndex, useSerial);
            }
        }
    }

    // Calculates number of rows to insert as a multiple of LCM of number of threads so that we always end up inserting
    // same number of rows, per thread as well as per test, irrespective of the number of threads.
    public static long getNumRows(int multFactor) {
        long lcm = 1;
        for (int numThreads: numThreadsArray) {
            lcm = ArithmeticUtils.lcm(lcm, numThreads);
        }
        return lcm * multFactor;
    }

    public static void main(String args[]) throws InterruptedException, SQLException {
        Long numRows = getNumRows(Integer.valueOf(args[0]));

        Handler consoleHandler = new ConsoleHandler();

        LOGGER.setLevel(Level.INFO);
        consoleHandler.setLevel(Level.INFO);
        LOGGER.addHandler(consoleHandler);

        // Perf tests with indexes and serial column
        runPerfTest(numRows, true, true);
        // Perf tests without indexes and with serial column
        runPerfTest(numRows, false, true);
        // Perf tests with indexes and without serial column
        runPerfTest(numRows, true, false);
        // Perf tests without indexes and without serial column
        runPerfTest(numRows, false, false);

        System.out.println(Joiner.on("\n").join(perfRecords));
    }

    // Each insertion thread inserts nRows with length len
    @Override
    public void run()
    {
        String threadName = Thread.currentThread().getName();

        try {
            conn = DriverManager.getConnection(DBUrl, User, Password);
            String insertCmd;
            PreparedStatement insertStmt;
            insertCmd = "INSERT INTO str_table" +
                    "(str" + (useSerial ? "" : ", id") + ")" +
                    " VALUES (?" + (useSerial ?  "" : ", ?") + ")";
            insertStmt = conn.prepareStatement(insertCmd);
            String pad = threadName + StringUtils.repeat('a', len);
            long cnt;

            conn.setAutoCommit(false);
            for (cnt = 0; cnt < nRows; cnt++) {
                String value = cnt + pad;
                value.substring(0, len);

                eta.eventStart();
                insertStmt.setString(1, value);
                if (!useSerial) {
                    insertStmt.setLong(2, rowCnt.getAndIncrement());
                }
                insertStmt.executeUpdate();
                conn.commit();
                eta.eventEnd();
            }
        } catch (Exception e) {
            LOGGER.info("Got exception " + e + " in thread " + threadName);
        } finally {
            try {
                conn.close();
            }
            catch (SQLException sqe) {
                LOGGER.info("Can not close connection in thread " + threadName);
                // Do nothing
            }
        }

        return;
    }

    private static class PerfRecord {
        private int numThreads;
        private long numRows;
        private int len;
        private boolean withIndex;
        private boolean useSerial;
        private long timeInMS;
        private long avgTimeInMSPerRow;
        private long minAvgTimeInMSPerRow;
        private long maxAvgTimeInMSPerRow;

        public PerfRecord(int numThreads, long numRows, int len, boolean withIndex, boolean useSerial, long timeInMS,
                           long avgTimeInMSPerThread, long minAvgTimeInMSPerRow, long maxAvgTimeInMSPerRow) {
            this.numThreads = numThreads;
            this.numRows = numRows;
            this.len = len;
            this.withIndex = withIndex;
            this.useSerial = useSerial;
            this.timeInMS = timeInMS;
            this.avgTimeInMSPerRow = avgTimeInMSPerThread;
            this.minAvgTimeInMSPerRow = minAvgTimeInMSPerRow;
            this.maxAvgTimeInMSPerRow = maxAvgTimeInMSPerRow;
        }

        public String toCsvString() {
            return numThreads + ", " + numRows + ", " + len + ", " +
                    BooleanUtils.toStringTrueFalse(withIndex) + ", " +
                    BooleanUtils.toStringTrueFalse(useSerial) + ", " +
                    timeInMS + ", " + avgTimeInMSPerRow + ", " + minAvgTimeInMSPerRow + ", " +
                    maxAvgTimeInMSPerRow;
        }

        public String toString() {return toCsvString();}
    }
}
