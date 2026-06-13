package com.grupo2.pulperiamarin

import android.app.AlertDialog
import android.content.ContentValues
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ProductsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etName: EditText
    private lateinit var etPrice: EditText
    private lateinit var etStock: EditText
    private lateinit var etBarcode: EditText
    private lateinit var etMinStock: EditText
    private lateinit var btnSave: Button
    private lateinit var lvProducts: ListView

    // Para saber si estamos editando
    private var editingId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        dbHelper = DatabaseHelper(this)

        etName     = findViewById(R.id.etName)
        etPrice    = findViewById(R.id.etPrice)
        etStock    = findViewById(R.id.etStock)
        etBarcode  = findViewById(R.id.etBarcode)
        etMinStock = findViewById(R.id.etMinStock)
        btnSave    = findViewById(R.id.btnSave)
        lvProducts = findViewById(R.id.lvProducts)

        btnSave.setOnClickListener { saveProduct() }

        loadProducts()
    }

    private fun saveProduct() {
        val name     = etName.text.toString().trim()
        val priceStr = etPrice.text.toString().trim()
        val stockStr = etStock.text.toString().trim()
        val barcode  = etBarcode.text.toString().trim()
        val minStockStr = etMinStock.text.toString().trim()

        // Validaciones básicas
        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Nombre, precio y stock son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val price    = priceStr.toDouble()
        val stock    = stockStr.toInt()
        val minStock = if (minStockStr.isEmpty()) 5 else minStockStr.toInt()
        val date     = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PROD_NAME, name)
            put(DatabaseHelper.COL_PROD_PRICE, price)
            put(DatabaseHelper.COL_PROD_STOCK, stock)
            put(DatabaseHelper.COL_PROD_BARCODE, barcode.ifEmpty { null })
            put(DatabaseHelper.COL_PROD_MIN_STOCK, minStock)
            put(DatabaseHelper.COL_PROD_CREATED_AT, date)
        }

        val db = dbHelper.writableDatabase

        if (editingId == -1) {
            // Crear nuevo
            db.insert(DatabaseHelper.TABLE_PRODUCTS, null, values)
            Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
        } else {
            // Actualizar existente
            db.update(
                DatabaseHelper.TABLE_PRODUCTS,
                values,
                "${DatabaseHelper.COL_PROD_ID} = ?",
                arrayOf(editingId.toString())
            )
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
            editingId = -1
            btnSave.text = "Guardar Producto"
        }

        clearFields()
        loadProducts()
    }

    private fun loadProducts() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_PROD_NAME} ASC"
        )

        val list = mutableListOf<String>()
        val ids  = mutableListOf<Int>()

        while (cursor.moveToNext()) {
            val id    = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_ID))
            val name  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_PRICE))
            val stock = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_STOCK))
            val minSt = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_MIN_STOCK))

            val lowStockWarning = if (stock <= minSt) " ⚠️" else ""
            list.add("$name | L. ${"%.2f".format(price)} | Stock: $stock$lowStockWarning")
            ids.add(id)
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        lvProducts.adapter = adapter

        // Clic largo = opciones editar/eliminar
        lvProducts.setOnItemLongClickListener { _, _, position, _ ->
            showOptions(ids[position], list[position])
            true
        }
    }

    private fun showOptions(productId: Int, productName: String) {
        AlertDialog.Builder(this)
            .setTitle(productName)
            .setItems(arrayOf("✏️ Editar", "🗑️ Eliminar")) { _, which ->
                when (which) {
                    0 -> loadForEdit(productId)
                    1 -> confirmDelete(productId)
                }
            }
            .show()
    }

    private fun loadForEdit(productId: Int) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PRODUCTS,
            null,
            "${DatabaseHelper.COL_PROD_ID} = ?",
            arrayOf(productId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME)))
            etPrice.setText(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_PRICE)).toString())
            etStock.setText(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_STOCK)).toString())
            etBarcode.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_BARCODE)) ?: "")
            etMinStock.setText(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_MIN_STOCK)).toString())
            editingId = productId
            btnSave.text = "Actualizar Producto"
        }
        cursor.close()
    }

    private fun confirmDelete(productId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Estás segura de que deseas eliminar este producto?")
            .setPositiveButton("Eliminar") { _, _ ->
                val db = dbHelper.writableDatabase
                db.delete(
                    DatabaseHelper.TABLE_PRODUCTS,
                    "${DatabaseHelper.COL_PROD_ID} = ?",
                    arrayOf(productId.toString())
                )
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                loadProducts()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearFields() {
        etName.text.clear()
        etPrice.text.clear()
        etStock.text.clear()
        etBarcode.text.clear()
        etMinStock.text.clear()
    }
}