package net.floodlightcontroller.atm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.staticflowentry.StaticFlowEntries;
import net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher;
import net.floodlightcontroller.util.MatchUtils;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.protocol.ver11.OFFactoryVer11;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.restlet.data.Status;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class SetPathResource extends ServerResource {
	protected static Logger log = LoggerFactory
			.getLogger(SetPathResource.class);
	static final long ASP_TIMEOUT_MS = 1;

	/*
	 * Expect a JSON like: { "switch": "AA:BB:CC:DD:EE:FF:00:11", "name":
	 * "flow-mod-1", "cookie": "0", "priority": "32768", "ingress-port": "1",
	 * "actions": "output=2", }
	 */

	@Put
	public String SetPath(String jsonBody) {
		log.debug("SetPathReceived:" + jsonBody);

		// TODO Set status and message if error occurs somehwere during
		// execution
		Status status = Status.SUCCESS_NO_CONTENT;
		String message = "";

		// Get Services
		IACIDUpdaterService updateService = (IACIDUpdaterService) this
				.getContext().getAttributes()
				.get(IACIDUpdaterService.class.getCanonicalName());

		IOFSwitchService switchService = (IOFSwitchService) this.getContext()
				.getAttributes().get(IOFSwitchService.class.getCanonicalName());

		// Extract info from Request
		String path = extractPath(jsonBody);

		// Prepare Update
		List<IOFSwitch> affectedSwitches = extractSwitches(switchService, path);
		Byte[] updateID = updateService.createNewUpdateIDAndPrepareMessages();
		List<OFFlowAdd> flowMods = createFlowMods(jsonBody, updateID);
		List<MessagePair> messages = updateService.getMessages(updateID);

		if (messages == null) {
			log.debug("Messages was null!");
		} else {
			// Update
			updateNetwork(updateService, flowMods, affectedSwitches, messages);
		}

		// Respond to user
		setStatus(status, message);
		return message; // Returning null will still send a response but it
						// seems broken / failure
	}

	public void updateNetwork(IACIDUpdaterService updateService,
			List<OFFlowAdd> flowMods, List<IOFSwitch> affectedSwitches,
			List<MessagePair> messages) {

		try {
			// Vote Lock and wait for commits
			List<IOFSwitch> unconfirmedSwitches = executeFirstPhase(
					updateService, affectedSwitches, flowMods, messages);

			// Rollback where necessary
			executeSecondPhase(updateService, affectedSwitches,
					unconfirmedSwitches);

			// Commit and wait for finishes
			List<IOFSwitch> unfinishedSwitches = executeThirdPhase(
					updateService, affectedSwitches, messages);

			if (unfinishedSwitches.size() == 0) {
				// Everything OK
			} else {
				// TODO Something did not work out!
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<IOFSwitch> executeFirstPhase(IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches, List<OFFlowAdd> flowMods,
			List<MessagePair> messages) throws InterruptedException {
		updateService.voteLock(affectedSwitches, flowMods);
		Thread.sleep(ASP_TIMEOUT_MS);

		// TODO is variable 'messages' still up-to-date (reference) or old
		// (copy)?
		List<IOFSwitch> unconfirmedSwitches = getUnconfirmedSwitches(messages,
				affectedSwitches);
		return unconfirmedSwitches;
	}

	public void executeSecondPhase(IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches,
			List<IOFSwitch> unconfirmedSwitches) {
		if (unconfirmedSwitches.size() > 0) {
			// TODO Calculate readySwitches (affectedSwitches -
			// unconfirmedSwitches)
			List<IOFSwitch> readySwitches = new ArrayList<>();
			updateService.rollback(readySwitches);
		}
	}

	public List<IOFSwitch> executeThirdPhase(IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches, List<MessagePair> messages)
			throws InterruptedException {
		updateService.commit(affectedSwitches);
		Thread.sleep(1000); // TODO how long to wait? Or actively check
							// whats inside messages?

		// TODO is variable 'messages' still up-to-date (reference) or old
		// (copy)?
		List<IOFSwitch> unfinishedSwitches = getUnfinishedSwitches(messages,
				affectedSwitches);
		return unfinishedSwitches;
	}

	public List<IOFSwitch> getUnfinishedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	public List<IOFSwitch> getUnconfirmedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	public List<IOFSwitch> extractSwitches(IOFSwitchService switchService,
			String path) {
		// TODO Auto-generated method stub
		return new ArrayList<>();
	}

	public String extractPath(String json) {
		// TODO
		return "NOT IMPLEMENTED YET";
	}

	public List<OFFlowAdd> createFlowMods(String json, Byte[] updateID) {
		// TODO Create FlowMod per switch within the path
		
		List<OFFlowAdd> result = new ArrayList<>();
		OFFlowAdd newFlowMod;
		String flowJson = "TODO";
		
        long xid = 0;
        for (int i = 0; i < updateID.length; i++) {
            xid = xid << 8;
            xid += updateID[i];
        }

		newFlowMod = createFlowMod(flowJson, xid);
		if (null == newFlowMod) {
			return null;
		} else {
			result.add(newFlowMod);
		}

		return result;
	}

	// Create a Flowmod that someone has to write to a switch with:
	// IOFSwitch.write(flow);
	public OFFlowAdd createFlowMod(String json, long xid) {
		// TODO set tableID here with some of the builders (is this neccesarry for for VoteLock?)

		TableId tableID = TableId.of(255);
		int outPort = 0, inPort = 0;
		OFFlowAdd flow = null;

		try {
			Map<String, String> map = convertJsonToMap(json);
			String rawOutPort = map.get("actions");
			outPort = Integer.parseInt(rawOutPort);
			String rawInPort = map.get("ingress-port");
			inPort = Integer.parseInt(rawInPort);

			OFFactoryVer14 myFactory = new OFFactoryVer14();

			List<OFAction> actions = createActions(myFactory, outPort);
			Match match = createMatch(myFactory, inPort);

			flow = myFactory.buildFlowAdd().setMatch(match).setActions(actions)
					.setOutPort(OFPort.of(outPort))
					.setBufferId(OFBufferId.NO_BUFFER).setXid(xid)
					.setTableId(tableID).build();

		} catch (Exception e) {
			log.debug("Convertion to Map was not successfull: " + e.getMessage());
		}

		return flow;
	}

	public Map<String, String> convertJsonToMap(String json) throws IOException {
		Map<String, String> entry = new HashMap<String, String>();

		MappingJsonFactory f = new MappingJsonFactory();
		JsonParser jp;

		try {
			jp = f.createParser(json);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}

		jp.nextToken();
		if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected START_OBJECT");
		}

		while (jp.nextToken() != JsonToken.END_OBJECT) {
			if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
				throw new IOException("Expected FIELD_NAME");
			}

			String n = jp.getCurrentName();
			jp.nextToken();

			switch (n) {
			case StaticFlowEntryPusher.COLUMN_SWITCH:
				entry.put(StaticFlowEntryPusher.COLUMN_SWITCH, jp.getText());
				break;
			case StaticFlowEntryPusher.COLUMN_NAME:
				entry.put(StaticFlowEntryPusher.COLUMN_NAME, jp.getText());
				break;
			case StaticFlowEntryPusher.COLUMN_COOKIE: // set manually, or
				// computed from name
				entry.put(StaticFlowEntryPusher.COLUMN_COOKIE, jp.getText());
				break;
			case StaticFlowEntryPusher.COLUMN_PRIORITY:
				entry.put(StaticFlowEntryPusher.COLUMN_PRIORITY, jp.getText());
				break;
			case "ingress-port":
				entry.put("ingress-port", jp.getText());
				break;
			case StaticFlowEntryPusher.COLUMN_ACTIONS:
				String rawValue = jp.getText();
				if (rawValue.contains("output=")) {
					rawValue = rawValue.replace("output=", "");
					try {
						Integer.parseInt(rawValue);
						entry.put(StaticFlowEntryPusher.COLUMN_ACTIONS,
								rawValue);
					} catch (NumberFormatException e) {
						log.debug("Json included action that was not convertable to int: "
								+ rawValue);
						throw e;
					}
				}
				break;
			}
		}

		return entry;
	}

	public List<OFAction> createActions(OFFactoryVer14 myFactory, int outPort) {
		ArrayList<OFAction> actions = new ArrayList<OFAction>();
		OFAction action = myFactory.actions().buildOutput()
				.setPort(OFPort.of(outPort)).build();
		actions.add(action);
		return actions;
	}

	public Match createMatch(OFFactoryVer14 myFactory, int inPort) {
		MatchField<OFPort> matchField1 = MatchField.IN_PORT;
		MatchField<EthType> matchField2 = MatchField.ETH_TYPE;

		Match match = myFactory.buildMatch()
				.setExact(matchField1, OFPort.of(inPort))
				.setExact(matchField2, EthType.IPv4).build();
		return match;
	}
}
