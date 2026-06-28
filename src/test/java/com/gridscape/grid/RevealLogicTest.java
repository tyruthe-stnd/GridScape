package com.gridscape.grid;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RevealLogicTest
{
	@Test
	public void centerAlwaysRevealedByClaimedPositions()
	{
		assertTrue(RevealLogic.revealedByClaimedPositions(0, 0, Collections.emptySet()));
		assertTrue(RevealLogic.revealedByClaimedPositions(0, 0, null));
	}

	@Test
	public void nonCenterNeedsClaimedNeighborSet()
	{
		assertFalse(RevealLogic.revealedByClaimedPositions(1, 0, null));
		assertFalse(RevealLogic.revealedByClaimedPositions(1, 0, Collections.emptySet()));
		assertTrue(RevealLogic.revealedByClaimedPositions(1, 0, Collections.singleton("x")));
	}

	@Test
	public void revealedByClaimedTaskIdsUsesCardinalNeighbors()
	{
		Set<String> claimed = new HashSet<>();
		assertTrue(RevealLogic.revealedByClaimedTaskIds(0, 0, claimed));
		assertFalse(RevealLogic.revealedByClaimedTaskIds(2, 0, claimed));

		claimed.add("1,0");
		assertTrue(RevealLogic.revealedByClaimedTaskIds(2, 0, claimed));
		assertFalse(RevealLogic.revealedByClaimedTaskIds(2, 1, claimed));
	}
}
