package za.co.synthesis.halo.halotestapp

object Config {
   // PEM file of private key to sign JWTs
   // Should match the public key uploaded to the portal
   const val PRIVATE_KEY_PEM = "your_private_key"

   // The iss claim that was provided when signing up on the developer portal
   const val ISSUER = "issuer.claim"

   // The MID of the merchant
   // Used to populate the "sub" claim of the JWT
   const val MERCHANT_ID = "{D8208288-E869-4726-B198-364D66EC9243}"

   // Username of the merchant
   // Will populate the "usr" claim in the JWT
   const val USERNAME = "username"

   // The backend host to send Halo traffic to
   // Under normal circumstances, this shouldn't be changed
   const val HOST = "kernelserver.qa.haloplus.io"
}
