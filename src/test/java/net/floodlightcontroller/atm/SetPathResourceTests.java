package net.floodlightcontroller.atm;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFBundleAddMsg;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlMsg;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlType;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.BundleId;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;

import net.floodlightcontroller.atm.IACIDUpdaterService.ASPSwitchStates;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.test.FloodlightTestCase;

public class SetPathResourceTests extends FloodlightTestCase {

	private SetPathResource testSPR;
	private ACIDUpdaterService testUpdaterService;
	private final String testDPID1 = "AA:BB:CC:DD:EE:FF:00:11";
	private final String testDPID2 = "AA:BB:CC:DD:EE:FF:00:22";
	private final String testFlowName = "flow-mode-1";
	private final int testInPort = 1;
	private final int testOutPort = 2;
	private final String testJson1 = "{\n" + "\"dpid\":       \"" + testDPID1
			+ "\",\n" + "\"name\":         \"" + testFlowName + "\",\n"
			+ "\"inPort\": 		 \"" + String.valueOf(testInPort) + "\",\n"
			+ "\"outPort\":      \"" + String.valueOf(testOutPort) + "\"\n"
			+ "}";
	private final String testJson2 = "{\n" + "\"dpid\":       \"" + testDPID2
			+ "\",\n" + "\"name\":         \"" + testFlowName + "\",\n"
			+ "\"inPort\": 		 \"" + String.valueOf(testInPort) + "\",\n"
			+ "\"outPort\":      \"" + String.valueOf(testOutPort) + "\"\n"
			+ "}";
	private final String testEmptyJson = "{}";
	private final String testWrongFormatJson = "\n" + "\"dpid\":       \""
			+ testDPID1 + "\",\n" + "\"name\":         \"" + testFlowName
			+ "\",\n" + "\"inPort\": \"" + String.valueOf(testInPort) + "\",\n"
			+ "\"outPort\":      \"" + String.valueOf(testOutPort) + "\"\n"
			+ "";
	private FlowModDTO testDTO1 = new FlowModDTO(testDPID1, "testName", 0, 1);
	private FlowModDTO testDTO2 = new FlowModDTO(testDPID2, "testName", 1, 6);
	private IOFSwitch testSwitch1;
	private IOFSwitch testSwitch2;
	private final String testMessagesRaw = "[" + testJson1 + "," + testJson2
			+ "]";

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.testSPR = new SetPathResource();
		this.testUpdaterService = new ACIDUpdaterService();
		mockSwitchManager = getMockSwitchService();
		Assert.assertNotNull(mockSwitchManager);

		// Mock switches
		DatapathId dpid1 = DatapathId.of(testDTO1.dpid);
		IOFSwitch sw1 = EasyMock.createNiceMock(IOFSwitch.class);
		expect(sw1.getId()).andReturn(dpid1).anyTimes();
		expect(sw1.getOFFactory()).andReturn(
				OFFactories.getFactory(OFVersion.OF_14)).anyTimes();
		replay(sw1);
		this.testSwitch1 = sw1;

		DatapathId dpid2 = DatapathId.of(testDTO2.dpid);
		IOFSwitch sw2 = EasyMock.createNiceMock(IOFSwitch.class);
		expect(sw2.getId()).andReturn(dpid2).anyTimes();
		expect(sw2.getOFFactory()).andReturn(
				OFFactories.getFactory(OFVersion.OF_14)).anyTimes();
		replay(sw2);
		this.testSwitch2 = sw2;

