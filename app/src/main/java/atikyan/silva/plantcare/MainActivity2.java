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

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity2 extends AppCompatActivity {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Bitmap lastSelectedBitmap;
    private FloatingActionButton fabCamera;
    private String currentMode = "detect";
    private String dailyAdviceFullText = "";

    private final String API_KEY_ADVICE = "AIzaSyByyHps3noOZ4deOeftSWRLrsclJ1E0aZY";
    private final String API_KEY_DETECT = "AIzaSyCUriqLyYW-F1mQsRMCRT5bVije4-Q0ui0";
    private final String API_KEY_DIAGNOSE = "AIzaSyALZVUn8bNbNBC1d4vDOZjwdzm4fgx9ZLY";
    private final String API_KEY_SEARCH = "AIzaSyAj88PTL-H6QHc4b4r5VbwnTp8sPog3XVM";
    private static final int CAMERA_PERMISSION_CODE = 101;
    private ProgressDialog progressDialog;
    private void showLoading(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false); // Чтобы пользователь случайно не закрыл окно
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

        fabCamera = findViewById(R.id.fabCamera);
        CardView cardDetect = findViewById(R.id.cardDetect);
        CardView cardDiagnose = findViewById(R.id.cardDiagnose);
        CardView cardAdvice = findViewById(R.id.cardAdvice);

        // Загружаем наш крутой совет с пользой
        loadDailyAdvice();

        cardAdvice.setOnClickListener(v -> {
            if (!dailyAdviceFullText.isEmpty()) {
                showResultSheet(dailyAdviceFullText, false);
            } else {
                Toast.makeText(this, "Идея еще подготавливается...", Toast.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                if (imageBitmap != null) {
                    lastSelectedBitmap = imageBitmap;
                    sendImageToAI(imageBitmap);
                }
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    lastSelectedBitmap = bitmap;
                    sendImageToAI(bitmap);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        EditText searchBar = findViewById(R.id.searchBar);

        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            // Добавляем проверку на ACTION_DONE и null event (для некоторых клавиатур)
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {

                String query = searchBar.getText().toString().trim();
                if (!query.isEmpty()) {
                    askAiQuestion(query); // Твой метод для ИИ-поиска
                    searchBar.setText(""); // Очищаем строку
                }
                return true;
            }
            return false;
        });
        fabCamera.setOnClickListener(v -> checkPermissionAndProceed());

        cardDetect.setOnClickListener(v -> {
            currentMode = "detect";
            checkPermissionAndProceed();
        });

        cardDiagnose.setOnClickListener(v -> {
            currentMode = "diagnose";
            checkPermissionAndProceed();
        });
    }

    private void loadDailyAdvice() {
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY_ADVICE); //2.5 для совета дня
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Новый промпт: просим пользу и результат
        String prompt = "Предложи одну необычную идею для домашнего мини-огорода (например, имбирь, микрозелень или авокадо). " +
                "Формат ответа СТРОГО через символ '|': " +
                "Название идеи | Как вырастить (2 предложения) | Какая польза и результат (1 предложение). " +
                "Пиши только по делу, без лишних слов в конце. На русском.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String res = result.getText();
                runOnUiThread(() -> {
                    if (res != null && res.contains("|")) {
                        String cleanRes = res.replace("*", "").replace("\"", "");
                        String[] parts = cleanRes.split("\\|");

                        TextView tvTitle = findViewById(R.id.tvAdviceTitle);
                        if (tvTitle != null && parts.length >= 1) {
                            tvTitle.setText(parts[0].trim());
                        }

                        // Собираем текст: Инструкция + Польза
                        StringBuilder fullBody = new StringBuilder();
                        if (parts.length >= 2) {
                            fullBody.append(parts[1].trim()).append("\n\n");
                        }
                        if (parts.length >= 3) {
                            fullBody.append("✨ Полезные свойства: ").append(parts[2].trim());
                        }
                        dailyAdviceFullText = fullBody.toString();
                    }
                });
            }
            @Override public void onFailure(Throwable t) { t.printStackTrace(); }
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

    private void sendImageToAI(Bitmap bitmap) {
        String prompt;
        String selectedApiKey;

        // Оставляем твои промпты без изменений, меняем только ключи
        if (currentMode.equals("detect")) {
            prompt = "Инструкция: Если на фото растение, начни ответ со слова РАСТЕНИЕ. Если на фото НЕ растение, со фразы Not plant. " +
                    "Далее: если растение, напиши название и краткое описание. На русском языке.";
            selectedApiKey = API_KEY_DETECT;
        } else {
            prompt = "Инструкция: Если на фото растение, начни ответ со слова РАСТЕНИЕ. Если на фото НЕ растение, начни со фразы Not plant. " +
                    "Далее: если растение, проведи диагностику болезней и дай советы по лечению. На русском языке.";
            selectedApiKey = API_KEY_DIAGNOSE;

        }
        showLoading("Анализирую фото... 🌱");
        // Используем выбранный ключ и модель 1.5-flash для стабильности
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", selectedApiKey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true);
        Content content = new Content.Builder().addImage(scaled).addText(prompt).build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    // 2. Скрываем загрузку, когда пришел ответ
                    hideLoading();
                    showResultSheet(result.getText(), true);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    // 3. Скрываем загрузку, если что-то пошло не так
                    hideLoading();
                    Toast.makeText(MainActivity2.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, executor);
    }

    @SuppressLint("MissingInflatedId")
    private void showResultSheet(String text, boolean isPhotoResult) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // 1. ПРОВЕРКА
        String upperText = (text != null) ? text.toUpperCase().trim() : "";
        // Проверяем на "РАСТЕНИЕ", а всё остальное (включая "NOT PLANT") считаем не растением
        boolean isPlant = upperText.startsWith("РАСТЕНИЕ");

        // 2. ОЧИСТКА ТЕКСТА
        // Добавляем (?i)Not plant для удаления английской метки
        String cleanText = (text != null) ?
                text.replaceFirst("(?i)РАСТЕНИЕ", "")
                        .replaceFirst("(?i)Not plant", "На фото нету растение")
                        .trim() : "";

        // Очистка от мусора в начале (точки, двоеточия, тире)
        // Цикл while поможет, если там вдруг и точка, и пробел одновременно
        while (cleanText.startsWith(".") || cleanText.startsWith(":") || cleanText.startsWith("-") || cleanText.startsWith(" ")) {
            cleanText = cleanText.substring(1).trim();
        }

        // 3. ВЫБИРАЕМ МАКЕТ
        int layoutId;
        if (isPhotoResult) {
            layoutId = isPlant ? R.layout.layout_bottom_sheet : R.layout.layout_bottom_sheet_no_btn;
        } else {
            layoutId = R.layout.layout_bottom_sheet_text;
        }

        View sheetView = getLayoutInflater().inflate(layoutId, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvContent = sheetView.findViewById(R.id.tvSheetContent);

        if (isPhotoResult) {
            tvTitle.setText(currentMode.equals("detect") ? "Распознавание" : "Диагностика");

            ImageView ivResult = sheetView.findViewById(R.id.ivPlantResult);
            Button btnAdd = sheetView.findViewById(R.id.btnAddToGarden);

            if (ivResult != null && lastSelectedBitmap != null) {
                ivResult.setImageBitmap(lastSelectedBitmap);
            }

            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> {
                    Toast.makeText(this, "Сохранено в Мой сад! 🌱", Toast.LENGTH_SHORT).show();
                    bottomSheetDialog.dismiss();
                });
            }
        } else {
            if (text != null && text.equals(dailyAdviceFullText)) {
                tvTitle.setText("Идея дня");
            } else {
                tvTitle.setText("Ответ ботаника");
            }
        }

        tvContent.setText(cleanText);
        bottomSheetDialog.show();
    }
    private void askAiQuestion(String userText) {
        Toast.makeText(this, "Бот-ботаник ищет ответ...", Toast.LENGTH_SHORT).show();

        // Используем отдельный ключ для поиска
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY_SEARCH);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Промпт для поиска (можно не менять, он универсальный)
        String prompt = "Ты эксперт по растениям. Ответь на вопрос: " + userText +
                ". Отвечай кратко, понятно и на русском языке.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    // Показываем ответ в том же Sheet, но без фото (isPhotoResult = false)
                    showResultSheet(result.getText(), false);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Ошибка поиска: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }
}