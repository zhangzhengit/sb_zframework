package com.vo.core;

public class StringToAscii {

	public static String stringToAscii(final String value) {
		final StringBuilder sbu = new StringBuilder();
		final char[] chars = value.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (i != chars.length - 1) {
				sbu.append((int) chars[i]).append(",");
			} else {
				sbu.append((int) chars[i]);
			}
		}
		return sbu.toString();
	}

	public static String asciiToString(final String value) {
		final StringBuilder sbu = new StringBuilder();
		final String[] chars = value.split(",");
		for (int i = 0; i < chars.length; i++) {
			sbu.append((char) Integer.parseInt(chars[i]));
		}
		return sbu.toString();
	}

}
