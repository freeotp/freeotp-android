/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;

import com.google.android.apps.authenticator.Base32String;
import com.google.android.apps.authenticator.Base32String.DecodingException;

public class Token {
	public static class TokenUriInvalidException extends Exception {
		private static final long serialVersionUID = -1108624734612362345L;
	}

	public static enum TokenType {
		HOTP, TOTP
	}

	private final String issuerInt;
	private final String issuerExt;
	private final String label;
	private TokenType type;
	private String algo;
	private byte[] key;
	private int digits;
	private long counter;
	private int period;

	private Token(Uri uri) throws TokenUriInvalidException {
		if (!uri.getScheme().equals("otpauth"))
			throw new TokenUriInvalidException();

		if (uri.getAuthority().equals("totp"))
			type = TokenType.TOTP;
		else if (uri.getAuthority().equals("hotp"))
			type = TokenType.HOTP;
		else
			throw new TokenUriInvalidException();

		String path = uri.getPath();
		if (path == null)
			throw new TokenUriInvalidException();

		// Strip the path of its leading '/'
		for (int i = 0; path.charAt(i) == '/'; i++)
			path = path.substring(1);
		if (path.length() == 0)
			throw new TokenUriInvalidException();

		int i = path.indexOf(':');
		issuerExt = i < 0 ? "" : path.substring(0, i);
		issuerInt = uri.getQueryParameter("issuer");
		label = path.substring(i >= 0 ? i + 1 : 0);

		algo = uri.getQueryParameter("algorithm");
		if (algo == null)
			algo = "sha1";
		algo = algo.toUpperCase(Locale.US);
		try {
			Mac.getInstance("Hmac" + algo);
		} catch (NoSuchAlgorithmException e1) {
			throw new TokenUriInvalidException();
		}

		try {
			String d = uri.getQueryParameter("digits");
			if (d == null)
				d = "6";
			digits = Integer.parseInt(d);
			if (digits != 6 && digits != 8)
				throw new TokenUriInvalidException();
		} catch (NumberFormatException e) {
			throw new TokenUriInvalidException();
		}

		switch (type) {
		case HOTP:
			try {
				String c = uri.getQueryParameter("counter");
				if (c == null)
					c = "0";
				counter = Long.parseLong(c);
			} catch (NumberFormatException e) {
				throw new TokenUriInvalidException();
			}
			break;
		case TOTP:
			try {
				String p = uri.getQueryParameter("period");
				if (p == null)
					p = "30";
				period = Integer.parseInt(p);
			} catch (NumberFormatException e) {
				throw new TokenUriInvalidException();
			}
			break;
		}

		try {
			String s = uri.getQueryParameter("secret");
			key = Base32String.decode(s);
		} catch (DecodingException e) {
			throw new TokenUriInvalidException();
		}
	}

	private String getHOTP(long counter) {
		// Encode counter in network byte order
		ByteBuffer bb = ByteBuffer.allocate(8);
		bb.putLong(counter);

		// Create digits divisor
		int div = 1;
		for (int i = digits; i > 0; i--)
			div *= 10;

		// Create the HMAC
		try {
			Mac mac = Mac.getInstance("Hmac" + algo);
			mac.init(new SecretKeySpec(key, "Hmac" + algo));

			// Do the hashing
			byte[] digest = mac.doFinal(bb.array());

			// Truncate
			int binary;
			int off = digest[digest.length - 1] & 0xf;
			binary  = (digest[off + 0] & 0x7f) << 0x18;
			binary |= (digest[off + 1] & 0xff) << 0x10;
			binary |= (digest[off + 2] & 0xff) << 0x08;
			binary |= (digest[off + 3] & 0xff) << 0x00;
			binary  = binary % div;

			// Zero pad
			String hotp = Integer.toString(binary);
			while (hotp.length() != digits)
				hotp = "0" + hotp;

			return hotp;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return "";
	}

	public Token(String uri) throws TokenUriInvalidException {
		this(Uri.parse(uri));
	}

	public String getID() {
		String id;
		if (issuerInt != null && !issuerInt.equals(""))
			id = issuerInt + ":" + label;
		else if (issuerExt != null && !issuerExt.equals(""))
			id = issuerExt + ":" + label;
		else
			id = label;

		return id;
	}

	public String getIssuer() {
		return issuerExt != null ? issuerExt : "";
	}

	public String getLabel() {
		return label != null ? label : "";
	}

	public String getCode() {
		switch (type) {
		case HOTP:
			return getHOTP(counter);
		case TOTP:
			return getHOTP(System.currentTimeMillis() / 1000 / period);
		}

		return null;
	}

	public String getPlaceholder() {
		StringBuilder sb = new StringBuilder(digits);
		for (int i = 0; i < digits; i++)
			sb.append('-');
		return sb.toString();
	}

	public void increment() {
		if (type == TokenType.HOTP)
			counter++;
	}

	public Uri toUri() {
		String issuerLabel = !issuerExt.equals("") ? issuerExt + ":" + label : label;

		Uri.Builder builder = new Uri.Builder()
			.scheme("otpauth")
			.path(issuerLabel)
			.appendQueryParameter("secret", Base32String.encode(key))
			.appendQueryParameter("issuer", issuerInt == null ? issuerExt : issuerInt)
			.appendQueryParameter("algorithm", algo)
			.appendQueryParameter("digits", Integer.toString(digits));

		switch (type) {
		case HOTP:
			builder.authority("hotp");
			builder.appendQueryParameter("counter", Long.toString(counter));
			break;
		case TOTP:
			builder.authority("totp");
			builder.appendQueryParameter("period", Integer.toString(period));
			break;
		}

		return builder.build();
	}

	public TokenType getType() {
		return type;
	}

	// Progress is on a scale from 0 - 1000.
	public int getProgress() {
		int p = period * 10;

		long time = System.currentTimeMillis() / 100;
		return (int) ((time % p) * 1000 / p);
	}

	@Override
	public String toString() {
		return toUri().toString();
	}
}
