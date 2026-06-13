package com.grupo2.pulperiamarin

import android.content.ContentValues
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MovementsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var spinnerProduct: Spinner
    private lateinit var rgType: RadioGroup
    private lateinit var rbIn: RadioButton
    private lateinit var etQty: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnRegister: Button
    private lateinit var lvMovements: ListView

    // Lista de ids para relacionar spinner con BD
    private val productIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movements)

        dbHelper      = DatabaseHelper(this)
        spinnerProduct = findViewById(R.id.spinnerProduct)
        rgType        = findViewById(R.id.rgType)
        rbIn          = findViewById(R.id.rbIn)
        etQty         = findViewById(R.id.etQty)
        etNotes       = findViewById(R.id.etNotes)
        btnRegister   = findViewById(R.id.btnRegister)
        lvMovements   = findViewById(R.id.lvMovements)

        loadProductsSpinner()
        loadMovements()

        btnRegister.setOnClickListener { registerMovement() }
    }

    private fun loadProductsSpinner() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            arrayOf(DatabaseHelper.COL_PROD_ID, DatabaseHelper.COL_PROD_NAME),
            null, null, null, null,
            "${DatabaseHelper.COL_PROD_NAME} ASC"
        )

        val names = mutableListOf<String>()
        productIds.clear()

        while (cursor.moveToNext()) {
            productIds.add(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_ID)))
            names.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME)))
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProduct.adapter = adapter
    }

    private fun registerMovement() {
        if (productIds.isEmpty()) {
            Toast.makeText(this, "Primero registra un producto", Toast.LENGTH_SHORT).show()
            return
        }

        val qtyStr = etQty.text.toString().trim()
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "La cantidad es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }

        val qty       = qtyStr.toInt()
        val notes     = etNotes.text.toString().trim()
        val type      = if (rbIn.isChecked) "IN" else "OUT"
        val productId = productIds[spinnerProduct.selectedItemPosition]
        val date      = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val db = dbHelper.writableDatabase

        // Verificar stock suficiente si es salida
        if (type == "OUT") {
            val cursor = db.query(
                DatabaseHelper.TABLE_PRODUCTS,
                arrayOf(DatabaseHelper.COL_PROD_STOCK),
                "${DatabaseHelper.COL_PROD_ID} = ?",
                arrayOf(productId.toString()),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                val currentStock = cursor.getInt(0)
                if (qty > currentStock) {
                    Toast.makeText(this, "Stock insuficiente. Disponible: $currentStock", Toast.LENGTH_LONG).show()
                    cursor.close()
                    return
                }
            }
            cursor.close()
        }

        // Guardar movimiento
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_MOV_PRODUCT_ID, productId)
            put(DatabaseHelper.COL_MOV_QTY, qty)
            put(DatabaseHelper.COL_MOV_TYPE, type)
            put(DatabaseHelper.COL_MOV_DATE, date)
            put(DatabaseHelper.COL_MOV_NOTES, notes.ifEmpty { null })
        }
        db.insert(DatabaseHelper.TABLE_MOVEMENTS, null, values)

        // Actualizar stock del producto
        val stockChange = if (type == "IN") qty else -qty
        db.execSQL(
            "UPDATE ${DatabaseHelper.TABLE_PRODUCTS} SET ${DatabaseHelper.COL_PROD_STOCK} = ${DatabaseHelper.COL_PROD_STOCK} + ? WHERE ${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(stockChange, productId)
        )

        // Verificar si quedó en stock mínimo
        checkLowStock(productId)

        Toast.makeText(this, "Movimiento registrado", Toast.LENGTH_SHORT).show()
        etQty.text.clear()
        etNotes.text.clear()
        loadMovements()
    }

    private fun checkLowStock(productId: Int) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            arrayOf(
                DatabaseHelper.COL_PROD_NAME,
                DatabaseHelper.COL_PROD_STOCK,
                DatabaseHelper.COL_PROD_MIN_STOCK
            ),
            "${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(productId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            val name     = cursor.getString(0)
            val stock    = cursor.getInt(1)
            val minStock = cursor.getInt(2)

            if (stock <= minStock) {
                // Guardar alerta en BD
                val alertValues = ContentValues().apply {
                    put(DatabaseHelper.COL_ALERT_PRODUCT_ID, productId)
                    put(DatabaseHelper.COL_ALERT_TYPE, "LOW_STOCK")
                    put(DatabaseHelper.COL_ALERT_MESSAGE, "Stock bajo: $name tiene solo $stock unidades")
                    put(DatabaseHelper.COL_ALERT_DATE,
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    put(DatabaseHelper.COL_ALERT_STATUS, "PENDING")
                }
                dbHelper.writableDatabase.insert(DatabaseHelper.TABLE_ALERTS, null, alertValues)

                // Mostrar alerta visual
                Toast.makeText(
                    this,
                    "⚠️ Stock bajo: $name solo tiene $stock unidades",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        cursor.close()
    }

    private fun loadMovements() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("""
            SELECT m.${DatabaseHelper.COL_MOV_TYPE},
                   m.${DatabaseHelper.COL_MOV_QTY},
                   m.${DatabaseHelper.COL_MOV_DATE},
                   p.${DatabaseHelper.COL_PROD_NAME}
            FROM ${DatabaseHelper.TABLE_MOVEMENTS} m
            INNER JOIN ${DatabaseHelper.TABLE_PRODUCTS} p
                ON m.${DatabaseHelper.COL_MOV_PRODUCT_ID} = p.${DatabaseHelper.COL_PROD_ID}
            ORDER BY m.${DatabaseHelper.COL_MOV_DATE} DESC
        """.trimIndent(), null)

        val list = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val type    = if (cursor.getString(0) == "IN") "📥" else "📤"
            val qty     = cursor.getInt(1)
            val date    = cursor.getString(2).substring(0, 10)
            val product = cursor.getString(3)
            list.add("$type $product | Cant: $qty | $date")
        }
        cursor.close()

        lvMovements.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
    }
}
