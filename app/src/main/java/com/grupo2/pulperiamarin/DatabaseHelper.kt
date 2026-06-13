package com.grupo2.pulperiamarin

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "pulperia.db"
        const val DATABASE_VERSION = 1

        // Tabla productos
        const val TABLE_PRODUCTS = "products"
        const val COL_PROD_ID = "id"
        const val COL_PROD_NAME = "name"
        const val COL_PROD_PRICE = "price"
        const val COL_PROD_STOCK = "stock"
        const val COL_PROD_BARCODE = "barcode"
        const val COL_PROD_MIN_STOCK = "min_stock"
        const val COL_PROD_CREATED_AT = "created_at"

        // Tabla movimientos
        const val TABLE_MOVEMENTS = "movements"
        const val COL_MOV_ID = "id"
        const val COL_MOV_PRODUCT_ID = "product_id"
        const val COL_MOV_QTY = "qty"
        const val COL_MOV_TYPE = "type"   // "IN" o "OUT"
        const val COL_MOV_DATE = "date"
        const val COL_MOV_NOTES = "notes"

        // Tabla alertas
        const val TABLE_ALERTS = "alerts"
        const val COL_ALERT_ID = "id"
        const val COL_ALERT_PRODUCT_ID = "product_id"
        const val COL_ALERT_TYPE = "alert_type"
        const val COL_ALERT_MESSAGE = "message"
        const val COL_ALERT_DATE = "date"
        const val COL_ALERT_STATUS = "status"  // "PENDING" o "READ"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_PROD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PROD_NAME TEXT NOT NULL,
                $COL_PROD_PRICE REAL NOT NULL,
                $COL_PROD_STOCK INTEGER NOT NULL DEFAULT 0,
                $COL_PROD_BARCODE TEXT UNIQUE,
                $COL_PROD_MIN_STOCK INTEGER NOT NULL DEFAULT 5,
                $COL_PROD_CREATED_AT TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_MOVEMENTS (
                $COL_MOV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MOV_PRODUCT_ID INTEGER NOT NULL,
                $COL_MOV_QTY INTEGER NOT NULL,
                $COL_MOV_TYPE TEXT NOT NULL,
                $COL_MOV_DATE TEXT NOT NULL,
                $COL_MOV_NOTES TEXT,
                FOREIGN KEY($COL_MOV_PRODUCT_ID) REFERENCES $TABLE_PRODUCTS($COL_PROD_ID)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_ALERTS (
                $COL_ALERT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ALERT_PRODUCT_ID INTEGER NOT NULL,
                $COL_ALERT_TYPE TEXT NOT NULL,
                $COL_ALERT_MESSAGE TEXT NOT NULL,
                $COL_ALERT_DATE TEXT NOT NULL,
                $COL_ALERT_STATUS TEXT NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY($COL_ALERT_PRODUCT_ID) REFERENCES $TABLE_PRODUCTS($COL_PROD_ID)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVEMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    // Habilitar foreign keys cada vez que se abre la BD
    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }
}
