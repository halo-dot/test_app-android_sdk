Project Name: Halo SDK Demo App

Description: This project is a demo app showcasing the integration of the Halo SDK into an Android application.

Project Information:

- Gradle version: 7.4.2
- Kotlin version: 1.6.21
- Dokka version: 1.4.32

Prerequisites: Before running this application, you need to replace the following placeholders with your own values:

- In `Config.kt`:
  - `PRIVATE_KEY_PEM` with your private key.
  - `ISSUER` with your issuer claim.
  - `USERNAME` with your username.

- In `local.properties` file (should be created in the root folder):
  - `sdk.dir` should point to your Android SDK location.
  - `aws.accessKey` should be replaced with your AWS access key.
  - `aws.secretKey` should be replaced with your AWS secret key.

---

Config.kt:

```kotlin
object Config {
    const val PRIVATE_KEY_PEM = "your_private_key"
    const val ISSUER = "issuer.claim"
    const val USERNAME = "username"
    const val MERCHANT_ID = "mer12345678"
    const val HOST = "kernelserver.qa.haloplus.io"
}
```

local.properties:
```properties
sdk.dir=~/Library/Android/sdk
aws.accessKey=your_access_key
aws.secretKey=your_secret_key
```