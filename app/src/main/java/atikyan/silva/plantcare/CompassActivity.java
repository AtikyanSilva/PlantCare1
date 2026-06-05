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

public class CompassActivity extends AppCompatActivity {

    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final String[] API_KEYS = {
        BuildConfig.GEMINI_KEY_8,
        BuildConfig.GEMINI_KEY_7,
        BuildConfig.GEMINI_KEY_6,
        BuildConfig.GEMINI_KEY_5,
        BuildConfig.GEMINI_KEY_4,
        BuildConfig.GEMINI_KEY_3,
        BuildConfig.GEMINI_KEY_2,
        BuildConfig.GEMINI_KEY_1,
    };
    private int currentKeyIndex = 0;

    // UI
    private ScrollView scrollView;
    private LinearLayout chatContainer;
    private CardView inputCard;
    private ChipGroup chipGroup;
    private TextInputEditText etCustomInput;
    private MaterialButton btnSend;
    private ProgressBar progressBar;
    private CardView resultCard;
    private ImageView ivFlowerPhoto;
    private TextView tvFlowerName;
    private TextView tvFlowerPersonality;
    private MaterialButton btnRestart;

    // Gemini
    private GenerativeModelFutures gemini;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Вопросы викторины "Какой ты цветок?"
    private static final Object[][] QUESTIONS = {
        {
            "🌅 Как ты просыпаешься утром?",
            null,
            new String[]{
                "Встаю с первым будильником, полон сил",
                "Долго нежусь, не хочу вставать",
                "Встаю тихо, нужно время прийти в себя",
                "По-разному, зависит от настроения"
            }
        },
        {
            "🎨 Что лучше всего описывает тебя в компании?",
            null,
            new String[]{
                "Душа компании — всегда в центре внимания",
                "Слушаю и поддерживаю других",
                "Веду интересные разговоры один на один",
                "Наблюдаю со стороны, но в нужный момент выхожу вперёд"
            }
        },
        {
            "🌦️ Как ты реагируешь на трудности?",
            "Можешь описать по-другому...",
            new String[]{
                "Сразу ищу решение, действую",
                "Даю себе время, потом разбираюсь",
                "Ищу поддержку у близких",
                "Анализирую и строю план"
            }
        },
        {
            "✨ Что тебя заряжает энергией?",
            null,
            new String[]{
                "Общение с людьми",
                "Одиночество и тишина",
                "Творчество и новые идеи",
                "Природа и прогулки"
            }
        },
        {
            "🌙 Какое время суток — твоё?",
            null,
            new String[]{
                "Раннее утро — свежесть и тишина",
                "День — на пике активности",
                "Вечер — расслабление и уют",
                "Ночь — мысли и вдохновение"
            }
        },
        {
            "💫 Какая твоя главная суперсила?",
            "Или опиши своё качество...",
            new String[]{
                "Умею вдохновлять и вести за собой",
                "Нахожу красоту в мелочах",
                "Стойкость — я не сломлюсь",
                "Интуиция и чуткость к людям"
            }
        }
    };

    private int currentQuestion = 0;
    private final StringBuilder answers = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        bindViews();
        initGemini(currentKeyIndex);

        ((ImageButton) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> onSendClicked());
        btnRestart.setOnClickListener(v -> restart());

