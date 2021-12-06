package com.example.runningapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupOfRuns extends AppCompatActivity {

    private ArrayList<String> runsOfMonth;
    private ListView lvRuns;
    private ListAdapter lvrunAdapter;
    private HashMap<Integer,String> months = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_of_runs);

        //todo get bundle from last activity to get Year and Month as key to retreive data from database ========================================
        /**data format  mm - dd - duration - miles - avgPace - calories */
        String run1 = "11-03-00:23:18-4.00-7'18-306";
        String run2 = "11-04-00:37:12-4.32-'8'23-436";
        String run3 = "11-05-00:33:08-5.82-'7'72-308";
        runsOfMonth = new ArrayList<>();
        runsOfMonth.add(run1);
        runsOfMonth.add(run2);
        runsOfMonth.add(run3);


        months.put(1,"Jan");
        months.put(2,"Feb");
        months.put(3,"Mar");
        months.put(4,"Apr");
        months.put(5,"May");
        months.put(6,"Jun");
        months.put(7,"Jul");
        months.put(8,"Aug");
        months.put(9,"Sep");
        months.put(10,"Oct");
        months.put(11,"Nov");
        months.put(12,"Dec");


        lvRuns = (ListView) findViewById(R.id.lvOfRuns);
        lvrunAdapter = new CustomAdapter_Run(GroupOfRuns.this,runsOfMonth , months);
        lvRuns.setAdapter(lvrunAdapter);
        lvRuns.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent detailIntent = new Intent(GroupOfRuns.this, DetailOfOneRecord.class);
                startActivity(detailIntent);
            }
        });
    }
}
class CustomAdapter_Run extends BaseAdapter {
    ArrayList<String> dataOfSignleRun;
    Context context;
    HashMap<Integer,String> months;

    public CustomAdapter_Run(Context baseContext, ArrayList<String> runsOfMonth,HashMap<Integer,String> months) {
        context = baseContext;
        dataOfSignleRun = runsOfMonth;
        this.months = months;
    }

    @Override
    public int getCount() {
        return dataOfSignleRun.size();
    }

    @Override
    public Object getItem(int i) {
        return dataOfSignleRun.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View row;
        if (view == null){  //indicates this is the first time we are creating this row.
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  //Inflater's are awesome, they convert xml to Java Objects!
            row = inflater.inflate(R.layout.inner_lv, viewGroup, false);
        }
        else
        {
            row = view;
        }


        TextView month = (TextView) row.findViewById(R.id.tvInnerLvMonth);
        TextView day = (TextView) row.findViewById(R.id.tvInnerLvDay);
        TextView innermile = (TextView) row.findViewById(R.id.tvInnerLvMiles);
        TextView duration = (TextView) row.findViewById(R.id.tvInnerLvDuration);
        TextView avgPace = (TextView) row.findViewById(R.id.tvInnerLvAvgPace);
        TextView calories = (TextView) row.findViewById(R.id.tvInnerLvCal);

        String[] tmpdata = dataOfSignleRun.get(i).split("-");

        String tmpMon = months.get(Integer.parseInt(tmpdata[0]));
        month.setText(tmpMon);
        day.setText(tmpdata[1]+"th");
        innermile.setText(tmpdata[3]);
        duration.setText(tmpdata[2]);
        avgPace.setText(tmpdata[4]);
        calories.setText(tmpdata[5]+"cal");

        return row;
    }


}
