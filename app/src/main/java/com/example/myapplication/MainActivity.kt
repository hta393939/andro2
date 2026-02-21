package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var counter: Int = 10

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

    fun init() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
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
                Text(
                    text = "サブ起動",
                    modifier = modifier
                )
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                buttonCount++
                counter ++
                AlertDialog.Builder(context)
                    .setTitle("あらーとダイアログ")
                    .setMessage("メッセージ $counter")
                    .show()
            }) {
                Text(
                    text = "アラーと Second $buttonCount",
                    modifier = modifier
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    //.fillMaxWidth(1f)
                    //.height(48.dp)
                    .background(Color.Red)
            ) {
                Text(
                    fontSize = 100.sp,
                    text = "Third",
                    modifier = modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.weight(1f))
            Row {
                Text(
                    text = "row1",
                    modifier = modifier
                )
                Checkbox(
                    checked = isCheck,
                    onCheckedChange = { isCheck = it }
                )
                Text(
                    text = "row3",
                    modifier = modifier
                )
            }
            Text(
                text = "last太文字",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
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
