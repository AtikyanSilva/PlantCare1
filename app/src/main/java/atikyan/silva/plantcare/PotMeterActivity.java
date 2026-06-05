package atikyan.silva.plantcare;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PotMeterActivity extends AppCompatActivity {

    private RecyclerView       rvForum;
    private EditText           etForumInput;
    private ImageButton        btnSend;
    private ProgressBar        pbForum;
    private ForumAdapter       forumAdapter;
    private List<ForumMessage> forumMessages;

    private DatabaseReference  forumRef;
    private FirebaseAuth       auth;
    private ValueEventListener forumListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pot_meter);

        auth     = FirebaseAuth.getInstance();
        forumRef = FirebaseDatabase.getInstance().getReference("pot_forum");

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvForum      = findViewById(R.id.rvForum);
        etForumInput = findViewById(R.id.etForumInput);
        btnSend      = findViewById(R.id.btnForumSend);
        pbForum      = findViewById(R.id.pbForum);

        forumMessages = new ArrayList<>();
        forumAdapter  = new ForumAdapter(forumMessages, this);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvForum.setLayoutManager(lm);
        rvForum.setAdapter(forumAdapter);

        btnSend.setOnClickListener(v -> sendMessage());

        ensureSignedIn();
    }

    private void ensureSignedIn() {
        if (auth.getCurrentUser() != null) {
            startListening();
        } else {
            auth.signInAnonymously()
                    .addOnSuccessListener(r -> startListening())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Не удалось войти в чат", Toast.LENGTH_SHORT).show());
        }
    }

    private void startListening() {
        pbForum.setVisibility(View.VISIBLE);

        forumListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                pbForum.setVisibility(View.GONE);
                forumMessages.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ForumMessage msg = child.getValue(ForumMessage.class);
                    if (msg != null) {
                        msg.setId(child.getKey());
                        forumMessages.add(msg);
                    }
                }
                forumAdapter.notifyDataSetChanged();
                if (!forumMessages.isEmpty()) {
                    rvForum.scrollToPosition(forumMessages.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                pbForum.setVisibility(View.GONE);
                Toast.makeText(PotMeterActivity.this,
                        "Ошибка загрузки: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        forumRef.limitToLast(200).addValueEventListener(forumListener);
    }

    private void sendMessage() {
        String text = etForumInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Пожалуйста, подождите...", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName;
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            displayName = user.getDisplayName();
        } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            displayName = user.getEmail().split("@")[0];
        } else {
            String uid = user.getUid();
            displayName = "Садовод_" + uid.substring(uid.length() - 4).toUpperCase();
        }

        ForumMessage msg = new ForumMessage(text, user.getUid(), displayName,
                System.currentTimeMillis());

        etForumInput.setEnabled(false);
        btnSend.setEnabled(false);

        forumRef.push().setValue(msg)
                .addOnSuccessListener(v -> {
                    etForumInput.setText("");
                    etForumInput.setEnabled(true);
                    btnSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    etForumInput.setEnabled(true);
                    btnSend.setEnabled(true);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (forumListener != null) {
            forumRef.limitToLast(200).removeEventListener(forumListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (forumListener == null && auth.getCurrentUser() != null) {
            startListening();
        }
    }
}
