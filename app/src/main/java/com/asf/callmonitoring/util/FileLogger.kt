package com.asf.callmonitoring.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val FILE_NAME = "AppLogASF.txt"

    fun log(context: Context, mensaje: String) {
        // También lo imprime en Logcat normal por si lo tienes conectado
        Log.d("DEBUG_CAJA_NEGRA", mensaje)

        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val linea = "$timestamp -> $mensaje\n"

            // Lo guardamos en una carpeta accesible de tu aplicación
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (directory != null) {
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, FILE_NAME)

                val fileOutputStream = FileOutputStream(file, true) // true = no borra lo anterior
                val outputStreamWriter = OutputStreamWriter(fileOutputStream)
                outputStreamWriter.append(linea)
                outputStreamWriter.close()
                fileOutputStream.close()
            }
        } catch (e: Exception) {
            Log.e("DEBUG_CAJA_NEGRA", "Error al escribir: ${e.message}")
        }
    }
}