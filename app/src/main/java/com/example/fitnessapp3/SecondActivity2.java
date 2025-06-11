package com.example.fitnessapp3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class SecondActivity2 extends AppCompatActivity {

    int[] newArray;
    private AdView mAdView,mAdView1;
    private ExerciseFilterManager exerciseFilterManager;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second2);

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView1 = findViewById(R.id.adView1);
        AdRequest adRequest1 = new AdRequest.Builder().build();
        mAdView1.loadAd(adRequest1);

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        
        // Initialize exercise filter manager
        exerciseFilterManager = new ExerciseFilterManager(this);
        
        // Set title for adult exercises
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Adult Exercises (18+)");
        }
        
        // Load user data and show appropriate message
        loadUserAgeInfo();

        // Adult exercise array (same IDs but will be filtered based on age)
        newArray = new int[]{
                R.id.bow_pose,
                R.id.bridge_pose,
                R.id.chair_pose,
                R.id.child_pose,
                R.id.cobbler_pose,
                R.id.cow_pose,
                R.id.playji_pose,
                R.id.pauseji_pose,
                R.id.plank_pose,
                R.id.crunches_pose,
                R.id.situp_pose,
                R.id.rotation_pose,
                R.id.twist_pose,
                R.id.windmill_pose,
                R.id.legup_pose
        };

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)  {
        int id = item.getItemId();
        if (id == R.id.id_privacy){

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://iotexpert1.blogspot.com/2020/09/privacy-policy-for-weight-loss.html"));
            startActivity(intent);
            return true;

        }
        if (id == R.id.id_term){

            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("https://iotexpert1.blogspot.com/2020/10/weight-loss-terms-and-conditions-page.html"));
            startActivity(intent);

            return true;
        }
        if (id == R.id.rate){

            try {

                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id" + getPackageName())));


            }catch (Exception ex){

                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.coms/store/apps/details?id=" + getPackageName())));
            }

            return true;
        }


        if (id == R.id.share){

            Intent myIntent = new Intent(Intent.ACTION_SEND);
            myIntent.setType("text/plain");
            String sharebody = "This is the best for Fitness \n By this app you can make your body fit \n this is for free download now \n" + "https://play.google.coms/store/apps/details?id=com.example.fitnessapp3=en";
            String sharehub = "Fitness App";
            myIntent.putExtra(Intent.EXTRA_SUBJECT,sharehub);
            myIntent.putExtra(Intent.EXTRA_TEXT,sharebody);
            startActivity(Intent.createChooser(myIntent,"share using"));

            return true;
        }
        return true;
    }
    
    private void loadUserAgeInfo() {
        exerciseFilterManager.getUserData(new ExerciseFilterManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(int age, String fitnessLevel) {
                if (age <= 18) {
                    // User is too young for this section
                    Toast.makeText(SecondActivity2.this, 
                        "Note: You're " + age + " years old. Consider using youth-appropriate exercises instead.", 
                        Toast.LENGTH_LONG).show();
                } else {
                    // Show appropriate intensity recommendation
                    String intensity = exerciseFilterManager.getRecommendedIntensity(age, fitnessLevel);
                    Toast.makeText(SecondActivity2.this, 
                        "Adult exercises loaded. " + intensity, 
                        Toast.LENGTH_LONG).show();
                }
                
                // Update toolbar with age info
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle("Age " + age + " • " + fitnessLevel.split(" ")[0]);
                }
            }

            @Override
            public void onError(String error) {
                // Default behavior for guests
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle("Guest Mode");
                }
            }
        });
    }

    public void Imagebuttonclicked(View view) {
        // Check if user age is appropriate for these exercises
        exerciseFilterManager.getUserData(new ExerciseFilterManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(int age, String fitnessLevel) {
                if (age <= 18) {
                    Toast.makeText(SecondActivity2.this, 
                        "This exercise is designed for adults. Consider youth-appropriate alternatives.", 
                        Toast.LENGTH_SHORT).show();
                }
                proceedWithExercise(view);
            }

            @Override
            public void onError(String error) {
                // Allow guests to proceed
                proceedWithExercise(view);
            }
        });
    }
    
    private void proceedWithExercise(View view) {
        for (int i = 0; i < newArray.length; i++) {
            if (view.getId() == newArray[i]) {
                int value = i + 1;
                Log.i("ADULT_EXERCISE", String.valueOf(value));
                Intent intent = new Intent(SecondActivity2.this, ThirdActivity2.class);
                intent.putExtra("value", String.valueOf(value));
                startActivity(intent);
                break;
            }
        }
    }
}