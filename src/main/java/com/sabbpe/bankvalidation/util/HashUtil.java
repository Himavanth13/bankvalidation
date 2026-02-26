package com.sabbpe.bankvalidation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil {

	private HashUtil() {
	}

	public static String sha256Hex(String input, String secret) {
		try {
			String raw = String.valueOf(input) + String.valueOf(secret);
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] digest = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));

			StringBuilder stringBuilder = new StringBuilder();
			for (byte b : digest) {
				stringBuilder.append(String.format("%02x", b));
			}
			return stringBuilder.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error generating SHA-256 hash", e);
		}
	}
}
