package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.floodlightcontroller.atm.IACIDUpdaterService.ASPSwitchStates;
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
	static final long ASP_CONFIRM_TIMEOUT_MS = 1500;
	static final long ASP_FINISH_TIMEOUT_MS = 1000;

	public UpdateID _updateID;
	public ArrayList<FlowModDTO> _flowModDTOs;
	public Map<IOFSwitch, OFFlowAdd> _switchesAndFlowMods;
	public IACIDUpdaterService _updateService;
	public IOFSwitchService _switchService;
	public ArrayList<IOFSwitch> _affectedSwitches;
	public HashMap<IOFSwitch, ASPSwitchStates> _switchStates;

	@Put
	public String SetPath(String jsonBody) {
		log.error("My: SetPathReceived:" + jsonBody);
		prepareUpdate(jsonBody, false);
		log.error("My: Update XID(" + this._updateID.toString() + ")");

		Header dry_run = this.getRequest().getHeaders().getFirst("dry_run");
		if (null != dry_run) {
			// dry run
			log.error("My: DryRun");

			if (this._flowModDTOs.size() == 0) {
				log.error(jsonBody);
			}

			this.setStatus(Status.SUCCESS_OK);
			return String.valueOf(this._flowModDTOs.size());
		} else {
			// "real" run

			Status status = new Status(418);
			String message = "";

			// TODO Refactor, May move more code to "prepareUpdate"
			// TODO Refactor, May make more use of class & public field
			// structures
			System.out.println("My: " + this._switchStates.toString());
			if (this._switchesAndFlowMods != null) {
				if (this._flowModDTOs.size() == this._switchesAndFlowMods
						.size()) {

					log.error("My: Affected Switch states");

					if (this._switchStates != null) {
						// Update
						try {
							log.error("My: Starting to update network");
							List<IOFSwitch> unfinishedSwitches = updateNetwork();
							log.error("My: Updated network");
							if (unfinishedSwitches == null) {
								log.error("My: Update rolledback");
								message = "Could not update due to ASP conform behaviour (lock set / rule conflict)";
							} else if (unfinishedSwitches.size() == 0) {
								status = Status.SUCCESS_NO_CONTENT;
							} else {
								status = Status.SERVER_ERROR_INTERNAL;
								message = "There are unfinished Switches: ";
								for (IOFSwitch sw : unfinishedSwitches) {
									message += sw.getId().toString();
									message += " ";
								}
							}
							log.error("My: Checked unfinished Switches");
						} catch (InterruptedException e) {
							log.error("My: Encountered Interrupt during Update: "
									+ e.getMessage());
							message = e.getMessage();
							status = Status.SERVER_ERROR_INTERNAL;
						} catch (Exception e) {
							log.error("My: Encountered Exception ("
									+ e.getClass() + ") during Update: "
									+ e.getMessage());
							message = e.toString();
							e.printStackTrace();
							status = Status.SERVER_ERROR_INTERNAL;
						}
					} else {
						log.error("My: Received message array was null");
						message = "Received message array was null";
						status = Status.SERVER_ERROR_INTERNAL;
					}
				} else {
					log.error("My: FlowModDTOs size was not equal to switchesAndFlowMods. Are there duplicates in DTOs or switch dpids that are not existent?");
					message = "FlowModDTOs size was not equal to switchesAndFlowMods. Are there duplicates in DTOs or switch dpids that are not existent?";
					status = Status.CLIENT_ERROR_NOT_FOUND;
				}
			} else {
				log.error("My: FlowMods could not be created fully");
				message = "FlowMods could not be created fully";
				status = Status.CLIENT_ERROR_BAD_REQUEST;
			}

			// Respond to user
			setStatus(status);
			return message;
		}
	}

	public void prepareUpdate(String jsonBody, boolean testRun) {
		// Get Services
		if (!testRun) {
			// TODO Do not know how to mock this context, so for now only
			// execute this part if not testRun
			this._updateService = (IACIDUpdaterService) this.getContext()
					.getAttributes()
					.get(IACIDUpdaterService.class.getCanonicalName());

			this._switchService = (IOFSwitchService) this.getContext()
					.getAttributes()
					.get(IOFSwitchService.class.getCanonicalName());
		}

		// Extract info from Request
		this._flowModDTOs = convertJsonToDTO(jsonBody);

		// Prepare Update
		this._updateID = this._updateService.createNewUpdateID();
		this._switchesAndFlowMods = getSwitchesAndFlowMods(this._switchService,
				this._flowModDTOs, this._updateID);

		this._affectedSwitches = new ArrayList<>(
				this._switchesAndFlowMods.keySet());
		this._updateService.initXIDStates(this._updateID,
				this._affectedSwitches);

		this._switchStates = this._updateService.getMessages(this._updateID);
	}

	public List<IOFSwitch> updateNetwork() throws InterruptedException {
		log.error("My: UpdateNetwork with xid: " + this._updateID.toString());

		// Vote Lock and wait for commits
		List<IOFSwitch> unconfirmedSwitches = executeFirstPhase();

		// State 1: No Answer - Do Nothing
		// State 2: Rejected - Do Nothing
		// State 3: Confirmed - Rollback (confirmed Switches) or Commit
		// (confirmed Switches)

		// Rollback or Commit + wait for finishes
		List<IOFSwitch> unfinished = executeSecondPhase(unconfirmedSwitches);
		return unfinished;
	}

	public List<IOFSwitch> executeFirstPhase() throws InterruptedException {
		log.error("My: Starting First Phase with xid: "
				+ this._updateID.toString());
		this._updateService.voteLock(this._switchesAndFlowMods);

		List<IOFSwitch> unconfirmedSwitches = getUnresponsiveSwitchesPhase1WithTimeout();
		
		log.error("My: Checking received messages of XID: "
				+ this._updateID.toString() + "\n" + this._switchStates);
		return unconfirmedSwitches;
	}

	public List<IOFSwitch> executeSecondPhase(
			List<IOFSwitch> unconfirmedSwitches) throws InterruptedException {
		log.error("My: Starting Second Phase with xid: "
				+ this._updateID.toString());

		List<IOFSwitch> unfinishedSwitches = null;
		if (unconfirmedSwitches.size() > 0) {
			log.error("My: Rolling Back" + unconfirmedSwitches.toString());
			// Rollback
			List<IOFSwitch> confirmedSwitches = new ArrayList<>(
					this._affectedSwitches);
			confirmedSwitches.removeAll(unconfirmedSwitches);

			this._updateService.rollback(confirmedSwitches,
					this._updateID.toLong());
			log.error("My: Rolledback");
		} else {
			log.error("My: Committing");
			// Commit
			this._updateService.commit(this._affectedSwitches,
					this._updateID.toLong());

			unfinishedSwitches = getUnresponsiveSwitchesPhase2WithTimeout();
			log.error("My: Encountered " + unfinishedSwitches.size()
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

	public List<IOFSwitch> getUnresponsiveSwitchesPhase1WithTimeout() {
		ASPSwitchStates[] unwantedStates = new ASPSwitchStates[] {
				ASPSwitchStates.CONFIRMED, ASPSwitchStates.REJECTED };
		List<IOFSwitch> unconfirmedAndUnrejected = waitForResponses(ASP_CONFIRM_TIMEOUT_MS, unwantedStates);
		
		return getAllSwitchesNotInState(new ASPSwitchStates[] {ASPSwitchStates.CONFIRMED});
	}

	public List<IOFSwitch> getUnresponsiveSwitchesPhase2WithTimeout() {
		ASPSwitchStates[] unwantedStates = new ASPSwitchStates[] {
				ASPSwitchStates.FINISHED };
		return waitForResponses(ASP_FINISH_TIMEOUT_MS, unwantedStates);
	}
	
	public List<IOFSwitch> waitForResponses(long timeoutMS, ASPSwitchStates[] unwantedStates){
		ExecutorService executor = Executors.newSingleThreadExecutor();

		SwitchStateChecker callable = new SwitchStateChecker(this,
				unwantedStates);
		Future<List<IOFSwitch>> handler = executor.submit(callable);
		try {
			return handler.get(timeoutMS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			System.out.println("Exception");
			handler.cancel(true);
		}

		executor.shutdownNow();
		return callable.lastResult;
	}
	
	public class SwitchStateChecker implements Callable<List<IOFSwitch>> {

		public SetPathResource res;
		public ASPSwitchStates[] unwantedStates;
		public List<IOFSwitch> lastResult = null;

		public SwitchStateChecker(SetPathResource res,
				ASPSwitchStates[] unwantedStates) {
			this.res = res;
			this.unwantedStates = unwantedStates;
		}

		@Override
		public List<IOFSwitch> call() throws Exception {
			// Thread.sleep(2000l);
			do {
				// TODO only check unresponsive Switches again (f.e. have a
				// second list that only contains unresponsive switches and only
				// check this list in every loop)
				lastResult = this.res
						.getAllSwitchesNotInState(this.unwantedStates);
				Thread.sleep(10);
			} while (lastResult.size() > 0);

			return lastResult;
		}
	}

	public List<IOFSwitch> getAllSwitchesNotInState(
			ASPSwitchStates[] unwantedStates) {
		List<IOFSwitch> result = new ArrayList<>();
		boolean inUnwantedState;
		for (Entry<IOFSwitch, ASPSwitchStates> currentState : this._switchStates
				.entrySet()) {
			inUnwantedState = false;
			for (ASPSwitchStates currentUnwantedState : unwantedStates) {
				if (currentState.getValue() == currentUnwantedState) {
					inUnwantedState = true;
					break;
				}
			}

			if (!inUnwantedState) {
				result.add(currentState.getKey());
			}
		}
		return result;
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
			// TODO: TableID = 49 does this work?
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
