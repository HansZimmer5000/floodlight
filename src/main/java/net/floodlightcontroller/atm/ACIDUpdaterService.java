package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.BundleIdGenerator;
import org.projectfloodlight.openflow.protocol.BundleIdGenerators;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlType;
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
	Map<UpdateID, List<MessagePair>> messages;
	byte atmID;

	final OFFactory FACTORY = new OFFactoryVer14();
	final TableId ASP_TABLE_ID = TableId.of(255);

	public ACIDUpdaterService() {
		this.bundleIdGenerator = BundleIdGenerators.global();
		messages = new HashMap<>();
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
		MessagePair newPair = new MessagePair(sw, msg);

		log.debug("Got OpenFlow Message: " + updateId.toString());

		List<MessagePair> currentList = this.messages.get(updateId);
		if (currentList == null) {
			log.debug("Unkown xid, creating new List");
			currentList = new ArrayList<>();
		}

		currentList.add(newPair);
		this.messages.put(updateId, currentList);

		return Command.CONTINUE;
	}

	@Override
	public void voteLock(Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods) {
		BundleId currentBundleId;
		long currentXid;
		OFMessage currentOpenBundle, currentLock, currentLockBundle, currentUpdate, currentUpdateBundle, currentCommitBundle;

		for (IOFSwitch currentSwitch : switchesAndFlowMods.keySet()) {
			currentBundleId = this.bundleIdGenerator.nextBundleId();
			currentUpdate = switchesAndFlowMods.get(currentSwitch);
			currentXid = currentUpdate.getXid();

			// OFPBCT_OPEN_REQUEST
			OFBundleCtrlType ctrOpentype = OFBundleCtrlType.OPEN_REQUEST;
			currentOpenBundle = this.FACTORY.buildBundleCtrlMsg()
					.setBundleCtrlType(ctrOpentype)
					.setBundleId(currentBundleId).setXid(currentXid).build();
			currentSwitch.write(currentOpenBundle);

			// OFPT_FLOW_MOD + OFPFC_MODIFY_STRICT TableId=255
			currentLock = this.FACTORY.buildFlowModifyStrict()
					.setTableId(this.ASP_TABLE_ID).setXid(currentXid).build();
			currentLockBundle = this.FACTORY.buildBundleAddMsg()
					.setBundleId(currentBundleId).setData(currentLock)
					.setXid(currentXid).build();
			currentSwitch.write(currentLockBundle);

			// Update
			currentUpdateBundle = this.FACTORY.buildBundleAddMsg()
					.setBundleId(currentBundleId).setData(currentUpdate)
					.setXid(currentXid).build();
			currentSwitch.write(currentUpdateBundle);

			// OFPBCT_COMMIT_REQUEST
			OFBundleCtrlType ctrCommitType = OFBundleCtrlType.COMMIT_REQUEST;
			currentCommitBundle = this.FACTORY.buildBundleCtrlMsg()
					.setBundleCtrlType(ctrCommitType)
					.setBundleId(currentBundleId).setXid(currentXid).build();
			currentSwitch.write(currentCommitBundle);

		}
	}

	@Override
	public void rollback(List<IOFSwitch> switches, long xid) {
		// OFPT_FLOW_MOD + OFPFC_DELETE_STRICT + TableId = 255

		OFMessage currentMsg;

		for (IOFSwitch currentSwitch : switches) {
			currentMsg = this.FACTORY.buildFlowDeleteStrict()
					.setTableId(this.ASP_TABLE_ID).setXid(xid).setPriority(65535).build();
			currentSwitch.write(currentMsg);
		}
	}

	@Override
	public void commit(List<IOFSwitch> switches, long xid) {
		// OFPT_FLOW_MOD + OFPFC_DELETE TableId=255

		OFMessage currentMsg;

		for (IOFSwitch currentSwitch : switches) {
			currentMsg = this.FACTORY.buildFlowDelete().setXid(xid)
					.setTableId(this.ASP_TABLE_ID).setPriority(65535).build();
			currentSwitch.write(currentMsg);
		}
	}

	@Override
	public UpdateID createNewUpdateIDAndPrepareMessages() {
		UpdateID updateID = new UpdateID(this.atmID);

		this.messages.put(updateID, new ArrayList<MessagePair>());

		return updateID;
	}

	@Override
	public List<MessagePair> getMessages(UpdateID updateID) {
		List<MessagePair> result = this.messages.get(updateID);
		return result;
	}
}
