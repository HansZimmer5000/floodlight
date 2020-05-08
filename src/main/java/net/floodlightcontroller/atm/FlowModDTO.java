package net.floodlightcontroller.atm;


public class FlowModDTO {

	public static final String STRING_DEFAULT="";
	public static final int INT_DEFAULT=-1;
	
	public String dpid = STRING_DEFAULT, name = STRING_DEFAULT;
	public int inPort = INT_DEFAULT, outPort = INT_DEFAULT;

	public FlowModDTO() {
	}
	
	public FlowModDTO(String dpid, String name, int inPort, int outPort){
		this.dpid = dpid;
		this.name = name;
		this.inPort = inPort;
		this.outPort = outPort;
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
