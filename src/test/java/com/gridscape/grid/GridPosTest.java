package com.gridscape.grid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GridPosTest
{
	@Test
	public void parseValidCoordinates()
	{
		assertArrayEquals(new int[]{ 0, 0 }, GridPos.parse("0,0"));
		assertArrayEquals(new int[]{ -1, 2 }, GridPos.parse(" -1 , 2 "));
	}

	@Test
	public void parseInvalidReturnsNull()
	{
		assertNull(GridPos.parse(null));
		assertNull(GridPos.parse(""));
		assertNull(GridPos.parse("0"));
		assertNull(GridPos.parse("a,b"));
	}

	@Test
	public void normalizeTrimsAndReformats()
	{
		assertEquals("0,0", GridPos.normalize(" 0 , 0 "));
		assertEquals("bad", GridPos.normalize("bad"));
		assertEquals("", GridPos.normalize(null));
	}

	@Test
	public void neighbors4CardinalOnly()
	{
		List<String> n = GridPos.neighbors4(0, 0);
		assertEquals(4, n.size());
		Set<String> expected = new HashSet<>(Arrays.asList("1,0", "-1,0", "0,1", "0,-1"));
		assertEquals(expected, new HashSet<>(n));
	}

	@Test
	public void neighbors4InvalidPosReturnsEmpty()
	{
		assertTrue(GridPos.neighbors4("not-a-pos").isEmpty());
	}

	@Test
	public void chebyshevDistAndRingNumber()
	{
		assertEquals(0, GridPos.chebyshevDist(0, 0, 0, 0));
		assertEquals(1, GridPos.chebyshevDist(0, 0, 1, 0));
		assertEquals(2, GridPos.chebyshevDist(1, 1, -1, -1));
		assertEquals(0, GridPos.ringNumber(0, 0));
		assertEquals(1, GridPos.ringNumber(1, 0));
		assertEquals(2, GridPos.ringNumber(2, -1));
	}
}
