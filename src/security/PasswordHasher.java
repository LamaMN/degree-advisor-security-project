package security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

final class PasswordHasher {
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordHasher() {
	}

	static String generateSalt() {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		String encoded = Base64.getEncoder().encodeToString(salt);
		Arrays.fill(salt, (byte) 0);
		return encoded;
	}

	static String hash(char[] password, String saltBase64) {
		byte[] salt = Base64.getDecoder().decode(saltBase64);
		byte[] hashedBytes = digest(password, salt);
		String encoded = Base64.getEncoder().encodeToString(hashedBytes);
		Arrays.fill(hashedBytes, (byte) 0);
		Arrays.fill(salt, (byte) 0);
		return encoded;
	}

	static boolean matches(char[] password, String saltBase64, String expectedHash) {
		byte[] expected = Base64.getDecoder().decode(expectedHash);
		byte[] salt = Base64.getDecoder().decode(saltBase64);
		byte[] actual = digest(password, salt);
		boolean equal = MessageDigest.isEqual(expected, actual);
		Arrays.fill(actual, (byte) 0);
		Arrays.fill(expected, (byte) 0);
		Arrays.fill(salt, (byte) 0);
		return equal;
	}

	private static byte[] digest(char[] password, byte[] salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(salt);
			byte[] passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8);
			byte[] result = digest.digest(passwordBytes);
			Arrays.fill(passwordBytes, (byte) 0);
			return result;
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm not available", ex);
		}
	}
}

