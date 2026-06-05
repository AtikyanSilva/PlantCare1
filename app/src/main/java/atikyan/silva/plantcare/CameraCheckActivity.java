package atikyan.silva.plantcare;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CameraCheckActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String taskId; // ID задачи, которую нужно отметить выполненной

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_check);

        // Получаем ID задачи из уведомления (ключ "TASK_ID")
        taskId = getIntent().getStringExtra("TASK_ID");

        // 1. Сразу запускаем звук
        startAlarmSound();

        // 2. Кнопка для запуска камеры
        Button btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> dispatchTakePictureIntent());

        // Кнопка "Отложить"
        Button btnSnooze = findViewById(R.id.btnSnooze);
        btnSnooze.setOnClickListener(v -> {
            stopAlarmSound();
            finish();
        });
    }

    private void startAlarmSound() {
        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // ФОТО СДЕЛАНО!
            stopAlarmSound();

            // Убираем уведомление из шторки
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(1);

            // Помечаем задачу выполненной в Firebase
            markTaskCompleted();

            Toast.makeText(this, "Отлично! Растение довольно.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void markTaskCompleted() {
        if (taskId == null || taskId.isEmpty()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("tasks")
                .child(taskId)
                .child("isCompleted");

        taskRef.setValue(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }
}
