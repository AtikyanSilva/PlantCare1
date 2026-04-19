package atikyan.silva.plantcare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity3 extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main3);

        // Настройка отступов для безрамочного экрана

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Снизу ставим строго 0
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

// 2. Находим саму панель и ЗАПРЕЩАЕМ ей добавлять отступы
        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setPadding(0, 0, 0, 0); // Обнуляем программно на всякий случай

// 3. Важный трюк: поглощаем инсеты именно для навигации
        ViewCompat.setOnApplyWindowInsetsListener(bottomAppBar, (v, insets) -> {
            return WindowInsetsCompat.CONSUMED;
        });

        // 1. Инициализация нижней навигации
        setupNavigation();

        // 2. Инициализация карточек инструментов
        setupCards();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Устанавливаем фокус на иконку инструментов
        bottomNav.setSelectedItemId(R.id.nav_instruments);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Переход назад на главный экран (MainActivity2)
                Intent intent = new Intent(MainActivity3.this, MainActivity2.class);
                // Флаг REORDER_TO_FRONT просто выводит уже открытую MainActivity2 на передний план
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            if (id == R.id.nav_instruments) {
                return true; // Мы уже здесь
            }

            // Можно добавить обработку для других кнопок здесь
            return false;
        });
    }

    private void setupCards() {
        // Находим карточки по ID, которые ты указала в XML
        CardView cardReminder = findViewById(R.id.cardReminder);
        CardView cardLight = findViewById(R.id.cardLight);
        CardView cardWater = findViewById(R.id.cardWater);
        CardView cardPot = findViewById(R.id.cardPot);

        // Пример обработки нажатия на карточку "График"
        cardReminder.setOnClickListener(v -> {
            Toast.makeText(this, "Функция графика в разработке", Toast.LENGTH_SHORT).show();
        });

        // Пример для "Света"
        cardLight.setOnClickListener(v -> {
            Toast.makeText(this, "Датчик люксметра скоро будет доступен", Toast.LENGTH_SHORT).show();
        });

        // Аналогично можно добавить для остальных
    }
}