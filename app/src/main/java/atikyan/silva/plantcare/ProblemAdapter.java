package atikyan.silva.plantcare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class ProblemAdapter extends RecyclerView.Adapter<ProblemAdapter.ViewHolder> {

    private List<Problem> problemList;

    public ProblemAdapter(List<Problem> problemList) {
        this.problemList = problemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_problem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Problem problem = problemList.get(position);
        holder.tvTitle.setText(problem.getTitle());
        holder.ivImage.setImageResource(problem.getImageResId());
        holder.tvDescription.setText(problem.getDescription());
        holder.tvTip.setText(problem.getTreatment());

        holder.itemView.setOnClickListener(v -> showProblemDialog(v.getContext(), problem));
    }

    @Override
    public int getItemCount() {
        return problemList.size();
    }

    private void showProblemDialog(Context context, Problem problem) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_problem, null);

        ImageView img = view.findViewById(R.id.dialogImage);
        TextView title = view.findViewById(R.id.dialogTitle);
        TextView desc = view.findViewById(R.id.dialogDescription);
        TextView treatment = view.findViewById(R.id.dialogTreatment);

        img.setImageResource(problem.getImageResId());
        title.setText(problem.getTitle());
        desc.setText(problem.getDescription());
        treatment.setText(problem.getTreatment());

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvTip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivProblemImage);
            tvTitle = itemView.findViewById(R.id.tvProblemName);
            tvDescription = itemView.findViewById(R.id.tvProblemDescription);
            tvTip = itemView.findViewById(R.id.tvProblemTip);
        }
    }
}
