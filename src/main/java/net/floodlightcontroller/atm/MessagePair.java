package net.floodlightcontroller.atm;

import org.projectfloodlight.openflow.protocol.OFMessage;

import net.floodlightcontroller.core.IOFSwitch;

public class MessagePair {
	public IOFSwitch ofswitch;
	public OFMessage ofmsg;

	public MessagePair(IOFSwitch ofswitch, OFMessage ofmsg) {
		this.ofswitch = ofswitch;
		this.ofmsg = ofmsg;
	}
}
