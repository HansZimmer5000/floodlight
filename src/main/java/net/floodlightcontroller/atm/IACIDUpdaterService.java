package net.floodlightcontroller.atm;

import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;

import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IACIDUpdaterService extends IFloodlightService, IOFMessageListener {
	
	public UpdateID createNewUpdateIDAndPrepareMessages();
	
	public List<MessagePair> getMessages(UpdateID updateID);
	
	public void voteLock(List<IOFSwitch> switches, List<OFFlowAdd> flowMods);
	
	public void rollback(List<IOFSwitch> switches);
	
	public void commit(List<IOFSwitch> switches);
}
