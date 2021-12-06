package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class PostActivity extends AppCompatActivity {

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabase;
    StorageReference mStorage;

    // UserData
    UserData userData;

    // Views
    ListView sharedPostList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Firebase
        mStorage = FirebaseStorage.getInstance().getReference();

        // UserData
        userData = new UserData();

        // Find views
        sharedPostList = (ListView) findViewById(R.id.shared_post_listview);

        // Use R.layout.lv_setting for the listview adapter layout
        /**
         * Render the post list
         * */
        userData.getBitmaps(false, new UserData.BitmapsCallback() {
            @Override
            public void onBitmaps(ArrayList bitmaps) {
                Collections.sort(bitmaps, new sharedPostComparator());
                sharedPostAdapter adapter = new sharedPostAdapter(getBaseContext(), R.layout.post_lv, bitmaps);
                sharedPostList.setAdapter(adapter);
            }
        });
    }
}

/**
 * Aux class for comparing dates
 * */
class sharedPostComparator implements Comparator<Map<String, Object>> {

    @Override
    public int compare(Map o1, Map o2) {
        String date1 = (String) o1.get("date");
        String date2 = (String) o2.get("date");
        // yyyy-MM-dd hh:mm
        String[] array1 = date1.split("-");
        String[] array2 = date2.split("-");
        int compare1, compare2;
        // Iteratively compare all fields of the date
        for (int i = 0; i < array1.length; i++) {
            compare1 = Integer.parseInt(array1[i]);
            compare2 = Integer.parseInt(array2[i]);
            if (compare1 != compare2) {
                return -(compare1 - compare2);
            }
        }
        // All equals (should be impossible): return 1
        return 1;
    }
}

class sharedPostAdapter extends ArrayAdapter<HashMap<String, Object>> {

    private static final String TAG = "sharedPostAdapter-class";

    private int layout;
    private Context context;

    public sharedPostAdapter(@NonNull Context context, int resource, ArrayList<HashMap<String, Object>> bitmaps) {
        super(context, resource, bitmaps);
        this.layout = resource;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layout, null);
        }

        // Get the bitmap file
        HashMap<String, Object> singleMap = (HashMap<String, Object>) getItem(position);

        if (singleMap != null) {
            // imgMapSettingLV, tvDatePostLV
            File file = (File) singleMap.get("bitmap");
            ImageView image = (ImageView) view.findViewById(R.id.imgMapSettingLV);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            image.setImageBitmap(bitmap);

            // Set the display name
            TextView displayNameLabel = (TextView) view.findViewById(R.id.tvDisplayNamePostLV);
            String displayName = file.getName().split("-")[0];
            Log.d(TAG, "getView: " + displayName);
            displayNameLabel.setText(displayName);

            // Set the creation time
            String creationDate = (String) singleMap.get("date");
            TextView date = (TextView) view.findViewById(R.id.tvDatePostLV);
            date.setText(creationDate);
        }

        return view;
    }
}