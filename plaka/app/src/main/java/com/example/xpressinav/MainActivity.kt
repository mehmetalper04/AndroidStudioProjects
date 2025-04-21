package com.example.xpressinav

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        val btngecis = findViewById<Button>(R.id.btngecis)
        btngecis.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }
        val rastgeleSayilar = (1..81).shuffled().take(8)
        val listView2 = findViewById<ListView>(R.id.listView2)
        val adapter1 = ArrayAdapter(this, android.R.layout.simple_list_item_1, rastgeleSayilar)
        listView2.adapter = adapter1

        val sehirler = arrayOf("Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin", "Aydın", "Balıkesir",
            "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum", "Denizli",
            "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari",
            "Hatay", "Isparta", "Mersin", "İstanbul", "İzmir", "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir",
            "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla", "Muş", "Nevşehir",
            "Niğde", "Ordu", "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas", "Tekirdağ", "Tokat",
            "Trabzon", "Tunceli", "Şanlıurfa", "Uşak", "Van", "Yozgat", "Zonguldak", "Aksaray", "Bayburt", "Karaman",
            "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan", "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye",
            "Düzce")
        val rastgeleSehirler = sehirler.toList().shuffled().take(8).toTypedArray()
        val listView = findViewById<ListView>(R.id.listView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rastgeleSehirler)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val secilensehir = sehirler[position]
            val secilenSayi = rastgeleSayilar[position]
            val dogruPlaka = sehirler.indexOf(secilensehir) + 1

            if (secilenSayi == dogruPlaka) {
                Toast.makeText(this, "✅ Doğru! $secilensehir'in plakası $dogruPlaka", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Yanlış! $secilensehir'in plakası $dogruPlaka ama sen $secilenSayi seçtin!", Toast.LENGTH_LONG).show()
            }
        }

    }
}