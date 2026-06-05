package atikyan.silva.plantcare;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddReminderActivity extends AppCompatActivity {

    private static final int[] DAY_CONSTANTS = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };
    private static final String[] PERIOD_OPTIONS = {"день", "неделя", "месяц"};

    private EditText     etReminderName;
    private TextView     tvSelectedTime;
    private LinearLayout layoutTimePicker;

    private TextView     tabRepeat, tabOnce;
    private LinearLayout sectionRepeat, sectionOnce;
    private boolean isRepeatMode = true;

    private TextView     tvIntervalValue, tvPeriodLabel, btnIntervalMinus, btnIntervalPlus;
    private LinearLayout layoutPeriodPicker;
    private int intervalValue = 1;
    private int periodIndex   = 1;

    private TextView[] dayChips;
    private boolean[]  selectedDays = {true, false, true, false, true, false, false};

    private LinearLayout layoutSpecificDates;
    private TextView     tvSelectedDatesLabel;
    private final List<Calendar> specificDates = new ArrayList<>();

    private LinearLayout layoutOnceDate;
    private TextView     tvOnceDate;
    private Calendar     onceDate;

    private Button btnSaveReminder;
    private int selectedHour   = 9;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        onceDate = Calendar.getInstance();

        String extraName = getIntent().getStringExtra("plant_name");
        bindViews();
        if (extraName != null && !extraName.isEmpty()) {
            etReminderName.setText(extraName);
        }
        setupDayChips();
        setupToggle();
        setupInterval();
        setupDateRows();
        setupListeners();
    }

    private void bindViews() {
        etReminderName   = findViewById(R.id.etReminderName);
        tvSelectedTime   = findViewById(R.id.tvSelectedTime);
        layoutTimePicker = findViewById(R.id.layoutTimePicker);
        btnSaveReminder  = findViewById(R.id.btnSaveReminder);

        tabRepeat     = findViewById(R.id.tabRepeat);
        tabOnce       = findViewById(R.id.tabOnce);
        sectionRepeat = findViewById(R.id.sectionRepeat);
        sectionOnce   = findViewById(R.id.sectionOnce);

        tvIntervalValue    = findViewById(R.id.tvIntervalValue);
        tvPeriodLabel      = findViewById(R.id.tvPeriodLabel);
        btnIntervalMinus   = findViewById(R.id.btnIntervalMinus);
        btnIntervalPlus    = findViewById(R.id.btnIntervalPlus);
        layoutPeriodPicker = findViewById(R.id.layoutPeriodPicker);

        layoutSpecificDates  = findViewById(R.id.layoutSpecificDates);
        tvSelectedDatesLabel = findViewById(R.id.tvSelectedDatesLabel);

        layoutOnceDate = findViewById(R.id.layoutOnceDate);
        tvOnceDate     = findViewById(R.id.tvOnceDate);

        dayChips = new TextView[]{
                findViewById(R.id.chipMon), findViewById(R.id.chipTue),
                findViewById(R.id.chipWed), findViewById(R.id.chipThu),
                findViewById(R.id.chipFri), findViewById(R.id.chipSat),
                findViewById(R.id.chipSun)
        };
    }

    private void setupDayChips() {
        for (int i = 0; i < dayChips.length; i++) {
            dayChips[i].setSelected(selectedDays[i]);
            final int idx = i;
            dayChips[i].setOnClickListener(v -> {
                selectedDays[idx] = !selectedDays[idx];
                dayChips[idx].setSelected(selectedDays[idx]);
            });
        }
    }

    private void setupToggle() {
        tabRepeat.setOnClickListener(v -> setRepeatMode(true));
        tabOnce.setOnClickListener(v   -> setRepeatMode(false));
        setRepeatMode(true);
    }

    private void setRepeatMode(boolean repeat) {
        isRepeatMode = repeat;
        tabRepeat.setBackgroundResource(repeat ? R.drawable.bg_tab_active : 0);
        tabRepeat.setTextColor(repeat ? 0xFF2D7040 : 0xFF9AAC90);
        tabOnce.setBackgroundResource(repeat ? 0 : R.drawable.bg_tab_active);
        tabOnce.setTextColor(repeat ? 0xFF9AAC90 : 0xFF2D7040);
        sectionRepeat.setVisibility(repeat ? View.VISIBLE : View.GONE);
        sectionOnce.setVisibility(repeat   ? View.GONE    : View.VISIBLE);
    }

    private void setupInterval() {
        updateIntervalDisplay();
        btnIntervalMinus.setOnClickListener(v -> {
            if (intervalValue > 1) { intervalValue--; updateIntervalDisplay(); }
        });
        btnIntervalPlus.setOnClickListener(v -> {
            intervalValue++; updateIntervalDisplay();
        });
        layoutPeriodPicker.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Период")
                        .setItems(PERIOD_OPTIONS, (d, which) -> {
                            periodIndex = which;
                            updateIntervalDisplay();
                        }).show());
    }

    private void updateIntervalDisplay() {
        tvIntervalValue.setText(String.valueOf(intervalValue));
        tvPeriodLabel.setText(getPeriodLabel(intervalValue, periodIndex));
    }

    private String getPeriodLabel(int n, int idx) {
        String[][] forms = {
                {"день", "дня", "дней"},
                {"неделя", "недели", "недель"},
                {"месяц", "месяца", "месяцев"}
        };
        String[] arr = forms[idx];
        int m100 = n % 100, m10 = n % 10;
        if (m100 >= 11 && m100 <= 19) return arr[2];
        if (m10 == 1) return arr[0];
        if (m10 >= 2 && m10 <= 4) return arr[1];
        return arr[2];
    }

    private void setupDateRows() {
        layoutSpecificDates.setOnClickListener(v -> showSpecificDatesPicker());
        layoutOnceDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this,
                    (dp, y, m, d) -> {
                        onceDate.set(y, m, d);
                        tvOnceDate.setText(formatDate(onceDate));
                    },
                    onceDate.get(Calendar.YEAR),
                    onceDate.get(Calendar.MONTH),
                    onceDate.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis());
            dpd.show();
        });
    }

    private void showSpecificDatesPicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (dp, y, m, d) -> {
            for (Calendar c : specificDates) {
                if (c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                        && c.get(Calendar.DAY_OF_MONTH) == d) {
                    Toast.makeText(this, "Эта дата уже выбрана", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Calendar chosen = Calendar.getInstance();
            chosen.set(y, m, d);
            specificDates.add(chosen);
            updateSpecificDatesLabel();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateSpecificDatesLabel() {
        int n = specificDates.size();
        tvSelectedDatesLabel.setText("Выбрано: " + n + " " + datesPluralForm(n));
    }

    private String datesPluralForm(int n) {
        int m10 = n % 10, m100 = n % 100;
        if (m100 >= 11 && m100 <= 19) return "дат";
        if (m10 == 1) return "дата";
        if (m10 >= 2 && m10 <= 4) return "даты";
        return "дат";
    }

    private String formatDate(Calendar cal) {
        return new SimpleDateFormat("d MMM yyyy г.", new Locale("ru")).format(cal.getTime());
    }

    private void setupListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        layoutTimePicker.setOnClickListener(v -> openTimePicker());
        tvSelectedTime.setOnClickListener(v   -> openTimePicker());
        btnSaveReminder.setOnClickListener(v  -> saveReminder());
    }

    private void openTimePicker() {
        new TimePickerDialog(this, (view, h, m) -> {
            selectedHour   = h;
            selectedMinute = m;
            tvSelectedTime.setText(String.format("%02d:%02d", h, m));
        }, selectedHour, selectedMinute, true).show();
    }

    private void saveReminder() {
        String name = etReminderName.getText().toString().trim();
        if (name.isEmpty()) {
            etReminderName.setError("Введите название");
            etReminderName.requestFocus();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int baseCode = (int) System.currentTimeMillis();
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if (!isRepeatMode) {
            Calendar cal = (Calendar) onceDate.clone();
            cal.set(Calendar.HOUR_OF_DAY, selectedHour);
            cal.set(Calendar.MINUTE, selectedMinute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                Toast.makeText(this, "Выберите будущую дату и время", Toast.LENGTH_SHORT).show();
                return;
            }
            String timeLabel = String.format("%02d:%02d · %s", selectedHour, selectedMinute, formatDate(onceDate));
            scheduleAndSave(am, user.getUid(), name, timeLabel,
                    dateFmt.format(onceDate.getTime()), baseCode,
                    cal.getTimeInMillis(), false, 0);

        } else if (!specificDates.isEmpty()) {
            for (int i = 0; i < specificDates.size(); i++) {
                Calendar cal = (Calendar) specificDates.get(i).clone();
                cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                cal.set(Calendar.MINUTE, selectedMinute);
                cal.set(Calendar.SECOND, 0);
                String timeLabel = String.format("%02d:%02d · %s",
                        selectedHour, selectedMinute, formatDate(specificDates.get(i)));
                long triggerAt = cal.getTimeInMillis();
                if (triggerAt > System.currentTimeMillis()) {
                    scheduleAndSave(am, user.getUid(), name, timeLabel,
                            dateFmt.format(specificDates.get(i).getTime()),
                            baseCode + 100 + i, triggerAt, false, 0);
                }
            }
        } else {
            boolean anyDay = false;
            for (boolean d : selectedDays) if (d) { anyDay = true; break; }
            if (!anyDay) {
                Toast.makeText(this, "Выберите хотя бы один день", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
            long repeatInterval = getRepeatInterval();
            for (int i = 0; i < selectedDays.length; i++) {
                if (!selectedDays[i]) continue;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, DAY_CONSTANTS[i]);
                cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                cal.set(Calendar.MINUTE, selectedMinute);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 7);
                }
                String timeLabel = "каждые " + intervalValue + " "
                        + getPeriodLabel(intervalValue, periodIndex)
                        + " · " + dayNames[i]
                        + " " + String.format("%02d:%02d", selectedHour, selectedMinute);
                scheduleAndSave(am, user.getUid(), name, timeLabel,
                        dateFmt.format(cal.getTime()),
                        baseCode + i, cal.getTimeInMillis(),
                        true, repeatInterval);
            }
        }

        Toast.makeText(this, "Напоминание сохранено!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void scheduleAndSave(AlarmManager am, String uid, String plantName,
                                  String timeLabel, String date, int code,
                                  long triggerAt, boolean repeating, long interval) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("reminders");
        String taskId = ref.push().getKey();
        if (taskId == null) return;

        TaskModel task = new TaskModel(taskId, plantName, "Полив", timeLabel, date);
        ref.child(taskId).setValue(task);

        Intent intent = new Intent(this, CareAlarmReceiver.class);
        intent.putExtra("reminder_name", plantName);
        intent.putExtra("TASK_ID", taskId);
        PendingIntent pi = PendingIntent.getBroadcast(this, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am == null) return;
        if (repeating) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, triggerAt, interval, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private long getRepeatInterval() {
        switch (periodIndex) {
            case 0: return (long) intervalValue * AlarmManager.INTERVAL_DAY;
            case 2: return (long) intervalValue * 30L * AlarmManager.INTERVAL_DAY;
            default: return (long) intervalValue * AlarmManager.INTERVAL_DAY * 7;
        }
    }
}
