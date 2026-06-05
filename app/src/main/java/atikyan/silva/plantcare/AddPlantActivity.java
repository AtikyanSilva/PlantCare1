package atikyan.silva.plantcare;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

public class AddPlantActivity extends AppCompatActivity {

    private TextInputEditText etPlantName;
    private TextInputEditText etNotes;
    private ImageView ivPhotoPreview;
    private MaterialButton btnAddToGarden;
    private ChipGroup chipGroupRoom;
    private ChipGroup chipGroupWater;
    private ChipGroup chipGroupLight;

    private LinearLayout potTerracotta, potLavender, potMint;
    private String selectedPot = "Терракота";

    private FirebaseFirestore db;
    private String uid;

    private Bitmap capturedPhoto = null;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null) {
                                capturedPhoto = (Bitmap) extras.get("data");
                                ivPhotoPreview.setImageBitmap(capturedPhoto);
                                ivPhotoPreview.setVisibility(View.VISIBLE);
                                findViewById(R.id.layoutCameraBtn).setVisibility(View.GONE);
                            }
                        }
                    });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            openCamera();
                        } else {
                            Toast.makeText(this,
                                    "Нужно разрешение для камеры", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        uid = currentUser.getUid();
        db  = FirebaseFirestore.getInstance();

        bindViews();
        setupPotSelection();
        setupButtons();

        String photoBase64 = getIntent().getStringExtra("photo_base64");
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            try {
                byte[] bytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT);
                capturedPhoto = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (capturedPhoto != null) {
                    ivPhotoPreview.setImageBitmap(capturedPhoto);
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    findViewById(R.id.layoutCameraBtn).setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void bindViews() {
        etPlantName    = findViewById(R.id.etPlantName);
        etNotes        = findViewById(R.id.etNotes);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        btnAddToGarden = findViewById(R.id.btnAddToGarden);
        chipGroupRoom  = findViewById(R.id.chipGroupRoom);
        chipGroupWater = findViewById(R.id.chipGroupWater);
        chipGroupLight = findViewById(R.id.chipGroupLight);
        potTerracotta  = findViewById(R.id.potTerracotta);
        potLavender    = findViewById(R.id.potLavender);
        potMint        = findViewById(R.id.potMint);
    }

    private void setupPotSelection() {
        potTerracotta.setOnClickListener(v -> selectPot("Терракота", potTerracotta));
        potLavender.setOnClickListener(v -> selectPot("Лавандовый", potLavender));
        potMint.setOnClickListener(v -> selectPot("Мята", potMint));
        highlightPot(potTerracotta);
    }

    private void selectPot(String potName, LinearLayout selected) {
        selectedPot = potName;
        highlightPot(selected);
    }

    private void highlightPot(LinearLayout selected) {
        LinearLayout[] pots = {potTerracotta, potLavender, potMint};
        for (LinearLayout pot : pots) {
            boolean isActive = (pot == selected);
            pot.setBackground(ContextCompat.getDrawable(this,
                    isActive ? R.drawable.btn_pot_selected : R.drawable.btn_pot_default));
            TextView label = (TextView) pot.getChildAt(pot.getChildCount() - 1);
            if (label != null) {
                label.setTextColor(ContextCompat.getColor(this,
                        isActive ? R.color.dark_green : R.color.text_secondary));
                label.setTypeface(null, isActive
                        ? android.graphics.Typeface.BOLD
                        : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void setupButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View cameraBlock = findViewById(R.id.layoutCameraBtn);
        cameraBlock.setOnClickListener(v -> checkCameraPermissionAndOpen());

        ivPhotoPreview.setOnClickListener(v -> checkCameraPermissionAndOpen());

        btnAddToGarden.setOnClickListener(v -> savePlantToFirestore());
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void savePlantToFirestore() {
        String name = etPlantName.getText() != null
                ? etPlantName.getText().toString().trim()
                : "";

        if (name.isEmpty()) {
            etPlantName.setError("Введите название растения");
            etPlantName.requestFocus();
            return;
        }

        String room = getSelectedRoom();
        int waterDays = getSelectedWaterDays();
        long nextWaterMs = System.currentTimeMillis() + (long) waterDays * 24 * 60 * 60 * 1000;
        String light = getSelectedLight();
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        btnAddToGarden.setEnabled(false);
        btnAddToGarden.setText("Сохраняем…");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        final String finalName = name;
        executor.execute(() -> {
            String photoBase64 = "";
            if (capturedPhoto != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Bitmap scaled = Bitmap.createScaledBitmap(capturedPhoto, 400, 400, true);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    photoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final String encodedPhoto = photoBase64;

            mainHandler.post(() -> {
                Map<String, Object> plant = new HashMap<>();
                plant.put("name",       finalName);
                plant.put("pot",        selectedPot);
                plant.put("room",       room);
                plant.put("nextWater",  nextWaterMs);
                plant.put("waterDays",  waterDays);
                plant.put("waterLabel", "Полить через " + waterDays + " дн.");
                plant.put("light",      light);
                plant.put("notes",      notes);
                plant.put("imageUrl",   "");
                plant.put("photoBase64", encodedPhoto);

                db.collection("users")
                        .document(uid)
                        .collection("plants")
                        .add(plant)
                        .addOnSuccessListener(ref -> {
                            btnAddToGarden.setText("Сохранено ✓");
                            Toast.makeText(this,
                                    "🌱 " + finalName + " добавлен в сад!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            btnAddToGarden.setEnabled(true);
                            btnAddToGarden.setText("Добавить в сад 🌿");
                            Toast.makeText(this,
                                    "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        });
        executor.shutdown();
    }

    private String getSelectedRoom() {
        int checkedId = chipGroupRoom.getCheckedChipId();
        if (checkedId == R.id.chipBedroom) return "Спальня";
        if (checkedId == R.id.chipKitchen) return "Кухня";
        if (checkedId == R.id.chipBalcony) return "Балкон";
        return "Гостиная";
    }

    private int getSelectedWaterDays() {
        int checkedId = chipGroupWater.getCheckedChipId();
        if (checkedId == R.id.chipWater2)  return 2;
        if (checkedId == R.id.chipWater14) return 14;
        if (checkedId == R.id.chipWater30) return 30;
        return 7;
    }

    private String getSelectedLight() {
        int checkedId = chipGroupLight.getCheckedChipId();
        if (checkedId == R.id.chipLightDirect) return "Прямой свет";
        if (checkedId == R.id.chipLightShade)  return "Тень";
        return "Рассеянный";
    }
}
