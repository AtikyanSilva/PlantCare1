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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity2 extends AppCompatActivity {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher; // Чтобы ушла ошибка с galleryLauncher
    private Bitmap lastSelectedBitmap;
    private String currentMode = "detect"; // "detect" или "diagnose"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Регистрация камеры
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        // ПРОВЕРКА: Если картинка пришла, отправляем её в AI
                        if (imageBitmap != null) {
                            lastSelectedBitmap = imageBitmap;
                            sendImageToAI(imageBitmap);
                        }
                    }
                }

        );
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            // 1. Получаем доступ к файлу по ссылке (uri)
                            InputStream inputStream = getContentResolver().openInputStream(uri);
                            // 2. Превращаем файл в картинку (Bitmap)
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            // 3. Сохраняем её, чтобы показать потом в окошке
                            lastSelectedBitmap = bitmap;
                            // 4. Отправляем в нейросеть
                            sendImageToAI(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );


        // Кнопка РАСПОЗНАВАНИЯ
        CardView cardDetect = findViewById(R.id.cardDetect);
        cardDetect.setOnClickListener(v -> {
            currentMode = "detect";
            openCamera();
        });

        // Кнопка ДИАГНОСТИКИ
        CardView cardDiagnose = findViewById(R.id.cardDiagnose);
        cardDiagnose.setOnClickListener(v -> {
            currentMode = "diagnose";
            openCamera();
        });
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Камера не найдена", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendImageToAI(Bitmap bitmap) {
        // Объявляем prompt заранее, чтобы он был доступен во всем методе
        final String prompt;

        if (currentMode.equals("detect")) {
            prompt = "Определи вид этого растения на фото. Напиши только название и краткое описание. Ответь на русском.";
            Toast.makeText(this, "Распознаю вид...", Toast.LENGTH_SHORT).show();
        } else {
            prompt = "Проанализируй состояние листьев и стебля этого растения. Определи, есть ли болезни или вредители, и дай совет по лечению. Ответь на русском.";
            Toast.makeText(this, "Диагностирую болезни...", Toast.LENGTH_SHORT).show();
        }

        // ЗАМЕНИ НА СВОЙ КЛЮЧ:
        String apiKey = "AIzaSyBznJl6zGdvvvlILuiuFI90kgSWug08sKo";

        // Используем актуальную модель 2.5, она точно есть на серверах v1
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", apiKey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
// Увеличим Bitmap, если он пришел слишком маленьким (эмуляция лучшего качества)
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1024, 1024, true);

        Content content = new Content.Builder()
                .addImage(scaledBitmap) // Отправляем масштабированное фото
                .addText(prompt)
                .build();
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponse = result.getText();
                runOnUiThread(() -> {
                    boolean isPlant = !aiResponse.toUpperCase().contains("ОТКАЗ");
                    showResultSheet(aiResponse, isPlant);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MainActivity2.this, "Ошибка AI: " + t.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, executor);
    }

    private void showResultSheet(String text, boolean isPlant) { // Добавили параметр isPlant
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvContent = sheetView.findViewById(R.id.tvSheetContent);
        Button btnAdd = sheetView.findViewById(R.id.btnAddToGarden);

        // 1. Находим ImageView в макете нашего BottomSheet
        ImageView ivResult = sheetView.findViewById(R.id.ivPlantResult);

        tvTitle.setText(currentMode.equals("detect") ? "Распознавание" : "Диагностика");

        // Убираем слово ОТКАЗ из текста, если оно там есть
        String cleanText = text.replace("ОТКАЗ:", "").replace("ОТКАЗ", "").trim();
        tvContent.setText(cleanText);

        // 2. Устанавливаем наше сохраненное фото в ImageView
        if (lastSelectedBitmap != null) {
            ivResult.setImageBitmap(lastSelectedBitmap);
        }

        // 3. Управляем видимостью кнопки: если растение - показываем, если нет - прячем
        if (isPlant) {
            btnAdd.setVisibility(View.VISIBLE);
        } else {
            btnAdd.setVisibility(View.GONE);
        }

        btnAdd.setOnClickListener(v -> {
            Toast.makeText(this, "Сохранено!", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
}