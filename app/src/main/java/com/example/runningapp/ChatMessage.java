package com.example.runningapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatMessage {

    private static final String TAG = "chat-message-class";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;

    private String uid;
    private String messageText;
    private String messageAuthor;
    private String messageTime;

    public ChatMessage() {
        // Always leave an empty constructor
    }

    public ChatMessage(String messageText) {
        // Firebase
        this.mAuth = FirebaseAuth.getInstance();
        this.mUser = mAuth.getCurrentUser();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.uid = mUser.getUid();
        this.messageText = messageText;
        this.messageAuthor = mUser.getDisplayName();
        this.messageTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());
    }

    public String getUid() {
        return uid;
    }

//    public void setUid(String uid) {
//        this.uid = uid;
//    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageAuthor() {
        return messageAuthor;
    }

    public void setMessageAuthor(String messageAuthor) {
        this.messageAuthor = messageAuthor;
    }

    public String getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(String messageTime) {
        this.messageTime = messageTime;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", this.mUser.getUid());
        result.put("author", this.messageAuthor);
        result.put("timestamp", this.messageTime);
        result.put("text", this.messageText);

        return result;
    }

    public void writeNewMessage() {
        String key = mDatabase.child("chat_messages").push().getKey();
        Map<String, Object> messageValues = this.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
//        childUpdates.put("/chat_messages/" + key, messageValues);
//        childUpdates.put("/user_chat_messages/" + this.mUser.getUid() + "/", messageValues);
        childUpdates.put("/chat_messages/" + key, this);
        childUpdates.put("/user_chat_messages/" + this.mUser.getUid() + "/", this);

        mDatabase.updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "updateMessage: success");
                } else {
                    Log.d(TAG, "updateMessage: failure");
                }
            }
        });
    }
}
