package net.floodlightcontroller.atm;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class UpdateID {

	public byte atmID;
	public byte[] restBytes;

	public UpdateID(int atmID) {
		// byte[] bytes = ByteBuffer.allocate(1).putShort(atmID).array();
		this.atmID = new Integer(atmID).byteValue();
		this.restBytes = this.createRestBytes();
	}

	public UpdateID(long atmID) {
		// byte[] bytes = ByteBuffer.allocate(1).putShort(atmID).array();
		this.atmID = new Long(atmID).byteValue();
		this.restBytes = this.createRestBytes();
	}

	public UpdateID(byte[] arr) {
		this.atmID = arr[0];
		this.restBytes = new byte[3];
		this.restBytes[0] = arr[1];
		this.restBytes[1] = arr[2];
		this.restBytes[2] = arr[3];
	}

	public static byte createNewATMID() {
		byte[] result = new byte[1];
		new Random().nextBytes(result);
		return result[0];
	}

	public byte[] createRestBytes() {
		byte[] result = new byte[3];
		new Random().nextBytes(result);
		return result;
	}

	public byte[] toArr() {
		byte[] result = new byte[4];

		result[0] = atmID;
		result[1] = this.restBytes[0];
		result[2] = this.restBytes[1];
		result[3] = this.restBytes[2];

		return result;
	}

	public static UpdateID ofValue(long rawUpdateID) {
		byte[] rawArr = new byte[4];

		for (int i = 3; i >= 0; i--) {
			rawArr[i] = (byte) (rawUpdateID & 0xFF);
			rawUpdateID >>= 8;
		}
		return new UpdateID(rawArr);
	}

	public String toString() {
		String atmStr = String.format("%02X", this.atmID);
	    StringBuilder sb = new StringBuilder();
	    for (byte b : this.restBytes) {
	        sb.append(String.format("%02X ", b));
	    }
		String output = "UpdateID: atmID(" + atmStr + ") restBytes("
				+ sb.toString() + "))";
		return output;
	}
	
	public int hashCode(){
		int atmVal = new Byte(this.atmID).hashCode();
		int restVal = Arrays.hashCode(this.restBytes);
		
		return atmVal + restVal;
	}

	public boolean equals(Object obj){
		boolean result = false;
		
		if (obj instanceof UpdateID) {
			UpdateID objID = (UpdateID) obj;
			if (this.hashCode() == objID.hashCode()){
				result = true;
			}
		}
		
		return result;
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
