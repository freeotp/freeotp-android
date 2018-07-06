# FreeOTP Branding

FreeOTP has long supported custom images, but in version 2.0 we are expanding
the scope of our token branding support. This document contains a guide on how
to brand tokens using FreeOTP.

## How to brand ...

There are different ways that you can brand tokens in FreeOTP, depending on
whether or not you control the service that is issuing OTP tokens.

### ... when you control the service.

If you control the service that you want to brand in FreeOTP, branding is
easy. Just specify the `image` and `color` parameters in your Token URI. While
these parameters are unique to FreeOTP, the are ignored by most other OTP
implementations. So there shouldn't be any drawback.

For more details, see the [FreeOTP Token URI documentation](URI.md).

### ... when you don't control the service.

Many popular services do not specify the `image` and `color` parameters in
their Token URI. For these services, FreeOTP will attempt to use the [Font
Awesome][FA] icon for the service if it can be derived from the issuer label
prefix or `issuer` parameter. This can often be successful. However, two other
cases remain.

If an image cannot be found, we can create a manual mapping from the Issuer to
the image name. This is done by adding an entry to the [drawables
file](mobile/src/main/res/values/drawables.xml). This requires that the
brand icon that you want to use is already in Font Awesome. If it isn't, you
need to add it there first. Once the icon is in Font Awesome, it can be added
to FreeOTP.

To specify a color for a service, first find a dark color from the service's
color palette. This is often aided by a color picking tool. Once you have
determined what the color is, simply add an entry in the [color
file](mobile/src/main/res/values/colors.xml).

[FA]: https://fontawesome.com