        chatContainer.post(() -> {
            addBotBubble("Привет! 🌸 Отвечу на 6 вопросов — и я открою, какой цветок живёт в твоей душе.");
            mainHandler.postDelayed(this::askQuestion, 500);
        });
    }

    private void initGemini(int keyIndex) {
        try {
            String key = API_KEYS[keyIndex];
            GenerativeModel gm = new GenerativeModel(GEMINI_MODEL, key);
            gemini = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            mainHandler.post(() ->
                addBotBubble("⚠️ Не удалось инициализировать ИИ: " + e.getMessage()));
        }
    }

    private void askQuestion() {
        if (currentQuestion >= QUESTIONS.length) {
            getPersonalityResult();
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
                etCustomInput.setError("Выбери вариант или напиши ответ");
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

    private void getPersonalityResult() {
        addBotBubble("Раскрываю твой цветочный характер… 🌸");
        progressBar.setVisibility(View.VISIBLE);

        if (gemini == null) {
            progressBar.setVisibility(View.GONE);
            addBotBubble("⚠️ ИИ недоступен. Попробуй начать заново.");
            return;
        }

        String prompt =
            "Ты творческий психолог и знаток цветов. На основе ответов пользователя определи, " +
            "какой цветок соответствует его личности.\n" +
            "Ответь ТОЛЬКО в формате JSON без markdown и без лишнего текста:\n" +
            "{\n" +
            "  \"flower_name\": \"Русское название цветка\",\n" +
            "  \"flower_latin\": \"Латинское название\",\n" +
            "  \"personality_title\": \"Яркий заголовок — кто этот человек (например: Романтик в душе, Тихая сила)\",\n" +
            "  \"personality_text\": \"4-5 предложений: какой это человек, его сильные стороны, как он взаимодействует с миром, почему именно этот цветок его отражает. Тёплый и вдохновляющий тон.\",\n" +
            "  \"flower_meaning\": \"2-3 предложения: символика этого цветка, что он означает в разных культурах, почему именно он.\",\n" +
            "  \"photo_query\": \"точное латинское название вида для поиска фото, 1-3 слова\"\n" +
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
                // Пробуем следующий ключ
                if (currentKeyIndex + 1 < API_KEYS.length) {
                    currentKeyIndex++;
                    initGemini(currentKeyIndex);
                    mainHandler.post(() -> getPersonalityResult());
                } else {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        addBotBubble("😔 Ошибка: " + t.getMessage() + "\nПопробуй начать заново.");
                        btnRestart.setVisibility(View.VISIBLE);
                    });
                }
            }
        }, executor);
    }

    private void handleResponse(String raw) {
        progressBar.setVisibility(View.GONE);
        if (raw == null || raw.isEmpty()) {
            addBotBubble("😔 Пустой ответ. Попробуй ещё раз.");
            btnRestart.setVisibility(View.VISIBLE);
            return;
        }

        try {
            String json = raw.trim();
            if (json.contains("{")) {
                json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1);
            }

            JSONObject obj          = new JSONObject(json);
            String flowerName       = obj.getString("flower_name");
            String flowerLatin      = obj.optString("flower_latin", flowerName);
            String personalityTitle = obj.optString("personality_title", "");
            String personalityText  = obj.getString("personality_text");
            String flowerMeaning    = obj.optString("flower_meaning", "");
            String photoQuery       = obj.optString("photo_query", flowerLatin);

            addBotBubble("🌸 Твой цветок раскрыт!");
            showResult(flowerName, personalityTitle, personalityText, flowerMeaning, photoQuery, flowerLatin);

        } catch (JSONException e) {
            addBotBubble("🌿 " + raw);
            btnRestart.setVisibility(View.VISIBLE);
        }
    }

    private void showResult(String flowerName, String personalityTitle,
                            String personalityText, String flowerMeaning,
                            String photoQuery, String flowerLatin) {

        tvFlowerName.setText(flowerName + "\n" + personalityTitle);

        String fullText = personalityText;
        if (!flowerMeaning.isEmpty()) fullText += "\n\n🌿 " + flowerMeaning;
        fullText += "\n\n✨ " + flowerLatin;
        tvFlowerPersonality.setText(fullText);

        String finalQuery = photoQuery;
        executor.execute(() -> {
            String imageUrl = fetchWikipediaImageUrl(finalQuery);
            mainHandler.post(() -> {
                if (imageUrl != null) {
                    Glide.with(this).load(imageUrl)
                         .placeholder(R.drawable.plant).error(R.drawable.plant)
                         .centerCrop().into(ivFlowerPhoto);
                } else {
                    Glide.with(this)
                         .load("https://source.unsplash.com/600x400/?" + finalQuery.replace(" ", "+") + "+flower")
                         .placeholder(R.drawable.plant).error(R.drawable.plant)
                         .centerCrop().into(ivFlowerPhoto);
                }
            });
        });

        resultCard.setVisibility(View.VISIBLE);
        scrollToBottom();
    }

    private String fetchWikipediaImageUrl(String query) {
        String result = tryWikipediaSummary(query);
        if (result != null) return result;
        return tryWikimediaCommons(query);
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
                if (json.has("originalimage"))
                    return json.getJSONObject("originalimage").getString("source");
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
                JSONObject q = json.optJSONObject("query");
                if (q != null) {
                    JSONObject pages = q.optJSONObject("pages");
                    if (pages != null && pages.length() > 0) {
                        String firstKey = pages.keys().next();
                        JSONObject page = pages.getJSONObject(firstKey);
                        return page.getJSONArray("imageinfo").getJSONObject(0).getString("thumburl");
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
        currentKeyIndex = 0;
        answers.setLength(0);
        chatContainer.removeAllViews();
        resultCard.setVisibility(View.GONE);
        btnRestart.setVisibility(View.GONE);
        initGemini(0);
        chatContainer.post(() -> {
            addBotBubble("Привет! 🌸 Отвечу на 6 вопросов — и я открою, какой цветок живёт в твоей душе.");
            mainHandler.postDelayed(this::askQuestion, 500);
        });
    }

    private void bindViews() {
        scrollView          = findViewById(R.id.scrollView);
        chatContainer       = findViewById(R.id.chatContainer);
        inputCard           = findViewById(R.id.inputCard);
        chipGroup           = findViewById(R.id.chipGroup);
        etCustomInput       = findViewById(R.id.etCustomInput);
        btnSend             = findViewById(R.id.btnSend);
        progressBar         = findViewById(R.id.progressBar);
        resultCard          = findViewById(R.id.resultCard);
        ivFlowerPhoto       = findViewById(R.id.ivFlowerPhoto);
        tvFlowerName        = findViewById(R.id.tvFlowerName);
        tvFlowerPersonality = findViewById(R.id.tvFlowerPersonality);
        btnRestart          = findViewById(R.id.btnRestart);
    }
}
