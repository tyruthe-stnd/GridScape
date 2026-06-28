package com.gridscape.icons;

import com.gridscape.constants.TaskTypes;
import org.junit.Assert;
import org.junit.Test;

public class IconResolverTest
{
	@Test
	public void killCountWithoutBossUsesCombatIcon()
	{
		String path = IconResolver.resolveTaskTileLocalIconPath(TaskTypes.KILL_COUNT, "Kill 5 goblins", null);
		Assert.assertNotNull(path);
		Assert.assertEquals(IconResolver.resolveTaskTypeLocalIconPath(TaskTypes.COMBAT), path);
		Assert.assertTrue(path.startsWith(IconResources.TASK_ICONS_RESOURCE_PREFIX));
	}

	@Test
	public void killCountWithBossUsesBossIcon()
	{
		String path = IconResolver.resolveTaskTileLocalIconPath(TaskTypes.KILL_COUNT, "Kill Zulrah", "zulrah");
		Assert.assertNotNull(path);
		Assert.assertTrue(path.startsWith(IconResources.BOSS_ICONS_RESOURCE_PREFIX));
		Assert.assertFalse(path.startsWith(IconResources.TASK_ICONS_RESOURCE_PREFIX));
	}

	@Test
	public void combatWithBossIdUsesBossIconNotCombatIcon()
	{
		String path = IconResolver.resolveTaskTileLocalIconPath(TaskTypes.COMBAT, "Defeat Zulrah", "zulrah");
		Assert.assertNotNull(path);
		Assert.assertTrue(path.startsWith(IconResources.BOSS_ICONS_RESOURCE_PREFIX));
		Assert.assertNotEquals(IconResolver.resolveTaskTypeLocalIconPath(TaskTypes.COMBAT), path);
	}

	@Test
	public void collectionLogWithBossIdUsesCollectionLogIconNotBossIcon()
	{
		String path = IconResolver.resolveTaskTileLocalIconPath(TaskTypes.COLLECTION_LOG, "Zulrah (Collection Log)", "zulrah");
		Assert.assertNotNull(path);
		Assert.assertEquals(IconResolver.resolveTaskTypeLocalIconPath(TaskTypes.COLLECTION_LOG), path);
		Assert.assertTrue(path.startsWith(IconResources.TASK_ICONS_RESOURCE_PREFIX));
		Assert.assertFalse(path.startsWith(IconResources.BOSS_ICONS_RESOURCE_PREFIX));
	}
}
