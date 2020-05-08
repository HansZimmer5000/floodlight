package net.floodlightcontroller.atm;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FlowModDTOTests {

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
}
