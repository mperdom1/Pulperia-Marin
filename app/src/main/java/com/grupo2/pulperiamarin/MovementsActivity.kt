package com.grupo2.pulperiamarin

import android.content.ContentValues
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class MovementsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var spinnerProduct: Spinner
    private lateinit var rgType: RadioGroup
    private lateinit var rbIn: RadioButton
    private lateinit var etQty: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var lvMovements: ListView

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
        val cursor = db.query(DatabaseHelper.TABLE_PRODUCTS, arrayOf(DatabaseHelper.COL_PROD_ID, DatabaseHelper.COL_PROD_NAME), null, null, null, null, "${DatabaseHelper.COL_PROD_NAME} ASC")

        val names = mutableListOf<String>()
        productIds.clear()

        while (cursor.moveToNext()) {
            productIds.add(cursor.getInt(0))
            names.add(cursor.getString(1))
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProduct.adapter = adapter
    }

    private fun registerMovement() {
        if (productIds.isEmpty()) {
            Toast.makeText(this, "No hay productos registrados", Toast.LENGTH_SHORT).show()
            return
        }

        val qtyStr = etQty.text.toString().trim()
        if (qtyStr.isEmpty()) {
            Toast.makeText(this, "Ingresa una cantidad", Toast.LENGTH_SHORT).show()
            return
        }

        val qty       = qtyStr.toIntOrNull() ?: 0
        val notes     = etNotes.text.toString().trim()
        val type      = if (rbIn.isChecked) "IN" else "OUT"
        val productId = productIds[spinnerProduct.selectedItemPosition]
        val date      = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val db = dbHelper.writableDatabase

        if (type == "OUT") {
            val cursor = db.query(DatabaseHelper.TABLE_PRODUCTS, arrayOf(DatabaseHelper.COL_PROD_STOCK), "${DatabaseHelper.COL_PROD_ID} = ?", arrayOf(productId.toString()), null, null, null)
            if (cursor.moveToFirst()) {
                val currentStock = cursor.getInt(0)
                if (qty > currentStock) {
                    Toast.makeText(this, "Stock insuficiente (Disponible: $currentStock)", Toast.LENGTH_LONG).show()
                    cursor.close()
                    return
                }
            }
            cursor.close()
        }

        // Insertar Movimiento
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_MOV_PRODUCT_ID, productId)
            put(DatabaseHelper.COL_MOV_QTY, qty)
            put(DatabaseHelper.COL_MOV_TYPE, type)
            put(DatabaseHelper.COL_MOV_DATE, date)
            put(DatabaseHelper.COL_MOV_NOTES, notes)
        }
        db.insert(DatabaseHelper.TABLE_MOVEMENTS, null, values)

        // Actualizar Stock
        val delta = if (type == "IN") qty else -qty
        db.execSQL("UPDATE ${DatabaseHelper.TABLE_PRODUCTS} SET ${DatabaseHelper.COL_PROD_STOCK} = ${DatabaseHelper.COL_PROD_STOCK} + ? WHERE ${DatabaseHelper.COL_PROD_ID} = ?", arrayOf(delta, productId))

        checkLowStock(productId)

        etQty.text?.clear()
        etNotes.text?.clear()
        loadMovements()
        Toast.makeText(this, "Movimiento registrado", Toast.LENGTH_SHORT).show()
    }

    private fun checkLowStock(productId: Int) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT ${DatabaseHelper.COL_PROD_NAME}, ${DatabaseHelper.COL_PROD_STOCK}, ${DatabaseHelper.COL_PROD_MIN_STOCK} FROM ${DatabaseHelper.TABLE_PRODUCTS} WHERE ${DatabaseHelper.COL_PROD_ID} = ?", arrayOf(productId.toString()))

        if (cursor.moveToFirst()) {
            val name = cursor.getString(0)
            val stock = cursor.getInt(1)
            val min = cursor.getInt(2)

            if (stock <= min) {
                Toast.makeText(this, "⚠️ ALERTA: $name tiene stock crítico ($stock)", Toast.LENGTH_LONG).show()
                // Opcional: Registrar en tabla alerts
                val alertValues = ContentValues().apply {
                    put(DatabaseHelper.COL_ALERT_PRODUCT_ID, productId)
                    put(DatabaseHelper.COL_ALERT_MESSAGE, "Stock bajo: $stock unidades")
                    put(DatabaseHelper.COL_ALERT_DATE, SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
                }
                dbHelper.writableDatabase.insert(DatabaseHelper.TABLE_ALERTS, null, alertValues)
            }
        }
        cursor.close()
    }

    private fun loadMovements() {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("""
            SELECT p.${DatabaseHelper.COL_PROD_NAME}, m.${DatabaseHelper.COL_MOV_QTY}, m.${DatabaseHelper.COL_MOV_TYPE}, m.${DatabaseHelper.COL_MOV_DATE}
            FROM ${DatabaseHelper.TABLE_MOVEMENTS} m
            JOIN ${DatabaseHelper.TABLE_PRODUCTS} p ON m.${DatabaseHelper.COL_MOV_PRODUCT_ID} = p.${DatabaseHelper.COL_PROD_ID}
            ORDER BY m.${DatabaseHelper.COL_MOV_ID} DESC LIMIT 20
        """, null)

        val list = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val name = cursor.getString(0)
            val qty  = cursor.getInt(1)
            val type = if (cursor.getString(2) == "IN") "📥 ENTRADA" else "📤 SALIDA"
            val date = cursor.getString(3)
            list.add("$type - $name\nCant: $qty | $date")
        }
        cursor.close()
        lvMovements.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
    }
}