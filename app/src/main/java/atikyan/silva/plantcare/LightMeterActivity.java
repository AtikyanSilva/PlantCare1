package atikyan.silva.plantcare;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LightMeterActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private TextView tvLuxValue, tvLightAdvice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_meter);

        // Инициализация UI элементов
        tvLuxValue = findViewById(R.id.tvLuxValue);
        tvLightAdvice = findViewById(R.id.tvLightAdvice);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Кнопка назад
        btnBack.setOnClickListener(v -> finish());

        // Получаем доступ к датчикам телефона
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Проверка: есть ли в телефоне датчик света
        if (lightSensor == null) {
            tvLuxValue.setText("Нет датчика");
            tvLightAdvice.setText("К сожалению, ваш телефон не поддерживает измерение света.");
        }
    }

    // Этот метод срабатывает каждый раз, когда меняется освещение
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0]; // Получаем значение в Люксах

            // Обновляем цифру на экране
            tvLuxValue.setText(String.format("%.0f LUX", lux));

            // Даем совет в зависимости от уровня света
            updateAdvice(lux);
        }
    }

    private void updateAdvice(float lux) {
        if (lux < 500) {
            tvLightAdvice.setText("Слишком темно. Растение будет расти медленно.");
        } else if (lux >= 500 && lux < 2500) {
            tvLightAdvice.setText("Умеренный свет. Подходит для большинства домашних растений.");
        } else if (lux >= 2500 && lux < 10000) {
            tvLightAdvice.setText("Яркий свет. Идеально для светолюбивых цветов.");
        } else {
            tvLightAdvice.setText("Очень ярко! Осторожно, возможны ожоги на листьях.");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Здесь обычно ничего не пишем
    }

    // Важно: включаем датчик только когда приложение открыто
    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    // Важно: выключаем датчик, когда выходим из экрана, чтобы не тратить батарею
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}