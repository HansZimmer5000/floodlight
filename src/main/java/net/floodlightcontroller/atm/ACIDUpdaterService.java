package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.BundleIdGenerator;
import org.projectfloodlight.openflow.protocol.BundleIdGenerators;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlMsg;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlType;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.BundleId;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

public class ACIDUpdaterService implements IACIDUpdaterService {
	protected static Logger log = LoggerFactory
			.getLogger(SetPathResource.class);

	BundleIdGenerator bundleIdGenerator;
	Map<UpdateID, HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates>> states;
	long atmID;

	final OFFactory FACTORY = new OFFactoryVer14();
	final TableId ASP_TABLE_ID = TableId.of(255);

	public ACIDUpdaterService() {
		this.bundleIdGenerator = BundleIdGenerators.global();
		states = new HashMap<>();
		this.atmID = UpdateID.createNewATMID();
	}

	@Override
	public String getName() {
		return "ACIDUpdaterService";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		// - OFP_ERROR_MESSAGE + OFPET_FLOW_MOD_FAILED OFPBC_UNKOWN = FINISH
		// - OFP_ERROR_MESSAGE + OFPET_BUNDLE_FAILED OFPBFC_MSG_FAILED = REJECT
		// - OFPT_BUNDLE_CONTROL + OFPBCT_COMMIT_REPLY = CONFIRM

		UpdateID updateId = UpdateID.ofValue(msg.getXid());

		log.error("My: Got OpenFlow Message (XID=" + updateId.toString()
				+ ") (Type=" + msg.getType().toString() + ")");

		HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> currentMap = this.states
				.get(updateId);

		if (currentMap == null) {
			log.error("My: Got Message with unkown XID");
		} else {
			adjustSwitchState(currentMap, sw, msg);
			this.states.put(updateId, currentMap);
		}

		return Command.CONTINUE;
	}

