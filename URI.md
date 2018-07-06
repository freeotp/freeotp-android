# Introduction

FreeOTP supports provisioning token with two methods:

  1. Clicking a link to a Token URI.
  2. Scanning a QR code containing a Token URI.

The FreeOTP Token URI is a superset of what [Google Authenticator][GAuth]
supports. Thus, this document copies the specification from the previous link
and expands it to define the new features.

The Token URI is formatted as follows: `otpauth://TYPE/LABEL?PARAMETERS`

## Examples

This is a token for user `eve@redhat.com` for use with a service provided by
Example, Inc.:

  `otpauth://totp/Example:eve@redhat.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Example`

The secret (`GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ`) is simply the Base32 encoding
of the ASCII encoded string `"12345678901234567890"`.

## Types

Valid types are `hotp` and `totp`, to distinguish whether the key will be used
for counter-based [HOTP][HOTP] or for [TOTP][TOTP].

## Label

The label is used to identify which account a key is associated with. It
contains an account name, which is a URI-encoded string, optionally prefixed
by an issuer string identifying the provider or service managing that account.
This issuer prefix can be used to prevent collisions between different
accounts with different providers that might be identified using the same
account name, e.g. the user's email address.

The issuer prefix and account name should be separated by a literal or
url-encoded colon, and optional spaces may precede the account name. Neither
issuer nor account name may themselves contain a colon. Represented in ABNF
according to [RFC 5234](http://tools.ietf.org/html/rfc5234):

  `label = accountname / issuer (“:” / “%3A”) *”%20” accountname`

Valid values include:

  * `Example:eve@redhat.com`
  * `Provider1:Eve%20Smith`
  * `Big%20Corporation%3A%20eve%40bigco.com`

Unlike Google Authenticator, FreeOTP recommends using only an issuer label
prefix. Further, we **STRONGLY RECOMMEND** that you specify an issuer label
prefix since this is the basis of several features in FreeOTP.

## Parameters

### Secret

**REQUIRED**: The `secret` parameter is an arbitrary key value encoded in Base32
according to [RFC 3548](http://tools.ietf.org/html/rfc3548). The padding specified
in [RFC 3548 section 2.2](https://tools.ietf.org/html/rfc3548#section-2.2) is not
required and should be omitted.

**NOTE**: This paragraph from [HOTP][HOTP] is enforced by FreeOTP:

    R6 - The algorithm MUST use a strong shared secret.  The length of
    the shared secret MUST be at least 128 bits.  This document
    RECOMMENDs a shared secret length of 160 bits.

### Issuer

**OPTIONAL**: The `issuer` parameter is a string value indicating the provider
or service this account is associated with, URL-encoded according to [RFC
3986](http://tools.ietf.org/html/rfc3986). If the issuer parameter is absent,
issuer information may be taken from the issuer prefix of the label. If both
issuer parameter and issuer label prefix are present, they should be equal.

Valid values corresponding to the label prefix examples above would be: `issuer=Example`,
`issuer=Provider1`, and `issuer=Big%20Corporation`.

FreeOTP does not use the `issuer` parameter for internal disambiguation but
does use it exactly the same as the issuer label prefix in the case that no
issuer label prefix is provided.

### Algorithm

**OPTIONAL**: The `algorithm` parameter may have one of the following values:

  * `SHA1`
  * `SHA224`
  * `SHA256`
  * `SHA384`
  * `SHA512`

The default is `SHA1`.

### Digits

**OPTIONAL**: The `digits` parameter is an integer determining how many
characters to show. In the default token encoding scheme, this can be any
integer from 6-9 (inclusive). Other token encodings schemes (selected by
FreeOTP using the issuer label prefix or `issuer` parameter) may have other
requirements.

The default is `6` (for the default token encoding scheme).

### Counter

**OPTIONAL**: The `counter` parameter is an integer which defines the initial
counter value to be used for [HOTP][HOTP] tokens. This parameter is ignored on
all other token types.

The default is `0`.

### Period

**OPTIONAL**: The `period` parameter is an integer (in seconds) that defines
the validity window for a token. On [TOTP][TOTP] tokens, it is used as an
input to the token algorithm itself. On all tokens, it determines how long
FreeOTP will display the token code.

The default is `30`.

### Image

**OPTIONAL**: The `image` parameter is a URL-encoded URL to an image to
display next to the token. If this parameter is omitted, is invalid or the
image is unable to be fetched, FreeOTP will use an internal algorithm to
display an appropriate image for the token.

Provided images should generally be in the style of [Font Awesome][FA] in
order to blend in.

There is no default value.

### Color

**OPTIONAL**: The `color` parameter is a color, specified in `RRGGBB` format
that will be displayed behind the token image. If this parameter is omitted,
an undefined - but persistant - dark-ish color will be displayed behind the
image.

There is no default value.

### Lock

**OPTIONAL**: The `lock` parameter is a boolean (either `true` or `false`)
which will ensure that the token secret is stored in such a way that it can
only be accessed by a recent authentication on the device. Recent is defined
as within one token validity window (specified by the `period` parameter).
This has the effect that whenever the user clicks to obtain a token code, the
user will have to authenticate. Possible authentication methods include:

  * PIN
  * Pattern
  * Fingerprint Scan
  * Facial Recognition

The default is `false`.

[GAuth]: https://github.com/google/google-authenticator/wiki/Key-Uri-Format
[HOTP]: https://tools.ietf.org/html/rfc4226
[TOTP]: https://tools.ietf.org/html/rfc6238
[FA]: https://fontawesome.com

