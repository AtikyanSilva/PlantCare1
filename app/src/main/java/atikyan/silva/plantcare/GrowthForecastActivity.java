package atikyan.silva.plantcare;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GrowthForecastActivity extends AppCompatActivity {

    private static final String GEMINI_MODEL = "gemini-2.5-flash";


    private ScrollView scrollView;
    private LinearLayout chatContainer;
    private CardView inputCard;
    private ChipGroup chipGroup;
    private TextInputEditText etCustomInput;
    private MaterialButton btnSend;
    private ProgressBar progressBar;
    private CardView resultCard;
    private ImageView ivPlantPhoto;
    private TextView tvPlantName;
    private TextView tvPlantDescription;
    private MaterialButton btnRestart;

    // Gemini
    private GenerativeModelFutures gemini;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Вопросы: { текст, подсказка (null = скрыть поле), варианты }
    private static final Object[][] QUESTIONS = {
        {
            "☀️ Где будет жить ваш будущий зелёный друг?",
            "Можете написать иначе...",
            new String[]{
                "На солнечном подоконнике",
                "В глубине комнаты, света мало",
                "В офисе под лампами",
                "На балконе или лоджии"
            }
        },
        {
            "🌿 Как вы себя чувствуете в роли садовода?",
            null,
            new String[]{
                "Новичок — хочу что-то живучее",
                "Поливаю раз в неделю, не больше",
                "Мне нравится ухаживать и следить",
                "Готов к опрыскиваниям и удобрениям"
            }
        },
        {
            "🐾 Дома есть дети или животные?",
            null,
            new String[]{
                "Да, нужно безопасное растение",
                "Нет, можно любое"
            }
        },
        {
            "📏 Какой размер вам подходит?",
            null,
            new String[]{
                "Маленькое — на полку или стол",
                "Среднее — напольный горшок",
                "Большое — акцент в интерьере"
            }
        },
        {
            "✨ Что для вас важнее всего в растении?",
            "Или напишите своё пожелание...",
            new String[]{
                "Очищает воздух в комнате",
                "Красиво цветёт",
                "Необычный, эффектный вид",
                "Ароматные листья или плоды"
            }
        }
    };

    private int currentQuestion = 0;
    private final StringBuilder answers = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_growth_forecast);

        bindViews();
        initGemini();

        ((ImageButton) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> onSendClicked());
        btnRestart.setOnClickListener(v -> restart());

        chatContainer.post(() -> {
            addBotBubble("Привет! 🌱 Отвечу на 5 вопросов — и я подберу растение, которое идеально впишется в вашу жизнь.");
            mainHandler.postDelayed(this::askQuestion, 500);
        });
    }

    private void initGemini() {
        try {
            GenerativeModel gm = new GenerativeModel(GEMINI_MODEL, BuildConfig.GEMINI_KEY_BOTANIST);
            gemini = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            mainHandler.post(() ->
                addBotBubble("⚠️ Не удалось инициализировать ИИ: " + e.getMessage()));
        }
    }

    private void askQuestion() {
        if (currentQuestion >= QUESTIONS.length) {
            getRecommendation();
            return;
        }
        Object[] q = QUESTIONS[currentQuestion];
        addBotBubble((String) q[0]);
        setupInput((String) q[1], (String[]) q[2]);
    }

    private void onSendClicked() {
        String answer = null;

        String custom = etCustomInput.getText() != null
                ? etCustomInput.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(custom)) {
            answer = custom;
        } else {
            int id = chipGroup.getCheckedChipId();
            if (id != View.NO_ID) {
                Chip chip = chipGroup.findViewById(id);
                if (chip != null) answer = chip.getText().toString();
            }
        }

        if (TextUtils.isEmpty(answer)) {
            if (etCustomInput.getVisibility() == View.VISIBLE) {
                etCustomInput.setError("Выберите вариант или напишите ответ");
            }
            return;
        }

        addUserBubble(answer);
        String qText = (String) QUESTIONS[currentQuestion][0];
        answers.append(qText).append(" → ").append(answer).append("\n");

        inputCard.setVisibility(View.GONE);
        etCustomInput.setText("");
        chipGroup.clearCheck();
        currentQuestion++;

        mainHandler.postDelayed(this::askQuestion, 300);
    }

    private void getRecommendation() {
        addBotBubble("Подбираю идеальный вариант… 🔍");
        progressBar.setVisibility(View.VISIBLE);

        if (gemini == null) {
            progressBar.setVisibility(View.GONE);
            addBotBubble("⚠️ ИИ недоступен. Проверьте ключ GEMINI_KEY_BOTANIST.");
            return;
        }

        String prompt =
            "Ты эксперт-ботаник. На основе ответов пользователя подбери ОДНО идеальное комнатное растение.\n" +
            "Ответь ТОЛЬКО в формате JSON без markdown и без лишнего текста:\n" +
            "{\n" +
            "  \"name\": \"Русское название\",\n" +
            "  \"latin\": \"Латинское название\",\n" +
            "  \"tagline\": \"Одна тёплая фраза — почему именно оно подходит этому человеку\",\n" +
            "  \"description\": \"4-5 предложений: характер растения, уход, особенности, почему понравится\",\n" +
            "  \"photo_query\": \"точное латинское название вида, 1-3 слова, например: Nephrolepis exaltata\"\n" +
            "}\n\n" +
            "Ответы пользователя:\n" + answers;

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> future = gemini.generateContent(content);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                mainHandler.post(() -> handleResponse(text));
            }

            @Override
            public void onFailure(Throwable t) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    addBotBubble("😔 Ошибка: " + t.getMessage() + "\nПопробуйте начать заново.");
                    btnRestart.setVisibility(View.VISIBLE);
                });
            }
        }, executor);
    }

    private void handleResponse(String raw) {
        progressBar.setVisibility(View.GONE);
        if (raw == null || raw.isEmpty()) {
            addBotBubble("😔 Пустой ответ. Попробуйте ещё раз.");
            btnRestart.setVisibility(View.VISIBLE);
            return;
        }

        try {
            String json = raw.trim();
            if (json.contains("{")) {
                json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1);
            }

            JSONObject plant   = new JSONObject(json);
            String name        = plant.getString("name");
            String latin       = plant.optString("latin", name);
            String tagline     = plant.optString("tagline", "");
            String description = plant.getString("description");
            String photoQuery  = plant.optString("photo_query", latin);

            addBotBubble("🌟 Нашёл идеальное растение именно для вас!");
            showResult(name, latin, tagline, description, photoQuery);

        } catch (JSONException e) {
            addBotBubble("🌿 " + raw);
            btnRestart.setVisibility(View.VISIBLE);
        }
    }

    private void showResult(String name, String latin, String tagline,
                            String description, String photoQuery) {
        tvPlantName.setText(TextUtils.isEmpty(tagline) ? name : name + "\n\n" + tagline);
        tvPlantDescription.setText(description + "\n\n🌿 " + latin);

        // Загружаем фото через Wikipedia API — надёжно и точно
        executor.execute(() -> {
            String imageUrl = fetchWikipediaImageUrl(photoQuery);
            mainHandler.post(() -> {
                if (imageUrl != null) {
                    Glide.with(this)
                         .load(imageUrl)
                         .placeholder(R.drawable.plant)
                         .error(R.drawable.plant)
                         .centerCrop()
                         .into(ivPlantPhoto);
                } else {
                    // Fallback: Unsplash
                    Glide.with(this)
                         .load("https://source.unsplash.com/600x400/?" + photoQuery.replace(" ", "+"))
                         .placeholder(R.drawable.plant)
                         .error(R.drawable.plant)
                         .centerCrop()
                         .into(ivPlantPhoto);
                }
            });
        });

        resultCard.setVisibility(View.VISIBLE);
        scrollToBottom();
    }

    /**
     * Ищет фото растения через Wikimedia Commons по латинскому названию.
     * Сначала пробует Wikipedia summary, потом Wikimedia Commons search.
     */
    private String fetchWikipediaImageUrl(String query) {
        // Попытка 1: Wikipedia summary (точное название страницы)
        String result = tryWikipediaSummary(query);
        if (result != null) return result;

        // Попытка 2: Wikimedia Commons поиск изображений
        result = tryWikimediaCommons(query);
        if (result != null) return result;

        return null;
    }

    private String tryWikipediaSummary(String query) {
        try {
            String encoded = URLEncoder.encode(query.replace(" ", "_"), "UTF-8");
            URL url = new URL("https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "PlantCareApp/1.0");

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn);
                JSONObject json = new JSONObject(body);
                if (json.has("originalimage")) {
                    return json.getJSONObject("originalimage").getString("source");
                }
                if (json.has("thumbnail")) {
                    String src = json.getJSONObject("thumbnail").getString("source");
                    return src.replaceAll("/\\d+px-", "/800px-");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String tryWikimediaCommons(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String apiUrl = "https://commons.wikimedia.org/w/api.php"
                + "?action=query&generator=search&gsrnamespace=6"
                + "&gsrsearch=" + encoded
                + "&gsrlimit=1&prop=imageinfo&iiprop=url&iiurlwidth=800&format=json";

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "PlantCareApp/1.0");

            if (conn.getResponseCode() == 200) {
                String body = readStream(conn);
                JSONObject json = new JSONObject(body);
                JSONObject query2 = json.optJSONObject("query");
                if (query2 != null) {
                    JSONObject pages = query2.optJSONObject("pages");
                    if (pages != null && pages.length() > 0) {
                        String firstKey = pages.keys().next();
                        JSONObject page = pages.getJSONObject(firstKey);
                        JSONObject imageinfo = page.getJSONArray("imageinfo").getJSONObject(0);
                        return imageinfo.getString("thumburl");
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private void setupInput(String hint, String[] options) {
        chipGroup.removeAllViews();
        for (String opt : options) {
            Chip chip = new Chip(this);
            chip.setText(opt);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                etCustomInput.setText("");
                etCustomInput.clearFocus();
            });
            chipGroup.addView(chip);
        }

        if (hint != null) {
            etCustomInput.setHint(hint);
            etCustomInput.setVisibility(View.VISIBLE);
        } else {
            etCustomInput.setVisibility(View.GONE);
        }

        inputCard.setVisibility(View.VISIBLE);
        scrollToBottom();
    }

    private void addBotBubble(String text) {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.item_chat_message_bot, chatContainer, false);
        ((TextView) v.findViewById(R.id.tvMessage)).setText(text);
        chatContainer.addView(v);
        scrollToBottom();
    }

    private void addUserBubble(String text) {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.item_chat_message_user, chatContainer, false);
        ((TextView) v.findViewById(R.id.tvMessage)).setText(text);
        chatContainer.addView(v);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void restart() {
        currentQuestion = 0;
        answers.setLength(0);
        chatContainer.removeAllViews();
        resultCard.setVisibility(View.GONE);
        btnRestart.setVisibility(View.GONE);
        chatContainer.post(() -> {
            addBotBubble("Привет! 🌱 Отвечу на 5 вопросов — и я подберу растение, которое идеально впишется в вашу жизнь.");
            mainHandler.postDelayed(this::askQuestion, 500);
        });
    }

    private void bindViews() {
        scrollView         = findViewById(R.id.scrollView);
        chatContainer      = findViewById(R.id.chatContainer);
        inputCard          = findViewById(R.id.inputCard);
        chipGroup          = findViewById(R.id.chipGroup);
        etCustomInput      = findViewById(R.id.etCustomInput);
        btnSend            = findViewById(R.id.btnSend);
        progressBar        = findViewById(R.id.progressBar);
        resultCard         = findViewById(R.id.resultCard);
        ivPlantPhoto       = findViewById(R.id.ivPlantPhoto);
        tvPlantName        = findViewById(R.id.tvPlantName);
        tvPlantDescription = findViewById(R.id.tvPlantDescription);
        btnRestart         = findViewById(R.id.btnRestart);
    }
}
