package atikyan.silva.plantcare;

import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;

public class WaterCalculatorActivity extends AppCompatActivity {

    private static final String[] DAY_LABELS = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    private static final int[] DAY_OF_WEEK = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };

    private static final String[] SOIL_OPTIONS = {
            "Сухая", "Нормальная", "Влажная"
    };

    private static final String[] LIGHT_OPTIONS = {
            "Тень", "Среднее", "Яркий свет"
    };

    private final String[] GEMINI_KEYS = {
            BuildConfig.GEMINI_KEY_ADVICE,
            BuildConfig.GEMINI_KEY_ADVICE2
    };
    private int currentKeyIndex = 0;

    private EditText etPlantName, etPotSize;
    private SeekBar seekTemperature;
    private TextView tvTemperatureValue;
    private Spinner spinnerSoilMoisture, spinnerLight;
    private LinearLayout cardResult, layoutTip;
    private TextView tvWaterResult, tvWaterNote, tvFrequency;
    private MaterialButton btnAddReminder, btnCalculate;

    private int temperatureValue = 20;
    private String soilMoisture = "Нормальная";
    private String lightLevel = "Среднее";
    private int lastWaterMl = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_calculator);

        etPlantName        = findViewById(R.id.etPlantName);
        etPotSize          = findViewById(R.id.etPotSize);
        seekTemperature    = findViewById(R.id.seekTemperature);
        tvTemperatureValue = findViewById(R.id.tvTemperatureValue);
        spinnerSoilMoisture = findViewById(R.id.spinnerSoilMoisture);
        spinnerLight       = findViewById(R.id.spinnerLight);
        cardResult         = findViewById(R.id.cardResult);
        layoutTip          = findViewById(R.id.layoutTip);
        tvWaterResult      = findViewById(R.id.tvWaterResult);
        tvWaterNote        = findViewById(R.id.tvWaterNote);
        tvFrequency        = findViewById(R.id.tvFrequency);
        btnAddReminder     = findViewById(R.id.btnAddReminder);
        btnCalculate       = findViewById(R.id.btnCalculate);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        seekTemperature.setMax(40);
        seekTemperature.setProgress(20);
        tvTemperatureValue.setText("20°C");
        seekTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                temperatureValue = progress;
                tvTemperatureValue.setText(progress + "°C");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ArrayAdapter<String> soilAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, SOIL_OPTIONS);
        soilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSoilMoisture.setAdapter(soilAdapter);
        spinnerSoilMoisture.setSelection(1);
        spinnerSoilMoisture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                soilMoisture = SOIL_OPTIONS[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        ArrayAdapter<String> lightAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, LIGHT_OPTIONS);
        lightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLight.setAdapter(lightAdapter);
        spinnerLight.setSelection(1);
        spinnerLight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                lightLevel = LIGHT_OPTIONS[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnCalculate.setOnClickListener(v -> calculateWaterWithAI(0));
        btnAddReminder.setOnClickListener(v -> {
            if (lastWaterMl == 0) {
                Toast.makeText(this, "Сначала выполните расчёт", Toast.LENGTH_SHORT).show();
                return;
            }
            String pName = etPlantName.getText().toString().trim();
            Intent intent = new Intent(this, AddReminderActivity.class);
            intent.putExtra("plant_name", pName);
            startActivity(intent);
        });
    }

    private void calculateWaterWithAI(int keyIndex) {
        String sizeStr   = etPotSize.getText().toString().trim();
        String plantName = etPlantName.getText().toString().trim();

        if (plantName.isEmpty()) {
            shakeView(etPlantName);
            Toast.makeText(this, "Введите название растения", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sizeStr.isEmpty()) {
            shakeView(etPotSize);
            Toast.makeText(this, "Введите диаметр горшка", Toast.LENGTH_SHORT).show();
            return;
        }

        String prompt =
                "Ты — эксперт по комнатным растениям. " +
                        "Если название не является реальным растением, верни: " +
                        "{\"ml\":0,\"freq\":\"\",\"tip\":\"Растение не найдено. Проверьте название.\"}\n\n" +
                        "Сначала определи латинское научное название растения, " +
                        "затем рассчитай норму полива.\n\n" +
                        "Рассчитай норму полива и верни ТОЛЬКО JSON без лишнего текста и без блоков кода.\n\n" +
                        "Данные:\n" +
                        "- Растение: " + plantName + "\n" +
                        "- Диаметр горшка: " + sizeStr + " см\n" +
                        "- Температура: " + temperatureValue + "°C\n" +
                        "- Влажность почвы: " + soilMoisture + "\n" +
                        "- Освещение: " + lightLevel + "\n\n" +
                        "Ответ строго в формате:\n" +
                        "{\"ml\":число,\"freq\":\"текст частоты на русском\",\"tip\":\"совет 1-2 предложения на русском\"}\n\n" +
                        "ml — целое число (мл воды за один полив).\n" +
                        "freq — например: \"раз в 3–4 дня\" или \"раз в 2 недели\".\n" +
                        "tip — короткий практичный совет по поливу.";

        setLoading(true);
        currentKeyIndex = keyIndex;
        GenerativeModel model = new GenerativeModel(
                "gemini-2.5-flash",
                GEMINI_KEYS[keyIndex % GEMINI_KEYS.length]
        );
        GenerativeModelFutures futures = GenerativeModelFutures.from(model);

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        Executor mainExecutor = getMainExecutor();

        ListenableFuture<GenerateContentResponse> response = futures.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String raw = result.getText();
                parseAndShow(raw);
            }

            @Override
            public void onFailure(Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "";

                if ((msg.contains("429") || msg.contains("403") || msg.contains("Quota") || msg.contains("quota"))
                        && keyIndex + 1 < GEMINI_KEYS.length) {
                    runOnUiThread(() -> calculateWaterWithAI(keyIndex + 1));
                } else {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(WaterCalculatorActivity.this,
                                "Ошибка ИИ: " + msg, Toast.LENGTH_LONG).show();
                    });
                }
            }
        }, mainExecutor);
    }

    private void parseAndShow(String raw) {
        try {
            String clean = raw.replaceAll("```[a-z]*", "").replace("```", "").trim();

            int start = clean.indexOf('{');
            int end   = clean.lastIndexOf('}');
            if (start == -1 || end == -1) throw new Exception("No JSON found");

            String jsonStr = clean.substring(start, end + 1);
            JSONObject j = new JSONObject(jsonStr);

            int    rawMl = (int) Math.round(j.getDouble("ml") / 10) * 10;
            String freq = j.optString("freq", "");
            String tip  = j.optString("tip",  "");

            if (rawMl == 0) {
                String errorMsg = tip.isEmpty() ? "Растение не найдено. Проверьте название." : tip;
                runOnUiThread(() -> {
                    setLoading(false);

                    cardResult.setVisibility(View.GONE);
                    layoutTip.setVisibility(View.GONE);
                    btnAddReminder.setVisibility(View.GONE);
                    lastWaterMl = 0;
                    Toast.makeText(WaterCalculatorActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
                return;
            }

            int ml = Math.max(30, rawMl);
            lastWaterMl = ml;

            runOnUiThread(() -> {
                tvWaterResult.setText(ml + " мл");
                tvFrequency.setText(freq);
                tvWaterNote.setText(tip);

                if (cardResult.getVisibility() == View.GONE) {
                    cardResult.setVisibility(View.VISIBLE);
                    cardResult.setAlpha(0f);
                    cardResult.animate().alpha(1f).setDuration(350).start();
                }
                if (tip != null && !tip.isEmpty()) {
                    if (layoutTip.getVisibility() == View.GONE) {
                        layoutTip.setVisibility(View.VISIBLE);
                        layoutTip.setAlpha(0f);
                        layoutTip.animate().alpha(1f).setDuration(350).start();
                    }
                }
                if (btnAddReminder.getVisibility() == View.GONE) {
                    btnAddReminder.setVisibility(View.VISIBLE);
                    btnAddReminder.setAlpha(0f);
                    btnAddReminder.animate().alpha(1f).setDuration(350).start();
                }
                setLoading(false);
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                setLoading(false);
                Toast.makeText(this,
                        "Не удалось разобрать ответ ИИ. Попробуйте ещё раз.",
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void setLoading(boolean loading) {
        btnCalculate.setEnabled(!loading);
        btnCalculate.setText(loading ? "⏳ ИИ рассчитывает..." : "Посчитать количество воды");
    }

    private void showReminderSheet() {
        if (lastWaterMl == 0) {
            Toast.makeText(this, "Сначала выполните расчёт", Toast.LENGTH_SHORT).show();
            return;
        }

        String plantName = etPlantName.getText().toString().trim();
        if (plantName.isEmpty()) {
            shakeView(etPlantName);
            Toast.makeText(this, "Введите название растения", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_reminder, null);
        sheet.setContentView(view);

        TextView btnTime         = view.findViewById(R.id.btnSelectTime);
        View     btnSave         = view.findViewById(R.id.btnSaveReminder);
        LinearLayout layoutTypes = view.findViewById(R.id.layoutTaskTypes);

        EditText etPlantField = view.findViewById(R.id.etPlantName);
        etPlantField.setText(plantName);

        final String[] selectedTime = {""};
        final int[]    selectedDay  = {-1};

        buildTypeChips(layoutTypes, selectedDay);

        btnTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this, (tp, hour, min) -> {
                selectedTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hour, min);
                btnTime.setText(selectedTime[0]);
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        });

        btnSave.setOnClickListener(v -> {
            if (selectedDay[0] == -1) {
                Toast.makeText(this, "Выберите день недели", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedTime[0].isEmpty()) {
                shakeView(btnTime);
                return;
            }
            String timeLabel = DAY_LABELS[selectedDay[0]] + " " + selectedTime[0];
            saveReminderToFirebase(plantName, timeLabel, selectedDay[0], selectedTime[0]);
            sheet.dismiss();
        });

        sheet.show();
    }

    private void buildTypeChips(LinearLayout container, int[] selectedDay) {
        container.removeAllViews();
        int dp8  = dp(8);
        int dp6  = dp(6);
        int dp14 = dp(14);

        TextView[] chips = new TextView[DAY_LABELS.length];

        for (int i = 0; i < DAY_LABELS.length; i++) {
            final int idx = i;
            TextView chip = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp8, 0);
            chip.setLayoutParams(lp);
            chip.setText(DAY_LABELS[i]);
            chip.setTextSize(12f);
            chip.setPadding(dp14, dp6, dp14, dp6);
            chip.setTextColor(Color.parseColor("#1D5C3D"));
            chip.setBackgroundResource(R.drawable.bg_type_chip);
            chips[i] = chip;

            chip.setOnClickListener(v -> {
                selectedDay[0] = idx;
                for (TextView c : chips) c.setBackgroundResource(R.drawable.bg_type_chip);
                chip.setBackgroundResource(R.drawable.bg_type_chip_active);
            });

            container.addView(chip);
        }
    }

    private void saveReminderToFirebase(String plantName, String timeLabel,
                                        int dayIdx, String timeStr) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).child("reminders");

        String taskId = ref.push().getKey();
        if (taskId == null) {
            Toast.makeText(this, "Ошибка сохранения, попробуйте снова", Toast.LENGTH_SHORT).show();
            return;
        }

        TaskModel task = new TaskModel(taskId, plantName, "Полив", timeLabel, "");

        ref.child(taskId).setValue(task)
                .addOnSuccessListener(v -> {
                    setAlarm(dayIdx, timeStr, taskId);
                    Toast.makeText(this,
                            "✅ Напоминание: " + timeLabel,
                            Toast.LENGTH_SHORT).show();

                    btnAddReminder.setText("✓  Напоминание добавлено!");
                    btnAddReminder.setEnabled(false);
                    btnAddReminder.postDelayed(() -> {
                        btnAddReminder.setText("Добавить напоминание о поливе");
                        btnAddReminder.setEnabled(true);
                    }, 3000);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Ошибка сохранения: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void setAlarm(int dayIdx, String timeStr, String taskId) {
        try {
            String[] parts = timeStr.split(":");
            int hour   = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, DAY_OF_WEEK[dayIdx]);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            }

            Intent intent = new Intent(this, CareAlarmReceiver.class);
            intent.putExtra("TASK_ID", taskId);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this, taskId.hashCode(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } catch (Exception e) {
            android.util.Log.e("WaterCalc", "Alarm error: " + e.getMessage());
        }
    }

    private void shakeView(View v) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(v, "translationX", 0f, 10f);
        shake.setDuration(400);
        shake.setInterpolator(new CycleInterpolator(3));
        shake.start();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}