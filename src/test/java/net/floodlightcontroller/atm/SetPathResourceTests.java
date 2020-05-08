package net.floodlightcontroller.atm;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.floodlightcontroller.test.FloodlightTestCase;

public class SetPathResourceTests extends FloodlightTestCase {

	private SetPathResource setPathResource;
	private static String exampleDPID = "AA:BB:CC:DD:EE:FF:00:11";
	private static String exampleFlowName = "flow-mode-1";
	private static int exampleInPort = 1;
	private static int exampleOutPort = 2;
	private static String exampleJson = "{\n" + "\"dpid\":       \""
			+ exampleDPID + "\",\n" + "\"name\":         \"" + exampleFlowName
			+ "\",\n" + "\"inPort\": 		 \"" + String.valueOf(exampleInPort)
			+ "\",\n" + "\"outPort\":      \"" + String.valueOf(exampleOutPort)
			+ "\"\n" + "}";
	private static String exampleEmptyJson = "{}";
	private static String exampleWrongFormatJson = "\n" + "\"dpid\":       \""
			+ exampleDPID + "\",\n" + "\"name\":         \"" + exampleFlowName
			+ "\",\n" + "\"inPort\": \"" + String.valueOf(exampleInPort)
			+ "\",\n" + "\"outPort\":      \"" + String.valueOf(exampleOutPort)
			+ "\"\n" + "";

	@Override
	public void setUp() throws Exception {
		this.setPathResource = new SetPathResource();
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
	public void whenGetAffectedSwitches_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenGetUnfinishedSwitches_thenCorrect() throws Exception {
		Assert.fail("NOT IMPLEMENTED YET");
	}

	@Test
	public void whenGetUnconfirmedSwitches_thenCorrect() throws Exception {
		Assert.fail("NOT IMPLEMENTED YET");
	}

	@Test
	public void whenCreateFlowMods_thenCorrect() throws Exception {
		Assert.fail("NOT IMPLEMENTED YET");
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

		Assert.assertEquals("OF_14", testMod1.getVersion().toString());
		Assert.assertEquals(testDTO.outPort, testMod1.getOutPort()
				.getPortNumber());
		Assert.assertEquals(16, testMod1.getXid());
		Assert.assertEquals("ADD", testMod1.getCommand().toString());

		// Test Instructions (Actions)
		Assert.assertEquals(1, testMod1.getInstructions().size());
		Assert.assertTrue(testMod1.getInstructions().get(0).toString()
				.indexOf("port=" + String.valueOf(testDTO.outPort)) > 0);

		Assert.assertEquals(testDTO.inPort,
				testMod1.getMatch().get(MatchField.IN_PORT).getPortNumber());
		Assert.assertEquals(EthType.IPv4,
				testMod1.getMatch().get(MatchField.ETH_TYPE));
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
	public void whenSerializeAndDeserializeUsingJackson_thenCorrect()
			throws IOException {
		FlowModDTO flowMod1 = new FlowModDTO();
		flowMod1.dpid = "testDPID";
		flowMod1.name = "Flow1";
		flowMod1.inPort = 0;
		flowMod1.outPort = 1;

		FlowModDTO flowMod2 = new FlowModDTO();
		flowMod2.dpid = "testDPID2";
		flowMod2.name = "Flow1";
		flowMod2.inPort = 3;
		flowMod2.outPort = 1;

		ArrayList<FlowModDTO> flowMods = new ArrayList<>();
		flowMods.add(flowMod1);
		flowMods.add(flowMod2);

		ObjectMapper mapper = new ObjectMapper();
		String jsonStr = mapper.writeValueAsString(flowMods);
		String expectedStr = "[{\"dpid\":\"testDPID\",\"name\":\"Flow1\",\"inPort\":0,\"outPort\":1},{\"dpid\":\"testDPID2\",\"name\":\"Flow1\",\"inPort\":3,\"outPort\":1}]";
		Assert.assertEquals(expectedStr, jsonStr);

		ArrayList<FlowModDTO> result = mapper.readValue(jsonStr,
				new TypeReference<ArrayList<FlowModDTO>>() {
				});
		Assert.assertEquals(flowMods.size(), result.size());

		for (int i = 0; i < 2; i++) {
			FlowModDTO currentResult = result.get(i);
			FlowModDTO currentOriginal = flowMods.get(i);

			Assert.assertTrue(currentOriginal.equals(currentResult));
		}
	}

	@Test
	public void whenConvertByteArrToLong_thenCorrect() {
		byte[] arr = new byte[4];
		arr[3] = new Integer(17).byteValue();

		long result = this.setPathResource.convertByteArrToLong(arr);
		
		Assert.assertEquals(17, result);
	}

	@Test
	public void whenCreateActions_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}

	@Test
	public void whenCreateMath_thenCorrect() {
		Assert.fail("NOT IMPLEMENTED YET!");
	}
}