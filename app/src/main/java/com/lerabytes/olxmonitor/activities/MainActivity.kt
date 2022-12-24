package com.lerabytes.olxmonitor.activities


import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lerabytes.olxmonitor.R

class MainActivity : AppCompatActivity() {
    private val sTag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SAMPLE RECYCLERVIEW ITEM ACCESS FROM OTHER CLASS.
//        val awesomeButton = findViewById<View>(R.id.button)
//        awesomeButton.setOnClickListener(View.OnClickListener {
//            val view = recyclerItems.layoutManager?.findViewByPosition(0)
//            if (view != null) {
//
//                (items[0].`object` as ItemTitle).itemTitle = "test"
//                (recyclerItems.adapter as ItemsAdapter).notifyItemChanged(0)
//            }
//        })
    }

    fun startAddItemActivity(view: View?) {
        val intent = Intent(this, AddItemActivity::class.java)
        startActivity(intent)
    }
}