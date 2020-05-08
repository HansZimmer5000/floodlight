package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SetPathResource extends ServerResource {
	protected static Logger log = LoggerFactory
			.getLogger(SetPathResource.class);
	static final long ASP_TIMEOUT_MS = 1;

	@Put
	public String SetPath(String jsonBody) {
		log.debug("SetPathReceived:" + jsonBody);

		// TODO 418 = new Status(418);

		Status status;
		String message = "";

		// Get Services
		IACIDUpdaterService updateService = (IACIDUpdaterService) this
				.getContext().getAttributes()
				.get(IACIDUpdaterService.class.getCanonicalName());

		IOFSwitchService switchService = (IOFSwitchService) this.getContext()
				.getAttributes().get(IOFSwitchService.class.getCanonicalName());

		// Extract info from Request
		ArrayList<FlowModDTO> flowModDTOs = convertJsonToDTO(jsonBody);

		// Prepare Update
		Byte[] updateID = updateService.createNewUpdateIDAndPrepareMessages();
		List<OFFlowAdd> flowMods = createFlowMods(flowModDTOs, updateID);
		if (flowMods != null) {
			status = getAffectedSwitchesAndGetMessagesAndUpdateNetwork(
					switchService, updateService, flowModDTOs, flowMods,
					updateID);
		} else {
			log.debug("FlowMods could not be created");
			status = Status.CLIENT_ERROR_BAD_REQUEST;
		}

		// Respond to user
		setStatus(status);
		return message;
	}

	public Status getAffectedSwitchesAndGetMessagesAndUpdateNetwork(
			IOFSwitchService switchService, IACIDUpdaterService updateService,
			ArrayList<FlowModDTO> flowModDTOs, List<OFFlowAdd> flowMods,
			Byte[] updateID) {
		Status status;
		ArrayList<IOFSwitch> affectedSwitches = getAffectedSwitches(
				switchService, flowModDTOs);
		if (flowModDTOs.size() == affectedSwitches.size()) {

			status = getMessagesAndUpdateNetwork(updateService, flowMods,
					affectedSwitches, updateID);
		} else {
			log.debug("Could not find all switches");
			status = Status.CLIENT_ERROR_NOT_FOUND;
		}

		return status;
	}

	public Status getMessagesAndUpdateNetwork(
			IACIDUpdaterService updateService, List<OFFlowAdd> flowMods,
			ArrayList<IOFSwitch> affectedSwitches, Byte[] updateID) {
		Status status;
		List<MessagePair> messages = updateService.getMessages(updateID);
		if (messages != null) {

			// Update
			updateNetwork(updateService, flowMods, affectedSwitches, messages);
			status = Status.SUCCESS_NO_CONTENT;
		} else {
			log.debug("Messages was null");
			status = Status.SERVER_ERROR_INTERNAL;
		}

		return status;
	}

	public void updateNetwork(IACIDUpdaterService updateService,
			List<OFFlowAdd> flowMods, ArrayList<IOFSwitch> affectedSwitches,
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
				log.debug("Encountered " + unfinishedSwitches.size()
						+ " unfinished Switches: "
						+ unfinishedSwitches.toString());
			}
		} catch (InterruptedException e) {
			log.debug("Encountered Interrupt during Update: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			log.debug("Encountered Exception during Update: " + e.getMessage());
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
			
			List<IOFSwitch> readySwitches = new ArrayList<>(affectedSwitches);
			readySwitches.removeAll(unconfirmedSwitches);
			
			updateService.rollback(readySwitches);
		}
	}

	public List<IOFSwitch> executeThirdPhase(IACIDUpdaterService updateService,
			ArrayList<IOFSwitch> affectedSwitches, List<MessagePair> messages)
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

	public ArrayList<IOFSwitch> getAffectedSwitches(
			IOFSwitchService switchService, List<FlowModDTO> flowModDTOs) {
		ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>();

		for (FlowModDTO currentFlowModDTO : flowModDTOs) {
			DatapathId dpid = DatapathId.of(currentFlowModDTO.dpid);
			IOFSwitch currentSwitch = switchService.getSwitch(dpid);
			affectedSwitches.add(currentSwitch);
		}

		return affectedSwitches;
	}

	public List<IOFSwitch> getUnfinishedSwitches(List<MessagePair> messages,
			ArrayList<IOFSwitch> affectedSwitches) {
		List<IOFSwitch> unfinishedSwitches = new ArrayList<>(affectedSwitches);
		IOFSwitch messageSwitch;
		boolean elemIsRemoved;

		for (MessagePair currentMP : messages) {
			messageSwitch = currentMP.ofswitch;
			elemIsRemoved = unfinishedSwitches.remove(messageSwitch);
			if (!elemIsRemoved) {
				log.debug("Finished Switch could not be removed: "
						+ messageSwitch.getId().toString());
			}
		}

		return unfinishedSwitches;
	}

	public List<IOFSwitch> getUnconfirmedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		List<IOFSwitch> unconfirmedSwitches = new ArrayList<>(affectedSwitches);
		IOFSwitch messageSwitch;
		boolean elemIsRemoved;

		for (MessagePair currentMP : messages) {
			messageSwitch = currentMP.ofswitch;
			elemIsRemoved = unconfirmedSwitches.remove(messageSwitch);
			if (!elemIsRemoved) {
				log.debug("Confirmed Switch could not be removed: "
						+ messageSwitch.getId().toString());
			}
		}

		return unconfirmedSwitches;
	}

	public List<OFFlowAdd> createFlowMods(ArrayList<FlowModDTO> flowModDTOs,
			Byte[] updateID) {

		List<OFFlowAdd> result = new ArrayList<>();
		OFFlowAdd newFlowMod;

		long xid = convertByteArrToLong(updateID);

		for (FlowModDTO currentFlowModDTO : flowModDTOs) {
			newFlowMod = createFlowMod(currentFlowModDTO, xid);
			if (newFlowMod == null) {
				return null;
			} else {
				result.add(newFlowMod);
			}
		}

		return result;
	}

	// Create a Flowmod that someone has to write to a switch with:
	// IOFSwitch.write(flow);
	public OFFlowAdd createFlowMod(FlowModDTO flowModDTO, long xid) {
		// TODO set tableID here with some of the builders (is this neccesarry
		// for for VoteLock?)

		if (flowModDTO.dpid == FlowModDTO.STRING_DEFAULT
				|| flowModDTO.name == FlowModDTO.STRING_DEFAULT
				|| flowModDTO.inPort == FlowModDTO.INT_DEFAULT
				|| flowModDTO.outPort == FlowModDTO.INT_DEFAULT) {
			return null;
		} else {
			TableId tableID = TableId.of(255);
			int inPort = flowModDTO.inPort;
			int outPort = flowModDTO.outPort;

			OFFactoryVer14 myFactory = new OFFactoryVer14();
			List<OFAction> actions = createActions(myFactory, outPort);
			Match match = createMatch(myFactory, inPort);

			OFFlowAdd flow = myFactory.buildFlowAdd().setMatch(match)
					.setActions(actions).setOutPort(OFPort.of(outPort))
					.setBufferId(OFBufferId.NO_BUFFER).setXid(xid)
					.setTableId(tableID).build();

			return flow;
		}
	}

	public long convertByteArrToLong(Byte[] bytes) {
		long result = 0;
		for (int i = 0; i < bytes.length; i++) {
			result = result << 8;
			result += bytes[i];
		}
		return result;
	}

	public ArrayList<FlowModDTO> convertJsonToDTO(String json) {
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<FlowModDTO> result = new ArrayList<>();

		try {
			result = mapper.readValue(json,
					new TypeReference<ArrayList<FlowModDTO>>() {
					});
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		return result;
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
