package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.restlet.data.Header;
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
	static final long ASP_TIMEOUT_MS = 2000;
	static final long FINISH_TIMEOUT_MS = 2000;

	public UpdateID _updateID;
	public ArrayList<FlowModDTO> _flowModDTOs;
	public Map<IOFSwitch, OFFlowAdd> _switchesAndFlowMods;
	public IACIDUpdaterService _updateService;
	public IOFSwitchService _switchService;

	@Put
	public String SetPath(String jsonBody) {
		log.error("SetPathReceived:" + jsonBody);
		prepareUpdate(jsonBody);

		Header dry_run = this.getRequest().getHeaders().getFirst("dry_run");
		if (null != dry_run) {
			// dry run
			log.error("DryRun");

			if (this._flowModDTOs.size() == 0) {
				System.out.println(jsonBody);
			}

			this.setStatus(Status.SUCCESS_OK);
			return String.valueOf(this._flowModDTOs.size());
		} else {
			// "real" run

			Status status = new Status(418);
			String message = "";

			// TODO Refactor, May move more code to "prepareUpdate"
			// TODO Refactor, May make more use of class & public field
			// structure!s
			if (this._switchesAndFlowMods != null) {
				if (this._flowModDTOs.size() == this._switchesAndFlowMods
						.size()) {

					ArrayList<IOFSwitch> affectedSwitches = new ArrayList<>(
							this._switchesAndFlowMods.keySet());
					
					log.error("Affected Switch states");
					for (IOFSwitch d : affectedSwitches){
						log.error(d.getStatus().toString());
						log.error(String.valueOf(d.getStatus().isControllable()));
					}

					List<MessagePair> messages = this._updateService
							.getMessages(this._updateID);
					if (messages != null) {
						// Update
						try {
							List<IOFSwitch> unfinishedSwitches = updateNetwork(this._updateService,
									this._switchesAndFlowMods,
									affectedSwitches, messages, this._updateID);
							
							if (unfinishedSwitches.size() > 0){
								status = Status.SERVER_ERROR_INTERNAL;
								message = "There are unfinished Switches: ";
								for (IOFSwitch sw : unfinishedSwitches){
									message += sw.getId().toString();
									message += " ";
								}
							}
						} catch (InterruptedException e) {
							log.error("Encountered Interrupt during Update: "
									+ e.getMessage());
							message = e.getMessage();
						} catch (Exception e) {
							log.error("Encountered Exception during Update: "
									+ e.getMessage());
							message = e.getMessage();
						}
						status = Status.SUCCESS_NO_CONTENT;
					} else {
						log.error("Received message array was null");
						message = "Received message array was null";
						status = Status.SERVER_ERROR_INTERNAL;
					}
				} else {
					log.error("FlowModDTOs size was not equal to switchesAndFlowMods. Are there duplicates in DTOs or switch dpids that are not existent?");
					message = "FlowModDTOs size was not equal to switchesAndFlowMods. Are there duplicates in DTOs or switch dpids that are not existent?";
					status = Status.CLIENT_ERROR_NOT_FOUND;
				}
			} else {
				log.error("FlowMods could not be created fully");
				message = "FlowMods could not be created fully";
				status = Status.CLIENT_ERROR_BAD_REQUEST;
			}

			// Respond to user
			setStatus(status);
			return message;
		}
	}

	public void prepareUpdate(String jsonBody) {
		// Get Services
		this._updateService = (IACIDUpdaterService) this.getContext()
				.getAttributes()
				.get(IACIDUpdaterService.class.getCanonicalName());

		this._switchService = (IOFSwitchService) this.getContext()
				.getAttributes().get(IOFSwitchService.class.getCanonicalName());

		// Extract info from Request
		this._flowModDTOs = convertJsonToDTO(jsonBody);

		// Prepare Update
		this._updateID = this._updateService
				.createNewUpdateIDAndPrepareMessages();
		this._switchesAndFlowMods = getSwitchesAndFlowMods(this._switchService,
				this._flowModDTOs, this._updateID);
	}

	public List<IOFSwitch> updateNetwork(IACIDUpdaterService updateService,
			Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods,
			ArrayList<IOFSwitch> affectedSwitches, List<MessagePair> messages,
			UpdateID updateID) throws InterruptedException {

		// TODO control that every message that is send to the switches has
		// correct xid!

		// Vote Lock and wait for commits
		List<IOFSwitch> unconfirmedSwitches = executeFirstPhase(updateService,
				affectedSwitches, switchesAndFlowMods, messages, updateID);

		// Rollback or Commit + wait for finishes
		// List<IOFSwitch> unfinishedSwitches =
		List<IOFSwitch> unfinished = executeSecondPhase(updateService, affectedSwitches,
				unconfirmedSwitches, messages, updateID);
		return unfinished;
	}

	public List<IOFSwitch> executeFirstPhase(IACIDUpdaterService updateService,
			List<IOFSwitch> affectedSwitches,
			Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods,
			List<MessagePair> messages, UpdateID updateID)
			throws InterruptedException {
		updateService.voteLock(switchesAndFlowMods);
		Thread.sleep(ASP_TIMEOUT_MS);// TODO how long to wait? Or actively check
		// whats inside messages?

		List<IOFSwitch> unconfirmedSwitches = getUnconfirmedSwitches(messages,
				affectedSwitches);
		return unconfirmedSwitches;
	}

	public List<IOFSwitch> executeSecondPhase(
			IACIDUpdaterService updateService,
			ArrayList<IOFSwitch> affectedSwitches,
			List<IOFSwitch> unconfirmedSwitches, List<MessagePair> messages,
			UpdateID updateID) throws InterruptedException {

		List<IOFSwitch> unfinishedSwitches = null;
		if (unconfirmedSwitches.size() > 0) {
			// Rollback
			List<IOFSwitch> readySwitches = new ArrayList<>(affectedSwitches);
			readySwitches.removeAll(unconfirmedSwitches);

			updateService.rollback(readySwitches, updateID.toLong());
			log.error("Rolledback");
		} else {
			// Commit
			updateService.commit(affectedSwitches, updateID.toLong());
			Thread.sleep(FINISH_TIMEOUT_MS); // TODO how long to wait? Or
												// actively check
			// whats inside messages?

			unfinishedSwitches = getUnfinishedSwitches(messages,
					affectedSwitches);
			log.error("Encountered " + unfinishedSwitches.size()
					+ " unfinished Switches: " + unfinishedSwitches.toString());
		}
		return unfinishedSwitches;
	}

	public IOFSwitch getAffectedSwitch(IOFSwitchService switchService,
			FlowModDTO flowModDTO) {

		DatapathId dpid = DatapathId.of(flowModDTO.dpid);
		IOFSwitch affectedSwitch = switchService.getSwitch(dpid);

		return affectedSwitch;
	}

	public Map<IOFSwitch, OFFlowAdd> getSwitchesAndFlowMods(
			IOFSwitchService switchService, ArrayList<FlowModDTO> flowModDTOs,
			UpdateID updateID) {
		Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods = new HashMap<>();
		IOFSwitch currentSwitch;
		OFFlowAdd currentFlowMod;
		long xid = updateID.toLong();

		for (FlowModDTO currentFlowModDTO : flowModDTOs) {
			currentSwitch = getAffectedSwitch(switchService, currentFlowModDTO);
			currentFlowMod = createFlowMod(currentFlowModDTO, xid);

			if (currentFlowMod == null || currentSwitch == null) {
				return null;
			}

			switchesAndFlowMods.put(currentSwitch, currentFlowMod);
		}

		return switchesAndFlowMods;
	}

	public List<IOFSwitch> getUnconfirmedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		List<IOFSwitch> unconfirmedSwitches = new ArrayList<>(affectedSwitches);
		IOFSwitch messageSwitch;
		OFMessage currentMessage;
		boolean elemIsRemoved;

		for (MessagePair currentMP : messages) {
			messageSwitch = currentMP.ofswitch;
			currentMessage = currentMP.ofmsg;
			if (currentMessage.toString().contains(
					"bundleCtrlType=COMMIT_REPLY")
					&& currentMessage.toString().contains("OFBundleCtrlMsg")) {
				elemIsRemoved = unconfirmedSwitches.remove(messageSwitch);
				if (!elemIsRemoved) {
					log.error("Confirmed Switch could not be removed: "
							+ messageSwitch.getId().toString());
				}
			}
		}

		return unconfirmedSwitches;
	}

	public List<IOFSwitch> getUnfinishedSwitches(List<MessagePair> messages,
			List<IOFSwitch> affectedSwitches) {
		List<IOFSwitch> unfinishedSwitches = new ArrayList<>(affectedSwitches);
		IOFSwitch messageSwitch;
		OFMessage currentMessage;
		boolean elemIsRemoved;

		for (MessagePair currentMP : messages) {
			messageSwitch = currentMP.ofswitch;
			currentMessage = currentMP.ofmsg;
			if (currentMessage.toString().contains("code=UNKNOWN")
					&& currentMessage.toString().contains(
							"OFFlowModFailedErrorMsg")) {
				elemIsRemoved = unfinishedSwitches.remove(messageSwitch);
				if (!elemIsRemoved) {
					log.error("Finished Switch could not be removed: "
							+ messageSwitch.getId().toString());
				}
			}
		}

		return unfinishedSwitches;
	}

	// Create a Flowmod that someone has to write to a switch with:
	// IOFSwitch.write(flow);
	public OFFlowAdd createFlowMod(FlowModDTO flowModDTO, long xid) {
		if (flowModDTO.dpid == FlowModDTO.STRING_DEFAULT
				|| flowModDTO.name == FlowModDTO.STRING_DEFAULT
				|| flowModDTO.inPort == FlowModDTO.INT_DEFAULT
				|| flowModDTO.outPort == FlowModDTO.INT_DEFAULT) {
			return null;
		} else {
			//TODO: TableID = 49 does this work?
			TableId tableID = TableId.of(49);
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

	public ArrayList<FlowModDTO> convertJsonToDTO(String json) {
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<FlowModDTO> result = new ArrayList<>();

		try {
			result = mapper.readValue(json,
					new TypeReference<ArrayList<FlowModDTO>>() {
					});
		} catch (Exception e) {
			log.error(e.getMessage());
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
