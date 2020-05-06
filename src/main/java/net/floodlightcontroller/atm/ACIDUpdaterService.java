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

	Map<Byte[], List<MessagePair>> messages;
	Byte atmID;

	public ACIDUpdaterService() {
		messages = new HashMap<>();
		this.atmID = getRandomByte();
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
		Byte[] simpleTestXid = new Byte[]{49};
		MessagePair newPair = new MessagePair(sw, msg);
		
		List<MessagePair> currentList = this.messages.get(simpleTestXid);
		if (currentList == null) {
			currentList = new ArrayList<>();
		}
		currentList.add(newPair);
		this.messages.put(simpleTestXid, currentList);
		
		return Command.CONTINUE;
	}

	@Override
	public void voteLock(List<IOFSwitch> switches, List<OFFlowAdd> flowMods) {
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
	public Byte[] createNewUpdateIDAndPrepareMessages() {
		Byte[] updateID = new Byte[4];
		
		updateID[0] = this.atmID;
		updateID[1] = getRandomByte();
		updateID[2] = getRandomByte();
		updateID[3] = getRandomByte();
		
		this.messages.put(updateID, new ArrayList<MessagePair>());

		return updateID;
	}
	
	private Byte getRandomByte(){
		Double randomDbl = Math.random() % 256;
		return randomDbl.byteValue();
	}

	@Override
	public List<MessagePair> getMessages(Byte[] updateID) {
		List<MessagePair> result = this.messages.get(updateID);
		return result;
	}
}
