package com.grupo2.pulperiamarin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTotalSales: TextView
    private lateinit var tvTotalInventory: TextView
    private lateinit var lvTop5: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        dbHelper         = DatabaseHelper(this)
        tvTotalSales     = findViewById(R.id.tvTotalSales)
        tvTotalInventory = findViewById(R.id.tvTotalInventory)
        lvTop5           = findViewById(R.id.lvTop5)

        loadSummary()
    }

    private fun loadSummary() {
        val db = dbHelper.readableDatabase

        // 1. Calcular Ventas Totales (Dinero cobrado por salidas de producto)
        val cursorVentas = db.rawQuery("""
            SELECT SUM(m.${DatabaseHelper.COL_MOV_QTY} * p.${DatabaseHelper.COL_PROD_PRICE})
            FROM ${DatabaseHelper.TABLE_MOVEMENTS} m
            JOIN ${DatabaseHelper.TABLE_PRODUCTS} p ON m.${DatabaseHelper.COL_MOV_PRODUCT_ID} = p.${DatabaseHelper.COL_PROD_ID}
            WHERE m.${DatabaseHelper.COL_MOV_TYPE} = 'OUT'
        """, null)
        
        if (cursorVentas.moveToFirst()) {
            val ventas = cursorVentas.getDouble(0)
            tvTotalSales.text = "L. ${"%.2f".format(ventas)}"
        }
        cursorVentas.close()

        // 2. Calcular Valor en Inventario (Dinero que tengo en mercadería actualmente)
        val cursorInv = db.rawQuery("SELECT SUM(${DatabaseHelper.COL_PROD_PRICE} * ${DatabaseHelper.COL_PROD_STOCK}) FROM ${DatabaseHelper.TABLE_PRODUCTS}", null)
        if (cursorInv.moveToFirst()) {
            val inv = cursorInv.getDouble(0)
            tvTotalInventory.text = "L. ${"%.2f".format(inv)}"
        }
        cursorInv.close()

        // 3. Obtener Top 5 por valor (detallado)
        val cursorTop = db.rawQuery("""
            SELECT ${DatabaseHelper.COL_PROD_NAME}, 
                   ${DatabaseHelper.COL_PROD_PRICE}, 
                   ${DatabaseHelper.COL_PROD_STOCK},
                   (${DatabaseHelper.COL_PROD_PRICE} * ${DatabaseHelper.COL_PROD_STOCK}) as val_total
            FROM ${DatabaseHelper.TABLE_PRODUCTS}
            ORDER BY val_total DESC LIMIT 5
        """, null)

        val list = mutableListOf<String>()
        var rank = 1
        while (cursorTop.moveToNext()) {
            val name   = cursorTop.getString(0)
            val price  = cursorTop.getDouble(1)
            val stock  = cursorTop.getInt(2)
            val valTot = cursorTop.getDouble(3)
            
            list.add("$rank. $name\nL. ${"%.2f".format(price)} x $stock und. = L. ${"%.2f".format(valTot)}")
            rank++
        }
        cursorTop.close()

        lvTop5.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
    }
}