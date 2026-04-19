package atikyan.silva.plantcare;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignUp, btnGuest;
    private ImageView ivTogglePassword;
    private FirebaseAuth mAuth; // Добавь эту строку под ImageView

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();// ID должен совпадать с тем, что в XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGuest = findViewById(R.id.btnGuest);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        // Если пользователь уже авторизован (и почта подтверждена или он гость) — сразу пускаем дальше
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isAnonymous() || currentUser.isEmailVerified()) {
                goToNextActivity();
            }
        }

        setupPasswordToggle();
        setupButtons();
    }

    private void setupPasswordToggle() {
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                ivTogglePassword.setAlpha(0.7f);
            } else {
                etPassword.setInputType(
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                ivTogglePassword.setAlpha(1f);
            }

            etPassword.setSelection(etPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });
    }

    private void setupButtons() {
        // ЛОГИКА ВХОДА
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (validateInputs(email, password)) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                // Проверяем, подтверждена ли почта ссылкой
                                if (user != null && user.isEmailVerified()) {
                                    goToNextActivity();
                                } else {
                                    Toast.makeText(this, "Пожалуйста, подтвердите Email в письме!", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            } else {
                                Toast.makeText(this, "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // ЛОГИКА РЕГИСТРАЦИИ
        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (validateInputs(email, password)) {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    // Отправляем ту самую ссылку на Мейл
                                    user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(this, "Регистрация успешна! Проверьте почту для подтверждения.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // ЛОГИКА ГОСТЯ (Анонимный вход)
        btnGuest.setOnClickListener(v -> {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Вход выполнен как гость", Toast.LENGTH_SHORT).show();
                            goToNextActivity();
                        } else {
                            Toast.makeText(this, "Ошибка гостевого входа", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }

        return hasLetter && hasDigit;
    }
    // Метод для перехода на следующий экран и закрытия текущего
    private void goToNextActivity() {
        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        startActivity(intent);
        finish();
    }

    // Объединенная валидация, чтобы не писать if-else по сто раз
    private boolean validateInputs(String email, String password) {
        if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        if (!isValidPassword(password)) {
            etPassword.setError("Password must be 8+ chars, letters & digits");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }
}