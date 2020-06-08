package net.floodlightcontroller.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;

import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;

//TODO maybe this needs a rename since it doesn do the full update itself and it rather a holder for the receiving messages and communicator to the switches.
public interface IACIDUpdaterService extends IFloodlightService, IOFMessageListener {
	public enum ASPSwitchStates {
		DEFAULT, CONFIRMED, REJECTED, FINISHED
	}
	
	public UpdateID createNewUpdateID();
	public void initXIDStates(UpdateID updateID, ArrayList<IOFSwitch> affectedSwitches);
	
	public HashMap<IOFSwitch, ASPSwitchStates> getMessages(UpdateID updateID);
	
	public void voteLock(Map<IOFSwitch, OFFlowAdd> switchesAndFlowMods);
	
	public void rollback(List<IOFSwitch> switches, long xid);
	
	public void commit(List<IOFSwitch> switches, long xid);
}
