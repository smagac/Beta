package core.common;

import static org.junit.Assert.*;

import org.junit.Test;

import com.badlogic.gdx.Input.Keys;

import core.common.Input;

public class InputTest
{

	/**
	 * Test insuring the static method for figuring out which input key a
	 * key code corresponds to works
	 */
	@Test
	public void testValueOf()
	{
		assertEquals(Input.UP, Input.valueOf(Keys.UP));
		assertEquals(Input.DOWN, Input.valueOf(Keys.D));
		assertEquals(Input.ACCEPT, Input.valueOf(Keys.ENTER));
		assertEquals(null, Input.valueOf(Keys.Z));
	}

	/**
	 * Test insuring a keycode for a type can be determined true if that
	 * input key is matched against
	 */
	@Test
	public void testMatch()
	{
		assertTrue(Input.UP.match(Keys.UP));
		assertFalse(Input.UP.match(Keys.DOWN));
	}

}
