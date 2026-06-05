package atikyan.silva.plantcare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

public class AvatarPickerAdapter extends BaseAdapter {

    private final Context context;
    private final List<String> avatars;
    private String selectedAvatar;

    public AvatarPickerAdapter(Context context, List<String> avatars, String selectedAvatar) {
        this.context       = context;
        this.avatars       = avatars;
        this.selectedAvatar = selectedAvatar;
    }

    public void setSelected(String avatar) {
        this.selectedAvatar = avatar;
        notifyDataSetChanged();
    }

    @Override public int getCount()          { return avatars.size(); }
    @Override public Object getItem(int pos) { return avatars.get(pos); }
    @Override public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_avatar, parent, false);
        }

        ImageView ivAvatar = convertView.findViewById(R.id.ivAvatarItem);
        String drawableName = avatars.get(position);

        int resId = context.getResources().getIdentifier(
                drawableName, "drawable", context.getPackageName());
        if (resId != 0) {
            ivAvatar.setImageResource(resId);
        }
        if (drawableName.equals(selectedAvatar)) {
            convertView.setBackgroundResource(R.drawable.bg_avatar_selected);
        } else {
            convertView.setBackgroundResource(R.drawable.bg_avatar_normal);
        }

        return convertView;
    }
}
