package za.co.synthesis.halo.multiactivitytemplate

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import za.co.synthesis.halo.sdk.HaloSDK
import java.math.BigDecimal
import java.lang.ref.WeakReference

class PayActivityTest : AppCompatActivity() {

    // UI Components
    private lateinit var txtLoadMessage: TextView
    private lateinit var txtAmount: TextView

    private var transactionAmount: String = ""
    private var transactionID: String = ""
    private val TAG: String = "PayActivity"

    companion object {
        var currentInstance: WeakReference<PayActivityTest>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)
        currentInstance = WeakReference(this)
        Log.d(TAG, "On Create is called")

        initializeUI()
        startTransaction()
    }

    /**
     * Initialize UI Components
     */
    private fun initializeUI() {
        transactionAmount = intent.getStringExtra("transactionAmount").toString()
        transactionID = intent.getStringExtra("transactionId").toString()

        txtLoadMessage = findViewById(R.id.txtLoadMessage)
        txtAmount = findViewById(R.id.txtAmount)
        txtAmount.text = "J$ $transactionAmount"
    }

    /**
     * Start Transaction
     */
    private fun startTransaction() {
        HaloSDK.onCreate(this, this)
        Log.d(TAG, "Begin transaction")
        txtLoadMessage.text = "Reading your card... Please do not remove it."
        txtAmount.text = "J$  $transactionAmount"
        val amount = getAmount()
        if (amount != BigDecimal.ZERO) {
            val result = HaloSDK.startTransaction(amount, transactionID)
            Log.d(TAG, result.toString())
        } else {
            Toast.makeText(this, "Enter amount before transacting", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAmount(): BigDecimal {
        val amount = transactionAmount
        return if (amount.isEmpty()) {
            BigDecimal.ZERO
        } else {
            BigDecimal(amount)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentInstance = null
    }
}
