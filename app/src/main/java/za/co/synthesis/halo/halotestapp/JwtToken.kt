package za.co.synthesis.halo.halotestapp

import android.util.Base64
import android.util.Log
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

private const val TAG = "HaloJwtToken"

class JwtToken {

    /**
     * Get JWT token
     * @param callback Callback function to pass the JWT token
     */
    fun getJWT(callback: (String) -> Unit) {
        Log.d(TAG, "Getting JWT")

        // Generate Private Key
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(
            PKCS8EncodedKeySpec(Base64.decode(Config.PRIVATE_KEY_PEM, Base64.DEFAULT))
        )

        // Create JWT token
        val jwt = JWT
            .create()
            .withAudience(Config.OFFLINE_HOST)
            .withIssuer(Config.ISSUER)
            .withSubject(Config.MERCHANT_ID)
            .withClaim("aud_fingerprints", "sha256/zc6c97JhKPZUa+rIrVqjknDE1lDcDK77G41sDo+1ay0=")
            .withClaim("ksk_pin", "sha256/1Zna4T6PKcJ3Kq/dbVylb8n62j/AdQYUzWrj/4sk5Q8=")
            .withClaim("usr", Config.USERNAME)
            .withIssuedAt(Date())
            .sign(Algorithm.RSA512(null, privateKey as RSAPrivateKey))

        Log.d(TAG, "JWT: $jwt")

        // Pass the JWT token through callback function
        callback(jwt)
    }
}