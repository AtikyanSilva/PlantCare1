package atikyan.silva.plantcare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity3 extends AppCompatActivity {

    private static final int TAB_HOME        = 0;
    private static final int TAB_INSTRUMENTS = 1;
    private static final int TAB_BOTANIST    = 2;
    private static final int TAB_GARDEN      = 3;

    private int currentTab = TAB_INSTRUMENTS;

    private LinearLayout navHome, navInstruments, navBotanist, navGarden;
    private ImageView iconHome, iconInstruments, iconBotanist, iconGarden;
    private TextView labelHome, labelInstruments, labelBotanist, labelGarden;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main3);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        CardView cardLightMeter = findViewById(R.id.cardLightMeter);
        cardLightMeter.setOnClickListener(v ->
                startActivity(new Intent(this, LightMeterActivity.class)));

        CardView cardWaterCalc = findViewById(R.id.cardWaterCalc);
        cardWaterCalc.setOnClickListener(v ->
                startActivity(new Intent(this, WaterCalculatorActivity.class)));

        CardView cardCalendar = findViewById(R.id.cardCalendar);
        cardCalendar.setOnClickListener(v ->
                startActivity(new Intent(this, RemindersActivity.class)));

        CardView cardPotMeter = findViewById(R.id.cardPotMeter);
        cardPotMeter.setOnClickListener(v ->
                startActivity(new Intent(this, PotMeterActivity.class)));

        CardView cardCompass = findViewById(R.id.cardCompass);
        cardCompass.setOnClickListener(v ->
                startActivity(new Intent(this, CompassActivity.class)));

        CardView cardGrowthForecast = findViewById(R.id.cardGrowthForecast);
        cardGrowthForecast.setOnClickListener(v ->
                startActivity(new Intent(this, GrowthForecastActivity.class)));

        setupNavigation();
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

        FloatingActionButton fabCamera = findViewById(R.id.fabCamera);
        fabCamera.setOnClickListener(v ->
                startActivity(new Intent(this, PlantRecognizeActivity.class)));

        setActiveTab(TAB_INSTRUMENTS);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        navInstruments.setOnClickListener(v -> setActiveTab(TAB_INSTRUMENTS));

        navBotanist.setOnClickListener(v -> {
            Intent intent1 = new Intent(this, AiBotanistActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent1);
        });

        navGarden.setOnClickListener(v -> {
            Intent intent2 = new Intent(this, MyGardenActivity.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent2);
        });
    }

    private void setActiveTab(int tab) {
        currentTab = tab;
        int activeColor   = ContextCompat.getColor(this, R.color.nav_active);
        int inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive);

        iconHome.setColorFilter(inactiveColor);
        iconInstruments.setColorFilter(inactiveColor);
        iconBotanist.setColorFilter(inactiveColor);
        iconGarden.setColorFilter(inactiveColor);

        labelHome.setTextColor(inactiveColor);
        labelInstruments.setTextColor(inactiveColor);
        labelBotanist.setTextColor(inactiveColor);
        labelGarden.setTextColor(inactiveColor);

        switch (tab) {
            case TAB_HOME:
                iconHome.setColorFilter(activeColor);
                labelHome.setTextColor(activeColor);
                break;
            case TAB_INSTRUMENTS:
                iconInstruments.setColorFilter(activeColor);
                labelInstruments.setTextColor(activeColor);
                break;
            case TAB_BOTANIST:
                iconBotanist.setColorFilter(activeColor);
                labelBotanist.setTextColor(activeColor);
                break;
            case TAB_GARDEN:
                iconGarden.setColorFilter(activeColor);
                labelGarden.setTextColor(activeColor);
                break;
        }
    }
}