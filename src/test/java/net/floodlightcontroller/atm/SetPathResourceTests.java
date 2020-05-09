package net.floodlightcontroller.atm;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.test.FloodlightTestCase;

public class SetPathResourceTests extends FloodlightTestCase {

	private SetPathResource setPathResource;
	private String exampleDPID = "AA:BB:CC:DD:EE:FF:00:11";
	private String exampleFlowName = "flow-mode-1";
	private int exampleInPort = 1;
	private int exampleOutPort = 2;
	private String exampleJson = "{\n" + "\"dpid\":       \"" + exampleDPID
			+ "\",\n" + "\"name\":         \"" + exampleFlowName + "\",\n"
			+ "\"inPort\": 		 \"" + String.valueOf(exampleInPort) + "\",\n"
			+ "\"outPort\":      \"" + String.valueOf(exampleOutPort) + "\"\n"
			+ "}";
	private String exampleEmptyJson = "{}";
	private String exampleWrongFormatJson = "\n" + "\"dpid\":       \""
			+ exampleDPID + "\",\n" + "\"name\":         \"" + exampleFlowName
			+ "\",\n" + "\"inPort\": \"" + String.valueOf(exampleInPort)
			+ "\",\n" + "\"outPort\":      \"" + String.valueOf(exampleOutPort)
			+ "\"\n" + "";
	private FlowModDTO testDTO1 = new FlowModDTO("ab", "testName", 0, 1);
	private FlowModDTO testDTO2 = new FlowModDTO("ab:cd", "testName", 1, 6);
	private IOFSwitch sw1;
	private IOFSwitch sw2;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.setPathResource = new SetPathResource();

		mockSwitchManager = getMockSwitchService();
		Assert.assertNotNull(mockSwitchManager);

		// Mock switches
		DatapathId dpid1 = DatapathId.of(testDTO1.dpid);
		IOFSwitch sw1 = EasyMock.createNiceMock(IOFSwitch.class);
		expect(sw1.getId()).andReturn(dpid1).anyTimes();
		expect(sw1.getOFFactory()).andReturn(
				OFFactories.getFactory(OFVersion.OF_14)).anyTimes();
		replay(sw1);
		this.sw1 = sw1;

		DatapathId dpid2 = DatapathId.of(testDTO2.dpid);
		IOFSwitch sw2 = EasyMock.createNiceMock(IOFSwitch.class);
		expect(sw2.getId()).andReturn(dpid2).anyTimes();
		expect(sw2.getOFFactory()).andReturn(
				OFFactories.getFactory(OFVersion.OF_14)).anyTimes();
		replay(sw2);
		this.sw2 = sw2;

