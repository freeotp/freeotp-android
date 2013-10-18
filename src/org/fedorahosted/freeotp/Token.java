/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 * see file 'COPYING' for use and warranty information
 *
 * This program is free software you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fedorahosted.freeotp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.SharedPreferences;
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

	public static List<Token> getTokens(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(Token.class.getName(), Context.MODE_PRIVATE);

		List<Token> tokens = new ArrayList<Token>();
		for (String key : prefs.getAll().keySet()) {
			try {
				tokens.add(new Token(prefs.getString(key, null)));
			} catch (TokenUriInvalidException e) {
				e.printStackTrace();
			}
		}

		return tokens;
	}

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

	private String getId() {
		String id;
		if (issuerInt != null && !issuerInt.equals(""))
			id = issuerInt + ":" + label;
		else if (issuerExt != null && !issuerExt.equals(""))
			id = issuerExt + ":" + label;
		else
			id = label;

		return id;
	}

	public void remove(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(Token.class.getName(), Context.MODE_PRIVATE);
		prefs.edit().remove(getId()).apply();
	}

	public void save(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(Token.class.getName(), Context.MODE_PRIVATE);
		prefs.edit().putString(getId(), toString()).apply();
	}

	public String getTitle() {
		String title = "";
		if (issuerExt != null && !issuerExt.equals(""))
			title += issuerExt + ": ";
		title += label;
		return title;
	}

	public String getCurrentTokenValue(Context ctx, boolean increment) {
		if (type == TokenType.HOTP) {
			if (increment) {
				try {
					return getHOTP(counter++);
				} finally {
					save(ctx);
				}
			} else {
				String placeholder = "";
				for (int i = 0; i < digits; i++)
					placeholder += "-";

				return placeholder;
			}
		}

		return getHOTP(System.currentTimeMillis() / 1000 / period);
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
