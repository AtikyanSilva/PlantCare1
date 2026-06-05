package atikyan.silva.plantcare;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // ── Список аватарок — имена drawable файлов ──────────────────
    public static final List<String> AVATARS = Arrays.asList(
            "avatar_gnome_green",
            "avatar_fairy_pink",
            "avatar_gnome_blue",
            "avatar_fairy_yellow",
            "avatar_wizard",
            "avatar_gnome_orange",
            "avatar_fairy_purple",
            "avatar_gnome_plant",
            "avatar_fairy_blue",
            "avatar_forest_spirit",
            "avatar_gnome_brown",
            "avatar_fairy_light",
            "avatar_gnome_red",
            "avatar_fairy_earth",
            "avatar_mushroom"
    );

    // ── UI ───────────────────────────────────────────────────────
    private ImageView ivAvatarDisplay;
    private EditText etFirstName;
    private EditText etLastName;
    private TextView tvEmail;
    private CardView btnSave;
    private CardView btnLogout;
    private TextView tvSaveText;

    // ── Firebase ─────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String uid;

    // ── Состояние ────────────────────────────────────────────────
    private String selectedAvatar = "avatar_gnome_green";
    private boolean isGuest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        isGuest = MainActivity.GUEST_EMAIL.equals(currentUser.getEmail());
        uid = isGuest ? null : currentUser.getUid();

        bindViews();
        loadUserProfile();
        setupListeners();
    }

    private void bindViews() {
        ivAvatarDisplay = findViewById(R.id.ivAvatarDisplay);
        etFirstName     = findViewById(R.id.etFirstName);
        etLastName      = findViewById(R.id.etLastName);
        tvEmail         = findViewById(R.id.tvEmail);
        btnSave         = findViewById(R.id.btnSave);
        btnLogout       = findViewById(R.id.btnLogout);
        tvSaveText      = findViewById(R.id.tvSaveText);
    }

    private void loadUserProfile() {
        if (isGuest) {
            tvEmail.setText("Гость");
            etFirstName.setEnabled(false);
            etLastName.setEnabled(false);
            btnSave.setVisibility(android.view.View.GONE);
            // Стиль кнопки — зелёный "Войти в аккаунт"
            btnLogout.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F0E6"));
            findTextViewIn(btnLogout).setTextColor(android.graphics.Color.parseColor("#1F6B3A"));
            findTextViewIn(btnLogout).setText("Войти в аккаунт");
            return;
        }
        tvEmail.setText(currentUser.getEmail());

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        String lastName  = doc.getString("lastName");
                        String avatar    = doc.getString("avatar");

                        if (!TextUtils.isEmpty(firstName)) etFirstName.setText(firstName);
                        if (!TextUtils.isEmpty(lastName))  etLastName.setText(lastName);
                        if (!TextUtils.isEmpty(avatar)) {
                            selectedAvatar = avatar;
                            applyAvatar(avatar);
                        }
                    } else {
                        String displayName = currentUser.getDisplayName();
                        if (!TextUtils.isEmpty(displayName)) {
                            String[] parts = displayName.split(" ", 2);
                            etFirstName.setText(parts[0]);
                            if (parts.length > 1) etLastName.setText(parts[1]);
                        }
                    }
                });
    }

    // Применяем аватарку по имени drawable
    private void applyAvatar(String drawableName) {
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            ivAvatarDisplay.setImageResource(resId);
        }
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ivAvatarDisplay.setOnClickListener(v -> showAvatarPicker());
        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void showAvatarPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_avatar_picker);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
        dialog.getWindow().setAttributes(lp);

        GridView gridAvatars = dialog.findViewById(R.id.gridAvatars);
        AvatarPickerAdapter adapter = new AvatarPickerAdapter(this, AVATARS, selectedAvatar);
        gridAvatars.setAdapter(adapter);

        gridAvatars.setOnItemClickListener((parent, view, position, id) -> {
            selectedAvatar = AVATARS.get(position);
            applyAvatar(selectedAvatar);
            adapter.setSelected(selectedAvatar);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("Введите имя");
            etFirstName.requestFocus();
            return;
        }

        tvSaveText.setText("Сохраняю...");
        btnSave.setEnabled(false);

        String fullName = firstName + (TextUtils.isEmpty(lastName) ? "" : " " + lastName);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();
        currentUser.updateProfile(profileUpdates);

        Map<String, Object> data = new HashMap<>();
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("avatar", selectedAvatar);
        data.put("email", currentUser.getEmail());

        db.collection("users").document(uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Профиль сохранён ✅", Toast.LENGTH_SHORT).show();
                    tvSaveText.setText("Сохранить");
                    btnSave.setEnabled(true);
                    setResult(RESULT_OK);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvSaveText.setText("Сохранить");
                    btnSave.setEnabled(true);
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private TextView findTextViewIn(android.view.ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof TextView) return (TextView) child;
            if (child instanceof android.view.ViewGroup) {
                TextView found = findTextViewIn((android.view.ViewGroup) child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