		Map<DatapathId, IOFSwitch> switches = new HashMap<>();
		switches.put(dpid1, sw1);
		switches.put(dpid2, sw2);
		mockSwitchManager.setSwitches(switches);
	}

	@Test
	public void whenSetPath_thenCorrect() throws Exception {
		Assert.fail("NOT IMPLEMENTED YET");
	}

	@Test
	public void whenGetAffectedSwitchesAndGetMessagesAndUpdateNetwork_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenGetMessagesAndUpdateNetwork_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenUpdateNetwork_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenExecuteFirstPhase_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenExecuteSecondPhase_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenExecuteThirdPhase_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenGetSwitchesAndFlowMods_thenCorrct() {
		mockSwitchManager = getMockSwitchService();
		Assert.assertNotNull(mockSwitchManager);

		byte atmID = UpdateID.createNewATMID();
		UpdateID updateID = new UpdateID(atmID);

		ArrayList<FlowModDTO> flowModDTOs = new ArrayList<>();
		flowModDTOs.add(testDTO1);
		flowModDTOs.add(testDTO2);

		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = this.setPathResource
				.getSwitchesAndFlowMods(mockSwitchManager, flowModDTOs,
						updateID);

		Assert.assertEquals(2, switchesAndFlowMods.size());
		Assert.assertTrue(switchesAndFlowMods.keySet().contains(this.sw1));
		Assert.assertTrue(switchesAndFlowMods.keySet().contains(this.sw2));

		equalsOFFlowAdd(switchesAndFlowMods.get(this.sw1), testDTO1.inPort,
				testDTO1.outPort, updateID.toLong());
		equalsOFFlowAdd(switchesAndFlowMods.get(this.sw2), testDTO2.inPort,
				testDTO2.outPort, updateID.toLong());
	}

	@Test
	public void whenGetAffectedSwitch_thenCorrect() {
		ArrayList<FlowModDTO> flowModDTOs = new ArrayList<>();
		flowModDTOs.add(testDTO1);
		flowModDTOs.add(testDTO2);

		IOFSwitch testSwitch;

		testSwitch = this.setPathResource.getAffectedSwitch(mockSwitchManager,
				testDTO1);
		Assert.assertEquals("00:00:00:00:00:00:00:" + testDTO1.dpid, testSwitch
				.getId().toString());

		testSwitch = this.setPathResource.getAffectedSwitch(mockSwitchManager,
				testDTO2);
		Assert.assertEquals("00:00:00:00:00:00:" + testDTO2.dpid, testSwitch
				.getId().toString());
	}

	@Test
	public void whenGetUnfinishedSwitches_thenCorrect() throws Exception {
		Assert.fail("NOT IMPLEMENTED YET");
	}

	@Test
	public void whenGetUnconfirmedSwitches_thenCorrect() throws Exception {
		Assert.fail("NOT IMPLEMENTED YET");
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

		OFFlowAdd testMod1 = this.setPathResource.createFlowMod(testDTO, xid);

		equalsOFFlowAdd(testMod1, testDTO.inPort, testDTO.outPort, xid);
	}

	@Test
	public void whenCreateFlowMod_thenFail() throws Exception {
		long xid = 16;

		FlowModDTO testDTO = new FlowModDTO();
		OFFlowAdd testMod1 = this.setPathResource.createFlowMod(testDTO, xid);

		Assert.assertEquals(null, testMod1);
	}

	@Test
	public void whenConvertJsonToMap_thenCorrect() {

		ArrayList<FlowModDTO> testFlows = this.setPathResource
				.convertJsonToDTO("[" + exampleJson + "]");
		Assert.assertEquals(1, testFlows.size());

		FlowModDTO testFlow = testFlows.get(0);

		Assert.assertEquals(exampleDPID, testFlow.dpid);
		Assert.assertEquals(exampleFlowName, testFlow.name);
		Assert.assertEquals(exampleInPort, testFlow.inPort);
		Assert.assertEquals(exampleOutPort, testFlow.outPort);

	}

	@Test
	public void whenConvertJsonToMap_thenFail1() {

		ArrayList<FlowModDTO> flowMods = this.setPathResource
				.convertJsonToDTO(exampleWrongFormatJson);
		Assert.assertEquals(0, flowMods.size());
	}

	@Test
	public void whenConvertJsonToMap_thenFail2() {

		ArrayList<FlowModDTO> flowMods = this.setPathResource
				.convertJsonToDTO(exampleEmptyJson);
		Assert.assertEquals(0, flowMods.size());
	}

	@Test
	public void whenCreateActions_thenCorrect() {
		OFFactoryVer14 testFactory = new OFFactoryVer14();
		int outPort = 49;

		List<OFAction> actions = this.setPathResource.createActions(
				testFactory, outPort);
		// Test Instructions (Actions)
		Assert.assertEquals(1, actions.size());
		Assert.assertTrue(actions.get(0).toString()
				.indexOf("port=" + String.valueOf(outPort)) > 0);
	}

	@Test
	public void whenCreateMatch_thenCorrect() {
		OFFactoryVer14 testFactory = new OFFactoryVer14();
		int inPort = 49;

		Match match = this.setPathResource.createMatch(testFactory, inPort);

		Assert.assertEquals(inPort, match.get(MatchField.IN_PORT)
				.getPortNumber());
		Assert.assertEquals(EthType.IPv4, match.get(MatchField.ETH_TYPE));
	}
}