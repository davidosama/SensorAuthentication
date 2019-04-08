package com.david.sensorauthentication;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    ProgressBar progressBar;
    TextView textView;
    Button button;

    SensorManager sensorManager;
    boolean sensorPresent;
    Sensor sensorAccelerometer;
    boolean started = false;
    CountDownTimer countDownTimer;
    int progress = 0;
    boolean sensorCalibrated = false;
    float [] calibrationValues = {-1,-1,-1};
    float refValue = -1;
    int shakes = 0;
    int patternNum = 1;

    String textViewOldText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(5);

        textView = findViewById(R.id.textView);
        button = findViewById(R.id.button);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        final List<android.hardware.Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        if(sensorList.size() > 0){
            sensorPresent = true;
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }
        else{
            sensorPresent = false;
        }

        if(sensorPresent){
            calibrateSensor();
        }
        else{
            Toast.makeText(MainActivity.this, "No accelerometer sensor detected", Toast.LENGTH_LONG).show();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!started && sensorPresent){
                    started = true;
                    textView.setBackgroundColor(Color.TRANSPARENT);
                    textView.setText("Enter the password");
                    progressBarLoading();
                    countDownTimer.start();
                }
                else if(!sensorPresent){
                    Toast.makeText(MainActivity.this, "No accelerometer sensor detected", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(MainActivity.this, "Already started", Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void calibrateSensor() {
        if(!sensorCalibrated){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Sensor Calibration");
            builder.setMessage("To calibrate the sensor please shake the device 3 shakes and then press Finish.");


            builder.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sensorCalibrated = true;
                    calculateRefValue();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void calculateRefValue() {
        float sum = 0;

        for(int i=0; i<calibrationValues.length; i++){
            sum += calibrationValues[i];
        }

        refValue = (sum/calibrationValues.length);

        // subtract 10% from the threshold value
        refValue = refValue - (refValue * 0.15f);
    }

    private void progressBarLoading() {
        countDownTimer = new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                progress++;
                progressBar.setProgress(progress,true);
            }

            @Override
            public void onFinish() {

                if(patternNum == 1){
                    patternNum = 2;
                    progressBar.setProgress(0);
                    progress = 0;
                    textView.setText(shakes+" - ");
                    textViewOldText = shakes+" - ";
                    countDownTimer.start();

                }
                else if(patternNum == 2){
                    patternNum = 3;
                    progressBar.setProgress(0);
                    progress = 0;
                    textView.setText(textViewOldText+shakes+" - ");
                    textViewOldText = textViewOldText+shakes+" - ";
                    countDownTimer.start();

                }
                else if(patternNum == 3){
                    patternNum = 1;
                    progressBar.setProgress(0);
                    progress = 0;
                    textView.setText(textViewOldText+shakes+"");
                    started = false;
                    if(textView.getText().toString().equals("1 - 2 - 3")){
                        Toast.makeText(MainActivity.this, "Correct password!", Toast.LENGTH_LONG).show();
                        textView.setBackgroundColor(Color.GREEN);
                        textView.append("\nCORRECT PASSWORD!");
                    }
                    else{
                        reset();
                    }

                }
                //reset shakes recorded
                shakes = 0;
            }
        };
    }

    private void reset(){
        textView.append("\nWrong password. Try again.");
        textView.setBackgroundColor(Color.RED);
        patternNum = 1;
        progressBar.setProgress(0);
        progress = 0;
        started = false;
        Toast.makeText(MainActivity.this, "Wrong password. Press Start to try again.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        Log.v("Sensor reading","x: "+x);
        Log.v("Sensor reading","y: "+y);
        Log.v("Sensor reading","z: "+z);


        if(!sensorCalibrated){
            Arrays.sort(calibrationValues);

            for (int i = 0; i < calibrationValues.length; i++){
                if(x > calibrationValues[i]){
                    calibrationValues[i] = x;
                    break;
                }
            }
        }
        else{
            if(Math.abs(x) > refValue){
                if (started) {
                    shakes++;
                    textView.setText("Shakes: "+shakes);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
