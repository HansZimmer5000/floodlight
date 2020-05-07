package net.floodlightcontroller.atm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

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
	private static String exampleCookieBool = "0";
	private static String examplePrio = "32768";
	private static int exampleIngressPort = 1;
	private static int exampleOutPort = 2;
	private static String exampleJson = "{\n" + "\"switch\":       \""
			+ exampleDPID + "\",\n" + "\"name\":         \"" + exampleFlowName
			+ "\",\n" + "\"cookie\":       \"" + exampleCookieBool + "\",\n"
			+ "\"priority\":     \"" + examplePrio + "\",\n"
			+ "\"ingress-port\": \"" + String.valueOf(exampleIngressPort)
			+ "\",\n" + "\"actions\":      \"output="
			+ String.valueOf(exampleOutPort) + "\"\n" + "}";
	private static String exampleEmptyJson = "{}";
	private static String exampleWrongFormatJson = "\n" + "\"switch\":       \""
			+ exampleDPID + "\",\n" + "\"name\":         \"" + exampleFlowName
			+ "\",\n" + "\"cookie\":       \"" + exampleCookieBool + "\",\n"
			+ "\"priority\":     \"" + examplePrio + "\",\n"
			+ "\"ingress-port\": \"" + String.valueOf(exampleIngressPort)
			+ "\",\n" + "\"actions\":      \"output="
			+ String.valueOf(exampleOutPort) + "\"\n" + "";
			
	@Override
	public void setUp() throws Exception {
		this.setPathResource = new SetPathResource();
	}

	@Test
	public void testgetUnfinishedSwitches() throws Exception {

	}

	@Test
	public void testgetUnconfirmedSwitches() throws Exception {

	}

	@Test
	public void testSetPath() throws Exception {

	}

	@Test
	public void testextractSwitches() throws Exception {

	}

	@Test
	public void testgextractHosts() throws Exception {

	}

	@Test
	public void testcreateFlowMods() throws Exception {

	}

	@Test
	public void whenCreateFlowMod_thenCorrect() throws Exception {
		long xid = 16;

		OFFlowAdd testMod1 = this.setPathResource.createFlowMod(exampleJson,
				xid);

		Assert.assertEquals("OF_14", testMod1.getVersion().toString());
		Assert.assertEquals(exampleOutPort, testMod1.getOutPort()
				.getPortNumber());
		Assert.assertEquals(16, testMod1.getXid());
		Assert.assertEquals("ADD", testMod1.getCommand().toString());

		// Test Instructions (Actions)
		Assert.assertEquals(1, testMod1.getInstructions().size());
		Assert.assertTrue(testMod1.getInstructions().get(0).toString()
				.indexOf("port=" + String.valueOf(exampleOutPort)) > 0);

		Assert.assertEquals(exampleIngressPort,
				testMod1.getMatch().get(MatchField.IN_PORT).getPortNumber());
		Assert.assertEquals(EthType.IPv4,
				testMod1.getMatch().get(MatchField.ETH_TYPE));
	}

	@Test
	public void whenCreateFlowMod_thenFail1() throws Exception {
		long xid = 16;

		OFFlowAdd testMod1 = this.setPathResource.createFlowMod(
				exampleEmptyJson, xid);

		Assert.assertEquals(null, testMod1);
	}

	@Test
	public void whenCreateFlowMod_thenFail2() throws Exception {
		long xid = 16;

		OFFlowAdd testMod1 = this.setPathResource.createFlowMod(
				exampleWrongFormatJson, xid);

		Assert.assertEquals(null, testMod1);
	}
	
	@Test
	public void whenConvertJsonToMap_thenCorrect() {
		try {
			Map<String, String> testMap = this.setPathResource
					.convertJsonToMap(exampleJson);

			Assert.assertEquals(exampleDPID, testMap.get("switch"));
			Assert.assertEquals(exampleFlowName, testMap.get("name"));
			Assert.assertEquals(exampleCookieBool, testMap.get("cookie"));
			Assert.assertEquals(examplePrio, testMap.get("priority"));
			Assert.assertEquals(exampleIngressPort,
					Integer.parseInt(testMap.get("ingress-port")));
			Assert.assertEquals(exampleOutPort,
					Integer.parseInt(testMap.get("actions")));
		} catch (IOException e) {
			Assert.fail(e.toString());
		}
	}
	
	@Test
	public void whenSerializeAndDeserializeUsingJackson_thenCorrect() 
	  throws IOException{
		FlowModDTO flowMod1 = new FlowModDTO();
		flowMod1.dpid="testDPID";
		flowMod1.name="Flow1";
		flowMod1.inPort=0;
		flowMod1.outPort=1;
		
		FlowModDTO flowMod2 = new FlowModDTO();
		flowMod2.dpid="testDPID2";
		flowMod2.name="Flow1";
		flowMod2.inPort=3;
		flowMod2.outPort=1;
		
	    ArrayList<FlowModDTO> flowMods = new ArrayList<>();
	    flowMods.add(flowMod1);
	    flowMods.add(flowMod2);
	    
	    ObjectMapper mapper = new ObjectMapper();
	    String jsonStr = mapper.writeValueAsString(flowMods);
	    String expectedStr = "[{\"dpid\":\"testDPID\",\"name\":\"Flow1\",\"inPort\":0,\"outPort\":1},{\"dpid\":\"testDPID2\",\"name\":\"Flow1\",\"inPort\":3,\"outPort\":1}]";
	    Assert.assertEquals(expectedStr, jsonStr);
	    
	    ArrayList<FlowModDTO> result = mapper.readValue(jsonStr, new TypeReference<ArrayList<FlowModDTO>>() {});
	    Assert.assertEquals(flowMods.size(),result.size());
	    
	    for (int i = 0; i < 2; i++) {
			FlowModDTO currentResult = result.get(i);
			FlowModDTO currentOriginal = flowMods.get(i);
			
			Assert.assertTrue(currentOriginal.equals(currentResult));
		}
	    
	}
}
