package atikyan.silva.plantcare;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MsgViewHolder> {

    private final List<ChatMessage> messages;
    private final Context context;

    public ChatAdapter(List<ChatMessage> messages, Context context) {
        this.messages = messages;
        this.context  = context;
    }

    @NonNull
    @Override
    public MsgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_chat_message, parent, false);
        return new MsgViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgViewHolder h, int position) {
        ChatMessage msg = messages.get(position);

        if (msg.isTyping()) {
            // ── Typing indicator (three animated dots) ──────────────────
            h.tvMessage.setVisibility(View.GONE);
            h.senderName.setVisibility(View.VISIBLE);
            h.avatarContainer.setVisibility(View.VISIBLE);
            h.typingIndicator.setVisibility(View.VISIBLE);
            h.quickReplyContainer.setVisibility(View.GONE);

            // Simple sequential dot animation using the position cycle
            int phase = (int) ((System.currentTimeMillis() / 400) % 3);
            int activeColor   = ContextCompat.getColor(context, R.color.dark_green);
            int inactiveColor = ContextCompat.getColor(context, R.color.nav_inactive);

            android.content.res.ColorStateList activeList =
                    android.content.res.ColorStateList.valueOf(activeColor);
            android.content.res.ColorStateList inactiveList =
                    android.content.res.ColorStateList.valueOf(inactiveColor);

            h.dot1.setBackgroundTintList(phase == 0 ? activeList : inactiveList);
            h.dot2.setBackgroundTintList(phase == 1 ? activeList : inactiveList);
            h.dot3.setBackgroundTintList(phase == 2 ? activeList : inactiveList);

            alignLeft(h);
            return;
        }

        // ── Normal message ─────────────────────────────────────────────
        h.typingIndicator.setVisibility(View.GONE);
        h.quickReplyContainer.setVisibility(View.GONE);
        h.tvMessage.setVisibility(View.VISIBLE);
        h.tvMessage.setText(msg.getText());

        if (msg.isBot()) {
            // Flora bubble (left-aligned)
            h.avatarContainer.setVisibility(View.VISIBLE);
            h.senderName.setVisibility(View.VISIBLE);
            h.tvMessage.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.bubble_flora));
            h.tvMessage.setTextColor(
                    ContextCompat.getColor(context, R.color.text_main));
            alignLeft(h);
        } else {
            // User bubble (right-aligned)
            h.avatarContainer.setVisibility(View.GONE);
            h.senderName.setVisibility(View.GONE);
            h.tvMessage.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.bubble_user));
            h.tvMessage.setTextColor(0xFF1C2E1C);
            alignRight(h);
        }
    }

    // ─── Alignment helpers ────────────────────────────────────────────

    private void alignLeft(@NonNull MsgViewHolder h) {
        h.messageRoot.setGravity(Gravity.START | Gravity.TOP);
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) h.bubbleWrapper.getLayoutParams();
        lp.gravity = Gravity.START;
        h.bubbleWrapper.setLayoutParams(lp);
        h.avatarContainer.setVisibility(View.VISIBLE);
    }

    private void alignRight(@NonNull MsgViewHolder h) {
        h.messageRoot.setGravity(Gravity.END | Gravity.TOP);
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams) h.bubbleWrapper.getLayoutParams();
        lp.gravity = Gravity.END;
        h.bubbleWrapper.setLayoutParams(lp);
        h.avatarContainer.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ─── ViewHolder ───────────────────────────────────────────────────

    static class MsgViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageRoot, bubbleWrapper, typingIndicator, quickReplyContainer;
        FrameLayout  avatarContainer;
        TextView     tvMessage, senderName, chip1, chip2;
        View         dot1, dot2, dot3;

        MsgViewHolder(@NonNull View v) {
            super(v);
            messageRoot         = v.findViewById(R.id.messageRoot);
            bubbleWrapper       = v.findViewById(R.id.bubbleWrapper);
            avatarContainer     = v.findViewById(R.id.avatarContainer);
            tvMessage           = v.findViewById(R.id.tvMessage);
            senderName          = v.findViewById(R.id.senderName);
            typingIndicator     = v.findViewById(R.id.typingIndicator);
            quickReplyContainer = v.findViewById(R.id.quickReplyContainer);
            chip1               = v.findViewById(R.id.chip1);
            chip2               = v.findViewById(R.id.chip2);
            dot1                = v.findViewById(R.id.dot1);
            dot2                = v.findViewById(R.id.dot2);
            dot3                = v.findViewById(R.id.dot3);
        }
    }
}