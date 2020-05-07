package net.floodlightcontroller.atm;


public class FlowModDTO {

	public String dpid, name;
	public int inPort, outPort;

	public FlowModDTO() {
	}
	
	public String toString(){
		String format = "FlowMod: dpid({1}) name({2}) inPort({3}) outPort({4})";
		Object[] args = {this.dpid, this.name, this.inPort, this.outPort};
		String output = String.format(format, args);
		return output;
	}

	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof FlowModDTO) {
			FlowModDTO objFM = (FlowModDTO) obj;
			if (this.dpid.equals(objFM.dpid)
					&& this.name.equals(objFM.name)
					&& this.inPort == objFM.inPort
					&& this.outPort == objFM.outPort) {
				result = true;
			}
		}

		return result;
	}

}
