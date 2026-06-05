package atikyan.silva.plantcare;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RemindersActivity extends AppCompatActivity {

    private RecyclerView    rvReminders;
    private View            layoutEmpty;
    private ReminderAdapter adapter;
    private final List<ReminderGroup> groups = new ArrayList<>();
    private DatabaseReference remindersRef;
    private ValueEventListener listener;

    private final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvReminders = findViewById(R.id.rvReminders);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        adapter = new ReminderAdapter(groups, this::onDoneClicked);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        rvReminders.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddReminder);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddReminderActivity.class)));

        loadReminders();
    }

    private void onDoneClicked(ReminderGroup group) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || group.activeTask == null || group.activeTask.id == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid())
                .child("reminders").child(group.activeTask.id);
        ref.child("isCompleted").setValue(true);
        ref.child("completedDate").setValue(today);

        group.activeTask.isCompleted   = true;
        group.activeTask.completedDate = today;
        int fromPos = groups.indexOf(group);
        if (fromPos >= 0) {
            groups.remove(fromPos);
            groups.add(group);
            int toPos = groups.size() - 1;
            adapter.notifyItemMoved(fromPos, toPos);
            adapter.notifyItemChanged(toPos);
        }
    }

    private void loadReminders() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { showEmpty(true); return; }

        remindersRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).child("reminders");

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<TaskModel> all = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TaskModel t = child.getValue(TaskModel.class);
                    if (t != null) all.add(t);
                }
                groups.clear();
                groups.addAll(buildGroups(all));
                adapter.notifyDataSetChanged();
                showEmpty(groups.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) { showEmpty(true); }
        };
        remindersRef.addValueEventListener(listener);
    }

    
    private List<ReminderGroup> buildGroups(List<TaskModel> all) {

        Map<String, List<TaskModel>> map = new LinkedHashMap<>();
        for (TaskModel t : all) {
            String key = (t.plantName != null ? t.plantName : "")
                    + "|" + (t.taskDescription != null ? t.taskDescription : "");
            if (!map.containsKey(key)) map.put(key, new ArrayList<>());
            map.get(key).add(t);
        }

        List<ReminderGroup> result = new ArrayList<>();

        for (Map.Entry<String, List<TaskModel>> entry : map.entrySet()) {
            List<TaskModel> tasks = entry.getValue();

            Collections.sort(tasks, (a, b) -> {
                if (a.date == null) return -1;
                if (b.date == null) return 1;
                return a.date.compareTo(b.date);
            });

            TaskModel activeTask = null;
            List<TaskModel> futureTasks = new ArrayList<>();

            for (TaskModel t : tasks) {
                if (t.date == null) continue;

                boolean isTodayOrPast = t.date.compareTo(today) <= 0;
                boolean isFuture      = t.date.compareTo(today) > 0;

                boolean completedToday = t.isCompleted && today.equals(t.completedDate);

                boolean completedPast  = t.isCompleted && !today.equals(t.completedDate);

                if (completedPast) continue;

                if (isTodayOrPast && activeTask == null) {
                    activeTask = t;
                } else if (isFuture) {
                    futureTasks.add(t);
                }
            }

            if (activeTask != null || !futureTasks.isEmpty()) {

                if (activeTask == null) {
                    activeTask = futureTasks.remove(0);
                }
                result.add(new ReminderGroup(activeTask, futureTasks));
            }
        }

        result.sort((a, b) -> {
            boolean aDone = a.activeTask != null && a.activeTask.isCompleted;
            boolean bDone = b.activeTask != null && b.activeTask.isCompleted;
            if (aDone != bDone) return aDone ? 1 : -1;
            return 0;
        });
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remindersRef != null && listener != null) {
            remindersRef.removeEventListener(listener);
        }
    }

    private void showEmpty(boolean empty) {
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvReminders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    static class ReminderGroup {
        TaskModel       activeTask;
        List<TaskModel> futureTasks;

        ReminderGroup(TaskModel activeTask, List<TaskModel> futureTasks) {
            this.activeTask  = activeTask;
            this.futureTasks = futureTasks;
        }
    }

    interface OnDoneListener { void onDone(ReminderGroup group); }

    static class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.VH> {

        private final List<ReminderGroup> items;
        private final OnDoneListener      doneListener;
        private final SimpleDateFormat    displayFmt =
                new SimpleDateFormat("d MMM", new Locale("ru"));
        private final SimpleDateFormat    parseFmt =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        private final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        ReminderAdapter(List<ReminderGroup> items, OnDoneListener doneListener) {
            this.items        = items;
            this.doneListener = doneListener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reminder, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ReminderGroup group  = items.get(position);
            TaskModel     active = group.activeTask;

            h.tvPlantName.setText(active.plantName != null ? active.plantName : "");
            h.tvTaskType.setText(active.taskDescription != null ? active.taskDescription : "Полив");

            String timeDisplay = active.time != null ? active.time : "";
            if (timeDisplay.contains(" · ")) timeDisplay = timeDisplay.substring(0, timeDisplay.indexOf(" · "));
            h.tvTime.setText(timeDisplay);

            boolean done    = active.isCompleted;
            boolean overdue = !done && isOverdue(active.date);

            if (done) {

                h.ivCheck.setImageResource(R.drawable.ic_check_filled);
                h.tvPlantName.setPaintFlags(h.tvPlantName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                h.tvPlantName.setTextColor(0xFFAAAAAA);
                h.tvTaskType.setTextColor(0xFFCCCCCC);
                h.tvTime.setTextColor(0xFFCCCCCC);
                h.tvBadge.setText("Выполнено");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_ok);
                h.tvBadge.setTextColor(0xFF9AAC90);
                h.ivCheck.setClickable(false);
            } else if (overdue) {
                resetTextStyles(h);
                h.ivCheck.setImageResource(R.drawable.ic_check_empty);
                h.tvBadge.setText("Просрочено");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_soon);
                h.tvBadge.setTextColor(0xFFE65100);
                h.ivCheck.setClickable(true);
            } else {
                resetTextStyles(h);
                h.ivCheck.setImageResource(R.drawable.ic_check_empty);
                h.tvBadge.setText("Ожидает");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_ok);
                h.tvBadge.setTextColor(0xFF5A8A6A);
                h.ivCheck.setClickable(true);
            }

            if (!group.futureTasks.isEmpty()) {
                StringBuilder sb = new StringBuilder("📅 Ещё: ");
                for (int i = 0; i < group.futureTasks.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(formatDisplayDate(group.futureTasks.get(i).date));
                }
                h.tvFutureDates.setText(sb.toString());
                h.tvFutureDates.setVisibility(View.VISIBLE);
            } else {
                h.tvFutureDates.setVisibility(View.GONE);
            }

            final int pos = position;
            h.ivCheck.setOnClickListener(v -> {
                int actualPos = h.getAdapterPosition();
                if (actualPos == RecyclerView.NO_ID) return;
                ReminderGroup g = items.get(actualPos);
                if (!g.activeTask.isCompleted) {
                    doneListener.onDone(g);
                }
            });
        }

        private void resetTextStyles(VH h) {
            h.tvPlantName.setPaintFlags(h.tvPlantName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvPlantName.setTextColor(0xFF1C2E1C);
            h.tvTaskType.setTextColor(0xFF8AA088);
            h.tvTime.setTextColor(0xFF1F6B3A);
        }

        private boolean isOverdue(String date) {
            if (date == null) return false;
            return date.compareTo(today) < 0;
        }

        private String formatDisplayDate(String date) {
            if (date == null) return "";
            try {
                Date d = parseFmt.parse(date);
                return d != null ? displayFmt.format(d) : date;
            } catch (Exception e) {
                return date;
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView  tvPlantName, tvTaskType, tvTime, tvBadge, tvFutureDates;
            ImageView ivCheck;

            VH(@NonNull View v) {
                super(v);
                tvPlantName  = v.findViewById(R.id.tvPlantName);
                tvTaskType   = v.findViewById(R.id.tvTaskType);
                tvTime       = v.findViewById(R.id.tvTime);
                tvBadge      = v.findViewById(R.id.tvBadge);
                tvFutureDates = v.findViewById(R.id.tvFutureDates);
                ivCheck      = v.findViewById(R.id.ivCheck);
            }
        }
    }
}
