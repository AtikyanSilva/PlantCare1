package atikyan.silva.plantcare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    // Создаем Handler и Runnable как поля класса, чтобы иметь к ним доступ везде
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = () -> {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView lottie = findViewById(R.id.lottie);
        
        // На всякий случай запускаем анимацию программно
        lottie.playAnimation();

        // Запускаем таймер перехода на 3 секунды
        handler.postDelayed(runnable, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Важный момент: если пользователь закроет приложение раньше 3 секунд,
        // мы отменяем переход, чтобы приложение не "вылетело" в фоне
        handler.removeCallbacks(runnable);
    }
}