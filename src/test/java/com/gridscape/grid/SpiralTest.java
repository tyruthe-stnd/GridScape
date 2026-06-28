package com.gridscape.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpiralTest
{
	/** Legacy inline spiral from TaskGridService (pre-dedup). */
	private static List<int[]> legacySpiralOrderForRing(int tier)
	{
		List<int[]> out = new ArrayList<>(8 * tier);
		for (int c = 1 - tier; c <= tier; c++)
			out.add(new int[]{ tier, c });
		for (int r = tier - 1; r >= -tier; r--)
			out.add(new int[]{ r, tier });
		for (int c = tier - 1; c >= -tier; c--)
			out.add(new int[]{ -tier, c });
		for (int r = -tier + 1; r <= tier; r++)
			out.add(new int[]{ r, -tier });
		return out;
	}

	@Test
	public void ringMatchesLegacyInlineOrderForTiersOneThroughThree()
	{
		for (int tier = 1; tier <= 3; tier++)
		{
			List<int[]> expected = legacySpiralOrderForRing(tier);
			List<int[]> actual = Spiral.ring(tier);
			assertEquals("tier " + tier + " size", expected.size(), actual.size());
			for (int i = 0; i < expected.size(); i++)
			{
				assertEquals("tier " + tier + " [" + i + "] row", expected.get(i)[0], actual.get(i)[0]);
				assertEquals("tier " + tier + " [" + i + "] col", expected.get(i)[1], actual.get(i)[1]);
			}
		}
	}

	@Test
	public void ringOneHasEightCellsRingTwoHasSixteenNoDuplicates()
	{
		List<int[]> ring1 = Spiral.ring(1);
		assertEquals(8, ring1.size());
		assertNoDuplicates(ring1);

		List<int[]> ring2 = Spiral.ring(2);
		assertEquals(16, ring2.size());
		assertNoDuplicates(ring2);
	}

	private static void assertNoDuplicates(List<int[]> positions)
	{
		Set<String> seen = new HashSet<>();
		for (int[] p : positions)
		{
			assertTrue(seen.add(p[0] + "," + p[1]));
		}
	}
}
