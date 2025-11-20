[![Build Status](https://github.com/freeotp/freeotp-android/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/freeotp/freeotp-android/actions/workflows/build.yml)

# FreeOTP

[FreeOTP](https://freeotp.github.io) is a two-factor authentication application for systems
utilizing one-time password protocols. Tokens can be added easily by scanning a QR code.

FreeOTP implements open standards:

* HOTP (HMAC-Based One-Time Password Algorithm) [RFC 4226](https://www.ietf.org/rfc/rfc4226.txt)
* TOTP (Time-Based One-Time Password Algorithm) [RFC 6238](https://www.ietf.org/rfc/rfc6238.txt)

This means that no proprietary server-side component is necessary: use any server-side component that implements these standards.

## Screenshots
<img src="screenshots/1.png" alt="FreeOTP screenshot 1" width="200" /> &nbsp;<img src="screenshots/2.png" alt="FreeOTP screenshot 2" width="200" /> &nbsp;<img src="screenshots/3.png" alt="FreeOTP screenshot 3" width="200" /> &nbsp;<img src="screenshots/4.png" alt="FreeOTP screenshot 4" width="200" />

## Download FreeOTP for Android

* [F-Droid](https://f-droid.org/packages/org.fedorahosted.freeotp)
* [Google Play](https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp)

<a href="https://f-droid.org/packages/org.fedorahosted.freeotp/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60"/></a>
<a href="https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="60"/></a>

## Contributing

Pull requests on GitHub are welcome under the Apache 2.0 license, see [CONTRIBUTING](CONTRIBUTING.md) for more details.

## Permissions

The FreeOTP app uses the following permissions

| Permission | Usage                    | Required | Permission type |
|------------|--------------------------|----------|-----------------|
| Camera     | Recognition of QR codes  | No       | Dangerous       |
| Internet   | Token image provisioning | No       | Normal          |

## Alternatives

Here are some open-source alternative apps providing similar functionality:
- [Aegis](https://github.com/beemdevelopment/Aegis)
- [FreeOTP+](https://github.com/helloworld1/FreeOTPPlus)
- [Proton Authenticator](https://github.com/protonpass/android-authenticator)
