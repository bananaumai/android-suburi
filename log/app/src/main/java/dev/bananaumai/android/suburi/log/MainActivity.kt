package dev.bananaumai.android.suburi.log

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import java.io.BufferedReader
import java.io.FileReader
import java.nio.file.Files
import java.time.Instant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val uploadButton = findViewById<Button>(R.id.upload_button)
        uploadButton.setOnClickListener {
            val now = Instant.now()

            val dir = filesDir
            val stream = Files.newDirectoryStream(dir.toPath(), "log.txt*")
            stream.forEach {
                val br = BufferedReader(FileReader(it.toFile()))
                var line: String?
                line = br.readLine()
                while (line != null) {
                    line
                }
                openFileInput(it.toString()).also {

                }
            }
        }
    }

    private fun compare(now: Instant, line: String): Boolean {
        return true
    }
}
