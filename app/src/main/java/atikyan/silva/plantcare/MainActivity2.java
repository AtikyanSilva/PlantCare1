package atikyan.silva.plantcare;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity2 extends AppCompatActivity {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Bitmap lastSelectedBitmap;
    private FloatingActionButton fabCamera;
    private String currentMode = "detect";
    private String dailyAdviceFullText = "";
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final String API_KEY_ADVICE = "AIzaSyAB6IOufxggN3RURxPeTqzXBKtjN5syCr4";
    private final String API_KEY_DETECT = "AIzaSyBCf1vy5Yms_E6wwZHbHMtr6lkNcgwsxSA";
    private final String API_KEY_DIAGNOSE = "AIzaSyCdMCNVG067Myvo9YmvpeyhHAg0gCzvC6I";
    private final String API_KEY_SEARCH = "AIzaSyAVzghKKbbBvnuhJpW60CviGg1HaaBB_Ak";
    private static final int CAMERA_PERMISSION_CODE = 101;
    private ProgressDialog progressDialog;

    private void showLoading(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void checkPermissionAndProceed() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showSourceSelectionDialog();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        // Кнопка выхода
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity2.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // 1. Инициализация списка проблем (с полными текстами)
        RecyclerView rvCommonProblems = findViewById(R.id.rvCommonProblems);
        List<Problem> problemList = new ArrayList<>();

        problemList.add(new Problem("Желтеют листья",
                "Листья теряют зелёный цвет и становятся жёлтыми.\nПричина чаще всего — перелив. Также это может быть недостаток азота или смена условий.",
                "Проверь влажность почвы. Если мокрая — сократи полив. Если давно не удобряли — внеси азот.", R.drawable.yellow_leaves));

        problemList.add(new Problem("Сухие кончики",
                "Кончики листьев подсыхают и темнеют.\nПричина — сухой воздух или жёсткая вода.",
                "Повышай влажность воздуха, опрыскивай растение. Используй отстоянную воду.", R.drawable.brown_tips));

        problemList.add(new Problem("Растение не растёт",
                "Нет новых листьев и побегов.\nПричина — недостаток света, питания или тесный горшок.",
                "Перемести в более светлое место, подкорми или пересади.", R.drawable.no_growth));

        problemList.add(new Problem("Вялые листья",
                "Листья становятся мягкими и опускаются.\nПричина — пересушенная почва или гниение корней.",
                "Если сухая — полей. Если мокрая — проверь корни на гниль.", R.drawable.wilted_leaves));

        problemList.add(new Problem("Опадают листья",
                "Листья массово опадают.\nПричина — стресс, сквозняки или перепады температуры.",
                "Избегай сквозняков, стабилизируй температуру.", R.drawable.falling_leaves));

        problemList.add(new Problem("Пятна на листьях",
                "На листьях появляются пятна.\nПричина — грибок или солнечные ожоги.",
                "Удали поражённые листья. Не опрыскивай под прямым солнцем.", R.drawable.leaf_spots));

        problemList.add(new Problem("Плесень в горшке",
                "На почве белый налёт.\nПричина — переувлажнение и плохая вентиляция.",
                "Замени верхний слой земли, уменьши полив.", R.drawable.mold_soil));

        problemList.add(new Problem("Вредители",
                "Насекомые, липкий налёт или паутина.\nПричина — заражение.",
                "Изолируй растение, промой мыльным раствором или спецсредством.", R.drawable.pests));

        problemList.add(new Problem("Ожоги от солнца",
                "Светлые сухие пятна.\nПричина — прямые лучи солнца.",
                "Переставь в место с рассеянным светом.", R.drawable.sunburn));

        problemList.add(new Problem("Недостаток света",
                "Стебли вытянуты, листья бледные.\nПричина — слишком темно.",
                "Перемести ближе к окну или используй фитолампу.", R.drawable.low_light));

        problemList.add(new Problem("Переизбыток воды",
                "Почва мокрая, листья желтеют.\nПричина — нет дренажа.",
                "Сократи полив, проверь отверстия в горшке.", R.drawable.overwatering));

        problemList.add(new Problem("Недостаток воды",
                "Почва сухая, листья скручены.\nПричина — редкий полив.",
                "Полей растение, установи график полива.", R.drawable.underwatering));

        problemList.add(new Problem("Скручивание листьев",
                "Листья сворачиваются.\nПричина — сухой воздух или вредители.",
                "Проверь влажность и наличие насекомых.", R.drawable.curling_leaves));

        rvCommonProblems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCommonProblems.setNestedScrollingEnabled(false);
        ProblemAdapter adapter = new ProblemAdapter(problemList);
        rvCommonProblems.setAdapter(adapter);

        // Настройка отступов (чтобы навигация была внизу)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom); // Вот эта строка
            return insets;
        });

        // Настройка BottomAppBar и BottomNavigationView

        fabCamera = findViewById(R.id.fabCamera);


        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navInstruments = findViewById(R.id.navInstruments);
        LinearLayout navBotanist = findViewById(R.id.navBotanist);
        LinearLayout navGarden = findViewById(R.id.navGarden);


        navInstruments.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity2.this, MainActivity3.class));
        });


        navBotanist.setOnClickListener(v -> {
            // Здесь можно открыть чат или вызвать функцию ИИ
            startActivity(new Intent(MainActivity2.this, AiBotanistActivity.class));
        });

        navGarden.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity2.this, MyGardenActivity.class)); // Логика для раздела Сад
        });

        fabCamera.setOnClickListener(v -> checkPermissionAndProceed());

        // Карточки ИИ
        CardView cardDetect = findViewById(R.id.cardDetect);
        CardView cardDiagnose = findViewById(R.id.cardDiagnose);
        CardView cardAdvice = findViewById(R.id.cardAdvice);

        loadDailyAdvice();

        cardAdvice.setOnClickListener(v -> {
            if (!dailyAdviceFullText.isEmpty()) {
                showResultSheet(dailyAdviceFullText, false);
            } else {
                Toast.makeText(this, "Идея еще подготавливается...", Toast.LENGTH_SHORT).show();
            }
        });

        cardDetect.setOnClickListener(v -> {
            currentMode = "detect";
            checkPermissionAndProceed();
        });

        cardDiagnose.setOnClickListener(v -> {
            currentMode = "diagnose";
            checkPermissionAndProceed();
        });

        // Лаунчеры фото
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                if (imageBitmap != null) {
                    lastSelectedBitmap = imageBitmap;
                    sendImageToAI(imageBitmap, 0);
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    lastSelectedBitmap = bitmap;
                    sendImageToAI(bitmap, 0);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        // Поиск
        EditText searchBar = findViewById(R.id.searchBar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                String query = searchBar.getText().toString().trim();
                if (!query.isEmpty()) {
                    askAiQuestion(query);
                    searchBar.setText("");
                }
                return true;
            }
            return false;
        });
    }

    // --- Методы ИИ (Gemini 2.5 Flash) ---

    private void loadDailyAdvice() {
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY_ADVICE);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        String prompt = "Предложи одну необычную идею для домашнего мини-огорода. Формат через '|': Название | Инструкция | Польза.";
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String res = result.getText();
                runOnUiThread(() -> {
                    if (res != null && res.contains("|")) {
                        String[] parts = res.replace("*", "").split("\\|");
                        TextView tvTitle = findViewById(R.id.tvAdviceTitle);
                        if (tvTitle != null && parts.length >= 1) tvTitle.setText(parts[0].trim());
                        dailyAdviceFullText = res;
                    }
                });
            }
            @Override public void onFailure(Throwable t) { t.printStackTrace(); }
        }, executor);
    }

    private void askAiQuestion(String userText) {
        Toast.makeText(this, "Бот-ботаник ищет ответ...", Toast.LENGTH_SHORT).show();
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY_SEARCH);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText("Ты эксперт по растениям. Ответь: " + userText).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> showResultSheet(result.getText(), false));
            }
            @Override public void onFailure(Throwable t) { t.printStackTrace(); }
        }, executor);
    }

    private void sendImageToAI(Bitmap bitmap, int attempt) {
        showLoading("Анализирую фото... 🌱");
        String prompt = currentMode.equals("detect") ? "РАСТЕНИЕ: название и описание" : "РАСТЕНИЕ: диагностика и лечение";
        String key = currentMode.equals("detect") ? API_KEY_DETECT : API_KEY_DIAGNOSE;

        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", key);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
        Content content = new Content.Builder().addImage(scaled).addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> { hideLoading(); showResultSheet(result.getText(), true); });
            }
            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    hideLoading();
                    if (t.getMessage() != null && t.getMessage().contains("503") && attempt < 2) {
                        sendImageToAI(bitmap, attempt + 1);
                    }
                });
            }
        }, executor);
    }

    private void showSourceSelectionDialog() {
        BottomSheetDialog sourceDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_select_source, null);
        view.findViewById(R.id.btnSourceCamera).setOnClickListener(v -> { openCamera(); sourceDialog.dismiss(); });
        view.findViewById(R.id.btnSourceGallery).setOnClickListener(v -> { galleryLauncher.launch("image/*"); sourceDialog.dismiss(); });
        sourceDialog.setContentView(view);
        sourceDialog.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) cameraLauncher.launch(takePictureIntent);
    }

    @SuppressLint("MissingInflatedId")
    private void showResultSheet(String text, boolean isPhotoResult) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        int layoutId = isPhotoResult ? R.layout.layout_bottom_sheet : R.layout.layout_bottom_sheet_text;
        View sheetView = getLayoutInflater().inflate(layoutId, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvContent = sheetView.findViewById(R.id.tvSheetContent);

        tvTitle.setText(isPhotoResult ? (currentMode.equals("detect") ? "Распознавание" : "Диагностика") : "Ответ ботаника");
        tvContent.setText(text != null ? text.replace("РАСТЕНИЕ", "").trim() : "");

        if (isPhotoResult) {
            ImageView ivResult = sheetView.findViewById(R.id.ivPlantResult);
            if (ivResult != null && lastSelectedBitmap != null) ivResult.setImageBitmap(lastSelectedBitmap);
        }
        bottomSheetDialog.show();
    }
}
