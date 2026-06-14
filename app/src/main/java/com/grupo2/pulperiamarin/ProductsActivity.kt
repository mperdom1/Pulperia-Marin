package com.grupo2.pulperiamarin

import android.app.AlertDialog
import android.content.ContentValues
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class ProductsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etName: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etStock: TextInputEditText
    private lateinit var etBarcode: TextInputEditText
    private lateinit var etMinStock: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var lvProducts: ListView

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

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val price    = priceStr.toDoubleOrNull() ?: 0.0
        val stock    = stockStr.toIntOrNull() ?: 0
        val minStock = if (minStockStr.isEmpty()) 5 else (minStockStr.toIntOrNull() ?: 5)
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

        try {
            if (editingId == -1) {
                db.insertOrThrow(DatabaseHelper.TABLE_PRODUCTS, null, values)
                Toast.makeText(this, "Producto guardado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                db.update(DatabaseHelper.TABLE_PRODUCTS, values, "${DatabaseHelper.COL_PROD_ID} = ?", arrayOf(editingId.toString()))
                Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                editingId = -1
                btnSave.text = "Guardar Producto"
            }
            clearFields()
            hideKeyboard()
            loadProducts()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: El código de barras ya existe", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadProducts() {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_PRODUCTS, null, null, null, null, null, "${DatabaseHelper.COL_PROD_NAME} ASC")

        val list = mutableListOf<String>()
        val ids  = mutableListOf<Int>()

        while (cursor.moveToNext()) {
            val id    = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_ID))
            val name  = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME))
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_PRICE))
            val stock = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_STOCK))
            val minSt = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_MIN_STOCK))

            val warning = if (stock <= minSt) " ⚠️ BAJO STOCK" else ""
            list.add("$name\nL. ${"%.2f".format(price)} | Stock: $stock$warning")
            ids.add(id)
        }
        cursor.close()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        lvProducts.adapter = adapter

        lvProducts.setOnItemLongClickListener { _, _, position, _ ->
            showOptions(ids[position], list[position])
            true
        }
    }

    private fun showOptions(productId: Int, productInfo: String) {
        AlertDialog.Builder(this)
            .setTitle("Opciones de Producto")
            .setMessage(productInfo)
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
        val cursor = db.query(DatabaseHelper.TABLE_PRODUCTS, null, "${DatabaseHelper.COL_PROD_ID} = ?", arrayOf(productId.toString()), null, null, null)

        if (cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_NAME)))
            etPrice.setText(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_PRICE)).toString())
            etStock.setText(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_STOCK)).toString())
            etBarcode.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_BARCODE)) ?: "")
            etMinStock.setText(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PROD_MIN_STOCK)).toString())
            editingId = productId
            btnSave.text = "Actualizar Producto"
            etName.requestFocus()
        }
        cursor.close()
    }

    private fun confirmDelete(productId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas borrar este producto y sus movimientos?")
            .setPositiveButton("Eliminar") { _, _ ->
                dbHelper.writableDatabase.delete(DatabaseHelper.TABLE_PRODUCTS, "${DatabaseHelper.COL_PROD_ID} = ?", arrayOf(productId.toString()))
                loadProducts()
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearFields() {
        etName.text?.clear()
        etPrice.text?.clear()
        etStock.text?.clear()
        etBarcode.text?.clear()
        etMinStock.setText("5")
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}