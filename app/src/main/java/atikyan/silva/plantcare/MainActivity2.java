package atikyan.silva.plantcare;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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

    private final String API_KEY = "AIzaSyDFNshs-EI95DrF5Tek6QVkcIsjnOpYXNo";

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

        fabCamera.setOnClickListener(v -> showSourceSelectionDialog());
        cardDetect.setOnClickListener(v -> { currentMode = "detect"; openCamera(); });
        cardDiagnose.setOnClickListener(v -> { currentMode = "diagnose"; openCamera(); });
    }

    private void loadDailyAdvice() {
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY);
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
        String prompt = currentMode.equals("detect") ?
                "Определи вид растения по фото. Напиши название и краткое описание. На русском." :
                "Проверь это растение на болезни. Дай краткий совет по лечению. На русском.";

        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true);
        Content content = new Content.Builder().addImage(scaled).addText(prompt).build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> showResultSheet(result.getText(), true));
            }
            @Override public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, executor);
    }

    private void showResultSheet(String text, boolean isPhotoResult) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvContent = sheetView.findViewById(R.id.tvSheetContent);
        ImageView ivResult = sheetView.findViewById(R.id.ivPlantResult);
        Button btnAdd = sheetView.findViewById(R.id.btnAddToGarden);

        tvTitle.setText(isPhotoResult ? "Результат" : "Идея дня");
        tvContent.setText(text);

        if (isPhotoResult && lastSelectedBitmap != null) {
            ivResult.setVisibility(View.VISIBLE);
            ivResult.setImageBitmap(lastSelectedBitmap);
            btnAdd.setVisibility(View.VISIBLE);
        } else {
            // Для совета дня ставим иконку (если она есть) или просто скрываем
            ivResult.setVisibility(View.GONE);
            btnAdd.setVisibility(View.GONE);
        }
        bottomSheetDialog.show();
    }
}