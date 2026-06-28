package com.gridscape.worldunlock;

import com.gridscape.GridScapeConfig;
import com.gridscape.grid.GridPos;
import com.gridscape.points.PointsService;
import com.gridscape.task.TaskDefinition;
import com.gridscape.task.TaskGridService;
import com.gridscape.task.TaskState;
import com.gridscape.task.TaskTile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalTaskListService.
 * Verifies: task list filling, task assignment to grid positions, tile reveal logic,
 * and that adjacent tiles appear when center is claimed.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GlobalTaskListServiceTest
{
	@Mock
	private ConfigManager configManager;

	@Mock
	private GridScapeConfig config;

	@Mock
	private PointsService pointsService;

	@Mock
	private WorldUnlockService worldUnlockService;

	@Mock
	private TaskGridService taskGridService;

	private GlobalTaskListService service;

	private static final String STATE_GROUP = com.gridscape.util.GridScapeConfigConstants.STATE_GROUP;
	private static final String KEY_CENTER_CLAIMED = "globalTaskProgress_centerClaimed";
	private static final String KEY_CLAIMED = "globalTaskProgress_claimed";
	private static final String KEY_POSITIONS = "globalTaskProgress_positions";
	private static final String KEY_PSEUDO_CENTER = "globalTaskProgress_pseudoCenter";

	@Before
	public void setUp()
	{
		service = new GlobalTaskListService(configManager, config, pointsService,
			worldUnlockService, taskGridService);
	}

	private TaskDefinition task(String displayName, int difficulty)
	{
		TaskDefinition t = new TaskDefinition();
		t.setDisplayName(displayName);
		t.setDifficulty(difficulty);
		return t;
	}

	private void stubTierPoints()
	{
		lenient().when(config.taskTier1Points()).thenReturn(10);
		lenient().when(config.taskTier2Points()).thenReturn(25);
		lenient().when(config.taskTier3Points()).thenReturn(50);
		lenient().when(config.taskTier4Points()).thenReturn(75);
		lenient().when(config.taskTier5Points()).thenReturn(100);
	}

	private void stubEmptyProgressState()
	{
		lenient().when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CENTER_CLAIMED))).thenReturn(null);
		lenient().when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CLAIMED))).thenReturn(null);
		lenient().when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_POSITIONS))).thenReturn(null);
		lenient().when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_PSEUDO_CENTER))).thenReturn(null);
		lenient().when(configManager.getConfiguration(eq(STATE_GROUP), eq("globalTaskProgress_completed"))).thenReturn(null);
		lenient().when(configManager.getConfiguration(eq(STATE_GROUP), eq("globalTaskProgress_lastViewed"))).thenReturn(null);
	}

	private Map<String, String> stubConfigStore()
	{
		Map<String, String> store = new HashMap<>();
		when(configManager.getConfiguration(eq(STATE_GROUP), anyString())).thenAnswer(inv -> {
			String key = inv.getArgument(1);
			return store.get(key);
		});
		doAnswer(inv -> {
			store.put(inv.getArgument(1), inv.getArgument(2));
			return null;
		}).when(configManager).setConfiguration(eq(STATE_GROUP), anyString(), anyString());
		return store;
	}

	private void stubCenterClaimed(Map<String, String> store)
	{
		store.put(KEY_CENTER_CLAIMED, "true");
		store.put(KEY_PSEUDO_CENTER, "0,0");
	}

	private void stubEmptyWorldUnlock()
	{
		lenient().when(worldUnlockService.getUnlockedIds()).thenReturn(Collections.emptySet());
		lenient().when(worldUnlockService.getTiles()).thenReturn(Collections.emptyList());
		lenient().when(worldUnlockService.getUnlockedOrRevealedTileIds()).thenReturn(Collections.emptySet());
		lenient().when(worldUnlockService.getUnlockedDiaryTierKeys()).thenReturn(Collections.emptySet());
		lenient().when(worldUnlockService.getTasksForUnlock(anyString())).thenReturn(Collections.emptyList());
		lenient().when(worldUnlockService.getSkillTileIdForLevel(anyString(), anyInt())).thenReturn(null);
	}

	private void stubZulrahCollectionLogWorldUnlock(boolean bossUnlocked, boolean areaUnlocked)
	{
		WorldUnlockTile boss = new WorldUnlockTile();
		boss.setType("boss");
		boss.setId("zulrah");
		WorldUnlockTile area = new WorldUnlockTile();
		area.setType("area");
		area.setId("isafdar");
		when(worldUnlockService.getTiles()).thenReturn(Arrays.asList(boss, area));
		when(worldUnlockService.resolvePrerequisiteToTileId(anyString())).thenAnswer(invocation -> {
			String s = invocation.getArgument(0);
			if (s == null) return null;
			String t = s.trim();
			if ("zulrah".equalsIgnoreCase(t)) return "zulrah";
			if ("isafdar".equalsIgnoreCase(t)) return "isafdar";
			return null;
		});
		when(worldUnlockService.getTileById(eq("zulrah"))).thenReturn(boss);
		when(worldUnlockService.getTileById(eq("isafdar"))).thenReturn(area);
		Set<String> unlocked = new HashSet<>();
		if (areaUnlocked) unlocked.add("isafdar");
		if (bossUnlocked) unlocked.add("zulrah");
		when(worldUnlockService.getUnlockedIds()).thenReturn(unlocked);
		when(worldUnlockService.getUnlockedOrRevealedTileIds()).thenReturn(new HashSet<>(unlocked));
		when(worldUnlockService.getUnlockedDiaryTierKeys()).thenReturn(Collections.emptySet());
		when(worldUnlockService.getTasksForUnlock(anyString())).thenReturn(Collections.emptyList());
		when(worldUnlockService.getSkillTileIdForLevel(anyString(), anyInt())).thenReturn(null);
	}

	private TaskDefinition tanzaniteFangCollectionLog()
	{
		TaskDefinition cl = new TaskDefinition();
		cl.setDisplayName("Obtain a Tanzanite fang");
		cl.setTaskType("Collection Log");
		cl.setDifficulty(4);
		cl.setArea("isafdar");
		cl.setRequirements("zulrah");
		cl.setBossId("zulrah");
		cl.setOnceOnly(true);
		return cl;
	}

	private static Set<String> gridPositions(List<TaskTile> grid)
	{
		Set<String> positions = new HashSet<>();
		for (TaskTile t : grid)
			positions.add(t.getRow() + "," + t.getCol());
		return positions;
	}

	private static void assertGridContainsAllNeighbors(List<TaskTile> grid, int row, int col)
	{
		Set<String> positions = gridPositions(grid);
		for (String neighbor : GridPos.neighbors4(row, col))
			assertTrue("Grid should contain neighbor " + neighbor + " of " + row + "," + col,
				positions.contains(neighbor));
	}

	@Test
	public void testGetGlobalTasksReturnsNoAreaTasksWhenUnlockedEmpty()
	{
		stubEmptyWorldUnlock();
		List<TaskDefinition> noAreaTasks = Arrays.asList(
			task("Chop some Logs", 1),
			task("Burn some Logs", 1)
		);
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(noAreaTasks);

		List<TaskDefinition> result = service.getGlobalTasks();

		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.stream().anyMatch(t -> "Chop some Logs".equals(t.getDisplayName())));
		assertTrue(result.stream().anyMatch(t -> "Burn some Logs".equals(t.getDisplayName())));
	}

	@Test
	public void testBuildGlobalGridReturnsCenterOnlyWhenCenterNotClaimed()
	{
		stubEmptyProgressState();
		stubTierPoints();
		stubEmptyWorldUnlock();
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(Collections.emptyList());

		List<TaskTile> grid = service.buildGlobalGrid(12345);

		assertNotNull(grid);
		assertEquals(1, grid.size());
		assertEquals("0,0", grid.get(0).getId());
		assertEquals("Free", grid.get(0).getDisplayName());
	}

	@Test
	public void testBuildGlobalGridRevealsAdjacentTilesWhenCenterClaimed()
	{
		stubTierPoints();
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CENTER_CLAIMED))).thenReturn("true");
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_POSITIONS))).thenReturn(null);
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_PSEUDO_CENTER))).thenReturn("0,0");
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CLAIMED))).thenReturn(null);
		when(configManager.getConfiguration(eq(STATE_GROUP), eq("globalTaskProgress_completed"))).thenReturn(null);

		List<TaskDefinition> tasks = Arrays.asList(
			task("Chop some Logs", 1),
			task("Burn some Logs", 1),
			task("Fletch Arrowshafts", 1),
			task("Mine Copper", 1),
			task("Fish Shrimp", 1)
		);
		stubEmptyWorldUnlock();
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(tasks);

		List<TaskTile> grid = service.buildGlobalGrid(12345);

		assertNotNull(grid);
		assertEquals(5, grid.size());
		assertTrue(grid.stream().anyMatch(t -> t.getRow() == 0 && t.getCol() == 0));
		assertGridContainsAllNeighbors(grid, 0, 0);
	}

	@Test
	public void testGetGlobalStateReturnsRevealedForAdjacentWhenCenterClaimed()
	{
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CENTER_CLAIMED))).thenReturn("true");
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CLAIMED))).thenReturn(null);
		when(configManager.getConfiguration(eq(STATE_GROUP), eq("globalTaskProgress_completed"))).thenReturn(null);

		List<TaskTile> grid = new ArrayList<>();
		grid.add(new TaskTile("0,0", 0, "Free", 0, 0, 0, null, null, true, null, null));
		grid.add(new TaskTile("1,0", 1, "Chop Logs", 10, 1, 0, "Woodcutting", null, true, null, null));

		assertEquals(TaskState.CLAIMED, service.getGlobalState("0,0", grid));
		assertEquals(TaskState.REVEALED, service.getGlobalState("1,0", grid));
		for (TaskTile t : grid)
		{
			if (t.getRow() == 0 && t.getCol() == 0) continue;
			for (String neighbor : GridPos.neighbors4(t.getRow(), t.getCol()))
			{
				if ("0,0".equals(neighbor))
					assertEquals(TaskState.REVEALED, service.getGlobalState(t.getId(), grid));
			}
		}
	}

	@Test
	public void testGetGlobalStateReturnsLockedForTileWithoutClaimedNeighbor()
	{
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CENTER_CLAIMED))).thenReturn("true");
		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CLAIMED))).thenReturn(null);

		List<TaskTile> grid = new ArrayList<>();
		grid.add(new TaskTile("0,0", 0, "Free", 0, 0, 0, null, null, true, null, null));
		grid.add(new TaskTile("1,0", 1, "Chop Logs", 10, 1, 0, "Woodcutting", null, true, null, null));
		grid.add(new TaskTile("2,0", 1, "Burn Logs", 10, 2, 0, "Firemaking", null, true, null, null));

		assertEquals(TaskState.LOCKED, service.getGlobalState("2,0", grid));
	}

	@Test
	public void testFallbackUsesAnyTasksWhenNoAreaTasksEmpty()
	{
		Map<String, String> store = stubConfigStore();
		stubCenterClaimed(store);
		stubTierPoints();
		stubEmptyWorldUnlock();

		TaskDefinition areaTask = new TaskDefinition();
		areaTask.setDisplayName("Lumbridge Task");
		areaTask.setDifficulty(1);
		areaTask.setArea("lumbridge");
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(Collections.singletonList(areaTask));

		List<TaskTile> grid = service.buildGlobalGrid(12345);

		assertEquals(2, grid.size());
		TaskTile adjacent = grid.stream()
			.filter(t -> t.getRow() != 0 || t.getCol() != 0)
			.findFirst()
			.orElse(null);
		assertNotNull(adjacent);
		assertEquals("Lumbridge Task", adjacent.getDisplayName());
		assertTrue(GridPos.neighbors4(0, 0).contains(adjacent.getRow() + "," + adjacent.getCol()));
	}

	@Test
	public void collectionLogWithBossIdIncludedInGlobalTasksWhenBossAndAreaUnlocked()
	{
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(Collections.singletonList(tanzaniteFangCollectionLog()));
		stubZulrahCollectionLogWorldUnlock(true, true);

		List<TaskDefinition> result = service.getGlobalTasks();
		assertTrue(result.stream().anyMatch(t -> "Obtain a Tanzanite fang".equals(t.getDisplayName())));
	}

	@Test
	public void collectionLogWithBossIdExcludedFromGlobalTasksWhenBossLocked()
	{
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(Collections.singletonList(tanzaniteFangCollectionLog()));
		stubZulrahCollectionLogWorldUnlock(false, true);

		List<TaskDefinition> result = service.getGlobalTasks();
		assertFalse(result.stream().anyMatch(t -> "Obtain a Tanzanite fang".equals(t.getDisplayName())));
	}

	@Test
	public void killCountChainNextStepOnlyAfterPreviousClaimed()
	{
		TaskDefinition first = new TaskDefinition();
		first.setDisplayName("Defeat Brutus");
		first.setTaskType("killCount");
		first.setDifficulty(1);
		first.setArea("lumbridge");
		first.setRequirements("Ides of Milk");

		TaskDefinition second = new TaskDefinition();
		second.setDisplayName("Defeat Brutus 5 times");
		second.setTaskType("killCount");
		second.setDifficulty(1);
		second.setArea("lumbridge");
		second.setRequirements("Defeat Brutus");

		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(Arrays.asList(first, second));
		when(worldUnlockService.getUnlockedIds()).thenReturn(new HashSet<>(Arrays.asList("lumbridge", "quest_ides")));
		when(worldUnlockService.getTasksForUnlock(anyString())).thenReturn(Collections.emptyList());

		WorldUnlockTile area = new WorldUnlockTile();
		area.setType("area");
		area.setId("lumbridge");
		when(worldUnlockService.getTiles()).thenReturn(Collections.singletonList(area));
		when(worldUnlockService.resolvePrerequisiteToTileId(anyString())).thenAnswer(invocation -> {
			String s = invocation.getArgument(0);
			if (s == null) return null;
			String t = s.trim();
			if ("Ides of Milk".equalsIgnoreCase(t)) return "quest_ides";
			if ("lumbridge".equalsIgnoreCase(t)) return "lumbridge";
			return null;
		});
		when(worldUnlockService.getTileById(anyString())).thenAnswer(invocation -> {
			WorldUnlockTile w = new WorldUnlockTile();
			w.setId(invocation.getArgument(0));
			w.setType("area");
			return w;
		});
		when(worldUnlockService.getUnlockedOrRevealedTileIds()).thenReturn(new HashSet<>(Arrays.asList("lumbridge", "quest_ides")));
		when(worldUnlockService.getUnlockedDiaryTierKeys()).thenReturn(Collections.emptySet());
		when(worldUnlockService.getSkillTileIdForLevel(anyString(), anyInt())).thenReturn(null);

		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CLAIMED))).thenReturn(null);
		List<TaskDefinition> pass1 = service.getGlobalTasks();
		assertTrue(pass1.stream().anyMatch(t -> "Defeat Brutus".equals(t.getDisplayName())));
		assertFalse(pass1.stream().anyMatch(t -> "Defeat Brutus 5 times".equals(t.getDisplayName())));

		when(configManager.getConfiguration(eq(STATE_GROUP), eq(KEY_CLAIMED))).thenReturn("defeat brutus");
		List<TaskDefinition> pass2 = service.getGlobalTasks();
		assertTrue(pass2.stream().anyMatch(t -> "Defeat Brutus 5 times".equals(t.getDisplayName())));
	}

	@Test
	public void buildGlobalGridFillsRingWhenOnlyCollectionLogTasksWithoutBossInPool()
	{
		List<TaskDefinition> tasks = new ArrayList<>();
		for (int i = 0; i < 8; i++)
		{
			TaskDefinition cl = new TaskDefinition();
			cl.setDisplayName("CL task " + i);
			cl.setTaskType("Collection Log");
			cl.setDifficulty(1);
			tasks.add(cl);
		}
		Map<String, String> store = stubConfigStore();
		stubCenterClaimed(store);
		stubTierPoints();
		stubEmptyWorldUnlock();
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(tasks);

		List<TaskTile> grid = service.buildGlobalGrid(42);
		assertEquals(5, grid.size());
		assertGridContainsAllNeighbors(grid, 0, 0);
		long clCount = grid.stream()
			.filter(t -> t.getRow() != 0 || t.getCol() != 0)
			.filter(t -> "Collection Log".equals(t.getTaskType()))
			.count();
		assertEquals(4, clCount);
	}

	@Test
	public void collectionLogBossLinkedPickedWithFixedSeedWhenCompetingWithMining()
	{
		Map<String, String> store = stubConfigStore();
		stubCenterClaimed(store);
		stubTierPoints();

		TaskDefinition cl = new TaskDefinition();
		cl.setDisplayName("CL zulrah item");
		cl.setTaskType("Collection Log");
		cl.setDifficulty(1);
		cl.setRequirements("zulrah");
		List<TaskDefinition> tasks = new ArrayList<>();
		tasks.add(cl);
		for (int i = 0; i < 5; i++)
			tasks.add(task("Mine ore " + i, 1));

		WorldUnlockTile boss = new WorldUnlockTile();
		boss.setType("boss");
		boss.setId("zulrah");
		when(worldUnlockService.getTiles()).thenReturn(Collections.singletonList(boss));
		when(worldUnlockService.resolvePrerequisiteToTileId(anyString())).thenAnswer(invocation -> {
			String s = invocation.getArgument(0);
			if (s != null && "zulrah".equalsIgnoreCase(s.trim())) return "zulrah";
			return null;
		});
		when(worldUnlockService.getTileById(eq("zulrah"))).thenReturn(boss);
		when(worldUnlockService.getUnlockedIds()).thenReturn(Collections.emptySet());
		when(worldUnlockService.getUnlockedOrRevealedTileIds()).thenReturn(Collections.emptySet());
		when(worldUnlockService.getUnlockedDiaryTierKeys()).thenReturn(Collections.emptySet());
		when(worldUnlockService.getTasksForUnlock(anyString())).thenReturn(Collections.emptyList());
		when(worldUnlockService.getSkillTileIdForLevel(anyString(), anyInt())).thenReturn(null);
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(tasks);

		List<TaskTile> grid = service.buildGlobalGrid(17);
		long clAssigned = grid.stream()
			.filter(t -> t.getRow() != 0 || t.getCol() != 0)
			.filter(t -> "Collection Log".equals(t.getTaskType()))
			.count();
		assertEquals("Boss-linked CL competes at full weight (1/6 per pick); seed 17 assigns CL once",
			1, clAssigned);
	}

	@Test
	public void collectionLogWithoutBossDownweightedWhenMiningAlternativesExist()
	{
		Map<String, String> store = stubConfigStore();
		stubCenterClaimed(store);
		stubTierPoints();
		stubEmptyWorldUnlock();

		TaskDefinition cl = new TaskDefinition();
		cl.setDisplayName("CL filler");
		cl.setTaskType("Collection Log");
		cl.setDifficulty(1);
		List<TaskDefinition> tasks = new ArrayList<>();
		tasks.add(cl);
		for (int i = 0; i < 5; i++)
			tasks.add(task("Mine ore " + i, 1));
		when(taskGridService.getEffectiveDefaultTasks()).thenReturn(tasks);

		int clAssignments = 0;
		int total = 0;
		for (int seed = 0; seed < 200; seed++)
		{
			store.remove(KEY_POSITIONS);
			List<TaskTile> grid = service.buildGlobalGrid(seed);
			for (TaskTile ti : grid)
			{
				if (ti.getRow() == 0 && ti.getCol() == 0) continue;
				total++;
				if ("Collection Log".equals(ti.getTaskType()))
					clAssignments++;
			}
		}
		assertTrue("Boss-less CL should be downweighted vs mining alternatives: " + clAssignments + "/" + total,
			clAssignments < total * 0.15);
	}
}
