package atikyan.silva.plantcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForumAdapter extends RecyclerView.Adapter<ForumAdapter.ForumViewHolder> {

    private final List<ForumMessage> messages;
    private final Context context;
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd MMM, HH:mm", new Locale("ru"));

    public ForumAdapter(List<ForumMessage> messages, Context context) {
        this.messages = messages;
        this.context  = context;
    }

    @NonNull
    @Override
    public ForumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_forum_message, parent, false);
        return new ForumViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumViewHolder h, int position) {
        ForumMessage msg = messages.get(position);

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        boolean isMine = msg.getAuthorId() != null && msg.getAuthorId().equals(currentUid);

        h.tvAuthor.setText(isMine ? "Вы" : (msg.getAuthorName() != null ? msg.getAuthorName() : "Аноним"));
        h.tvText.setText(msg.getText());
        h.tvTime.setText(SDF.format(new Date(msg.getTimestamp())));

        if (isMine) {
            h.tvAuthor.setTextColor(0xFF1D5C3D);
            h.tvText.setBackgroundResource(R.drawable.bg_forum_bubble_mine);
        } else {
            h.tvAuthor.setTextColor(0xFF5C7260);
            h.tvText.setBackgroundResource(R.drawable.bg_forum_bubble_other);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ForumViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvText, tvTime;

        ForumViewHolder(@NonNull View v) {
            super(v);
            tvAuthor = v.findViewById(R.id.tvForumAuthor);
            tvText   = v.findViewById(R.id.tvForumText);
            tvTime   = v.findViewById(R.id.tvForumTime);
        }
    }
}
