package atikyan.silva.plantcare;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyPlantsAdapter extends RecyclerView.Adapter<MyPlantsAdapter.PlantViewHolder> {

    private final List<PlantModel> plants;
    private final Context context;

    public MyPlantsAdapter(List<PlantModel> plants, Context context) {
        this.plants  = plants;
        this.context = context;
    }

    @NonNull
    @Override
    public PlantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_plant, parent, false);
        return new PlantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlantViewHolder holder, int position) {
        PlantModel plant = plants.get(position);

        holder.tvPlantName.setText(plant.getName() != null ? plant.getName() : "");
        holder.tvWaterLabel.setText(plant.getWaterLabel() != null ? plant.getWaterLabel() : "");

        // Фото из base64
        String photoBase64 = plant.getPhotoBase64();
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(photoBase64, Base64.DEFAULT);
                Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.ivPlantPhoto.setImageBitmap(bmp);
                holder.ivPlantPhoto.setVisibility(View.VISIBLE);
                holder.tvPlantEmoji.setVisibility(View.GONE);
            } catch (Exception e) {
                holder.ivPlantPhoto.setVisibility(View.GONE);
                holder.tvPlantEmoji.setVisibility(View.VISIBLE);
            }
        } else {
            holder.ivPlantPhoto.setVisibility(View.GONE);
            holder.tvPlantEmoji.setVisibility(View.VISIBLE);
        }

        // Иконка-капля (нужен полив)
        long today = System.currentTimeMillis();
        Long nextWater = plant.getNextWater();
        holder.ivWaterDrop.setVisibility(
                (nextWater != null && nextWater <= today) ? View.VISIBLE : View.GONE);

        // Клик → экран деталей
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlantDetailActivity.class);
            intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID,   plant.getDocId());
            intent.putExtra(PlantDetailActivity.EXTRA_PLANT_NAME, plant.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return plants.size(); }

    static class PlantViewHolder extends RecyclerView.ViewHolder {
        TextView  tvPlantName;
        TextView  tvWaterLabel;
        TextView  tvPlantEmoji;
        ImageView ivPlantPhoto;
        ImageView ivWaterDrop;

        PlantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlantName  = itemView.findViewById(R.id.tvPlantName);
            tvWaterLabel = itemView.findViewById(R.id.tvWaterLabel);
            tvPlantEmoji = itemView.findViewById(R.id.tvPlantEmoji);
            ivPlantPhoto = itemView.findViewById(R.id.ivPlantPhoto);
            ivWaterDrop  = itemView.findViewById(R.id.ivWaterDrop);
        }
    }
}
