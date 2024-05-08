package za.co.synthesis.halo.halotestapp

object Config {
    // PEM file of private key to sign JWTs
    // Should match the public key uploaded to the portal
    const val PRIVATE_KEY_PEM = "-----BEGIN PUBLIC KEY-----\n" +
                                "<your key here\n" +
                                "-----END PUBLIC KEY-----"

    // The iss claim that was provided when signing up on the developer portal
    const val ISSUER = "your iss claim"

    // The MID of the merchant
    // Used to populate the "sub" claim of the JWT
    const val MERCHANT_ID = "mer12345678"

    // Username of the merchant
    // Will populate the "usr" claim in the JWT
    const val USERNAME = "merchant user name"


    // The backend host to send Halo traffic to
    // Under normal circumstances, this shouldn't be changed
    const val HOST = "kernelserver.qa.haloplus.io"
}