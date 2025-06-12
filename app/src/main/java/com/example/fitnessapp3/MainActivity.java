package com.example.fitnessapp3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    Button button1,button2;
    private FirebaseAuth mAuth;
    private ExerciseFilterManager exerciseFilterManager;
    private TextView tvAgeGroup, tvRecommendations;
    private LinearLayout layoutBeforeAge18, layoutAfterAge18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        exerciseFilterManager = new ExerciseFilterManager(this);

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        // Update toolbar title based on user status
        updateToolbarTitle();

        button1 = findViewById(R.id.startfitness1);
        button2 = findViewById(R.id.startfitness2);
        
        // Initialize layout references for age-based hiding
        layoutBeforeAge18 = findViewById(R.id.layoutBeforeAge18);
        layoutAfterAge18 = findViewById(R.id.layoutAfterAge18);
        
        // Load user data and setup age-appropriate recommendations
        loadUserRecommendations();

        button1.setOnClickListener((v) -> {
            Intent intent = new Intent(MainActivity.this,SecondActivity.class);
            startActivity(intent);
        });

        button2.setOnClickListener((v) -> {
            Intent intent = new Intent(MainActivity.this,SecondActivity2.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updateToolbarTitle() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Welcome, " + displayName.split(" ")[0]);
                }
            } else {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Fitness App");
                }
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Fitness App (Guest)");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        
        if (id == R.id.logout){
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                mAuth.signOut();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                // Guest mode - just go to HomeActivity
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        }
        
        return true;
    }



    private void loadUserRecommendations() {
        exerciseFilterManager.getUserData(new ExerciseFilterManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(int age, String fitnessLevel) {
                // Update UI with personalized recommendations
                String ageLabel = exerciseFilterManager.getAgeGroupLabel(age);
                String intensity = exerciseFilterManager.getRecommendedIntensity(age, fitnessLevel);
                
                runOnUiThread(() -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(ageLabel);
                    }
                    
                    // Hide/show age-appropriate sections
                    updateUIBasedOnAge(age);
                });
            }

            @Override
            public void onError(String error) {
                // Handle guest users or errors gracefully - show all sections for guests
                runOnUiThread(() -> {
                    updateUIBasedOnAge(16); // Default to youth view for guests
                });
            }
        });
    }
    
    private void updateUIBasedOnAge(int age) {
        if (layoutBeforeAge18 != null && layoutAfterAge18 != null) {
            if (age <= 18) {
                // Show youth section, hide adult section
                layoutBeforeAge18.setVisibility(View.VISIBLE);
                layoutAfterAge18.setVisibility(View.GONE);
                Toast.makeText(this, "Showing youth-appropriate exercises", Toast.LENGTH_SHORT).show();
            } else {
                // Show adult section, hide youth section  
                layoutBeforeAge18.setVisibility(View.GONE);
                layoutAfterAge18.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Showing adult exercises for age " + age, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void beforeage18(View view) {
        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        startActivity(intent);
    }

    public void afterage18(View view) {
        Intent intent = new Intent(MainActivity.this, SecondActivity2.class);
        startActivity(intent);
    }
    


    public void Food(View view) {
        Intent intent = new Intent(MainActivity.this, FoodActivity.class);
        startActivity(intent);
    }
}