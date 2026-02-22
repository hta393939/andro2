package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.OutputStreamWriter
import androidx.core.net.toUri
import java.lang.ref.WeakReference

object Processing {
    //var uriString: String = ""
    lateinit var uri: Uri
    private var context: WeakReference<Context>? = null

    fun init(ctx: Context, inUri: Uri) {
        context = WeakReference(ctx.applicationContext)
        uri = inUri
    }

    fun getContext(): Context? {
        return context?.get()
    }

    fun appendLine(line: String) {
        try {
            getContext()?.let { ctx ->
                val resolver = ctx.contentResolver
                resolver.openOutputStream(uri, "wa")?.use { os ->
                    OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                        writer.appendLine(line)
                        writer.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LogWriter", "追記失敗", e)
        }
    }

    fun i(tag: String, message: String) {
        this.appendLine("$tag, $message")
    }
    fun w(tag: String, message: String) {
        this.appendLine("$tag $message")
    }
    fun e(tag: String, message: String) {
        this.appendLine("$tag $message")
    }
    fun d(tag: String, message: String) {
        this.appendLine("$tag $message")
    }
}

