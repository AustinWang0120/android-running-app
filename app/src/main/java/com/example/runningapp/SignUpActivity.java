package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "sign-up-page";

    private EditText displayNameInput, emailInput, passwordInput, heightInput, weightInput;
    private RadioGroup genderSelectionGroup;
    private Button signUpButton;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    /**
     * Aux functions
     * convertEditTextToString: convert input 2 string
     * reload: finish the current activity and restart it
     * updateUI: go to the main activity if the user is not null, otherwise reload the page
     * */
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
            mDatabase.child("users").child(uid).setValue(user);
            Log.d(TAG, "writeUser: success");
        } else {
            Log.d(TAG, "writeUser: failure (no database reference)");
        }
    }

    // reload the page
    private void reload() {
        finish();
        startActivity(getIntent());
    }

    // login to the main page after signing up
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            reload();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // find views
        displayNameInput = (EditText) findViewById(R.id.display_name_input);
        emailInput = (EditText) findViewById(R.id.email_input);
        passwordInput = (EditText) findViewById(R.id.password_input);
        genderSelectionGroup = (RadioGroup) findViewById(R.id.gender_selection_group);
        heightInput = (EditText) findViewById(R.id.height_input);
        weightInput = (EditText) findViewById(R.id.weight_input);
        signUpButton = (Button) findViewById(R.id.signup_button);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // sign up
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.createUserWithEmailAndPassword(convertEditTextToString(emailInput), convertEditTextToString(passwordInput))
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "createUserWithEmailAndpPassword: success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    // update display name
                                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(convertEditTextToString(displayNameInput))
                                            // we don't have avatars yet
                                            .setPhotoUri(null)
                                            .build();
                                    user.updateProfile(profileUpdate)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        // update gender, height and weight by loading into the database
                                                        writeUser(
                                                                user.getUid(),
                                                                getGenderRadioButtonSelection(genderSelectionGroup),
                                                                Float.parseFloat(convertEditTextToString(heightInput)),
                                                                Float.parseFloat(convertEditTextToString(weightInput)));
                                                        Log.d(TAG, "updateProfile: success");
                                                        updateUI(user);
                                                    } else {
                                                        Log.d(TAG, "updateProfile: failure");
                                                        reload();
                                                    }
                                                }
                                            });
                                } else {
                                    Log.d(TAG, "createUserWithEmailAndPassword: failure", task.getException());
                                    Toast.makeText(SignUpActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}