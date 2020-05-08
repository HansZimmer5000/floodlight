package net.floodlightcontroller.atm;

import java.util.Random;

public class UpdateID {

	public byte atmID;
	public byte[] restBytes;
	
	public UpdateID(int atmID) {
		//byte[] bytes = ByteBuffer.allocate(1).putShort(atmID).array();
		this.atmID = new Integer(atmID).byteValue();
		this.restBytes = this.createRestBytes();
	}
	
	public UpdateID(byte[] arr){
		this.atmID = arr[0];
		this.restBytes = new byte[3];
		this.restBytes[0] = arr[1];
		this.restBytes[1] = arr[2];
		this.restBytes[2] = arr[3];
	}
	
	public static byte createNewATMID(){
		byte[] result = new byte[1];
		new Random().nextBytes(result);
		return result[0];
	}
	
	public byte[] createRestBytes(){
		byte[] result = new byte[3];
		
		new Random().nextBytes(result);
		
		return result;
	}
	
	public byte[] toArr(){
		byte[] result = new byte[4];
		
		result[0] = atmID;
		result[1] = this.restBytes[0];
		result[2] = this.restBytes[1];
		result[3] = this.restBytes[2];
		
		return result;
	}
	
	public String toString(){
		String format = "UpdateID: atmID({1}) restBytes({2}))";
		Object[] args = {this.atmID, this.restBytes};
		String output = String.format(format, args);
		return output;
	}
	
	public long toLong() {
		long result = 0;
		byte[] fullArr = new byte[4];
		fullArr[0] = atmID;
		fullArr[1] = this.restBytes[0];
		fullArr[2] = this.restBytes[1];
		fullArr[3] = this.restBytes[2];
		
		for (int i = 0; i < fullArr.length; i++) {
			result = result << 8;
			byte tmpVal = fullArr[i];			
			result += tmpVal;
		}
		return result;
	}
}
