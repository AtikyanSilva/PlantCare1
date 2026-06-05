package atikyan.silva.plantcare;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

public class PlantRecognizeActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;

    private final String[] KEYS_DETECT   = { BuildConfig.GEMINI_KEY_DETECT,  BuildConfig.GEMINI_KEY_DETECT2 };
    private final String[] KEYS_DIAGNOSE = { BuildConfig.GEMINI_KEY_DIAGNOSE, BuildConfig.GEMINI_KEY_DIAGNOSE2 };

    private int idxDetect   = 0;
    private int idxDiagnose = 0;

    private String currentMode = "detect";
    private Bitmap lastSelectedBitmap;
    private ProgressDialog progressDialog;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bitmap bmp = (Bitmap) result.getData().getExtras().get("data");
                if (bmp != null) {
                    lastSelectedBitmap = bmp;
                    sendImageToAI(bmp, 0);
                }
            } else {
                finish();
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                executor.execute(() -> {
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        InputStream probe = getContentResolver().openInputStream(uri);
                        BitmapFactory.decodeStream(probe, null, options);
                        if (probe != null) probe.close();

                        int maxDim = 1024, scale = 1;
                        while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) scale *= 2;
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = scale;

                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                        if (inputStream != null) inputStream.close();

                        if (bitmap != null) {
                            lastSelectedBitmap = bitmap;
                            sendImageToAI(bitmap, 0);
                        } else {
                            runOnUiThread(() -> { Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show(); finish(); });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> { Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show(); finish(); });
                    }
                });
            } else {
                finish();
            }
        });

        checkPermissionAndProceed();
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSourceSelectionDialog();
            } else {
                Toast.makeText(this, "Нужно разрешение на камеру", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void showSourceSelectionDialog() {
        BottomSheetDialog sourceDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_select_source, null);
        sourceDialog.setContentView(view);
        sourceDialog.setOnCancelListener(d -> finish());
        view.findViewById(R.id.btnSourceCamera).setOnClickListener(v -> {
            sourceDialog.dismiss();
            openCamera();
        });
        view.findViewById(R.id.btnSourceGallery).setOnClickListener(v -> {
            sourceDialog.dismiss();
            galleryLauncher.launch("image/*");
        });
        sourceDialog.show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        }
    }

    private void showLoading(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    private String detectKey()   { return KEYS_DETECT  [idxDetect   % KEYS_DETECT.length];   }
    private String diagnoseKey() { return KEYS_DIAGNOSE[idxDiagnose % KEYS_DIAGNOSE.length]; }
    private void rotateDetect()   { idxDetect   = (idxDetect   + 1) % KEYS_DETECT.length;   }
    private void rotateDiagnose() { idxDiagnose = (idxDiagnose + 1) % KEYS_DIAGNOSE.length; }

    private void sendImageToAI(Bitmap bitmap, int attempt) {
        boolean isDetect = currentMode.equals("detect");
        int maxAttempts = isDetect ? KEYS_DETECT.length : KEYS_DIAGNOSE.length;
        if (attempt >= maxAttempts) {
            runOnUiThread(() -> {
                hideLoading();
                Toast.makeText(this, "Все ключи исчерпаны", Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }

        runOnUiThread(() -> showLoading("Анализирую фото... 🌱"));

        executor.execute(() -> {
            try {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                String prompt = isDetect
                        ? "Посмотри на изображение. Если это НЕ растение — ответь ровно одним словом: НЕ_РАСТЕНИЕ. Если это растение — напиши его название и подробное описание."
                        : "Посмотри на изображение. Если это НЕ растение — ответь ровно одним словом: НЕ_РАСТЕНИЕ. Если это растение — диагностируй проблемы и напиши лечение.";
                String key = isDetect ? detectKey() : diagnoseKey();

                GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", key);
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);
                Content content = new Content.Builder().addImage(scaled).addText(prompt).build();
                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        runOnUiThread(() -> {
                            hideLoading();
                            showResultSheet(result.getText());
                        });
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        String msg = t.getMessage() != null ? t.getMessage() : "";
                        runOnUiThread(() -> hideLoading());
                        if (msg.contains("429") || msg.contains("403")) {
                            if (isDetect) rotateDetect(); else rotateDiagnose();
                            sendImageToAI(bitmap, attempt + 1);
                        } else if (msg.contains("503") && attempt < 2) {
                            sendImageToAI(bitmap, attempt + 1);
                        } else {
                            runOnUiThread(() -> { Toast.makeText(PlantRecognizeActivity.this, "Ошибка анализа фото", Toast.LENGTH_SHORT).show(); finish(); });
                            t.printStackTrace();
                        }
                    }
                }, executor);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(this, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
                    finish();
                });
                e.printStackTrace();
            }
        });
    }

    private void showResultSheet(String text) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet, null);
        dialog.setContentView(sheetView);
        dialog.setOnDismissListener(d -> finish());

        TextView tvTitle   = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvContent = sheetView.findViewById(R.id.tvSheetContent);

        boolean isNotPlant = text != null && text.trim().contains("НЕ_РАСТЕНИЕ");

        if (isNotPlant) {
            tvTitle.setText("Не распознано");
            tvContent.setText("На фото не обнаружено растение. Попробуйте сфотографировать растение крупнее.");
        } else {
            tvTitle.setText("Распознавание");
            tvContent.setText(text != null ? text.trim() : "");
        }

        ImageView ivResult = sheetView.findViewById(R.id.ivPlantResult);
        if (ivResult != null && lastSelectedBitmap != null) ivResult.setImageBitmap(lastSelectedBitmap);

        View btnAddToGarden = sheetView.findViewById(R.id.btnAddToGarden);
        if (btnAddToGarden != null) {
            if (!isNotPlant) {
                btnAddToGarden.setVisibility(View.VISIBLE);
                btnAddToGarden.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(this, AddPlantActivity.class);
                    if (lastSelectedBitmap != null) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        Bitmap s = Bitmap.createScaledBitmap(lastSelectedBitmap, 400, 400, true);
                        s.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        String photoBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
                        intent.putExtra("photo_base64", photoBase64);
                    }
                    startActivity(intent);
                });
            } else {
                btnAddToGarden.setVisibility(View.GONE);
            }
        }

        dialog.show();
    }
}
