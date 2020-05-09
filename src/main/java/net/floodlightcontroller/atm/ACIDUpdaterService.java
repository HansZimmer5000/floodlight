package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.BundleIdGenerators;
import org.projectfloodlight.openflow.protocol.OFBundleCtrlType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFBundleAddMsg.Builder;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.protocol.ver14.OFFactoryVer14;
import org.projectfloodlight.openflow.types.BundleId;
import org.projectfloodlight.openflow.types.TableId;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

public class ACIDUpdaterService implements IACIDUpdaterService {

	Map<UpdateID, List<MessagePair>> messages;
	byte atmID;

	final OFFactory FACTORY = new OFFactoryVer14();
	final TableId TABLE_ID = TableId.of(255);

	public ACIDUpdaterService() {
		messages = new HashMap<>();
		this.atmID = UpdateID.createNewATMID();
	}

	@Override
	public String getName() {
		return "ACIDUpdaterService";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Not Tested

		// - OFP_ERROR_MESSAGE + OFPET_FLOW_MOD_FAILED OFPBC_UNKOWN = What Primitive? TODO
		// - OFP_ERROR_MESSAGE + OFPET_BUNDLE_FAILED OFPBFC_MSG_FAILED = What Primitive? TODO
		// - OFPT_BUNDLE_CONTROL + OFPBCT_COMMIT_REPLY = What Primitive? TODO

		UpdateID updateId = new UpdateID(msg.getXid());
		MessagePair newPair = new MessagePair(sw, msg);

		List<MessagePair> currentList = this.messages.get(updateId);
		if (currentList == null) {
			currentList = new ArrayList<>();
		}
		
		currentList.add(newPair);
		this.messages.put(updateId, currentList);

		return Command.CONTINUE;
	}

	@Override
	public void voteLock(Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods) {
		// TODO Not Tested
		BundleId currentBundleId;
		Builder currentBundleBuild;
		OFMessage currentBundle, currentOpen, currentLock, currentUpdate, currentCommit;

		for (IOFSwitch currentSwitch : switchesAndFlowMods.keySet()) {

			// OFPBCT_OPEN_REQUEST
			OFBundleCtrlType ctrtype = OFBundleCtrlType.OPEN_REQUEST;
			currentOpen = this.FACTORY.buildBundleCtrlMsg()
					.setBundleCtrlType(ctrtype).build();

			// OFPT_FLOW_MOD + OFPFC_MODIFY_STRICT TableId=255
			currentLock = this.FACTORY.buildFlowModifyStrict()
					.setTableId(this.TABLE_ID).build();

			// Update
			currentUpdate = switchesAndFlowMods.get(currentSwitch);

			// OFPBCT_COMMIT_REQUEST
			OFBundleCtrlType ctrtype2 = OFBundleCtrlType.COMMIT_REQUEST;
			currentCommit = this.FACTORY.buildBundleCtrlMsg()
					.setBundleCtrlType(ctrtype2).build();

			// Build Bundle
			currentBundleBuild = this.FACTORY.buildBundleAddMsg();
			currentBundleId = BundleIdGenerators.create().nextBundleId();

			currentBundleBuild.setBundleId(currentBundleId);
			currentBundleBuild.setData(currentOpen);
			currentBundleBuild.setData(currentLock);
			currentBundleBuild.setData(currentUpdate);
			currentBundleBuild.setData(currentCommit);
			currentBundle = currentBundleBuild.build();

			// Send Bundle
			currentSwitch.write(currentBundle);
		}
	}

	@Override
	public void rollback(List<IOFSwitch> switches) {
		// TODO Not Tested
		// OFPT_FLOW_MOD + OFPFC_DELETE_STRICT + TableId = 255

		OFMessage currentMsg;

		for (IOFSwitch currentSwitch : switches) {
			currentMsg = this.FACTORY.buildFlowDeleteStrict()
					.setTableId(this.TABLE_ID).build();
			currentSwitch.write(currentMsg);
		}
	}

	@Override
	public void commit(List<IOFSwitch> switches) {
		// TODO Not Tested
		// OFPT_FLOW_MOD + OFPFC_DELETE TableId=255

		OFMessage currentMsg;

		for (IOFSwitch currentSwitch : switches) {
			currentMsg = this.FACTORY.buildFlowDelete().setTableId(this.TABLE_ID).build();
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
