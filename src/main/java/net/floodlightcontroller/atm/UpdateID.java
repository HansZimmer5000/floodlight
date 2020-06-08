package net.floodlightcontroller.atm;

import java.util.Arrays;
import java.util.Random;

public class UpdateID {

	public long atmID;
	public long[] restBytes;

	public UpdateID(long atmID) {
		// byte[] bytes = ByteBuffer.allocate(1).putShort(atmID).array();
		this.atmID = atmID;
		this.restBytes = this.createRestBytes();
	}

	public static long byteToLong(byte raw) {
		if (raw < 0) {
			return 256 + raw;
		} else {
			return raw;
		}
	}

	public UpdateID(byte[] arr) {
		this.atmID = byteToLong(arr[0]);
		this.restBytes = new long[3];
		this.restBytes[0] = byteToLong(arr[1]);
		this.restBytes[1] = byteToLong(arr[2]);
		this.restBytes[2] = byteToLong(arr[3]);
	}

	public static long createNewATMID() {
		byte[] tmpResult = new byte[1];
		new Random().nextBytes(tmpResult);
		return byteToLong(tmpResult[0]);
	}

	public long[] createRestBytes() {
		byte[] tmpResult = new byte[3];
		long[] result = new long[3];
		new Random().nextBytes(tmpResult);

		result[0] = byteToLong(tmpResult[0]);
		result[1] = byteToLong(tmpResult[1]);
		result[2] = byteToLong(tmpResult[2]);

		return result;
	}

	public long[] toArr() {
		long[] result = new long[4];

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
		for (long b : this.restBytes) {
			sb.append(String.format("%02X ", b));
		}
		String output = "UpdateID: atmID(" + atmStr + ") restBytes("
				+ sb.toString() + ")";
		return output;
	}

	public int hashCode() {
		int atmVal = new Long(this.atmID).hashCode();
		int restVal = Arrays.hashCode(this.restBytes);

		return atmVal + restVal;
	}

	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof UpdateID) {
			UpdateID objID = (UpdateID) obj;
			if (this.hashCode() == objID.hashCode()) {
				result = true;
			}
		}

		return result;
	}

	public long toLong() {
		long result = 0;

		result += (long) (Math.pow(16, 6) * this.atmID);
		result += (long) (Math.pow(16, 4)) * this.restBytes[0];
		result += (long) (Math.pow(16, 2)) * this.restBytes[1];
		result += (long) (Math.pow(16, 0)) * this.restBytes[2];

		return result;
	}

}
