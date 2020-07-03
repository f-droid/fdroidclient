package org.fdroid.fdroid.views.filebrowser;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.fdroid.fdroid.R;

import java.io.File;


public class fileBrowserHolder extends RecyclerView.ViewHolder {

    TextView Name;
    ImageView Icon;

    fileBrowserHolder(View itemView) {
        super(itemView);
        Name = itemView.findViewById(R.id.dir_name);
        Icon = itemView.findViewById(R.id.df_icon);
    }

    public void bind(File file) {
        Name.setText(file.getName());

        if (file.isDirectory()) {
            if (file.getPath().equals(fileBrowserActivity.defaultPath)) {
                Icon.setImageResource(R.drawable.ic_twotone_folder_special_24);
            } else {
                Icon.setImageResource(R.drawable.ic_twotone_folder_24);
            }
        } else {
            Icon.setImageResource(R.drawable.ic_twotone_insert_drive_file_24);
        }
    }

}
