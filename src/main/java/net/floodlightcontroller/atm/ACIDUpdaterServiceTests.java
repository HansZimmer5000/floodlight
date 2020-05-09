package net.floodlightcontroller.atm;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.test.FloodlightTestCase;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlType;
import org.projectfloodlight.openflow.protocol.OFBundleFailedCode;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowModFailedCode;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.BundleId;
import org.projectfloodlight.openflow.types.DatapathId;

public class ACIDUpdaterServiceTests extends FloodlightTestCase {

	private OFFactoryVer14 factory = new OFFactoryVer14();
	private ACIDUpdaterService testAUS;
	private IOFSwitch testSwitch;
	private DatapathId testDPID;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		testAUS = new ACIDUpdaterService();
		testDPID = DatapathId.of("ab");
		testSwitch = EasyMock.createNiceMock(IOFSwitch.class);
		expect(testSwitch.getId()).andReturn(testDPID).anyTimes();
		expect(testSwitch.getOFFactory()).andReturn(
				OFFactories.getFactory(OFVersion.OF_14)).anyTimes();
		replay(testSwitch);
	}

	@Test
	public void whenReceive_thenCorrect() {
		FloodlightContext testContext = null;
		Command currentCommand;
		UpdateID currentUpdateId;
		OFMessage currentMsg;
		MessagePair currentMsgPair;

		UpdateID[] testIDs = new UpdateID[] {
				new UpdateID(new byte[] { 3, 0, 0, 0 }),
				new UpdateID(new byte[] { 7, 0, 0, 0 }),
				new UpdateID(new byte[] { 49, 0, 0, 0 }) };
		OFMessage[] testMsgs = new OFMessage[] {
				this.factory.errorMsgs().buildFlowModFailedErrorMsg()
						.setXid(testIDs[0].toLong())
						.setCode(OFFlowModFailedCode.UNKNOWN).build(),
				this.factory.errorMsgs().buildBundleFailedErrorMsg()
						.setXid(testIDs[1].toLong())
						.setCode(OFBundleFailedCode.MSG_FAILED).build(),
				this.factory.buildBundleCtrlMsg().setXid(testIDs[2].toLong())
						.setBundleCtrlType(OFBundleCtrlType.COMMIT_REPLY)
						.setBundleId(BundleId.of(0)).build() };

		// Test what happens if no UpdateID exists (= ArrayList is null), also
		// if the context if Null
		currentUpdateId = new UpdateID(1);
		currentUpdateId.restBytes = new byte[] { 0, 0, 0 };
		try {
			currentMsg = this.factory.errorMsgs().buildFlowModFailedErrorMsg()
					.setXid(currentUpdateId.toLong())
					.setCode(OFFlowModFailedCode.UNKNOWN).build();
			currentCommand = this.testAUS.receive(testSwitch, currentMsg,
					testContext);
		} catch (NullPointerException e) {
			Assert.fail("NullPointException thrown, was context used (MockContext = null)? ");
		}
		this.testAUS.messages.clear();

		// Test the different Messages
		for (int i = 0; i < testMsgs.length; i++) {
			System.out.println(" -- Iteration Number: " + i);
			currentMsg = testMsgs[i];
			currentUpdateId = testIDs[i];

			this.testAUS.messages.put(currentUpdateId,
					new ArrayList<MessagePair>());
			currentMsgPair = new MessagePair(this.testSwitch, currentMsg);
			currentCommand = this.testAUS.receive(testSwitch, currentMsg,
					testContext);

			Assert.assertEquals(Command.CONTINUE, currentCommand);
			Assert.assertNotNull(this.testAUS.getMessages(currentUpdateId));
			Assert.assertTrue(currentMsgPair.equals(this.testAUS
					.getMessages(currentUpdateId).get(0)));
		}

		Assert.assertEquals(3, this.testAUS.messages.size());
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
		ArrayList<IOFSwitch> testSwitches = new ArrayList<>();
		testSwitches.add(this.testSwitch);
		
		this.testAUS.commit(testSwitches);
		Assert.fail("N fully IPY");
	}

	@Test
	public void whenCreateNewUpdateIDAndPrepareMessages_thenCorrect() {
		UpdateID testUpdateId = this.testAUS
				.createNewUpdateIDAndPrepareMessages();

		Assert.assertNotNull(testUpdateId);
		Assert.assertNotNull(this.testAUS.getMessages(testUpdateId));
		Assert.assertEquals(1, this.testAUS.messages.size());
		Assert.assertEquals(0, this.testAUS.getMessages(testUpdateId).size());
	}

	@Test
	public void whenGetMessages_thenCorrect() {
		UpdateID testUpdateId1 = new UpdateID(3), testUpdateId2 = new UpdateID(
				7), testUpdateId3 = new UpdateID(49);

		Assert.assertNull(this.testAUS.getMessages(testUpdateId1));

		this.testAUS.messages.put(testUpdateId2, new ArrayList<MessagePair>());
		Assert.assertEquals(0, this.testAUS.getMessages(testUpdateId2).size());

		ArrayList<MessagePair> test3List = new ArrayList<MessagePair>();
		this.testAUS.messages.put(testUpdateId3, test3List);
		test3List.add(null);
		Assert.assertEquals(1, this.testAUS.getMessages(testUpdateId3).size());

		Assert.assertEquals(2, this.testAUS.messages.size());
	}
}
