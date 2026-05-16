package atikyan.silva.plantcare;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class LightMeterActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private TextView tvLuxValue, tvLightAdvice, tvLightStatus;
    private CardView cardSunCore, cardAdvice;
    private View viewLevelThumb, viewStatusDot;
    private ImageButton btnBack;
    private LinearLayout layoutPlants;
    private View ivSunRays;

    // Анимация вращения лучей
    private ObjectAnimator raysAnimator;

    // Уровни освещённости
    private static final float[] LUX_THRESHOLDS = {500f, 2500f, 10000f};
    private static final int[] SUN_COLORS      = {0xFFFFD000, 0xFFFFD000, 0xFFFFC107, 0xFFFF9800};
    private static final int[] CARD_COLORS     = {0xFFE8F5E9, 0xFFDCEDC8, 0xFFFFF9C4, 0xFFFFEBEE};
    private static final int[] DOT_COLORS      = {0xFF6DB88A, 0xFF43A047, 0xFFF9A825, 0xFFE53935};
    private static final String[] STATUSES     = {
        "Слабое освещение",
        "Умеренный свет",
        "Яркий свет",
        "Очень ярко!"
    };
    private static final String[] ADVICES      = {
        "Растение будет расти медленно. Переместите его ближе к окну.",
        "Подходит для большинства домашних растений. Хорошее место у окна.",
        "Идеально для светолюбивых растений. Прямые лучи подойдут суккулентам.",
        "Осторожно — возможны ожоги листьев. Защитите чувствительные растения."
    };
    private static final String[][] PLANTS     = {
        {"Папоротник", "Сансевиерия", "Потос"},
        {"Фикус", "Монстера", "Хлорофитум"},
        {"Суккулент", "Герань", "Алоэ"},
        {"Кактус", "Агава", "Юкка"}
    };
    // Позиция бегунка (0..1) для каждого уровня
    private static final float[] THUMB_POSITIONS = {0.04f, 0.30f, 0.65f, 0.95f};

    private int currentLevel = -1; // чтобы не перерисовывать лишний раз

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_meter);

        // Привязка Views
        tvLuxValue   = findViewById(R.id.tvLuxValue);
        tvLightAdvice = findViewById(R.id.tvLightAdvice);
        tvLightStatus = findViewById(R.id.tvLightStatus);
        cardSunCore  = findViewById(R.id.cardSunCore);
        cardAdvice   = findViewById(R.id.cardAdvice);
        viewLevelThumb = findViewById(R.id.viewLevelThumb);
        viewStatusDot  = findViewById(R.id.viewStatusDot);
        layoutPlants   = findViewById(R.id.layoutPlants);
        ivSunRays      = findViewById(R.id.ivSunRays);
        btnBack        = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Запускаем вращение лучей
        startRaysAnimation();

        // Датчик света
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            tvLuxValue.setText("—");
            tvLightStatus.setText("Датчик не найден");
            tvLightAdvice.setText("К сожалению, ваш телефон не поддерживает измерение света.");
        } else {
            // Начальное состояние
            applyLevel(0, 0);
        }
    }

    private void startRaysAnimation() {
        raysAnimator = ObjectAnimator.ofFloat(ivSunRays, View.ROTATION, 0f, 360f);
        raysAnimator.setDuration(8000);
        raysAnimator.setRepeatCount(ValueAnimator.INFINITE);
        raysAnimator.setInterpolator(new LinearInterpolator());
        raysAnimator.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0];
            int level = getLevelIndex(lux);

            // Обновляем цифру всегда
            tvLuxValue.setText(String.format("%.0f", lux));

            // Обновляем цвета и тексты только при смене уровня
            if (level != currentLevel) {
                applyLevel(level, lux);
            }
        }
    }

    private int getLevelIndex(float lux) {
        for (int i = 0; i < LUX_THRESHOLDS.length; i++) {
            if (lux < LUX_THRESHOLDS[i]) return i;
        }
        return LUX_THRESHOLDS.length; // последний уровень
    }

    private void applyLevel(int level, float lux) {
        currentLevel = level;

        // Цвет ядра солнца
        cardSunCore.setCardBackgroundColor(SUN_COLORS[level]);

        // Карточка совета
        cardAdvice.setCardBackgroundColor(CARD_COLORS[level]);

        // Точка-индикатор
        viewStatusDot.getBackground().setTint(DOT_COLORS[level]);

        // Тексты
        tvLightStatus.setText(STATUSES[level]);
        tvLightAdvice.setText(ADVICES[level]);

        // Бегунок на полоске
        positionThumb(THUMB_POSITIONS[level]);

        // Чипы растений
        updatePlantChips(PLANTS[level]);
    }

    private void positionThumb(float fraction) {
        // Ждём, пока View получит размер, затем двигаем бегунок
        viewLevelThumb.post(() -> {
            View barView = viewLevelThumb.getParent() instanceof View
                    ? (View) viewLevelThumb.getParent() : null;
            if (barView == null) return;
            int barWidth  = barView.getWidth();
            int thumbWidth = viewLevelThumb.getWidth();
            float targetX = fraction * (barWidth - thumbWidth);
            viewLevelThumb.animate().translationX(targetX).setDuration(300).start();
        });
    }

    private void updatePlantChips(String[] plants) {
        layoutPlants.removeAllViews();
        int dp8  = dpToPx(8);
        int dp6  = dpToPx(6);
        int dp12 = dpToPx(12);
        int dp5  = dpToPx(5);

        for (String plant : plants) {
            // Chip-контейнер
            LinearLayout chip = new LinearLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp5, 0, dp5, 0);
            chip.setLayoutParams(lp);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp12, dp6, dp12, dp6);
            chip.setBackgroundResource(R.drawable.bg_plant_chip);

            // Название
            TextView tv = new TextView(this);
            tv.setText(plant);
            tv.setTextColor(Color.parseColor("#1D5C3D"));
            tv.setTextSize(12f);

            chip.addView(tv);
            layoutPlants.addView(chip);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (raysAnimator != null && !raysAnimator.isRunning()) {
            raysAnimator.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (raysAnimator != null) {
            raysAnimator.pause();
        }
    }
}
