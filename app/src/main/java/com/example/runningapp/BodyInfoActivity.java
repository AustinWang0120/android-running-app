package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BodyInfoActivity extends AppCompatActivity {

    private static final String TAG = "body-info";

    private RadioGroup genderSelectionGroup;
    private EditText heightInput, weightInput;
    private Button saveButton;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabase;

    // Aux functions
    private String convertEditTextToString(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String getGenderRadioButtonSelection(RadioGroup radioGroup) {
        int selectedID = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton) findViewById(selectedID);
        return radioButton.getText().toString().trim();
    }

    private void writeUser(String uid, String gender, float height, float weight) {
        User user = new User(gender, height, weight);
        if (mDatabase != null) {
            mDatabase.child("users").child(uid).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(getBaseContext(), "Body Info has been updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getBaseContext(), "Authentication Failure", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Log.d(TAG, "writeUser: failure (no database reference)");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_info);

        genderSelectionGroup = (RadioGroup) findViewById(R.id.gender_selection_group);
        heightInput = (EditText) findViewById(R.id.height_input);
        weightInput = (EditText) findViewById(R.id.weight_input);
        saveButton = (Button) findViewById(R.id.save_button);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        String uid = mUser.getUid();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the database "users/{uid}"
                float height = Float.parseFloat(convertEditTextToString(heightInput));
                float weight = Float.parseFloat(convertEditTextToString(weightInput));
                writeUser(uid, getGenderRadioButtonSelection(genderSelectionGroup), height, weight);
            }
        });
    }
}