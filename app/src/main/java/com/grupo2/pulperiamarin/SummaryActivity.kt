package com.grupo2.pulperiamarin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTotalValue: TextView
    private lateinit var lvTop5: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        dbHelper    = DatabaseHelper(this)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        lvTop5      = findViewById(R.id.lvTop5)

        loadSummary()
    }

    private fun loadSummary() {
        val db = dbHelper.readableDatabase

        // Valor total del inventario (price * stock de todos los productos)
        val cursorTotal = db.rawQuery("""
            SELECT SUM(${DatabaseHelper.COL_PROD_PRICE} * ${DatabaseHelper.COL_PROD_STOCK})
            FROM ${DatabaseHelper.TABLE_PRODUCTS}
        """.trimIndent(), null)

        if (cursorTotal.moveToFirst()) {
            val total = cursorTotal.getDouble(0)
            tvTotalValue.text = "L. ${"%.2f".format(total)}"
        }
        cursorTotal.close()

        // Top 5 productos por valor (price * stock) de mayor a menor
        val cursorTop5 = db.rawQuery("""
            SELECT ${DatabaseHelper.COL_PROD_NAME},
                   ${DatabaseHelper.COL_PROD_PRICE},
                   ${DatabaseHelper.COL_PROD_STOCK},
                   (${DatabaseHelper.COL_PROD_PRICE} * ${DatabaseHelper.COL_PROD_STOCK}) AS total_value
            FROM ${DatabaseHelper.TABLE_PRODUCTS}
            ORDER BY total_value DESC
            LIMIT 5
        """.trimIndent(), null)

        val list = mutableListOf<String>()
        var rank = 1

        while (cursorTop5.moveToNext()) {
            val name  = cursorTop5.getString(0)
            val price = cursorTop5.getDouble(1)
            val stock = cursorTop5.getInt(2)
            val total = cursorTop5.getDouble(3)

            list.add("#$rank  $name\n     L. ${"%.2f".format(price)} x $stock = L. ${"%.2f".format(total)}")
            rank++
        }
        cursorTop5.close()

        lvTop5.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
    }
}
