package com.example.ospp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Main page's buttons
        Button recordButton = (Button) findViewById(R.id.hikingRecord); // 산행 기록
        recordButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), recordActivity.class);
                startActivity(intent);
            }
        });

        Button zoneButton = (Button) findViewById(R.id.recordZone); // 기록함
        zoneButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), zoneActivity.class);
                startActivity(intent);
            }
        });

        Button giveButton = (Button) findViewById(R.id.giveTrails); // 등산로 추천
        giveButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(getApplicationContext(), giveActivity.class);
                startActivity(intent);
            }
        });
    }
}