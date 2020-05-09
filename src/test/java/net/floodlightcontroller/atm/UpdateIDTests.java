package net.floodlightcontroller.atm;

import net.floodlightcontroller.test.FloodlightTestCase;

import org.junit.Assert;
import org.junit.Test;

public class UpdateIDTests extends FloodlightTestCase {

	long testAtmID;
	UpdateID testUpdateID;
	byte[] testRestBytes;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		this.testAtmID = 3;
		this.testRestBytes = new byte[] { 0, 1, 2 };
		this.testUpdateID = new UpdateID(this.testAtmID);
		this.testUpdateID.restBytes = this.testRestBytes;
	}

	@Test
	public void whenOfValue_thenCorrect() {
		long testRaw = this.testUpdateID.toLong();
		UpdateID test2 = UpdateID.ofValue(testRaw);
		Assert.assertEquals(this.testUpdateID.toString(), test2.toString());
	}

	@Test
	public void whenToLong_thenCorrect() {
		long result = this.testUpdateID.toLong();
		System.out.println(result);
		Assert.assertTrue(result > 3);
	}

	@Test
	public void whenToString_thenCorrect() {
		String output =  this.testUpdateID.toString();
		Assert.assertEquals("UpdateID: atmID(03) restBytes(00 01 02 ))", output);
	}
}
