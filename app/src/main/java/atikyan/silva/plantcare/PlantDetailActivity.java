package atikyan.silva.plantcare;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PlantDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PLANT_ID   = "plantId";
    public static final String EXTRA_PLANT_NAME = "plantName";

    private FirebaseFirestore db;
    private String uid;
    private String plantId;

    private ImageView    ivDetailPhoto;
    private LinearLayout layoutNoPhoto;
    private TextView     tvDetailName;
    private TextView     tvDetailPot;
    private TextView     tvDetailRoom;
    private TextView     tvDetailWater;
    private TextView     tvDetailLight;
    private TextView     tvDetailNotes;
    private CardView     cardNotes;
    private MaterialButton btnWaterNow;


    private Long currentWaterDays = 7L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        uid     = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        db      = FirebaseFirestore.getInstance();
        plantId = getIntent().getStringExtra(EXTRA_PLANT_ID);

        if (uid == null || plantId == null) { finish(); return; }

        bindViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());

        btnWaterNow.setOnClickListener(v -> waterPlant());

        loadPlant();
    }

    private void bindViews() {
        ivDetailPhoto  = findViewById(R.id.ivDetailPhoto);
        layoutNoPhoto  = findViewById(R.id.layoutNoPhoto);
        tvDetailName   = findViewById(R.id.tvDetailName);
        tvDetailPot    = findViewById(R.id.tvDetailPot);
        tvDetailRoom   = findViewById(R.id.tvDetailRoom);
        tvDetailWater  = findViewById(R.id.tvDetailWater);
        tvDetailLight  = findViewById(R.id.tvDetailLight);
        tvDetailNotes  = findViewById(R.id.tvDetailNotes);
        cardNotes      = findViewById(R.id.cardNotes);
        btnWaterNow    = findViewById(R.id.btnWaterNow);
    }

    private void loadPlant() {
        db.collection("users").document(uid)
                .collection("plants").document(plantId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    // Название
                    String name = doc.getString("name");
                    tvDetailName.setText(name != null ? name : "—");

                    // Горшок
                    String pot = doc.getString("pot");
                    tvDetailPot.setText(pot != null ? "🪴 " + pot : "");

                    // Комната
                    String room = doc.getString("room");
                    tvDetailRoom.setText(room != null ? "📍 " + room : "");

                    // Полив
                    Long nextWater = doc.getLong("nextWater");
                    Long waterDays = doc.getLong("waterDays");
                    if (waterDays != null) currentWaterDays = waterDays;

                    tvDetailWater.setText(buildWaterLabel(nextWater, currentWaterDays));

                    // Состояние кнопки полива
                    if (nextWater != null && nextWater > System.currentTimeMillis()) {
                        // Уже полито - следующий полив в будущем
                        btnWaterNow.setText("✅ Уже полито");
                        btnWaterNow.setEnabled(false);
                    } else {
                        // Нужен полив
                        btnWaterNow.setText("💧 Полить сейчас");
                        btnWaterNow.setEnabled(true);
                    }

                    // Освещение
                    String light = doc.getString("light");
                    tvDetailLight.setText(light != null ? light : "—");

                    // Заметки
                    String notes = doc.getString("notes");
                    if (notes != null && !notes.isEmpty()) {
                        tvDetailNotes.setText(notes);
                        cardNotes.setVisibility(View.VISIBLE);
                    }

                    // Фото (base64)
                    String photoBase64 = doc.getString("photoBase64");
                    if (photoBase64 != null && !photoBase64.isEmpty()) {
                        try {
                            byte[] bytes = Base64.decode(photoBase64, Base64.DEFAULT);
                            Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            ivDetailPhoto.setImageBitmap(bmp);
                            layoutNoPhoto.setVisibility(View.GONE);
                        } catch (Exception e) {
                            layoutNoPhoto.setVisibility(View.VISIBLE);
                        }
                    } else {
                        layoutNoPhoto.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> finish());
    }

    private String buildWaterLabel(Long nextWaterMs, Long waterDays) {
        if (nextWaterMs == null) return "—";
        long diffMs   = nextWaterMs - System.currentTimeMillis();
        long diffDays = diffMs / (1000 * 60 * 60 * 24);

        String schedule = waterDays != null ? " (раз в " + waterDays + " дн.)" : "";

        if (diffMs <= 0)      return "Сегодня" + schedule;
        if (diffDays == 0)    return "Завтра" + schedule;
        if (diffDays == 1)    return "Через 1 день" + schedule;
        return "Через " + diffDays + " дн." + schedule;
    }

    private void waterPlant() {
        btnWaterNow.setEnabled(false);
        btnWaterNow.setText("Сохраняем…");

        long nextWaterMs = System.currentTimeMillis() + currentWaterDays * 24 * 60 * 60 * 1000L;

        Map<String, Object> upd = new HashMap<>();
        upd.put("nextWater",  nextWaterMs);
        upd.put("waterLabel", "Полить через " + currentWaterDays + " дн.");

        db.collection("users").document(uid)
                .collection("plants").document(plantId)
                .update(upd)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "💧 Полито!", Toast.LENGTH_SHORT).show();
                    tvDetailWater.setText(buildWaterLabel(nextWaterMs, currentWaterDays));
                    btnWaterNow.setText("✅ Уже полито");
                    // кнопка остаётся disabled - полив только что сделали
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnWaterNow.setEnabled(true);
                    btnWaterNow.setText("💧 Полить сейчас");
                });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить растение?")
                .setMessage("Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (d, w) -> deletePlant())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deletePlant() {
        db.collection("users").document(uid)
                .collection("plants").document(plantId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Растение удалено", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
    }
}
