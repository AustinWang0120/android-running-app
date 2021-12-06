package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingActivity extends AppCompatActivity {

    private TextView displayNameLabel, emailLabel;
    private Button bodyInfoButton, changePasswordButton, signOutButton;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        // Listen to the state of the user
        mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Intent intent = new Intent(SettingActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        // find views
        displayNameLabel = (TextView) findViewById(R.id.displayName_label);
        emailLabel = (TextView) findViewById(R.id.email_label);
        bodyInfoButton = (Button) findViewById(R.id.body_info_button);
        changePasswordButton = (Button) findViewById(R.id.change_password_button);
        signOutButton = (Button) findViewById(R.id.sign_out_button);

        // Set display texts of user's profile
        displayNameLabel.setText("Name: " + mUser.getDisplayName());
        emailLabel.setText("Email: " + mUser.getEmail());

        // change body info
        bodyInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, BodyInfoActivity.class);
                startActivity(intent);
            }
        });

        // send email for resetting password
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.sendPasswordResetEmail(mUser.getEmail()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingActivity.this, "Email Sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingActivity.this, "Authentication Failure", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // sign out
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
            }
        });
    }
}