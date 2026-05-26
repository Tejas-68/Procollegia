package com.procollegia;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddInventoryActivity extends AppCompatActivity {

    private EditText etName, etSport, etQty;
    private Button btnSave;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_inventory);

        etName = findViewById(R.id.etItemName);
        etSport = findViewById(R.id.etSport);
        etQty = findViewById(R.id.etQuantity);
        btnSave = findViewById(R.id.btnSaveItem);
        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveItem());
    }

    private void saveItem() {
        String name = etName.getText().toString().trim();
        String sport = etSport.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(sport) || TextUtils.isEmpty(qtyStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyStr);
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("category", sport);
        item.put("quantity", qty);
        item.put("remaining", qty);
        item.put("status", "available");

        btnSave.setEnabled(false);
        db.collection("inventory").add(item)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
