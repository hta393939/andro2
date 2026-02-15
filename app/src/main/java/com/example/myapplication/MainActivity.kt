package com.example.myapplication

import android.graphics.Paint.Align
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var buttonCount by remember { mutableIntStateOf(1) }
    //var buttonCount = { mutableIntStateOf(1) }
    var isCheck by remember {mutableStateOf(true)}
    Column {
        Text(
            text = "Hello $name! corge",
            modifier = modifier
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            buttonCount ++
        }) {
            Text(
                text = "Second $buttonCount",
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}