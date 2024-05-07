package za.co.synthesis.halo.halotestapp
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import za.co.synthesis.halo.haloCommonInterface.*
import za.co.synthesis.halo.sdk.model.*

private const val TAG = "HaloCallbacks"

@SuppressLint("SetTextI18n")
class HaloCallbacks(private val activity: MainActivity, private val timer: Timer) : IHaloCallbacks() {
    /**
     * Callback for initialization result - needs to be connected to internet
     */
    override fun onInitializationResult(result: HaloInitializationResult) {
        val elapsed = timer.end()
        Log.d(TAG, "Elapsed Time: ${elapsed.toFloat() / 1000.0} seconds")
        Log.v(TAG, "Initialisation Result - Type: ${result.resultType}")
        Log.d(TAG, "onInitializationResult ${result.resultType}")
        Log.d(TAG, "onInitializationResult Error Code: ${result.errorCode}")
        Log.d(TAG, "onInitializationResult Country Code: ${result.terminalCountryCode}")

        activity.runOnUiThread {
            activity.findViewById<TextView>(R.id.tvTapInstruction).text =
                "Initialize: ${result.errorCode}"
            val btnCharge = activity.findViewById<Button>(R.id.btnCharge)

            // Proceed with payment if initialized successfully
            if (result.errorCode != HaloErrorCode.OK) {
                Toast.makeText(activity, "SDK failed to initialize", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "SDK initialized", Toast.LENGTH_SHORT).show()
                btnCharge.isEnabled = true
                btnCharge.isClickable = true
            }

            // Log any warnings
            if (result.warnings.isNotEmpty()) {
                for (warning in result.warnings) {
                    Log.e(
                        "Warnings", "Warnings: ${warning.errorCode}${
                            warning.details.fold("") { acc, s ->
                                "$acc $s,"
                            }
                        }"
                    )
                }
            }
        }
    }

    /**
     * Callback for UI message from Halo
     */
    override fun onHaloUIMessage(message: HaloUIMessage) {
        Log.d(TAG, "Message ID: ${message.msgID}")
        Log.d(TAG, "Offline Balance: ${message.offlineBalance}")
        Log.d(TAG, "Transaction Amount: ${message.transactionAmount}")

        activity.runOnUiThread {
            activity.findViewById<TextView>(R.id.tvTapInstruction).text = message.msgID.Value()
        }
    }

    /**
     * Callback for transaction result
     */
    override fun onHaloTransactionResult(result: HaloTransactionResult) {
        Log.d(TAG, "onHaloTransactionResult")
        Log.d(TAG, result.resultType.toString())
        Log.d(TAG, result.errorCode.toString())
        Log.d(TAG, result.errorDetails.toString())

        val resultString = buildString {
            append("<h1>${splitCamelCase(result.resultType.toString())}</h1><br><br>")
            append("Code: <b>${result.errorCode}</b><br><br>")
            result.merchantTransactionReference?.let { append("Transaction Reference: <b>$it</b><br><br>") }
            result.receipt?.apply {
                transactionDate.takeIf { it.isNotBlank() }?.let { append("Transaction Date: <b>$it</b><br><br>") }
                transactionTime.takeIf { it.isNotBlank() }?.let { append("Transaction Time: <b>$it</b><br><br>") }
                maskedPAN.takeIf { it.isNotBlank() }?.let { append("Masked PAN: <b>$it</b><br><br>") }
                panSequenceNumber?.takeIf { it.isNotBlank() }?.let { append("Pan Sequence Number: <b>$it</b><br><br>") }
                applicationLabel?.takeIf { it.isNotBlank() }?.let { append("Application Label: <b>$it</b><br><br>") }
                aid?.takeIf { it.isNotBlank() }?.let { append("Aid: <b>$it</b><br><br>") }
                tvr?.takeIf { it.isNotBlank() }?.let { append("Tvr: <b>$it</b><br><br>") }
                currencyCode?.let { append("Currency Code: <b>$it</b><br><br>") }
                disposition?.let { append("Disposition: <b>$it</b><br><br>") }
                amountAuthorised?.let { append("Amount Authorised: <b>$it</b><br><br>") }
            }
        }

        activity.runOnUiThread {
            val tvTapInstruction = activity.findViewById<TextView>(R.id.tvTapInstruction)
            tvTapInstruction.text =
                HtmlCompat.fromHtml(resultString, HtmlCompat.FROM_HTML_MODE_COMPACT)
        }
    }

    private fun splitCamelCase(input: String): String {
        return input.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    }

    override fun onRequestJWT(callback: (String) -> Unit) {
        JwtToken().getJWT(callback)
    }

    override fun onSecurityError(errorCode: HaloErrorCode) {
        Log.d(TAG, "onSecurityError - $errorCode; should crash now")
    }

    override fun onAttestationError(details: HaloAttestationHealthResult) {
        Log.d(TAG, "onAttestationError - $details")
    }

    override fun onGetVerificationToken(result: HaloVerificationTokenResult) {
        Log.d(TAG, "onGetVerificationToken")
    }

    override fun onCameraControlLost() {
        Log.d(TAG, "onCameraControlLost")
    }
}