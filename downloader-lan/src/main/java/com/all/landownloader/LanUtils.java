package com.all.landownloader;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public final class LanUtils {

	private LanUtils() {
	}

	public static int decodeInt(byte[] b) {
		return byteArrayToInt(b);
	}

	public static byte[] encodeInt(int i) {
		return intToByteArray(i);
	}

	public static byte[] intToByteArray(int i) {
		byte b[] = new byte[4];
		ByteBuffer buf = ByteBuffer.wrap(b);
		buf.putInt(i);
		return b;
	}

	public static int byteArrayToInt(byte[] b) {
		ByteBuffer buf = ByteBuffer.wrap(b);
		return buf.getInt();
	}

	public static String getLocalAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return null;
		}
	}

}
