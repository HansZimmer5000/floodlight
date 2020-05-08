package net.floodlightcontroller.atm;

import org.junit.Assert;
import org.junit.Test;

public class UpdateIDTests {

	@Test
	public void whenToLong_thenCorrect() {
		UpdateID test = new UpdateID(3);

		long result = test.toLong();
		System.out.println(result);
		
		Assert.assertTrue(result > 3);
	}
}
