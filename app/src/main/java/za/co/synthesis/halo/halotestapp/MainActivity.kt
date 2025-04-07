package za.co.synthesis.halo.halotestapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.Api
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import za.co.synthesis.halo.sdk.HaloSDK
import za.co.synthesis.halo.sdk.model.HaloInitializationParameters
import java.math.BigDecimal
import java.util.UUID

private const val TAG = "HaloTest"
class MainActivity : AppCompatActivity() {
    // UI Components
    private lateinit var btnCharge: Button
    private lateinit var tfAmount: TextInputEditText
    private lateinit var tfReference: TextInputLayout
    private lateinit var tfReferenceValue: TextInputEditText
    private lateinit var keypadLayout: GridLayout
    private lateinit var tvTapInstruction: TextView
    private lateinit var btnCancel: Button
    private var permissionRetries: Int = 2
    private var permissionsGranted: Boolean = false

    // does not mean successful initialization
    var initialized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Halo SDK
        if (requestNecessaryPermissions()) {
            initializeHaloSdk()
        }
        // Initialize UI Components
        initializeUI()
        // Set Click Listeners
        setClickListeners()
    }

    /**
     * Initialize UI Components
     */
    private fun initializeUI() {
        tfAmount = findViewById(R.id.tfAmount)
        btnCharge = findViewById(R.id.btnCharge)
        tfReference = findViewById(R.id.tfReference)
        tfReferenceValue = findViewById(R.id.tfReferenceValue)
        keypadLayout = findViewById(R.id.keypadLayout)
        tvTapInstruction = findViewById(R.id.tvTapInstruction)
        btnCancel = findViewById(R.id.btnCancel)

        // Disable the focus on amount field until reference is tapped
        tfAmount.isFocusableInTouchMode = false

        // Show soft keyboard when reference field is tapped
        tfReference.setOnClickListener {
            tfAmount.isFocusableInTouchMode = true
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(tfReference, InputMethodManager.SHOW_IMPLICIT)
        }

        // Hide soft keyboard when amount field gains focus
        tfAmount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            }
        }
    }

    private fun requestNecessaryPermissions(): Boolean {
        permissionRetries--

        if (permissionRetries >= 0) {
            Log.d(TAG, "Permission retries left: $permissionRetries")
        } else {
            Toast.makeText(this, "Necessary permissions not granted", Toast.LENGTH_LONG).show()
            return false
        }

        val outstandingPermissions = mutableListOf<String>()
        val requiredPermissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        requiredPermissions.forEach { permission ->
            val res = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            if (!res) {
                outstandingPermissions.add(permission)
            }
        }

        if (outstandingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, outstandingPermissions.toTypedArray(), 10)
            Toast.makeText(this, "Please grant all permissions to use the app", Toast.LENGTH_LONG).show()
            permissionsGranted = false
            return false
        } else {
            Log.d(TAG, "All permissions granted")
            permissionsGranted = true
            return true
        }
    }

    /**
     * Initialize Halo SDK
     */
    private fun initializeHaloSdk() {
        Toast.makeText(this, "Initializing SDK", Toast.LENGTH_LONG).show()
        HaloSDK.onCreate(this, this)
        Thread {
            val timer = Timer()
            val haloServices = HaloCallbacks(this, timer)
            timer.start()

            HaloSDK.initialize(
                HaloInitializationParameters(
                    haloServices,
                    60000,
                    BuildConfig.APPLICATION_ID,
                    BuildConfig.VERSION_NAME
                )
            )
        }.start()
    }

    /**
     * Set Click Listeners
     */
    private fun setClickListeners() {
        // Start Transaction
        btnCharge.setOnClickListener {
            startTransaction()
        }

        // Cancel Transaction
        btnCancel.setOnClickListener {
            cancelTransaction()
        }
    }

    /**
     * Start Transaction
     */
    private fun startTransaction() {
        Log.d(TAG, "Begin transaction")

        val amount = getAmount()
        val reference = getReference()

        if (amount != BigDecimal.ZERO) {
            val result = HaloSDK.startTransaction(amount, reference)
            Log.d(TAG, result.toString())
            showTapScreen()
        } else {
            Toast.makeText(this, "Enter amount before transacting", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Cancel Transaction
     */
    private fun cancelTransaction() {
        Log.d(TAG, "Cancel transaction")
        showMainScreen()
        tvTapInstruction.text = getString(R.string.present_card)
        tfAmount.setText("0")
    }

    /**
     * Toggle Main Screen
     */
    private fun showMainScreen() {
        btnCharge.visibility = View.VISIBLE
        tfAmount.visibility = View.VISIBLE
        tfReferenceValue.visibility = View.VISIBLE
        tfReference.visibility = View.VISIBLE
        keypadLayout.visibility = View.VISIBLE

        tvTapInstruction.visibility = View.GONE
        btnCancel.visibility = View.GONE
    }

    /**
     * Toggle Tap Screen
     */
    private fun showTapScreen() {
        btnCharge.visibility = View.GONE
        tfAmount.visibility = View.GONE
        tfReferenceValue.visibility = View.GONE
        tfReference.visibility = View.GONE
        keypadLayout.visibility = View.GONE

        tvTapInstruction.visibility = View.VISIBLE
        btnCancel.visibility = View.VISIBLE
    }

    /**
     * Handle Key Press for on-screen keypad
     */
    fun onKeyPress(view: View) {
        val button = view as Button
        val value = button.text.toString()

        val currentAmount = tfAmount.text.toString()
        if (currentAmount == "0") {
            tfAmount.setText(value)
        } else {
            tfAmount.append(value)
        }
    }

    /**
     * Clear transaction amount value
     */
    fun onClear(view: View) {
        tfAmount.setText("0")
        tfReferenceValue.setText("")
        tfReferenceValue.clearFocus()
    }

    /**
     * Get transaction amount value
     */
    private fun getAmount(): BigDecimal {
        val amount = tfAmount.text.toString()
        return if (amount.isEmpty()) {
            BigDecimal.ZERO
        } else {
            BigDecimal(amount)
        }
    }

    /**
     * Get transaction reference
     */
    private fun getReference(): String {
        val reference = tfReferenceValue.text.toString()
        return reference.ifEmpty {
            UUID.randomUUID().toString()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsAccepted = grantResults.fold(true) { acc, i -> acc && (i == PackageManager.PERMISSION_GRANTED) }
        if (requestCode == 10 && grantResults.isNotEmpty() && permissionsAccepted) {
            permissionsGranted = true
            initializeHaloSdk()
        } else {
            if (!requestNecessaryPermissions()) {
                Toast.makeText(this, "Please grant all permissions to use the app", Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        // your mobile app code here
        Log.d(TAG, "On start is called")
        HaloSDK.onStart()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ON RESUME IS CALLED")
        // your mobile app code here
        if (permissionsGranted && initialized) {
            HaloSDK.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "On pause is called")
        // your mobile app code here
        HaloSDK.onPause()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "On stop is called")
        // your mobile app code here
        HaloSDK.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.d(TAG, "On save instance is called")
        // your mobile app code here
        HaloSDK.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "On save instance state is called")
        // your mobile app code here
        HaloSDK.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "On destroy is called")
        // your mobile app code here
        HaloSDK.onDestroy()
    }
}