package com.ashutosh.BoardSolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class BoardSolver {
    private final BoardState initialState;
    private final int numThreads;
    private static final Logger LOGGER = Logger.getLogger(BoardSolver.class.getName());
    // The number of states grows quite fast since we are traversing the tree of
    // states breadth-wise. If we keep on queueing those in a queue we get
    // killed because of lack of memory. Instead we use a bounded queue,
    // refilling it from the database whenever it gets empty.
    private static final int QCAPACITY = 150000;
    private static final short NUM_STATES_TO_PROCESS_PER_THREAD = 2;

    // We should be really measuing this using EventTimeAggregator. For now set it to 1ms.
    private static final short TIME_REQUIRED_TO_PROCESS_ONE_STATE = 1;

    // Initial db connection passed by users of this class. Any thread specific connections need to relevant properties
    // of this connection.
    private final BoardSolverDB db;


    public BoardSolver(int numThreads, BoardState initialState, BoardSolverDB db) {
        this.initialState = initialState;
        this.numThreads = numThreads;
        this.db = db;
    }

    public void solve() throws SQLException, InterruptedException {
        if (numThreads > 1) {
            multiThreadedSolver();
        } else {
            singleThreadedSolver();
        }
        LOGGER.info("Board solved.");
    }


    private void singleThreadedSolver()
            throws SQLException {
        Queue<BoardState> unprocessedStates;
        AtomicLong idCounter = new AtomicLong();
        final int REPORTING_PERIOD = 10000;

        // Main body of the function
        try (BoardSolverDB.BSDCConn dbConn = db.getConnection()) {
            long numstates = 0;
            BoardState curState;
            StateProcessor stateProcessor = new StateProcessor(db, idCounter);

            // readUnprocessedStates also marks the states as queued in DB, so just adding the initial state in DB followed
            // by queueing it doesn't work.
            dbConn.searchAndInsertState(initialState, idCounter.getAndIncrement());
            unprocessedStates = dbConn.readUnprocessedStates(initialState);
            while(true)
            {
                numstates++;
                curState = unprocessedStates.poll();
                if (curState == null)
                {
                    // Finished processing all the elements in the queue, check
                    // if database has any unprocessed elements.
                    unprocessedStates = dbConn.readUnprocessedStates(initialState);
                    LOGGER.info("Fetched " + unprocessedStates.size() + " states.");
                    curState = unprocessedStates.poll();
                    if (curState == null) {
                        // Even the database does not have any elements, quit the
                        // loop.
                        break;
                    }
                }
                stateProcessor.process(curState, dbConn);
                if (numstates % REPORTING_PERIOD == 0) {
                    LOGGER.info("finished processing " + numstates + " states.");
                }
            }

            LOGGER.info("finished processing ALL the " + numstates + " states.");
        } catch (SQLException sqle) {
            LOGGER.severe("database error: " + sqle.getMessage());
            throw sqle;
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            throw e;
        }
    }

    // Function to run multiple threads to enumerate the board states and moves.
    // It doesn't know what to do in case a waiting thread is interrupted, hence
    // throws an exception. In future this function may catch the exception and
    // take an appropriate action.
    private void multiThreadedSolver()
            throws InterruptedException, SQLException {
        int numStateProcessors = numThreads - 1;
        int cnt;
        BlockingQueue<BoardState> statesQ = new ArrayBlockingQueue<>(QCAPACITY);
        List<StateProcessor> stateProcessors = new ArrayList<>();
        AtomicLong idCounter = new AtomicLong();

        try (BoardSolverDB.BSDCConn dbConn = db.getConnection()) {
            // Add initial state to the database.
            dbConn.searchAndInsertState(initialState, idCounter.getAndIncrement());

            // Initialize and start state processor threads
            for (cnt = 0; cnt < numStateProcessors; cnt++) {
                StateProcessor stateProcessor = new StateProcessor(db, statesQ, " State processor thread #" + cnt,
                                                                    idCounter);
                stateProcessors.add(stateProcessor);
                stateProcessor.start();
            }

            keepFeedingUnprocessedStates(dbConn, statesQ, stateProcessors);

            // Signal each state processor to end their processing. We achieve this by adding as many
            // invalid states as the number of state processors. Each of them is expected to stop
            // pulling from a queue as soon as it gets one end element; thus never getting more than one
            // invalid state.
            LOGGER.info("Signalling all threads to quit.");
            for (StateProcessor sp : stateProcessors) {
                sp.interrupt();
            }

            LOGGER.info("Waiting for all threads to finish.");
            for (StateProcessor sp : stateProcessors) {
                sp.join();
            }

        } catch (SQLException sqe){
            LOGGER.severe("database error " + sqe.getMessage());
            throw sqe;
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            throw e;
        }
    }

    private void keepFeedingUnprocessedStates(BoardSolverDB.BSDCConn dbConn, BlockingQueue<BoardState> statesQ,
                                              List<StateProcessor> stateProcessors)
            throws InterruptedException, SQLException {
        // For now we have only one thread (this) which fetches the unprocessed states from the database and adds them
        // to the queue. But we may want to add more threads for doing that, in which case a. we have to add a loop to
        // start as many readers as needed and b. add some logic to synchronize among the thread not to add same state
        // twice in the queue.
        while (true) {
            int minQueueLengthToMaintain = stateProcessors.size() * NUM_STATES_TO_PROCESS_PER_THREAD;
            // Read unprocessed states from the database, when the number of unprocessed states is lower than
            // preconfigured multiple of number of threads.
            if (minQueueLengthToMaintain >= statesQ.size()) {
                Queue<BoardState> tempStatesQueue = dbConn.readUnprocessedStates(initialState);
                LOGGER.info(tempStatesQueue.size() + "states fetched and being queued for processing.");
                while (tempStatesQueue.size() > 0) {
                    statesQ.add(tempStatesQueue.remove());
                }
            }

            if (statesQ.size() == 0) {
                // If there are no states to process, check if any of the threads are still processing any states.
                boolean finishProcessing = true;
                for (StateProcessor sp : stateProcessors) {
                    if (!sp.isWaitingForNextState()) {
                        finishProcessing = false;
                    }
                }

                if (finishProcessing) {
                    // All threads are waiting for new state and there is no state in the queue as checked above.
                    // Between that check and now, nobody else is going to feed any new states so that check is
                    // still valid. The state processors are still waiting on the queue. Make a last attempt to read
                    // new states from the database, in case any of the state processors has added a new state after
                    // we read unprocessed states from the database.
                    Queue<BoardState> tempStatesQueue = dbConn.readUnprocessedStates(initialState);
                    if (tempStatesQueue.size() == 0) {
                        LOGGER.info("All the threads are waiting for a new state to process and did not find any states for processing.");
                        // There are no new states in the queue, none of the state processors are processing any
                        // threads and there is no unprocessed states in the database. We are done.
                        return;
                    }

                    LOGGER.info("All the threads are waiting for a new state to process but queued " + tempStatesQueue.size() + " states for processing.");
                    while (tempStatesQueue.size() > 0) {
                        statesQ.add(tempStatesQueue.remove());
                    }
                }
            }

            long numExtraStates = statesQ.size() - minQueueLengthToMaintain;
            if (numExtraStates > 0) {
                long sleepTime = TIME_REQUIRED_TO_PROCESS_ONE_STATE * numExtraStates;
                LOGGER.info("Sleeping for " + sleepTime);
                // Wait for some time proportional to the number of states queued, before reading next states to be
                // processed. We should ideally wait for the queue size to drop to considerable amount before doing so.
                Thread.sleep(sleepTime);
            }
        }
    }
}
