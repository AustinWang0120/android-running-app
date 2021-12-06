package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class ChattingActivity extends AppCompatActivity {

    private static final String TAG = "chatting-page";

    private EditText messageInput;
    private FloatingActionButton sendButton;
    private ListView messagesList;
    private ArrayAdapter adapter;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;

    // UserData
    private UserData userData;

    // aux func for converting EditText's content to string
    private String convertEditTextToString(EditText editText) {
        String result = editText.getText().toString().trim();
        if (TextUtils.isEmpty(result)) {
            return "invalid";
        }
        return result;
    }

    private void getMessages() {
        // UserData
        userData = new UserData();
        userData.getMessages(new UserData.MessagesCallback() {

            @Override
            public void onMessages(ArrayList messages) {
                // Sort the arraylist first
                Collections.sort(messages, new ChatMessageComparator());
                // retrieve data, setup the listview adapter
                adapter = new ChatMessageAdapter(ChattingActivity.this, R.layout.message, messages);
                messagesList.setAdapter(adapter);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // find views
        messageInput = (EditText) findViewById(R.id.message_input);
        sendButton = (FloatingActionButton) findViewById(R.id.send_button);
        messagesList = (ListView) findViewById(R.id.messages_listView);

        // render messages and listen to data change
        getMessages();
        FirebaseDatabase.getInstance().getReference("chat_messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                getMessages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // register button click event
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatMessage chatMessage = new ChatMessage(convertEditTextToString(messageInput));
                FirebaseDatabase.getInstance().getReference("chat_messages").push().setValue(chatMessage);
                messageInput.setText("");
            }
        });
    }
}

/**
 * Aux class for comparing dates
 * */
class ChatMessageComparator implements Comparator<Map<String, Object>> {

    @Override
    public int compare(Map o1, Map o2) {
        String date1 = (String) o1.get("messageTime");
        String date2 = (String) o2.get("messageTime");
        // yyyy-MM-dd hh:mm
        String[] array1 = date1.split("[\\s/:]+");
        String[] array2 = date2.split("[\\s/:]+");
        int compare1, compare2;
        // Iteratively compare all fields of the date
        for (int i = 0; i < array1.length; i++) {
            compare1 = Integer.parseInt(array1[i]);
            compare2 = Integer.parseInt(array2[i]);
            if (compare1 != compare2) {
                return compare1 - compare2;
            }
        }
        // All equals (should be impossible): return 1
        return 1;
    }
}

class ChatMessageAdapter extends ArrayAdapter<Map<String, Object>> {

    private int layout;
    private Context context;

    public ChatMessageAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Map<String, Object>> objects) {
        super(context, resource, objects);
        this.layout = resource;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View view = convertView;

        // For first time
        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layout, null);
        }

        Map<String, Object> chatMessage = getItem(position);

        if (chatMessage != null) {
            // find views and set texts
            TextView author_label = (TextView) view.findViewById(R.id.author_label);
            TextView time_label = (TextView) view.findViewById(R.id.time_label);
            TextView content_label = (TextView) view.findViewById(R.id.content_label);

            author_label.setText((String) chatMessage.get("messageAuthor"));
            time_label.setText((String) chatMessage.get("messageTime"));
            content_label.setText((String) chatMessage.get("messageText"));
        }

        // return super.getView(position, convertView, parent);
        return view;
    }
}