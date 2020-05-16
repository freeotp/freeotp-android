package org.fedorahosted.freeotp;

/**
 * Generates a placeholder {@link String} to show when
 * a {@link Token}'s code should not be shown.
 */
public final class TokenPlaceholderGenerator {
    private TokenPlaceholderGenerator(){
        // no-op
    }

    public static String generate(final Token token){
        char[] placeholder = new char[token.getDigits()];
        for (int i = 0; i < placeholder.length; i++)
            placeholder[i] = '-';
        return new String(placeholder);
    }
}
