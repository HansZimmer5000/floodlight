package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

public class ACIDUpdaterService implements IACIDUpdaterService {

	Map<UpdateID, List<MessagePair>> messages;
	byte atmID;

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

		// TODO simple, in the future extract XID and put each message according
		// to its xid in specific Lists
		UpdateID simpleTestUpdateID = new UpdateID(3);
		MessagePair newPair = new MessagePair(sw, msg);
		
		List<MessagePair> currentList = this.messages.get(simpleTestUpdateID);
		if (currentList == null) {
			currentList = new ArrayList<>();
		}
		currentList.add(newPair);
		this.messages.put(simpleTestUpdateID, currentList);
		
		return Command.CONTINUE;
	}

	@Override
	public void voteLock(Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rollback(List<IOFSwitch> switches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commit(List<IOFSwitch> switches) {
		// TODO Auto-generated method stub
		
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
