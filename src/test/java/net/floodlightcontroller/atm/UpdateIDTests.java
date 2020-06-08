package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;

import net.floodlightcontroller.test.FloodlightTestCase;

import org.junit.Assert;
import org.junit.Test;

public class UpdateIDTests extends FloodlightTestCase {

	long testAtmID;
	UpdateID testUpdateID;
	long[] testRestBytes;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		this.testAtmID = 3;
		this.testRestBytes = new long[] { 0, 1, 2 };
		this.testUpdateID = new UpdateID(this.testAtmID);
		this.testUpdateID.restBytes = this.testRestBytes;
	}
	
	@Test
	public void whenNew_thenCorrect(){
		long testATMID = UpdateID.createNewATMID();
		UpdateID testID = new UpdateID(testATMID);
	}

	@Test
	public void whenOfValue_thenCorrect() {
		long testRaw = this.testUpdateID.toLong();
		UpdateID test2 = UpdateID.ofValue(testRaw);
		Assert.assertEquals(this.testUpdateID.toString(), test2.toString());

		long testRaw2 = (long) (Math.pow(16, 6) * 128
				+ Math.pow(16, 4) * 128 + Math.pow(16, 2) * 128 + 128);
		UpdateID test3 = UpdateID.ofValue(testRaw2);
		Assert.assertEquals(128, test3.atmID);
		Assert.assertEquals(128, test3.restBytes[0]);
		Assert.assertEquals(128, test3.restBytes[1]);
		Assert.assertEquals(128, test3.restBytes[2]);
		Assert.assertEquals(testRaw2, test3.toLong());		
	}

	@Test
	public void whenByteToLong_thenCorrect() {
		Assert.assertEquals(255, this.testUpdateID.byteToLong((byte) 255));
		Assert.assertEquals(255, this.testUpdateID.byteToLong((byte) -1));
	}

	@Test
	public void whenToLong_thenCorrect() {
		ArrayList<byte[]> testRawUpdateIDs = new ArrayList<>();
		testRawUpdateIDs.add(new byte[] { (byte) 255, (byte) 255, (byte) 255,
				(byte) 255 });
		testRawUpdateIDs.add(new byte[] { 1, 0, 0, 1 });
		testRawUpdateIDs.add(new byte[] { 1, 0, 1, 0 });
		testRawUpdateIDs.add(new byte[] { 1, 1, 0, 0 });

		long[] expectedResults = new long[] {
				(long) (Math.pow(16, 6) * 255 + Math.pow(16, 4) * 255
						+ Math.pow(16, 2) * 255 + 255),
				(long) (Math.pow(16, 6) + Math.pow(16, 0)),
				(long) (Math.pow(16, 6) + Math.pow(16, 2)),
				(long) (Math.pow(16, 6) + Math.pow(16, 4)) };

		long result, expectedResult;
		for (int index = 0; index < testRawUpdateIDs.size(); index++) {
			result = new UpdateID(testRawUpdateIDs.get(index)).toLong();
			expectedResult = expectedResults[index];

			Assert.assertEquals(expectedResult, result);
		}

		// 181779558 == 66 bcd50A
		//
	}

	@Test
	public void whenToString_thenCorrect() {
		String output = this.testUpdateID.toString();
		Assert.assertEquals("UpdateID: atmID(03) restBytes(00 01 02 )", output);
	}
}
