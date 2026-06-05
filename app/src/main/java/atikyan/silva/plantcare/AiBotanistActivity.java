package atikyan.silva.plantcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiBotanistActivity extends AppCompatActivity {
    private final String[] GEMINI_KEYS = {
            BuildConfig.GEMINI_KEY_BOTANIST
    };
    private int currentKeyIndex = 0;

    private static final String FLORA_SYSTEM_PROMPT =
            "Ты Флора — дружелюбный AI-ботаник в приложении PlantCare. " +
                    "Отвечай коротко, по-русски, тёплым тоном. " +
                    "Помогай с уходом за растениями: болезни, полив, освещение, пересадка, размножение. " +
                    "Если вопрос не о растениях, мягко верни разговор к теме растений. " +
                    "Используй эмодзи 🌿🌱🪴 изредка для живости. " +
                    "Максимум 3 предложения в ответе.";

    private static final String[] CHIP_LABELS = {
            "💧 Полив", "☀️ Освещение", "🪴 Пересадка", "🌿 Размножение", "🐛 Вредители"
    };
    private static final String[] CHIP_QUESTIONS = {
            "Как часто нужно поливать комнатные растения?",
            "Какое освещение нужно большинству комнатных растений?",
            "Как понять, что растению нужна пересадка?",
            "Как размножить комнатное растение?",
            "Как бороться с вредителями на комнатных растениях?"
    };

    private RecyclerView    rvChat;
    private EditText        etMessage;
    private ImageView       btnSend;
    private LinearLayout    chipContainer;

    private LinearLayout navHome, navInstruments, navBotanist, navGarden;
    private ImageView    iconHome, iconInstruments, iconBotanist, iconGarden;
    private TextView     labelHome, labelInstruments, labelBotanist, labelGarden;

    private ChatAdapter       chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();

    private int      typingIndex = -1;
    private Runnable typingAnimRunnable;

    private FirebaseAuth      mAuth;
    private DatabaseReference chatRef;
    private boolean           isGuest;

    private GenerativeModelFutures model;
    private ChatFutures            chat;
    private final Executor  executor    = Executors.newSingleThreadExecutor();
    private final Handler   mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_botanist);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime  = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(bars.bottom, ime.bottom);
            v.setPadding(bars.left, bars.top, bars.right, bottomPadding);
            return insets;
        });

        initViews();
        initGemini(currentKeyIndex);
        initFirebase();
        setupQuickChips();
        setupNavigation();
        setupSendButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTypingAnimation();
    }

    private void initViews() {
        rvChat        = findViewById(R.id.rvChat);
        etMessage     = findViewById(R.id.etMessage);
        btnSend       = findViewById(R.id.btnSend);
        chipContainer = findViewById(R.id.chipContainer);

        chatAdapter = new ChatAdapter(messages, this);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void initGemini(int keyIndex) {
        String key = GEMINI_KEYS[keyIndex % GEMINI_KEYS.length];
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", key);
        model = GenerativeModelFutures.from(gm);

        List<Content> history = new ArrayList<>();
        history.add(new Content("user",
                Collections.singletonList(new TextPart(FLORA_SYSTEM_PROMPT))));
        history.add(new Content("model",
                Collections.singletonList(
                        new TextPart("Привет! Я Флора, ваш AI-ботаник 🌿 Готова помочь!"))));
        chat = model.startChat(history);
    }

    private boolean rotateGeminiKey() {
        int nextIndex = (currentKeyIndex + 1) % GEMINI_KEYS.length;
        if (nextIndex == currentKeyIndex) return false;
        currentKeyIndex = nextIndex;
        initGemini(currentKeyIndex);
        return true;
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            isGuest = false;
            chatRef = FirebaseDatabase.getInstance()
                    .getReference("chats")
                    .child(user.getUid());
            loadChatHistory();
        } else {
            isGuest = true;
            chatRef = null;
            showWelcomeMessage();
        }
    }


    private void loadChatHistory() {
        chatRef.orderByChild("timestamp")
                .limitToLast(50)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<ChatMessage> history = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String text  = child.child("text").getValue(String.class);
                            Boolean isBot = child.child("isBot").getValue(Boolean.class);
                            if (text != null && isBot != null) {
                                history.add(new ChatMessage(text, isBot));
                            }
                        }
                        mainHandler.post(() -> {
                            if (history.isEmpty()) {
                                showWelcomeMessage();
                            } else {
                                messages.addAll(history);
                                chatAdapter.notifyDataSetChanged();
                                scrollToBottom();
                            }
                        });
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { showWelcomeMessage(); }
                });
    }

    private void saveMessage(String text, boolean isBot) {
        if (isGuest || chatRef == null) return;
        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("isBot", isBot);
        msg.put("timestamp", System.currentTimeMillis());
        chatRef.push().setValue(msg);
    }

    private void showWelcomeMessage() {
        addBotMessage("Привет! Я Флора, ваш AI-ботаник 🌿 Задайте вопрос о растениях!");
    }


    private void setupQuickChips() {
        chipContainer.removeAllViews();
        for (int i = 0; i < CHIP_LABELS.length; i++) {
            String label    = CHIP_LABELS[i];
            String question = CHIP_QUESTIONS[i];

            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(13f);
            chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            chip.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_background));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dpToPx(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> sendUserMessage(question));
            chipContainer.addView(chip);
        }
    }


    private void setupSendButton() {
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        etMessage.setText("");
        sendUserMessage(text);
    }

    private void sendUserMessage(String text) {
        sendUserMessage(text, 0);
    }

    private void sendUserMessage(String text, int attempt) {
        if (attempt == 0) {
            addUserMessage(text);
            showTypingIndicator();
        }

        Content userContent = new Content.Builder().addText(text).build();
        ListenableFuture<GenerateContentResponse> future = chat.sendMessage(userContent);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String reply = result.getText();
                mainHandler.post(() -> {
                    hideTypingIndicator();
                    if (reply != null && !reply.isEmpty()) {
                        addBotMessage(reply);
                    } else {
                        addBotMessage("Упс, пустой ответ 🌿 Попробуйте ещё раз.");
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "";
                if ((msg.contains("429") || msg.contains("403")) && attempt < GEMINI_KEYS.length - 1) {
                    boolean rotated = rotateGeminiKey();
                    if (rotated) {
                        sendUserMessage(text, attempt + 1); // повтор с новым ключом
                    } else {
                        mainHandler.post(() -> {
                            hideTypingIndicator();
                            addBotMessage("Все ключи исчерпаны 🌿 Попробуйте позже.");
                        });
                    }
                } else {
                    mainHandler.post(() -> {
                        hideTypingIndicator();
                        addBotMessage("Не удалось получить ответ 🌿 Проверьте интернет.");
                    });
                }
            }
        }, executor);
    }

    private void addUserMessage(String text) {
        ChatMessage msg = new ChatMessage(text, false);
        messages.add(msg);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        saveMessage(text, false);
    }

    private void addBotMessage(String text) {
        ChatMessage msg = new ChatMessage(text, true);
        messages.add(msg);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        saveMessage(text, true);
    }

    private void showTypingIndicator() {
        if (typingIndex != -1) return;
        ChatMessage typing = new ChatMessage("...", true);
        typing.setTyping(true);
        messages.add(typing);
        typingIndex = messages.size() - 1;
        chatAdapter.notifyItemInserted(typingIndex);
        scrollToBottom();
        startTypingAnimation();
    }

    private void hideTypingIndicator() {
        stopTypingAnimation();
        if (typingIndex != -1 && typingIndex < messages.size()) {
            int idx = typingIndex;
            typingIndex = -1;
            messages.remove(idx);
            chatAdapter.notifyItemRemoved(idx);
        } else {
            typingIndex = -1;
        }
    }

    private void startTypingAnimation() {
        typingAnimRunnable = new Runnable() {
            @Override
            public void run() {
                if (typingIndex != -1 && typingIndex < messages.size()
                        && messages.get(typingIndex).isTyping()) {
                    chatAdapter.notifyItemChanged(typingIndex);
                    mainHandler.postDelayed(this, 400);
                }
            }
        };
        mainHandler.postDelayed(typingAnimRunnable, 400);
    }

    private void stopTypingAnimation() {
        if (typingAnimRunnable != null) {
            mainHandler.removeCallbacks(typingAnimRunnable);
            typingAnimRunnable = null;
        }
    }

    private void setupNavigation() {
        navHome        = findViewById(R.id.navHome);
        navInstruments = findViewById(R.id.navInstruments);
        navBotanist    = findViewById(R.id.navBotanist);
        navGarden      = findViewById(R.id.navGarden);

        iconHome        = findViewById(R.id.iconHome);
        iconInstruments = findViewById(R.id.iconInstruments);
        iconBotanist    = findViewById(R.id.iconBotanist);
        iconGarden      = findViewById(R.id.iconGarden);

        labelHome        = findViewById(R.id.labelHome);
        labelInstruments = findViewById(R.id.labelInstruments);
        labelBotanist    = findViewById(R.id.labelBotanist);
        labelGarden      = findViewById(R.id.labelGarden);

        setActiveTab(2);

        FloatingActionButton fabCamera = findViewById(R.id.fabCamera);
        fabCamera.setOnClickListener(v ->
                startActivity(new Intent(this, PlantRecognizeActivity.class)));

        navHome.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity2.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        });

        navInstruments.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity3.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        });

        navBotanist.setOnClickListener(v -> {});

        navGarden.setOnClickListener(v -> {
            Intent i = new Intent(this, MyGardenActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        });
    }

    private void setActiveTab(int tab) {
        int active   = ContextCompat.getColor(this, R.color.nav_active);
        int inactive = ContextCompat.getColor(this, R.color.nav_inactive);

        iconHome.setColorFilter(inactive);        labelHome.setTextColor(inactive);
        iconInstruments.setColorFilter(inactive); labelInstruments.setTextColor(inactive);
        iconBotanist.setColorFilter(inactive);    labelBotanist.setTextColor(inactive);
        iconGarden.setColorFilter(inactive);      labelGarden.setTextColor(inactive);

        switch (tab) {
            case 0: iconHome.setColorFilter(active);        labelHome.setTextColor(active);        break;
            case 1: iconInstruments.setColorFilter(active); labelInstruments.setTextColor(active); break;
            case 2: iconBotanist.setColorFilter(active);    labelBotanist.setTextColor(active);    break;
            case 3: iconGarden.setColorFilter(active);      labelGarden.setTextColor(active);      break;
        }
    }

    private void scrollToBottom() {
        rvChat.post(() -> {
            int count = chatAdapter.getItemCount();
            if (count > 0) rvChat.smoothScrollToPosition(count - 1);
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}