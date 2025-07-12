package com.ashutosh.BrainVitae;

import com.ashutosh.BoardSolver.BoardSolver;
import com.ashutosh.BoardSolver.BoardSolverDB;
import com.ashutosh.BoardSolver.BoardState;

import java.lang.IllegalArgumentException;
import java.sql.SQLException;
import java.util.Map;

class BrainVitae
{
	private final static String dbUrl = "jdbc:postgresql://localhost/boardgames";
	private final static String dbUser = "boardgames";
	private final static String dbPassword = "boardgames";
	private final static String statesTableName = "states";
	private final static String movesTableName = "moves";
	private final static String dbSchema = "brainvitae";

	public static void main(String args[])
		throws SQLException,
			   IllegalArgumentException,
			   InterruptedException
	{
		if (args.length <= 0) {
			throw new IllegalArgumentException("expected non-zero arguments, but got " + args.length);
		}

		int	numThreads = Integer.parseInt(args[0]);
		BoardSolverDB bvBoardSolverDb = new BoardSolverDB(dbUrl, dbUser, dbPassword, statesTableName, movesTableName,
															dbSchema);
		final BrainVitaeBoard board = new BrainVitaeBoard(7);
		BVBoardState initialState =  new BVBoardState(board);
		bvBoardSolverDb.createObjects();
		BoardSolver bvBoardSolver = new BoardSolver(numThreads, initialState, bvBoardSolverDb);
		bvBoardSolver.solve();
		Map<String, BoardState> solutions = bvBoardSolverDb.findSolutions(initialState);
		solutions.entrySet().stream().forEach(solution -> System.out.println("Moves " + solution.getKey() + " lead to final state " + solution.getValue().getDesc()));
	}
}
