package net.floodlightcontroller.atm;

import java.util.ArrayList;

import net.floodlightcontroller.test.FloodlightTestCase;

import org.junit.Assert;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;

public class ACIDUpdaterServiceTests extends FloodlightTestCase {

	private OFFactoryVer14 factory = new OFFactoryVer14();
	private ACIDUpdaterService testAUS;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		testAUS = new ACIDUpdaterService();
	}

	@Test
	public void whenReceive_thenCorrect() {
		Assert.fail("NIPY");
	}

	@Test
	public void whenVoteLock_thenCorrect() {
		Assert.fail("NIPY");
	}

	@Test
	public void whenRollback_thenCorrect() {
		Assert.fail("NIPY");
	}

	@Test
	public void whenCommit_thenCorrect() {
		Assert.fail("NIPY");
	}

	@Test
	public void whenCreateNewUpdateIDAndPrepareMessages_thenCorrect() {
		Assert.fail("NIPY");
	}

	@Test
	public void whenGetMessages_thenCorrect() {
		UpdateID testUpdateId1 = new UpdateID(3), testUpdateId2 = new UpdateID(7), testUpdateId3 = new UpdateID(49);
		
		Assert.assertNull(this.testAUS.getMessages(testUpdateId1));
		
		this.testAUS.messages.put(testUpdateId2, new ArrayList<MessagePair>());
		Assert.assertEquals(0, this.testAUS.getMessages(testUpdateId2).size());
		
		ArrayList<MessagePair> test3List = new ArrayList<MessagePair>();
		this.testAUS.messages.put(testUpdateId3, test3List);
		test3List.add(null);
		Assert.assertEquals(1, this.testAUS.getMessages(testUpdateId3).size());
	}
}
