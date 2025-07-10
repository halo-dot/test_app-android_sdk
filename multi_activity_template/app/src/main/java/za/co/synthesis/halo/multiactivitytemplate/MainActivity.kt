package za.co.synthesis.halo.multiactivitytemplate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import za.co.synthesis.halo.sdk.HaloSDK

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MAIN ACTIVITY"
    private var permissionRetries: Int = 2
    private var permissionsGranted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartTransaction: Button = findViewById(R.id.btn_start_transaction)
        val btnSomethingElse: Button = findViewById(R.id.btn_something_else)

        btnStartTransaction.setOnClickListener {

            val intent = Intent(this, PayActivityTest::class.java).apply {
                putExtra("transactionId", "sometranscationId")
                putExtra("transactionAmount", "100")
            }
            startActivity(intent)
        }

        // Initialize Halo SDK
        if (requestNecessaryPermissions()) {
                val app = application as MyApplication
                app.initializeHaloSdk(this)
        }

        btnSomethingElse.setOnClickListener {
            // Handle other action here
        }
    }

    @SuppressLint("ObsoleteSdkInt")
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsAccepted = grantResults.fold(true) { acc, i -> acc && (i == PackageManager.PERMISSION_GRANTED) }
        if (requestCode == 10 && grantResults.isNotEmpty() && permissionsAccepted) {
            permissionsGranted = true
            val app = application as MyApplication
            app.initializeHaloSdk(this)
        } else {
            if (!requestNecessaryPermissions()) {
                Toast.makeText(
                    this,
                    "Please grant all permissions to use the app",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "On start is called")
        HaloSDK.onStart()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ON RESUME IS CALLED")
        if (permissionsGranted) {
            HaloSDK.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "On pause is called")
        HaloSDK.onPause()
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "On stop is called")
        HaloSDK.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.d(TAG, "On save instance is called")
        HaloSDK.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "On save instance state is called")
        HaloSDK.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "On destroy is called")
    }
}
