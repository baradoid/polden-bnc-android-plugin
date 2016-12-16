package com.example.murinets.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;

import com.ftdi.j2xx.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.unity3d.player.UnityPlayerActivity;

public class MainActivity extends UnityPlayerActivity {

    public ftWorker ftw = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("test test");
        ftw = new ftWorker(this);
//        setContentView(R.layout.activity_main);
//
//        TextView tv = (TextView)findViewById(R.id.text123);
//        tv.setText("this string is set dynamically from java code\n");
//
//        final Button but = (Button) findViewById(R.id.buttonRescan);
//        but.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                rescanFt();
//            }
//        });
    }

    int data = 111 ;
    public int getData(){
        data = data + 1 ;
        return data;
    }
}
