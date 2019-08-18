package dev.bananaumai.android.suburi.log

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.BufferedReader
import java.io.FileReader
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS' 'Z")
    private val timeRegex = Regex(pattern = "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\+\\d{4})")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)
        val uploadButton = findViewById<Button>(R.id.upload_button)
        uploadButton.setOnClickListener {
            val now = ZonedDateTime.now()

            Log.d("LogToFile", "Now is $now")

            val dir = filesDir
            val stream = Files.newDirectoryStream(dir.toPath(), "log.txt*")
            var content = ""

            stream.forEach {
                val br = BufferedReader(FileReader(it.toFile()))
                var line = br.readLine()
                while (line != null) {
                    if (compare(now, line)) {
                        content += line
                    }
                    line = br.readLine()
                }
            }

            textView.text = content
        }
    }

    private fun compare(now: ZonedDateTime, line: String): Boolean {
        val matchResult = timeRegex.find(line)

        return if (matchResult == null) {
            Log.d("LogToFile", "doesn't match")
            false
        } else {
            val time = ZonedDateTime.parse(matchResult.value, timeFormatter)
            Log.d("LogToFile", "parsed time is $time")
            time > (now.minusMinutes(1))
        }
    }
}
