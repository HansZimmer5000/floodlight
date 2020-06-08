package net.floodlightcontroller.atm;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.servicechaining.ServiceNode.InsertionType;
import net.floodlightcontroller.test.FloodlightTestCase;
import net.floodlightcontroller.util.OFMessageUtils;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFBundleAddMsg;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlMsg;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlType;
import org.projectfloodlight.openflow.protocol.OFBundleFailedCode;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFailedCode;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFFlowModifyStrict;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.BundleId;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;

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
	public void whenReceive_thenCorrect1() {
		FloodlightContext testContext = null;
		UpdateID currentUpdateId;
		OFMessage currentMsg;

		// Test what happens if no UpdateID exists (= ArrayList is null), also
		// if the context if Null
		currentUpdateId = new UpdateID(1);
		currentUpdateId.restBytes = new long[] { 0, 0, 0 };
		try {
			currentMsg = this.factory.errorMsgs().buildFlowModFailedErrorMsg()
					.setXid(currentUpdateId.toLong())
					.setCode(OFFlowModFailedCode.UNKNOWN).build();
			this.testAUS.receive(testSwitch, currentMsg, testContext);
		} catch (NullPointerException e) {
			Assert.fail("NullPointException thrown, was context used (MockContext = null)? ");
		}
		this.testAUS.states.clear();
	}

	@Test
	public void whenReceive_thenCorrect2() {
		FloodlightContext testContext = null;
		Command currentCommand;
		UpdateID currentUpdateId;
		OFMessage currentMsg;
		IACIDUpdaterService.ASPSwitchStates currentStates;

		UpdateID[] testIDs = new UpdateID[] {
				new UpdateID(new byte[] { 3, 0, 0, 0 }),
				new UpdateID(new byte[] { 7, 0, 0, 0 }),
				new UpdateID(new byte[] { 49, 0, 0, 0 }),
				new UpdateID(new byte[] { 51, 0, 0, 0 }) };
		OFMessage[] testMsgs = new OFMessage[] {
				this.factory.errorMsgs().buildBundleFailedErrorMsg()
						.setXid(testIDs[0].toLong())
						.setCode(OFBundleFailedCode.MSG_FAILED).build(),
				this.factory.buildBundleCtrlMsg().setXid(testIDs[1].toLong())
						.setBundleCtrlType(OFBundleCtrlType.COMMIT_REPLY)
						.setBundleId(BundleId.of(2)).build(),
				this.factory.errorMsgs().buildFlowModFailedErrorMsg()
						.setXid(testIDs[2].toLong())
						.setCode(OFFlowModFailedCode.UNKNOWN).build(),
				this.factory.errorMsgs().buildFlowModFailedErrorMsg()
						.setXid(testIDs[3].toLong())
						.setCode(OFFlowModFailedCode.UNKNOWN).build() };
		IACIDUpdaterService.ASPSwitchStates[] initStates = new IACIDUpdaterService.ASPSwitchStates[] {
				IACIDUpdaterService.ASPSwitchStates.DEFAULT,
				IACIDUpdaterService.ASPSwitchStates.DEFAULT,
				IACIDUpdaterService.ASPSwitchStates.CONFIRMED,
				IACIDUpdaterService.ASPSwitchStates.REJECTED };
		IACIDUpdaterService.ASPSwitchStates[] expectedStates = new IACIDUpdaterService.ASPSwitchStates[] {
				IACIDUpdaterService.ASPSwitchStates.REJECTED,
				IACIDUpdaterService.ASPSwitchStates.CONFIRMED,
				IACIDUpdaterService.ASPSwitchStates.FINISHED,
				IACIDUpdaterService.ASPSwitchStates.REJECTED };

		// Test the different Messages
		for (int i = 0; i < testMsgs.length; i++) {
			currentMsg = testMsgs[i];
			currentUpdateId = testIDs[i];
			currentStates = expectedStates[i];
			System.out.println("-- Current Iteration " + String.valueOf(i)
					+ " with xid: " + currentUpdateId.toString());

			// Reset States for this XID
			HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> resetStates = new HashMap<>();
			resetStates.put(testSwitch, initStates[i]);
			this.testAUS.states.put(currentUpdateId, resetStates);

			currentCommand = this.testAUS.receive(testSwitch, currentMsg,
					testContext);

			Assert.assertEquals(Command.CONTINUE, currentCommand);
			Assert.assertNotNull(this.testAUS.getMessages(currentUpdateId));
			System.out.println(this.testAUS.states);
			Assert.assertNotNull(this.testAUS.getMessages(currentUpdateId).get(
					testSwitch));
			Assert.assertEquals(currentStates,
					this.testAUS.getMessages(currentUpdateId).get(testSwitch));
		}

		Assert.assertEquals(4, this.testAUS.states.size());
	}

	@Test
	public void whenVoteLock_thenCorrect() {
		Capture<OFMessage> wc1 = new Capture<OFMessage>(CaptureType.ALL);
		Capture<OFMessage> wc2 = new Capture<OFMessage>(CaptureType.ALL);
		Capture<OFMessage> wc3 = new Capture<OFMessage>(CaptureType.ALL);
		Capture<OFMessage> wc4 = new Capture<OFMessage>(CaptureType.ALL);

		IOFSwitch testSpecialSwitch = createMock(IOFSwitch.class);
		expect(testSpecialSwitch.getId()).andReturn(DatapathId.of(1L))
				.anyTimes();
		expect(testSpecialSwitch.getBuffers()).andReturn((long) 100).anyTimes();
		expect(testSpecialSwitch.write(EasyMock.capture(wc1))).andReturn(true)
				.once(); // expect Open
		expect(testSpecialSwitch.write(EasyMock.capture(wc2))).andReturn(true)
				.once(); // expect flowmod table=255
		expect(testSpecialSwitch.write(EasyMock.capture(wc3))).andReturn(true)
				.once(); // expect actual FlowMods
		expect(testSpecialSwitch.write(EasyMock.capture(wc4))).andReturn(true)
				.once(); // expect Commit
		replay(testSpecialSwitch);

		UpdateID testUpdateID = UpdateID.ofValue(3);
		TableId testTableID = TableId.of(34);
		int outPort = 7, inPort = 49;
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFAction action = this.factory.actions().buildOutput()
				.setPort(OFPort.of(outPort)).build();
		actions.add(action);
		MatchField<OFPort> matchField1 = MatchField.IN_PORT;
		MatchField<EthType> matchField2 = MatchField.ETH_TYPE;

		Match match = this.factory.buildMatch()
				.setExact(matchField1, OFPort.of(inPort))
				.setExact(matchField2, EthType.IPv4).build();

		OFFlowAdd testFlowAdd = this.factory.buildFlowAdd().setMatch(match)
				.setActions(actions).setOutPort(OFPort.of(outPort))
				.setBufferId(OFBufferId.NO_BUFFER)
				.setXid(testUpdateID.toLong()).setTableId(testTableID).build();

		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = new HashMap<>();
		switchesAndFlowMods.put(testSpecialSwitch, testFlowAdd);

		// FAILS AS mock is not suited for writing messages as list
		this.testAUS.voteLock(switchesAndFlowMods);

		Assert.assertTrue(wc1.hasCaptured());
		Assert.assertTrue(wc2.hasCaptured());
		Assert.assertTrue(wc3.hasCaptured());
		Assert.assertTrue(wc4.hasCaptured());

		Assert.assertTrue(wc1.getValue() instanceof OFBundleCtrlMsg);
		Assert.assertTrue(wc2.getValue() instanceof OFBundleAddMsg);
		Assert.assertTrue(wc3.getValue() instanceof OFBundleAddMsg);
		Assert.assertTrue(wc4.getValue() instanceof OFBundleCtrlMsg);

		OFBundleCtrlMsg openBundle = (OFBundleCtrlMsg) wc1.getValue();
		OFBundleAddMsg staticFlowModBundle = (OFBundleAddMsg) wc2.getValue();
		OFBundleAddMsg updateBundle = (OFBundleAddMsg) wc3.getValue();
		OFBundleCtrlMsg commitBundle = (OFBundleCtrlMsg) wc4.getValue();

		BundleId bundleId = openBundle.getBundleId();
		Assert.assertEquals(bundleId, staticFlowModBundle.getBundleId());
		Assert.assertEquals(bundleId, updateBundle.getBundleId());
		Assert.assertEquals(bundleId, commitBundle.getBundleId());

		Assert.assertEquals(OFBundleCtrlType.OPEN_REQUEST,
				openBundle.getBundleCtrlType());
		Assert.assertEquals(OFBundleCtrlType.COMMIT_REQUEST,
				commitBundle.getBundleCtrlType());

		Assert.assertTrue(staticFlowModBundle.getData() instanceof OFFlowModifyStrict);
		Assert.assertTrue(updateBundle.getData() instanceof OFFlowAdd);
		OFFlowModifyStrict staticFlowModMsg = (OFFlowModifyStrict) staticFlowModBundle
				.getData();
		OFFlowAdd updateMsg = (OFFlowAdd) updateBundle.getData();

		Assert.assertEquals(testUpdateID.toLong(), openBundle.getXid());
		Assert.assertEquals(testUpdateID.toLong(), staticFlowModBundle.getXid());
		Assert.assertEquals(testUpdateID.toLong(), updateBundle.getXid());
		Assert.assertEquals(testUpdateID.toLong(), commitBundle.getXid());
		Assert.assertEquals(testUpdateID.toLong(), staticFlowModMsg.getXid());
		Assert.assertEquals(testUpdateID.toLong(), updateMsg.getXid());

		Assert.assertEquals(TableId.of(255), staticFlowModMsg.getTableId());
		Assert.assertEquals(testFlowAdd, updateMsg);
	}

	@Test
	public void whenRollback_thenCorrect() {
		UpdateID testID = new UpdateID(UpdateID.createNewATMID());
		Capture<OFMessage> wc1 = new Capture<OFMessage>(CaptureType.ALL);
		IOFSwitch mockSwitch = createMock(IOFSwitch.class);
		expect(mockSwitch.getId()).andReturn(DatapathId.of(1L)).anyTimes();
		expect(mockSwitch.getBuffers()).andReturn((long) 100).anyTimes();
		expect(mockSwitch.write(EasyMock.capture(wc1))).andReturn(true).once(); // expect
																				// packetOut
		replay(mockSwitch);

		ArrayList<IOFSwitch> switches = new ArrayList<>();
		switches.add(mockSwitch);

		this.testAUS.rollback(switches, testID.toLong());

		assertTrue(wc1.hasCaptured());
		Assert.assertTrue(wc1.getValue() instanceof OFFlowMod);
		OFFlowMod rollback = (OFFlowMod) wc1.getValue();
		Assert.assertEquals(testID.toLong(), rollback.getXid());
		Assert.assertEquals(OFFlowModCommand.DELETE_STRICT,
				rollback.getCommand());
		Assert.assertEquals(65535, rollback.getPriority());
	}

	@Test
	public void whenCommit_thenCorrect() {
		UpdateID testID = new UpdateID(UpdateID.createNewATMID());
		Capture<OFMessage> wc1 = new Capture<OFMessage>(CaptureType.ALL);
		IOFSwitch mockSwitch = createMock(IOFSwitch.class);
		expect(mockSwitch.getId()).andReturn(DatapathId.of(1L)).anyTimes();
		expect(mockSwitch.getBuffers()).andReturn((long) 100).anyTimes();
		expect(mockSwitch.write(EasyMock.capture(wc1))).andReturn(true).once(); // expect
																				// packetOut
		replay(mockSwitch);

		ArrayList<IOFSwitch> switches = new ArrayList<>();
		switches.add(mockSwitch);

		this.testAUS.commit(switches, testID.toLong());

		assertTrue(wc1.hasCaptured());
		Assert.assertTrue(wc1.getValue() instanceof OFFlowMod);
		OFFlowMod commit1 = (OFFlowMod) wc1.getValue();
		Assert.assertEquals(testID.toLong(), commit1.getXid());
		Assert.assertEquals(OFFlowModCommand.DELETE, commit1.getCommand());
		Assert.assertEquals(65535, commit1.getPriority());
	}

	@Test
	public void whenCreateNewUpdateIDAndPrepareMessages_thenCorrect() {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		// TODO FIll array up and check if aall there and init with DEFUALT
		// enum.
		UpdateID testUpdateId = this.testAUS.createNewUpdateID();
		this.testAUS.initXIDStates(testUpdateId,
				affectedSwitches);

		Assert.assertNotNull(testUpdateId);
		Assert.assertNotNull(this.testAUS.getMessages(testUpdateId));
		Assert.assertEquals(1, this.testAUS.states.size());
		Assert.assertEquals(0, this.testAUS.getMessages(testUpdateId).size());
	}

	@Test
	public void whenGetMessages_thenCorrect() {
		UpdateID testUpdateId1 = new UpdateID(3), testUpdateId2 = new UpdateID(
				7), testUpdateId3 = new UpdateID(49);

		Assert.assertNull(this.testAUS.getMessages(testUpdateId1));

		this.testAUS.states.put(testUpdateId2,
				new HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates>());
		Assert.assertEquals(0, this.testAUS.getMessages(testUpdateId2).size());

		HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> test3Map = new HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates>();
		this.testAUS.states.put(testUpdateId3, test3Map);
		test3Map.put(null, null);
		Assert.assertEquals(1, this.testAUS.getMessages(testUpdateId3).size());

		Assert.assertEquals(2, this.testAUS.states.size());
	}
}