		Map<DatapathId, IOFSwitch> switches = new HashMap<>();
		switches.put(dpid1, sw1);
		switches.put(dpid2, sw2);
		mockSwitchManager.setSwitches(switches);
	}

	@Test
	public void whenUpdateNetwork_thenCorrect() throws Exception {
		Capture<OFMessage> wc1 = new Capture<OFMessage>(CaptureType.ALL); // VoteLock:
																			// Bundle
																			// Open
		Capture<OFMessage> wc2 = new Capture<OFMessage>(CaptureType.ALL); // VoteLock:
																			// FlowMod
		Capture<OFMessage> wc3 = new Capture<OFMessage>(CaptureType.ALL); // VoteLock:
																			// FlowMod
																			// Update
		Capture<OFMessage> wc4 = new Capture<OFMessage>(CaptureType.ALL); // VoteLock:
																			// Bundle
																			// Commit
		Capture<OFMessage> wc5 = new Capture<OFMessage>(CaptureType.ALL); // Commit:
																			// Commit

		IOFSwitch switch1 = createMock(IOFSwitch.class);
		expect(switch1.getId()).andReturn(this.testSwitch1.getId()).anyTimes();
		expect(switch1.getBuffers()).andReturn((long) 100).anyTimes();
		expect(switch1.write(EasyMock.capture(wc1))).andReturn(true).once();
		expect(switch1.write(EasyMock.capture(wc2))).andReturn(true).once();
		expect(switch1.write(EasyMock.capture(wc3))).andReturn(true).once();
		expect(switch1.write(EasyMock.capture(wc4))).andReturn(true).once();
		expect(switch1.write(EasyMock.capture(wc5))).andReturn(true).once();
		replay(switch1);

		UpdateID testID = new UpdateID(3);
		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = new HashMap<>();
		OFFlowAdd updateMsg = this.testSPR.createFlowMod(testDTO1,
				testID.toLong());
		switchesAndFlowMods.put(switch1, updateMsg);
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(switch1);
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(switch1, ASPSwitchStates.FINISHED); 

		try {
			// FAILS AS mock is not suited for writing messages as list
			this.testSPR._updateService = this.testUpdaterService;
			this.testSPR._switchesAndFlowMods = switchesAndFlowMods;
			this.testSPR._switchStates = states;
			this.testSPR._updateID = testID;
			List<IOFSwitch> unfinishedSwitches = this.testSPR.updateNetwork();

			Assert.assertEquals(0, unfinishedSwitches.size());
			testIfWCsAreFullVoteLock(wc1, wc2, wc3, wc4, testID.toLong(),
					updateMsg);
			testIfWCIsASPCommit(wc5, testID.toLong());
		} catch (InterruptedException e) {
			System.out.println("Encountered Interrupt during Update: "
					+ e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Encountered Exception during Update: "
					+ e.getMessage());
			e.printStackTrace();
		}

	}

	public void testIfWCsAreFullVoteLock(Capture<OFMessage> wcOpen,
			Capture<OFMessage> wcFlowMod, Capture<OFMessage> wcUpdate,
			Capture<OFMessage> wcBundleCommit, long xid, OFFlowMod update) {
		BundleId bundleID = null;

		// check open
		bundleID = testWCBundleCtrl(wcOpen, OFBundleCtrlType.OPEN_REQUEST, xid,
				bundleID);

		// check flowmod
		testWCBundleAdd(wcFlowMod, OFFlowModCommand.MODIFY_STRICT, xid,
				bundleID);

		// check Update
		testWCBundleAdd(wcUpdate, OFFlowModCommand.ADD, xid, bundleID);

		// check bundle commit
		testWCBundleCtrl(wcBundleCommit, OFBundleCtrlType.COMMIT_REQUEST, xid,
				bundleID);
	}

	public BundleId testWCBundleCtrl(Capture<OFMessage> wc,
			OFBundleCtrlType type, long xid, BundleId bundleID) {
		BundleId result = null;

		Assert.assertTrue(wc.hasCaptured());
		Assert.assertTrue(wc.getValue() instanceof OFBundleCtrlMsg);
		OFBundleCtrlMsg ctrlMsg = (OFBundleCtrlMsg) wc.getValue();
		Assert.assertEquals(xid, ctrlMsg.getXid());
		if (bundleID == null) {
			result = ctrlMsg.getBundleId();
		} else {
			Assert.assertEquals(bundleID, ctrlMsg.getBundleId());
		}
		Assert.assertEquals(type, ctrlMsg.getBundleCtrlType());

		return result;
	}

	public void testWCBundleAdd(Capture<OFMessage> wc, OFFlowModCommand cmd,
			long xid, BundleId bundleID) {
		Assert.assertTrue(wc.hasCaptured());
		Assert.assertTrue(wc.getValue() instanceof OFBundleAddMsg);
		OFBundleAddMsg addMsg = (OFBundleAddMsg) wc.getValue();
		Assert.assertEquals(xid, addMsg.getXid());

		Assert.assertEquals(bundleID, addMsg.getBundleId());

		Assert.assertTrue(addMsg.getData() instanceof OFFlowMod);
		OFFlowMod flowmod = (OFFlowMod) addMsg.getData();
		Assert.assertEquals(xid, flowmod.getXid());
		Assert.assertEquals(cmd, flowmod.getCommand());
	}

	public void testIfWCIsASPCommit(Capture<OFMessage> wcASPCommit, long xid) {
		Assert.assertTrue(wcASPCommit.hasCaptured());
		Assert.assertTrue(wcASPCommit.getValue() instanceof OFFlowMod);
		OFFlowMod commit1 = (OFFlowMod) wcASPCommit.getValue();
		Assert.assertEquals(xid, commit1.getXid());
		Assert.assertEquals(OFFlowModCommand.DELETE, commit1.getCommand());
	}

	@Test
	public void whenExecuteFirstPhase_thenCorrect1() {
		OFFactoryVer14 factory = new OFFactoryVer14();
		UpdateID testID = new UpdateID(3);

		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = new HashMap<>();
		switchesAndFlowMods.put(testSwitch1,
				this.testSPR.createFlowMod(testDTO1, testID.toLong()));
		switchesAndFlowMods.put(testSwitch2,
				this.testSPR.createFlowMod(testDTO2, testID.toLong()));
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(testSwitch1);
		affectedSwitches.add(testSwitch2);

		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.CONFIRMED); 
		states.put(testSwitch2, ASPSwitchStates.REJECTED); 

		try {
			this.testSPR._affectedSwitches = affectedSwitches;
			this.testSPR._switchesAndFlowMods = switchesAndFlowMods;
			this.testSPR._switchStates =  states;
			this.testSPR._updateID = testID;
			this.testSPR._updateService = testUpdaterService;
			List<IOFSwitch> unconfirmedSwitches = this.testSPR
					.executeFirstPhase();

			Assert.assertEquals(1, unconfirmedSwitches.size());
			Assert.assertTrue(unconfirmedSwitches.contains(testSwitch2));

		} catch (InterruptedException e) {
			Assert.assertFalse(true);
		}
	}

	@Test
	public void whenExecuteFirstPhase_thenCorrect2() {
		UpdateID testID = new UpdateID(3);

		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = new HashMap<>();
		switchesAndFlowMods.put(testSwitch1,
				this.testSPR.createFlowMod(testDTO1, testID.toLong()));
		switchesAndFlowMods.put(testSwitch2,
				this.testSPR.createFlowMod(testDTO2, testID.toLong()));
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(testSwitch1);
		affectedSwitches.add(testSwitch2);

		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.CONFIRMED); 
		states.put(testSwitch2, ASPSwitchStates.REJECTED);

		try {
			this.testSPR._affectedSwitches = affectedSwitches;
			this.testSPR._switchesAndFlowMods = switchesAndFlowMods;
			this.testSPR._switchStates =  states;
			this.testSPR._updateID = testID;
			this.testSPR._updateService = testUpdaterService;
			List<IOFSwitch> unconfirmedSwitches = this.testSPR
					.executeFirstPhase();

			Assert.assertEquals(1, unconfirmedSwitches.size());
			Assert.assertTrue(unconfirmedSwitches.contains(testSwitch2));

		} catch (InterruptedException e) {
			Assert.assertFalse(true);
		}
	}

	@Test
	public void whenExecuteSecondPhase_thenCorrect1() {
		Capture<OFMessage> wc1 = new Capture<OFMessage>(CaptureType.ALL);
		Capture<OFMessage> wc2 = new Capture<OFMessage>(CaptureType.ALL);

		IOFSwitch commitSwitch = createMock(IOFSwitch.class);
		expect(commitSwitch.getId()).andReturn(DatapathId.of(1L)).anyTimes();
		expect(commitSwitch.getBuffers()).andReturn((long) 100).anyTimes();
		expect(commitSwitch.write(EasyMock.capture(wc1))).andReturn(true)
				.once(); // expect Commit
		replay(commitSwitch);
		IOFSwitch unresponsiveSwitch = createMock(IOFSwitch.class);
		expect(unresponsiveSwitch.getId()).andReturn(DatapathId.of(1L))
				.anyTimes();
		expect(unresponsiveSwitch.getBuffers()).andReturn((long) 100)
				.anyTimes();
		expect(unresponsiveSwitch.write(EasyMock.capture(wc2))).andReturn(true)
				.once(); // expect Commit
		replay(unresponsiveSwitch);

		UpdateID testID = new UpdateID(3);

		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(commitSwitch);
		affectedSwitches.add(unresponsiveSwitch);

		List<IOFSwitch> unconfirmedSwitches = new ArrayList<>();
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(commitSwitch, ASPSwitchStates.FINISHED); 
		states.put(unresponsiveSwitch, ASPSwitchStates.CONFIRMED); 

		try {
			this.testSPR._updateService = this.testUpdaterService;
			this.testSPR._affectedSwitches = affectedSwitches;
			this.testSPR._switchStates = states;
			this.testSPR._updateID = testID;
			List<IOFSwitch> unfinishedSwitches = this.testSPR
					.executeSecondPhase(unconfirmedSwitches);

			Assert.assertEquals(1, unfinishedSwitches.size());
			Assert.assertTrue(unfinishedSwitches.contains(unresponsiveSwitch));

			Assert.assertTrue(wc1.hasCaptured());
			Assert.assertTrue(wc2.hasCaptured());

			Assert.assertTrue(wc1.getValue() instanceof OFFlowMod);
			Assert.assertTrue(wc2.getValue() instanceof OFFlowMod);

			OFFlowMod commit1 = (OFFlowMod) wc1.getValue();
			OFFlowMod commit2 = (OFFlowMod) wc2.getValue();

			Assert.assertEquals(testID.toLong(), commit1.getXid());
			Assert.assertEquals(testID.toLong(), commit2.getXid());
			Assert.assertEquals(OFFlowModCommand.DELETE, commit1.getCommand());
			Assert.assertEquals(OFFlowModCommand.DELETE, commit2.getCommand());

		} catch (InterruptedException e) {
			Assert.assertFalse(true);
		}
	}

	@Test
	public void whenExecuteSecondPhase_thenCorrect2() {
		Capture<OFMessage> wc1 = new Capture<OFMessage>(CaptureType.ALL);
		Capture<OFMessage> wc2 = new Capture<OFMessage>(CaptureType.ALL);

		IOFSwitch rollbackSwitch = createMock(IOFSwitch.class);
		expect(rollbackSwitch.getId()).andReturn(DatapathId.of(1L)).anyTimes();
		expect(rollbackSwitch.getBuffers()).andReturn((long) 100).anyTimes();
		expect(rollbackSwitch.write(EasyMock.capture(wc1))).andReturn(true)
				.once(); // expect Rollback
		replay(rollbackSwitch);
		IOFSwitch unconfirmedSwitch = createMock(IOFSwitch.class);
		expect(unconfirmedSwitch.getId()).andReturn(DatapathId.of(1L))
				.anyTimes();
		expect(unconfirmedSwitch.getBuffers()).andReturn((long) 100).anyTimes();
		expect(unconfirmedSwitch.write(EasyMock.capture(wc2))).andReturn(true)
				.once(); // expect Nothing
		replay(unconfirmedSwitch);

		OFFactoryVer14 factory = new OFFactoryVer14();
		UpdateID testID = new UpdateID(3);

		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(rollbackSwitch);
		affectedSwitches.add(unconfirmedSwitch);

		List<IOFSwitch> unconfirmedSwitches = new ArrayList<>();
		unconfirmedSwitches.add(unconfirmedSwitch);
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.FINISHED); 

		try {
			this.testSPR._updateService = this.testUpdaterService;
			this.testSPR._affectedSwitches = affectedSwitches;
			this.testSPR._switchStates =  states;
			this.testSPR._updateID = testID;
			List<IOFSwitch> unfinishedSwitches = this.testSPR
					.executeSecondPhase(unconfirmedSwitches);

			Assert.assertEquals(null, unfinishedSwitches);

			Assert.assertTrue(wc1.hasCaptured());
			Assert.assertFalse(wc2.hasCaptured());

			Assert.assertTrue(wc1.getValue() instanceof OFFlowMod);
			OFFlowMod commit1 = (OFFlowMod) wc1.getValue();
			Assert.assertEquals(testID.toLong(), commit1.getXid());
			Assert.assertEquals(OFFlowModCommand.DELETE_STRICT,
					commit1.getCommand());

		} catch (InterruptedException e) {
			Assert.assertFalse(true);
		}
	}

	@Test
	public void whenGetSwitchesAndFlowMods_thenCorrct() {
		mockSwitchManager = getMockSwitchService();
		Assert.assertNotNull(mockSwitchManager);

		long atmID = UpdateID.createNewATMID();
		UpdateID updateID = new UpdateID(atmID);

		ArrayList<FlowModDTO> flowModDTOs = new ArrayList<>();
		flowModDTOs.add(testDTO1);
		flowModDTOs.add(testDTO2);

		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = this.testSPR
				.getSwitchesAndFlowMods(mockSwitchManager, flowModDTOs,
						updateID);

		Assert.assertEquals(2, switchesAndFlowMods.size());
		Assert.assertTrue(switchesAndFlowMods.keySet().contains(
				this.testSwitch1));
		Assert.assertTrue(switchesAndFlowMods.keySet().contains(
				this.testSwitch2));

		equalsOFFlowAdd(switchesAndFlowMods.get(this.testSwitch1),
				testDTO1.inPort, testDTO1.outPort, updateID.toLong());
		equalsOFFlowAdd(switchesAndFlowMods.get(this.testSwitch2),
				testDTO2.inPort, testDTO2.outPort, updateID.toLong());
	}

	@Test
	public void whenGetAffectedSwitch_thenCorrect() {
		ArrayList<FlowModDTO> flowModDTOs = new ArrayList<>();
		flowModDTOs.add(testDTO1);
		flowModDTOs.add(testDTO2);

		IOFSwitch testSwitch;

		testSwitch = this.testSPR
				.getAffectedSwitch(mockSwitchManager, testDTO1);
		Assert.assertEquals(testDPID1, testSwitch
				.getId().toString().toUpperCase());

		testSwitch = this.testSPR
				.getAffectedSwitch(mockSwitchManager, testDTO2);
		Assert.assertEquals(testDPID2, testSwitch
				.getId().toString().toUpperCase());
	}

	@Test
	public void whenGetUnresponsiveSwitchesPhase1WithTimeout_thenCorrect1() throws Exception {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(this.testSwitch1);		
		
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.CONFIRMED); 

		this.testSPR._switchStates = states;
		this.testSPR._affectedSwitches = affectedSwitches;
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase1WithTimeout();

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void whenGetUnresponsiveSwitchesPhase1WithTimeout_thenCorrect2() throws Exception {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(this.testSwitch1);

		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.DEFAULT); 
		
		this.testSPR._switchStates = states;
		this.testSPR._affectedSwitches = affectedSwitches;
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase1WithTimeout();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(this.testSwitch1, result.get(0));
	}

	@Test
	public void whenGetUnresponsiveSwitchesPhase1WithTimeout_thenCorrect3() throws Exception {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(this.testSwitch1);
		
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.REJECTED); 
		
		this.testSPR._switchStates = states;
		this.testSPR._affectedSwitches = affectedSwitches;
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase1WithTimeout();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(this.testSwitch1, result.get(0));
	}

	@Test
	public void whenGetUnresponsiveSwitchesPhase2WithTimeout_thenCorrect1() throws Exception {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(this.testSwitch1);

		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.FINISHED); 
		
		this.testSPR._switchStates = states;
		this.testSPR._affectedSwitches = affectedSwitches;
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase2WithTimeout();

		Assert.assertEquals(0, result.size());
	}

	@Test
	public void whenGetUnresponsiveSwitchesPhase2WithTimeout_thenCorrect2() throws Exception {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(this.testSwitch1);
		
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.DEFAULT); 
		
		this.testSPR._switchStates = states;
		this.testSPR._affectedSwitches = affectedSwitches;
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase2WithTimeout();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(this.testSwitch1, result.get(0));
	}

	@Test
	public void whenGetUnresponsiveSwitchesPhase2WithTimeout_thenCorrect3() throws Exception {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();
		affectedSwitches.add(this.testSwitch1);
		
		HashMap<IOFSwitch, ASPSwitchStates> states = new HashMap<>();
		states.put(testSwitch1, ASPSwitchStates.CONFIRMED); 
		
		this.testSPR._switchStates = states;
		this.testSPR._affectedSwitches = affectedSwitches;
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase2WithTimeout();

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(this.testSwitch1, result.get(0));
	}

	private void equalsOFFlowAdd(OFFlowAdd testMod1, int inPort, int outPort,
			long updateID) {
		Assert.assertEquals("OF_14", testMod1.getVersion().toString());
		Assert.assertEquals(outPort, testMod1.getOutPort().getPortNumber());
		Assert.assertEquals(updateID, testMod1.getXid());
		Assert.assertEquals("ADD", testMod1.getCommand().toString());

		// Test Instructions (Actions)
		Assert.assertEquals(1, testMod1.getInstructions().size());
		Assert.assertTrue(testMod1.getInstructions().get(0).toString()
				.indexOf("port=" + String.valueOf(outPort)) > 0);

		// Test Match
		Assert.assertEquals(inPort, testMod1.getMatch().get(MatchField.IN_PORT)
				.getPortNumber());
		Assert.assertEquals(EthType.IPv4,
				testMod1.getMatch().get(MatchField.ETH_TYPE));

	}

	@Test
	public void whenCreateFlowMod_thenCorrect() throws Exception {
		long xid = 16;

		FlowModDTO testDTO = new FlowModDTO();
		testDTO.inPort = 0;
		testDTO.outPort = 1;
		testDTO.dpid = "abc";
		testDTO.name = "testName";

		OFFlowAdd testMod1 = this.testSPR.createFlowMod(testDTO, xid);

		equalsOFFlowAdd(testMod1, testDTO.inPort, testDTO.outPort, xid);
	}

	@Test
	public void whenCreateFlowMod_thenFail() throws Exception {
		long xid = 16;

		FlowModDTO testDTO = new FlowModDTO();
		OFFlowAdd testMod1 = this.testSPR.createFlowMod(testDTO, xid);

		Assert.assertEquals(null, testMod1);
	}

	@Test
	public void whenConvertJsonToMap_thenCorrect() {

		ArrayList<FlowModDTO> testFlows = this.testSPR.convertJsonToDTO("["
				+ testJson1 + "]");
		Assert.assertEquals(1, testFlows.size());

		FlowModDTO testFlow = testFlows.get(0);

		Assert.assertEquals(testDPID1, testFlow.dpid);
		Assert.assertEquals(testFlowName, testFlow.name);
		Assert.assertEquals(testInPort, testFlow.inPort);
		Assert.assertEquals(testOutPort, testFlow.outPort);

	}

	@Test
	public void whenConvertJsonToMap_thenFail1() {

		ArrayList<FlowModDTO> flowMods = this.testSPR
				.convertJsonToDTO(testWrongFormatJson);
		Assert.assertEquals(0, flowMods.size());
	}

	@Test
	public void whenConvertJsonToMap_thenFail2() {

		ArrayList<FlowModDTO> flowMods = this.testSPR
				.convertJsonToDTO(testEmptyJson);
		Assert.assertEquals(0, flowMods.size());
	}

	@Test
	public void whenCreateActions_thenCorrect() {
		OFFactoryVer14 testFactory = new OFFactoryVer14();
		int outPort = 49;

		List<OFAction> actions = this.testSPR.createActions(testFactory,
				outPort);
		// Test Instructions (Actions)
		Assert.assertEquals(1, actions.size());
		Assert.assertTrue(actions.get(0).toString()
				.indexOf("port=" + String.valueOf(outPort)) > 0);
	}

	@Test
	public void whenCreateMatch_thenCorrect() {
		OFFactoryVer14 testFactory = new OFFactoryVer14();
		int inPort = 49;

		Match match = this.testSPR.createMatch(testFactory, inPort);

		Assert.assertEquals(inPort, match.get(MatchField.IN_PORT)
				.getPortNumber());
		Assert.assertEquals(EthType.IPv4, match.get(MatchField.ETH_TYPE));
	}
	
	@Test
	public void whenPrepareUpdate_thenCorrect(){
		this.testSPR._switchService = this.mockSwitchManager;
		this.testSPR._updateService = this.testUpdaterService;
		this.testSPR.prepareUpdate(this.testMessagesRaw, true);
	}
	
	@Test
	public void whenCheck_thenCorrect(){
		this.testSPR._affectedSwitches = new ArrayList<>();
		this.testSPR._switchStates = new HashMap<>();
		System.out.println(System.currentTimeMillis());
		List<IOFSwitch> result = this.testSPR.getUnresponsiveSwitchesPhase1WithTimeout();
		System.out.println(System.currentTimeMillis());
		
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.size());
	}
}