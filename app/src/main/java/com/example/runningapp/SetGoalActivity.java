package com.example.runningapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SetGoalActivity extends AppCompatActivity {

    private Button btn3mi, btn5mi, btn10mi, btnHalf, btnFull, btnCustomize, btnConfirm;
    private EditText GoalDistance;
    private static double Goal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_goal);

        // find views
        btn3mi = (Button) findViewById(R.id.btn3mi);
        btn5mi = (Button) findViewById(R.id.btn5mi);
        btn10mi = (Button) findViewById(R.id.btn10mi);
        btnHalf = (Button) findViewById(R.id.btnHalfMara);
        btnFull = (Button) findViewById(R.id.btnFullMara);
        btnCustomize = (Button) findViewById(R.id.btnCustomize);
        btnConfirm = (Button) findViewById(R.id.btnConfirmGoal);
        GoalDistance = (EditText) findViewById(R.id.edtGoalnum);

        btn3mi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoalDistance.setText("3.00");
                Goal = 3;
            }
        });

        btn5mi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoalDistance.setText("5.00");
                Goal = 5;
            }
        });

        btn10mi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoalDistance.setText("10.00");
                Goal = 10;
            }
        });

        btnHalf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoalDistance.setText("13.1");
                Goal = 13.1;
            }
        });

        btnFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoalDistance.setText("26.21");
                Goal = 26.21;
            }
        });

        btnCustomize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoalDistance.setEnabled(true);
                GoalDistance.setText("");
                GoalDistance.setHint("Input your goal mileage");
            }
        });

        // go to the OneForAll activity
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Goal = Double.parseDouble(GoalDistance.getText().toString());
                if(Goal>=30 || Goal<0.1) {
                    Toast.makeText(getBaseContext(),"Please set a goal between 0.1 and 30 miles", Toast.LENGTH_SHORT).show();
                } else {
                    Intent goMonitorActivity = new Intent(getBaseContext(), OneForAll.class);
                    goMonitorActivity.putExtra("goal", GoalDistance.getText().toString());
                    startActivity(goMonitorActivity);
                }
            }
        });
    }
}