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

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignUp, btnGuest, btnNext;
    private ImageView ivTogglePassword;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        btnNext = findViewById(R.id.btnNextActivity); // ID должен совпадать с тем, что в XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGuest = findViewById(R.id.btnGuest);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

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

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (!isValidEmail(email)) {
                etEmail.setError("Enter a valid email");
                etEmail.requestFocus();
                return;
            }

            if (!isValidPassword(password)) {
                etPassword.setError("Password must be 8+ chars, letters & digits");
                etPassword.requestFocus();
                return;
            }
            Toast.makeText(this, "Validation passed ✅", Toast.LENGTH_SHORT).show();
        });

        btnSignUp.setOnClickListener(v ->
                Toast.makeText(this, "Sign Up clicked", Toast.LENGTH_SHORT).show()
        );

        btnGuest.setOnClickListener(v ->
                Toast.makeText(this, "Guest mode", Toast.LENGTH_SHORT).show()
        );
        btnNext.setOnClickListener(v -> {
            // Создаем "намерение" перейти во вторую активити
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
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
}