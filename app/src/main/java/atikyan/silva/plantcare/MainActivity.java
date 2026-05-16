package atikyan.silva.plantcare;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.util.Patterns;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnLogin, btnSignUp, btnGuest;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private RelativeLayout layoutConfirmPassword;
    private FirebaseAuth mAuth;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGuest = findViewById(R.id.btnGuest);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isAnonymous() || currentUser.isEmailVerified()) {
                goToNextActivity();
            }
        }

        setupPasswordToggles();
        setupButtons();
    }

    private void setupPasswordToggles() {
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setAlpha(0.7f);
            } else {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setAlpha(1f);
            }
            etPassword.setSelection(etPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleConfirmPassword.setAlpha(0.7f);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleConfirmPassword.setAlpha(1f);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        });
    }

    private void setupButtons() {

        // ЛОГИКА ВХОДА
        btnLogin.setOnClickListener(v -> {
            // Скрываем поле подтверждения при входе
            layoutConfirmPassword.setVisibility(View.GONE);
            etConfirmPassword.setText("");

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (!isValidEmail(email)) {
                etEmail.setError("Enter a valid email");
                etEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Enter your password");
                etPassword.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
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
        });

        // ЛОГИКА РЕГИСТРАЦИИ
        btnSignUp.setOnClickListener(v -> {
            // Показываем поле подтверждения
            layoutConfirmPassword.setVisibility(View.VISIBLE);

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (!isValidEmail(email)) {
                etEmail.setError("Enter a valid email");
                etEmail.requestFocus();
                return;
            }

            String passwordError = getPasswordError(password);
            if (passwordError != null) {
                etPassword.setError(passwordError);
                etPassword.requestFocus();
                return;
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.setError("Повторите пароль");
                etConfirmPassword.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Пароли не совпадают");
                etConfirmPassword.requestFocus();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        Toast.makeText(this, "Регистрация успешна! Проверьте почту для подтверждения.", Toast.LENGTH_LONG).show();
                                        layoutConfirmPassword.setVisibility(View.GONE);
                                        etConfirmPassword.setText("");
                                        etPassword.setText("");
                                        etEmail.setText("");
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // ЛОГИКА ГОСТЯ
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

    /**
     * Проверяет сложность пароля, возвращает текст ошибки или null если всё ок.
     * Ошибка показывается прямо на поле (в "уголке").
     */
    private String getPasswordError(String password) {
        if (password.length() < 8)
            return "Минимум 8 символов, загл. буква, цифра и спецсимвол (!@#$%...)";
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        String specials = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specials.indexOf(c) >= 0) hasSpecial = true;
        }
        if (!hasUpper) return "Нужна заглавная буква (A-Z), цифра и спецсимвол (!@#$%...)";
        if (!hasLower) return "Нужна строчная буква (a-z), цифра и спецсимвол (!@#$%...)";
        if (!hasDigit) return "Нужна цифра (0-9) и спецсимвол (!@#$%...)";
        if (!hasSpecial) return "Нужен спецсимвол: !@#$%^&*()_+-= и т.д.";
        return null;
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void goToNextActivity() {
        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        startActivity(intent);
        finish();
    }
}
