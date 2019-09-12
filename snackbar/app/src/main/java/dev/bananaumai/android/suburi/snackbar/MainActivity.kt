package dev.bananaumai.android.suburi.snackbar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Snackbar.make(findViewById(R.id.main_container), "test1", Snackbar.LENGTH_LONG).show()
        Snackbar.make(findViewById(R.id.main_container), "test2", Snackbar.LENGTH_LONG).show()
    }
}
