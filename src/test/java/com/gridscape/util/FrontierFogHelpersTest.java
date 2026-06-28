package com.gridscape.util;

import com.gridscape.task.TaskState;
import com.gridscape.task.TaskTile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrontierFogHelpersTest
{
	@Test
	public void isRevealedUnclaimedTaskState()
	{
		assertTrue(FrontierFogHelpers.isRevealedUnclaimedTaskState(TaskState.REVEALED));
		assertTrue(FrontierFogHelpers.isRevealedUnclaimedTaskState(TaskState.COMPLETED_UNCLAIMED));
		assertFalse(FrontierFogHelpers.isRevealedUnclaimedTaskState(TaskState.CLAIMED));
		assertFalse(FrontierFogHelpers.isRevealedUnclaimedTaskState(TaskState.LOCKED));
	}

	@Test
	public void cardinalFlagsForHiddenCellMarksRevealedUnclaimedNeighbors()
	{
		Map<String, TaskState> states = new HashMap<>();
		states.put("1,0", TaskState.REVEALED);
		states.put("2,1", TaskState.CLAIMED);

		boolean[] flags = FrontierFogHelpers.cardinalFlagsForHiddenCell(1, 1,
			states::get,
			id -> states.containsKey(id));

		assertArrayEquals(new boolean[]{ false, false, false, true }, flags);
	}

	@Test
	public void hiddenCellHasRevealedUnclaimedNeighbor()
	{
		List<TaskTile> grid = Arrays.asList(
			TaskTile.of("0,0", 0, "A", 0, 0, 0),
			TaskTile.of("0,1", 1, "B", 10, 0, 1)
		);
		Map<String, TaskState> states = new HashMap<>();
		states.put("0,0", TaskState.LOCKED);
		states.put("0,1", TaskState.REVEALED);

		assertTrue(FrontierFogHelpers.hiddenCellHasRevealedUnclaimedNeighbor(0, 0, grid, states::get));
		assertFalse(FrontierFogHelpers.hiddenCellHasRevealedUnclaimedNeighbor(2, 2, grid, states::get));
	}

	@Test
	public void idMapSkipsNullEntries()
	{
		Map<String, TaskTile> map = FrontierFogHelpers.idMap(Arrays.asList(
			TaskTile.of("0,0", 0, "Center", 0, 0, 0),
			null
		));
		assertTrue(map.containsKey("0,0"));
		assertFalse(FrontierFogHelpers.idMap(Collections.emptyList()).containsKey("0,0"));
	}
}
