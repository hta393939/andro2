package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.theme.MyApplicationTheme

class SubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ActivityじゃなくてComponentActivityにすると出てきた
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme() {
                PageComponent()
            }
        }
    }

    @Composable
    fun PageComponent() {
        // Unitを返す@Composableは大文字スタートらしい
        Column(
            modifier = Modifier
        ) {
            Text(
                text = "サブアクティビティ"
            )
            Text(
                text = "サブだよ"
            )
            Button(onClick = {
                // TODO: ボタン
            }) {
                Text(
                    text = "ボタン0",
                    modifier = Modifier
                )
            }
            Button(onClick = {
                // TODO: ボタン実装

            }) {
                Text(
                    text = "ボタン1",
                    modifier = Modifier
                )
            }
        }
    }

}