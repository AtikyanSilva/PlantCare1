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
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;

import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private Bitmap lastSelectedBitmap;
    private FloatingActionButton fabCamera;
    private String currentMode = "detect";
    private String dailyAdviceFullText = "";
    private RecyclerView rvCommonProblems;
    private ProblemAdapter problemsAdapter;
    private List<Problem> problemList;

    private final String API_KEY_ADVICE = "AIzaSyDDxeg-Li3a_Quu5_mo2qHIJWuH3z5v0O0";
    private final String API_KEY_DETECT = "AIzaSyALhf9D9edQaaq73vJgI1LLuYWVgQ9FvHQ";
    private final String API_KEY_DIAGNOSE = "AIzaSyCQHKNY5hU-La6UboOHSp0mf_si1OdKSJE";
    private final String API_KEY_SEARCH = "AIzaSyDk9erjPmsf9iBcynB2WTrgSScRmV060xQ";
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
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // 1. Разлогиниваемся в Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. Возвращаемся на первый экран (MainActivity)
            Intent intent = new Intent(MainActivity2.this, MainActivity.class);
            startActivity(intent);

            // 3. Закрываем текущий экран
            finish();
        });

        // 1. Инициализация списка проблем
        RecyclerView rvCommonProblems = findViewById(R.id.rvCommonProblems);
        List<Problem> problemList = new ArrayList<>();

        // Добавляй карточки здесь (каждая карточка — это новая строка .add)
        problemList.add(new Problem(
                "Желтеют листья",
                "Листья теряют зелёный цвет и становятся жёлтыми.\n" +
                        "Причина чаще всего — перелив (корни не получают кислород). Также это может быть недостаток азота, естественное старение нижних листьев или резкая смена условий.",
                "Проверь влажность почвы. Если она мокрая — сократи полив и проверь дренаж.\n" +
                        "Если давно не удобряли — внеси комплексное удобрение.\n" +
                        "Удаляй старые нижние листья.",
                R.drawable.yellow_leaves // Проверь, что эта картинка есть в drawable
        ));
        problemList.add(new Problem(
                "Сухие и коричневые кончики",
                "Кончики листьев подсыхают и темнеют.\n" +
                        "Причина — сухой воздух, жёсткая вода или избыток удобрений (накопление солей в почве).",
                "Повышай влажность воздуха.\n" +
                        "Используй отстоянную или фильтрованную воду.\n" +
                        "Промой почву чистой водой при переизбытке удобрений.",
                R.drawable.brown_tips
        ));
        problemList.add(new Problem(
                "Растение не растёт",
                "Нет новых листьев и побегов.\n" +
                        "Причина — недостаток света, питания, тесный горшок или период покоя.",
                "Перемести растение в более светлое место.\n" +
                        "Подкорми удобрением в сезон роста.\n" +
                        "При необходимости пересади в больший горшок.",
                R.drawable.no_growth
        ));
        problemList.add(new Problem(
                "Вялые листья",
                "Листья становятся мягкими и опускаются.\n" +
                        "Причина — пересушенная почва или гниение корней из-за перелива.",
                "Проверь почву.\n" +
                        "Если сухая — полей.\n" +
                        "Если слишком влажная — сократи полив и проверь корни.",
                R.drawable.wilted_leaves
        ));
        problemList.add(new Problem(
                "Опадают листья",
                "Листья массово опадают.\n" +
                        "Причина — стресс, сквозняки, перепады температуры или неправильный полив.",
                "Избегай резких изменений условий.\n" +
                        "Стабилизируй температуру.\n" +
                        "Отрегулируй режим полива.",
                R.drawable.falling_leaves
        ));
        problemList.add(new Problem(
                "Пятна на листьях",
                "На листьях появляются жёлтые, коричневые или чёрные пятна.\n" +
                        "Причина — грибковая инфекция, ожоги от солнца или попадание воды при ярком свете.",
                "Удаляй поражённые листья.\n" +
                        "Не опрыскивай на солнце.\n" +
                        "При необходимости обработай фунгицидом.",
                R.drawable.leaf_spots
        ));
        problemList.add(new Problem(
                "Плесень в горшке",
                "На поверхности почвы появляется белый или серый налёт.\n" +
                        "Причина — переувлажнение и плохая вентиляция.",
                "Уменьши полив.\n" +
                        "Замени верхний слой грунта.\n" +
                        "Обеспечь доступ воздуха.",
                R.drawable.mold_soil
        ));
        problemList.add(new Problem(
                "Вредители",
                "На листьях появляются насекомые, липкий налёт или паутина.\n" +
                        "Причина — заражение от других растений или ослабленный иммунитет.",
                "Изолируй растение.\n" +
                        "Промой листья мыльным раствором.\n" +
                        "Используй средство от вредителей.",
                R.drawable.pests
        ));
        problemList.add(new Problem(
                "Ожоги от солнца",
                "На листьях появляются светлые сухие пятна.\n" +
                        "Причина — прямые солнечные лучи.",
                "Переставь растение в место с рассеянным светом.\n" +
                        "Не ставь тенелюбивые растения под прямое солнце.",
                R.drawable.sunburn
        ));
        problemList.add(new Problem(
                "Недостаток света",
                "Стебли вытягиваются, листья становятся бледными.\n" +
                        "Причина — слишком тёмное место.",
                "Перемести ближе к окну.\n" +
                        "Используй фитолампу при необходимости.",
                R.drawable.low_light
        ));
        problemList.add(new Problem(
                "Переизбыток воды",
                "Почва постоянно влажная, листья желтеют.\n" +
                        "Причина — слишком частый полив и отсутствие дренажа.",
                "Сократи полив.\n" +
                        "Проверь дренажные отверстия.\n" +
                        "Дай почве просохнуть.",
                R.drawable.overwatering
        ));
        problemList.add(new Problem(
                "Недостаток воды",
                "Почва сухая, листья скручиваются и сохнут.\n" +
                        "Причина — редкий или нерегулярный полив.",
                "Полей растение равномерно.\n" +
                        "Установи регулярный график полива.",
                R.drawable.underwatering
        ));
        problemList.add(new Problem(
                "Скручивание листьев",
                "Листья сворачиваются внутрь или наружу.\n" +
                        "Причина — сухой воздух, неправильный полив или вредители.",
                "Проверь влажность воздуха.\n" +
                        "Отрегулируй полив.\n" +
                        "Осмотри растение на наличие вредителей.",
                R.drawable.curling_leaves
        ));

        rvCommonProblems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Отключаем вложенный скролл, чтобы NestedScrollView работал правильно
        rvCommonProblems.setNestedScrollingEnabled(false);

        ProblemAdapter adapter = new ProblemAdapter(problemList);
        rvCommonProblems.setAdapter(adapter);

        // 2. Настройка навигации (без дубликатов)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setBackground(null);
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Отключаем центральную кнопку-заглушку один раз
        if (bottomNav.getMenu().findItem(R.id.placeholder) != null) {
            bottomNav.getMenu().findItem(R.id.placeholder).setEnabled(false);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_instruments) return true;
            if (id == R.id.nav_interactive) return true;
            if (id == R.id.nav_garden) return true;
            return false;
        });
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
            // Оставляем отступы только сверху (для статус-бара) и по бокам.
            // Снизу ставим 0, чтобы панель прилегала к краю экрана.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
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