	private void adjustSwitchState(
			HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> currentMap,
			IOFSwitch sw, OFMessage msg) {
		log.error("My: Adjust Switch(" + sw.getId().toString() + "/" + currentMap.get(sw) + ") states due to msg: "+ msg.getType());
		switch (msg.getType()) {
		case BUNDLE_CONTROL:
			// Detect Confirm
			OFBundleCtrlMsg bundleCtrl = (OFBundleCtrlMsg) msg;
			switch (bundleCtrl.getBundleCtrlType()) {
			case COMMIT_REPLY:
				// Confirm
				// Next Check is important as a bundle_msg_failed error will be received before this bundle_commit message! So this message will always the received, the question is if an error (REJECT) was received before.
				if (IACIDUpdaterService.ASPSwitchStates.DEFAULT == currentMap
						.get(sw)) {
					currentMap.put(sw,
							IACIDUpdaterService.ASPSwitchStates.CONFIRMED);
					log.error("My: Switch(" + sw.getId().toString() + ") is now in state CONFIRMED");
				} else {
					log.error("My: Could not CONFIRM due to wrong state: " + currentMap
						.get(sw));
				}
				break;
			default:
				break;
			}
			break;
		case ERROR:
			// Detect Reject and Finish
			OFErrorMsg error = (OFErrorMsg) msg;
			switch (error.getErrType()) {
			case BUNDLE_FAILED:
				// Reject
				if (IACIDUpdaterService.ASPSwitchStates.DEFAULT == currentMap
						.get(sw)) {
					currentMap.put(sw,
							IACIDUpdaterService.ASPSwitchStates.REJECTED);
					log.error("My: Switch(" + sw.getId().toString() + ") is now in state REJECTED");
				} else {
					log.error("My: Could not REJECT due to wrong state: " + currentMap
						.get(sw));
				}
				break;
			case FLOW_MOD_FAILED:
				// Finish
				if (IACIDUpdaterService.ASPSwitchStates.CONFIRMED == currentMap
						.get(sw)) {
					currentMap.put(sw,
							IACIDUpdaterService.ASPSwitchStates.FINISHED);
					log.error("My: Switch(" + sw.getId().toString() + ") is now in state FINISHED");
				} else {
					log.error("My: Could not FINISH due to wrong state: " + currentMap
						.get(sw));
				}
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}

	}

	@Override
	public void voteLock(Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods) {
		BundleId currentBundleId;
		long currentXid;
		OFMessage currentOpenBundle, currentLock, currentLockBundle, currentUpdate, currentUpdateBundle, currentCommitBundle;
		ArrayList<OFMessage> currentMessages;

		for (IOFSwitch currentSwitch : switchesAndFlowMods.keySet()) {
			currentBundleId = this.bundleIdGenerator.nextBundleId();
			currentUpdate = switchesAndFlowMods.get(currentSwitch);
			currentXid = currentUpdate.getXid();
			log.error("Sending VoteLock Xid="
					+ UpdateID.ofValue(currentXid).toString());

			currentMessages = new ArrayList<>();

			// OFPBCT_OPEN_REQUEST
			OFBundleCtrlType ctrOpentype = OFBundleCtrlType.OPEN_REQUEST;
			currentOpenBundle = this.FACTORY.buildBundleCtrlMsg()
					.setBundleCtrlType(ctrOpentype)
					.setBundleId(currentBundleId).setXid(currentXid).build();
			currentMessages.add(currentOpenBundle);

			// OFPT_FLOW_MOD + OFPFC_MODIFY_STRICT TableId=255
			currentLock = this.FACTORY.buildFlowModifyStrict()
					.setTableId(this.ASP_TABLE_ID).setXid(currentXid).build();
			currentLockBundle = this.FACTORY.buildBundleAddMsg()
					.setBundleId(currentBundleId).setData(currentLock)
					.setXid(currentXid).build();
			currentMessages.add(currentLockBundle);

			// Update
			currentUpdateBundle = this.FACTORY.buildBundleAddMsg()
					.setBundleId(currentBundleId).setData(currentUpdate)
					.setXid(currentXid).build();
			currentMessages.add(currentUpdateBundle);

			// OFPBCT_COMMIT_REQUEST
			OFBundleCtrlType ctrCommitType = OFBundleCtrlType.COMMIT_REQUEST;
			currentCommitBundle = this.FACTORY.buildBundleCtrlMsg()
					.setBundleCtrlType(ctrCommitType)
					.setBundleId(currentBundleId).setXid(currentXid).build();
			currentMessages.add(currentCommitBundle);

			currentSwitch.write(currentMessages);
		}
	}

	@Override
	public void rollback(List<IOFSwitch> switches, long xid) {
		// OFPT_FLOW_MOD + OFPFC_DELETE_STRICT + TableId = 255

		OFMessage currentMsg;

		for (IOFSwitch currentSwitch : switches) {
			log.error("Sending Rollback Xid="
					+ UpdateID.ofValue(xid).toString());
			currentMsg = this.FACTORY.buildFlowDeleteStrict()
					.setTableId(this.ASP_TABLE_ID).setXid(xid)
					.setPriority(65535).build();
			currentSwitch.write(currentMsg);
		}
	}

	@Override
	public void commit(List<IOFSwitch> switches, long xid) {
		// OFPT_FLOW_MOD + OFPFC_DELETE TableId=255

		OFMessage currentMsg;

		for (IOFSwitch currentSwitch : switches) {
			log.error("Sending Commit Xid=" + UpdateID.ofValue(xid).toString());
			currentMsg = this.FACTORY.buildFlowDelete().setXid(xid)
					.setTableId(this.ASP_TABLE_ID).setPriority(65535).build();
			currentSwitch.write(currentMsg);
		}
	}

	@Override
	public UpdateID createNewUpdateID() {
		UpdateID updateID = new UpdateID(this.atmID);
		return updateID;
	}
	
	@Override
	public void initXIDStates(UpdateID updateID, ArrayList<IOFSwitch> affectedSwitches) {
		HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> newMap = new HashMap<>();
		for (IOFSwitch currentSwitch : affectedSwitches) {
			newMap.put(currentSwitch,
					IACIDUpdaterService.ASPSwitchStates.DEFAULT);
		}
		this.states.put(updateID, newMap);
	}

	@Override
	public HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> getMessages(
			UpdateID updateID) {
		HashMap<IOFSwitch, IACIDUpdaterService.ASPSwitchStates> result = this.states
				.get(updateID);
		return result;
	}
}
