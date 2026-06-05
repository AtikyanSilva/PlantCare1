package atikyan.silva.plantcare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyGardenActivity extends AppCompatActivity {

    private static final int REQUEST_PROFILE = 101;

    // ── Цитаты ──────────────────────────────────────────────────
    private static final String[] QUOTES = {
        "Растения — это молчаливые друзья, которые всегда рады тебе 🌿",
        "Забота о растении — это забота о себе 🌱",
        "Каждый новый листок — маленькая победа 🍃",
        "Твой сад растёт вместе с тобой ✨",
        "Вода, свет и любовь — всё, что нужно для роста 💧",
        "Зелень дома — покой в душе 🌾",
        "Даже маленький кактус нуждается в твоём внимании 🌵",
        "Природа не торопится — и всё же успевает всё 🌍",
        "Посади семя добра — и оно обязательно прорастёт 🌻",
        "Твои растения чувствуют твою любовь 💚",
        "Каждый день в саду — это день хорошо прожитый 🌸",
        "Не бойся обрезать — так растут сильнее 🌿",
        "Монстера тянется к свету — и ты тянись к мечте 🌟",
        "Лучшее время посадить дерево — сегодня 🌳",
        "Зелёный уголок дома — твоя личная терапия 🍀"
    };

    // ── UI ──────────────────────────────────────────────────────
    private TextView tvGardenUserName;
    private TextView tvGardenStatus;
    private TextView tvGardenQuote;
    private ImageView tvGardenEmoji;
    private RecyclerView rvMyPlants;
    private FloatingActionButton fabAddPlant;

    // ── Firebase ────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    // ── Данные ──────────────────────────────────────────────────
    private final List<PlantModel> plantsList = new ArrayList<>();
    private MyPlantsAdapter plantsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_garden);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        uid = currentUser.getUid();

        bindViews();
        setupAvatarClick();
        loadUserAvatar();
        setRandomQuote();

        tvGardenUserName.setText("Мой сад");

        plantsAdapter = new MyPlantsAdapter(plantsList, this);
        rvMyPlants.setLayoutManager(new GridLayoutManager(this, 2));
        rvMyPlants.setAdapter(plantsAdapter);

        loadPlantsFromFirestore();
        setupNavigation();

        fabAddPlant.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddPlantActivity.class);
            startActivity(intent);
        });
    }

    private void bindViews() {
        tvGardenUserName = findViewById(R.id.tvGardenUserName);
        tvGardenStatus   = findViewById(R.id.tvGardenStatus);
        tvGardenQuote    = findViewById(R.id.tvGardenQuote);
        tvGardenEmoji    = findViewById(R.id.tvGardenEmoji);
        rvMyPlants       = findViewById(R.id.rvMyPlants);
        fabAddPlant      = findViewById(R.id.fabAddPlant);
    }

    // ── Случайная цитата ─────────────────────────────────────────
    private void setRandomQuote() {
        int idx = new Random().nextInt(QUOTES.length);
        tvGardenQuote.setText(QUOTES[idx]);
    }

    // ── Загрузка аватара из Firestore ────────────────────────────
    private void loadUserAvatar() {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String avatar = doc.getString("avatar");
                        if (avatar != null && !avatar.isEmpty()) {
                            int resId = getResources().getIdentifier(
                                    avatar, "drawable", getPackageName());
                            if (resId != 0) tvGardenEmoji.setImageResource(resId);
                        }
                    }
                });
    }

    // ── Клик на аватарку → ProfileActivity ───────────────────────
    private void setupAvatarClick() {
        tvGardenEmoji.setClickable(true);
        tvGardenEmoji.setFocusable(true);
        tvGardenEmoji.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivityForResult(intent, REQUEST_PROFILE);
        });
    }

    // ── Обновляем аватар при возврате из ProfileActivity ─────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PROFILE && resultCode == Activity.RESULT_OK) {
            loadUserAvatar();
        }
    }

    private void loadPlantsFromFirestore() {
        long todayMs = System.currentTimeMillis();

        db.collection("users")
                .document(uid)
                .collection("plants")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    plantsList.clear();

                    int countWater = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PlantModel plant = doc.toObject(PlantModel.class);
                        plant.setDocId(doc.getId());
                        plantsList.add(plant);

                        Long nextWater = doc.getLong("nextWater");
                        if (nextWater != null && nextWater <= todayMs) {
                            countWater++;
                        }
                    }

                    int totalPlants = plantsList.size();
                    String statusText = totalPlants + " растени" + pluralRu(totalPlants, "е", "я", "й");
                    if (countWater == 0) {
                        statusText += " · всё хорошо 🌿";
                    } else {
                        statusText += " · нужен уход 💧";
                    }
                    tvGardenStatus.setText(statusText);

                    plantsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    tvGardenStatus.setText("Ошибка загрузки");
                });
    }

    private void setupNavigation() {
        LinearLayout navHome        = findViewById(R.id.navHome);
        LinearLayout navInstruments = findViewById(R.id.navInstruments);
        LinearLayout navBotanist    = findViewById(R.id.navBotanist);

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity2.class));
            finish();
        });

        navInstruments.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity3.class));
        });

        navBotanist.setOnClickListener(v -> {
            startActivity(new Intent(this, AiBotanistActivity.class));
        });
    }

    private String pluralRu(int n, String one, String few, String many) {
        int mod10  = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11)                            return one;
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return few;
        return many;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRandomQuote();
        if (uid != null) {
            loadPlantsFromFirestore();
        }
    }
}
