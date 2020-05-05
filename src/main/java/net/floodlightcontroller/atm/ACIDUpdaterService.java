package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

public class ACIDUpdaterService implements IACIDUpdaterService {

	Map<Byte, List<MessagePair>> messages;

	public ACIDUpdaterService() {
		messages = new HashMap<>();
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
		Byte simpleTestXid = 49;
		MessagePair newPair = new MessagePair(sw, msg);
		
		List<MessagePair> currentList = this.messages.get(simpleTestXid);
		if (currentList == null) {
			currentList = new ArrayList<>();
		}
		currentList.add(newPair);
		this.messages.put(simpleTestXid, currentList);
		
		return Command.CONTINUE;
	}
}
