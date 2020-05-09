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

	public boolean equals(Object obj) {
		boolean result = false;
		
		if (obj instanceof MessagePair) {
			MessagePair objPair = (MessagePair) obj;
			
			if (this.ofswitch.equals(objPair.ofswitch) && this.ofmsg.equals(objPair.ofmsg)) {
				result = true;
			}
		}

		return result;
	}
	
	public String toString(){
		return "" + this.ofswitch.getId().toString() + " //// " + ofmsg.toString();
	}

}
