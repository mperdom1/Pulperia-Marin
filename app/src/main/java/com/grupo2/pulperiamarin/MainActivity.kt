package com.grupo2.pulperiamarin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Manejar Insets para diseño edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Configurar botones del menú principal
        findViewById<MaterialButton>(R.id.btnProducts).setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnMovements).setOnClickListener {
            startActivity(Intent(this, MovementsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnSummary).setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }
    }
}