package com.gridscape.task;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TaskGridServiceComputeRingBonusTest
{
	@Test
	public void ringTimesTierPoints()
	{
		assertEquals(10, TaskGridService.computeRingBonus(1, 1, tier -> tier * 10));
		assertEquals(50, TaskGridService.computeRingBonus(5, 2, tier -> tier * 5));
	}

	@Test
	public void cappedAt250()
	{
		assertEquals(250, TaskGridService.computeRingBonus(100, 5, tier -> tier * 10));
		assertEquals(250, TaskGridService.computeRingBonus(10, 5, tier -> tier * 100));
	}
}
