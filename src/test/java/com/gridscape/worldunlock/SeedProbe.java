package com.gridscape.worldunlock;

import com.google.gson.Gson;
import com.gridscape.GridScapeConfig;
import com.gridscape.util.GridScapeConfigConstants;
import com.gridscape.points.PointsService;
import com.gridscape.task.TaskDefinition;
import com.gridscape.task.TaskGridService;
import com.gridscape.task.TaskTile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SeedProbe
{
	@Mock private ConfigManager configManager;
	@Mock private GridScapeConfig config;
	@Mock private PointsService pointsService;
	@Mock private WorldUnlockService worldUnlockService;
	@Mock private TaskGridService taskGridService;

	@Test
	public void probe()
	{
		for (int seed = 0; seed < 200; seed++)
		{
			Map<String, String> store = new HashMap<>();
			when(configManager.getConfiguration(eq(GridScapeConfigConstants.STATE_GROUP), anyString())).thenAnswer(inv -> store.get(inv.getArgument(1)));
			doAnswer(inv -> { store.put(inv.getArgument(1), inv.getArgument(2)); return null; }).when(configManager).setConfiguration(eq(GridScapeConfigConstants.STATE_GROUP), anyString(), anyString());
			store.put("globalTaskProgress_centerClaimed", "true");
			store.put("globalTaskProgress_pseudoCenter", "0,0");
			when(config.taskTier1Points()).thenReturn(10);
			when(config.taskTier2Points()).thenReturn(25);
			when(config.taskTier3Points()).thenReturn(50);
			when(config.taskTier4Points()).thenReturn(75);
			when(config.taskTier5Points()).thenReturn(100);
			when(worldUnlockService.getUnlockedIds()).thenReturn(Collections.emptySet());
			when(worldUnlockService.getTiles()).thenReturn(Collections.emptyList());
			when(worldUnlockService.getUnlockedOrRevealedTileIds()).thenReturn(Collections.emptySet());
			when(worldUnlockService.getUnlockedDiaryTierKeys()).thenReturn(Collections.emptySet());
			when(worldUnlockService.getTasksForUnlock(anyString())).thenReturn(Collections.emptyList());
			when(worldUnlockService.getSkillTileIdForLevel(anyString(), anyInt())).thenReturn(null);
			List<TaskDefinition> tasks = new ArrayList<>();
			TaskDefinition cl = new TaskDefinition();
			cl.setDisplayName("CL filler");
			cl.setTaskType("Collection Log");
			cl.setDifficulty(1);
			tasks.add(cl);
			for (int i = 0; i < 5; i++)
			{
				TaskDefinition m = new TaskDefinition();
				m.setDisplayName("Mine ore " + i);
				m.setTaskType("Mining");
				m.setDifficulty(1);
				tasks.add(m);
			}
			when(taskGridService.getEffectiveDefaultTasks()).thenReturn(tasks);
			GlobalTaskListService service = new GlobalTaskListService(configManager, config, pointsService, worldUnlockService, taskGridService, new Gson());
			List<TaskTile> grid = service.buildGlobalGrid(seed);
			long clCount = grid.stream().filter(t -> t.getRow() != 0 || t.getCol() != 0).filter(t -> "Collection Log".equals(t.getTaskType())).count();
			if (clCount == 0) System.out.println("all-mining seed=" + seed);
			if (clCount == 1) System.out.println("one-cl seed=" + seed);
		}
	}
}
