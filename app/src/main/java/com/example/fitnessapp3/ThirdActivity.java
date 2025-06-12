package com.example.fitnessapp3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ThirdActivity extends AppCompatActivity {


    String buttonvalue;
    String exerciseCategory;
    Button startBtn;
    private CountDownTimer countDownTimer;
    TextView mtextview;
    private boolean MTimeRunning;
    private long MTimeleftinmills;
    private ProgressTracker progressTracker;


  @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);

      getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
          @Override
          public void handleOnBackPressed() {
              goBackToExerciseList();
          }
      });

        Intent intent = getIntent();
        buttonvalue = intent.getStringExtra("value");
        exerciseCategory = intent.getStringExtra("exerciseCategory");
        
        // Initialize progress tracker
        progressTracker = new ProgressTracker(this);
        
        // Default to youth exercises if not specified
        if (exerciseCategory == null) {
            exerciseCategory = ProgressTracker.YOUTH_EXERCISES;
        }


        int intvalue = Integer.valueOf(buttonvalue);

        switch (intvalue){


            case 1:
                setContentView(R.layout.activity_bow);
                break;
            case 2:
                setContentView(R.layout.activity_bridge);
                break;
            case 3:
                setContentView(R.layout.activity_chair);
                break;
            case 4:
                setContentView(R.layout.activity_child);
                break;
            case 5:
                setContentView(R.layout.activity_cobbler);
                break;
            case 6:
                setContentView(R.layout.activity_cow);
                break;
            case 7:
                setContentView(R.layout.activity_playji);
                break;
            case 8:
                setContentView(R.layout.activity_pauseji);
                break;
            case 9:
                setContentView(R.layout.activity_plank);
                break;
            case 10:
                setContentView(R.layout.activity_crunches);
                break;
            case 11:
                setContentView(R.layout.activity_situp);
                break;
            case 12:
                setContentView(R.layout.activity_rotation);
                break;
            case 13:
                setContentView(R.layout.activity_twist);
                break;
            case 14:
                setContentView(R.layout.activity_windmill);
                break;
            case 15:
                setContentView(R.layout.activity_legup);
                break;

        }
        startBtn = findViewById(R.id.startbutton);
        mtextview = findViewById(R.id.time);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MTimeRunning)
                {

                    stoptimer();

                }

                else{

                    startTimer();
                }

            }
        });




    }

    private void stoptimer()
    {
        countDownTimer.cancel();
        MTimeRunning = false;
        startBtn.setText("START");
    }
    private void startTimer()
    {

        final CharSequence value1 = mtextview.getText();
        String num1 = value1.toString();
        String num2 = num1.substring(0 , 2);
        String num3 = num1.substring(3,5);



        final int number = Integer.valueOf(num2) * 60 + Integer.valueOf(num3);
        MTimeleftinmills = number*1000;

        countDownTimer = new CountDownTimer(MTimeleftinmills,1000) {
            @Override
            public void onTick(long millisUntilFinished) {


                MTimeleftinmills = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                // Mark current exercise as completed
                int currentExercise = Integer.valueOf(buttonvalue);
                progressTracker.completeExercise(exerciseCategory, currentExercise, new ProgressTracker.UpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("ThirdActivity", "Progress updated successfully for exercise " + currentExercise);
                        Toast.makeText(ThirdActivity.this, "Exercise completed! 🎉", Toast.LENGTH_SHORT).show();
                        
                        // Check if there are more exercises to unlock
                        int totalExercises = exerciseCategory.equals(ProgressTracker.YOUTH_EXERCISES) ? 15 : 15;
                        int nextExercise = currentExercise + 1;
                        
                        if (nextExercise <= totalExercises) {
                            // Go to next exercise if available
                            Intent intent = new Intent(ThirdActivity.this, ThirdActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("value", String.valueOf(nextExercise));
                            intent.putExtra("exerciseCategory", exerciseCategory);
                            startActivity(intent);
                        } else {
                            // All exercises completed - return to exercise list
                            Toast.makeText(ThirdActivity.this, "Congratulations! All exercises completed! 🏆", Toast.LENGTH_LONG).show();
                            goBackToExerciseList();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("ThirdActivity", "Error saving progress for exercise " + currentExercise + ": " + error);
                        Toast.makeText(ThirdActivity.this, "Error saving progress: " + error, Toast.LENGTH_SHORT).show();
                        // Still proceed to next exercise even if saving failed
                        int nextExercise = currentExercise + 1;
                        int totalExercises = exerciseCategory.equals(ProgressTracker.YOUTH_EXERCISES) ? 15 : 15;
                        
                        if (nextExercise <= totalExercises) {
                            Intent intent = new Intent(ThirdActivity.this, ThirdActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("value", String.valueOf(nextExercise));
                            intent.putExtra("exerciseCategory", exerciseCategory);
                            startActivity(intent);
                        } else {
                            goBackToExerciseList();
                        }
                    }
                });
            }
        }.start();
        startBtn.setText("Pause");
        MTimeRunning = true;

    }

    private void updateTimer()

    {

        int minutes = (int) MTimeleftinmills/60000;
        int seconds = (int) MTimeleftinmills%60000/1000;


        String timeLeftText = "";
        if(minutes < 10 )
            timeLeftText="0";
        timeLeftText = timeLeftText + minutes+":";
        if(seconds < 10)
            timeLeftText += "0";
        timeLeftText+=seconds;
        mtextview.setText(timeLeftText);


    }
    
    private void goBackToExerciseList() {
        Intent intent;
        if (exerciseCategory.equals(ProgressTracker.YOUTH_EXERCISES)) {
            intent = new Intent(ThirdActivity.this, SecondActivity.class);
        } else {
            intent = new Intent(ThirdActivity.this, SecondActivity2.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }



}