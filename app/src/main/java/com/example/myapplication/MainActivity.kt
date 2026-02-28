package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File
import androidx.core.net.toUri
import kotlin.experimental.or
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    private val ver = 1125
    private var counter: Int = 10

    private val logPrefs: SharedPreferences by lazy {
        val context = this
        context.getSharedPreferences("log_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_LOG_URI = "persistent_log_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        if (grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this, SubActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    /** permissionの獲得 */
    fun init() {
        val neededPermissions = mutableListOf<String>()

        if (checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (checkSelfPermission(
                Manifest.permission.BLUETOOTH_ADVERTISE
        ) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissions(
                neededPermissions.toTypedArray(),
                0
            )
        } else {
            val intent = Intent(this, SubActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier, context: Context) {
        var buttonCount by remember { mutableIntStateOf(1) }
        //var buttonCount = { mutableIntStateOf(1) }
        var isCheck by remember { mutableStateOf(true) }

            Column {
                Text(
                    text = "Hello $name! corge",
                    modifier = modifier
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    val intent = Intent(context, SubActivity::class.java)
                    startActivity(
                        context,
                        intent,
                        null,
                    )

                }) {
                    Text(text = "~~サブ起動~~")
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    // ファイル書き出しオープンしてそれから
                    pickAndCreateLogFile(createFileLauncher)
                }) {
                    Text(text = "create log message file")
                }
                Spacer(Modifier.weight(1f))
                Button(shape = RoundedCornerShape(8.dp),
                    onClick = {
                    buttonCount++
                    counter++
                    AlertDialog.Builder(context)
                        .setTitle("alert dialog")
                        .setMessage("message $counter")
                        .show()
                }) {
                    Text(text = "alert Second $buttonCount")
                }
                Spacer(Modifier.weight(1f))
                Button(shape = RoundedCornerShape(8.dp),
                    onClick = { init() }) {
                    Text(text = "start $ver")
                }
                Spacer(Modifier.weight(1f))
                Row {
                    Text(text = "row1")
                    Checkbox(
                        checked = isCheck,
                        onCheckedChange = { isCheck = it }
                    )
                    Text(text = "row3")
                }
                Text(
                    text = "last bold",
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }

    }

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                onFileCreatedResult(uri)
            }
        }
    }

    fun getDownloadsUri(): Uri {
        val downloadDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val downloadDirPath = downloadDir.absolutePath
        return downloadDirPath.toUri()
    }

    fun pickAndCreateLogFile(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(
            Intent.ACTION_CREATE_DOCUMENT
        ).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "app_logs_${System.currentTimeMillis()}.txt")
            // あくまで最初の提案場所
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, getDownloadsUri())
        }
        launcher.launch(intent)
    }

    fun onFileCreatedResult(uri: Uri?) {
        uri ?: return

        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val context = this
            context.contentResolver.takePersistableUriPermission(
                uri, takeFlags
            )
            logPrefs.edit { putString(KEY_LOG_URI, uri.toString()) }
            Processing.init(context, uri)
            Processing.appendLine("start")
        } catch (e: Exception) {
            Log.e("LogWriter", "永続失敗")
        }
    }

    /*
//@Preview(showBackground = true)
@Composable
fun GreetingPreview(context: Context) {
    MyApplicationTheme {
        Greeting("Android", context = context)
    }
}*/

}
