package com.example.fitnessapp3;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExerciseFilterManager {
    
    private static final String PREFS_NAME = "FitnessAppPrefs";
    private static final String KEY_USER_AGE = "user_age";
    private static final String KEY_FITNESS_LEVEL = "user_fitness_level";
    
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    
    public ExerciseFilterManager(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    // Age-appropriate exercises for users <= 18
    private List<String> youthExercises = Arrays.asList(
        "jumping_jacks",
        "push_ups", 
        "squats",
        "lunges",
        "plank",
        "mountain_climbers",
        "burpees",
        "high_knees",
        "butt_kicks",
        "star_jumps",
        "bear_crawl",
        "crab_walk",
        "wall_sit",
        "bicycle_crunches",
        "leg_raises"
    );
    
    // Advanced exercises for users > 18
    private List<String> adultExercises = Arrays.asList(
        "jumping_jacks",
        "push_ups",
        "squats", 
        "lunges",
        "plank",
        "mountain_climbers",
        "burpees",
        "high_knees",
        "butt_kicks",
        "star_jumps",
        "bear_crawl",
        "crab_walk",
        "wall_sit",
        "bicycle_crunches",
        "leg_raises",
        // Additional adult exercises
        "deadlifts",
        "pull_ups",
        "dips",
        "box_jumps",
        "kettlebell_swings",
        "battle_ropes",
        "barbell_rows",
        "overhead_press",
        "bench_press",
        "clean_and_jerk",
        "turkish_getups"
    );
    
    public interface UserDataCallback {
        void onUserDataLoaded(int age, String fitnessLevel);
        void onError(String error);
    }
    
    public void getUserData(UserDataCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Try to get from SharedPreferences as fallback
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int age = prefs.getInt(KEY_USER_AGE, 0);
            String fitnessLevel = prefs.getString(KEY_FITNESS_LEVEL, "Beginner");
            
            if (age > 0) {
                callback.onUserDataLoaded(age, fitnessLevel);
            } else {
                callback.onError("User not logged in and no cached data");
            }
            return;
        }
        
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Cache the data locally
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putInt(KEY_USER_AGE, user.getAge())
                                    .putString(KEY_FITNESS_LEVEL, user.getFitnessLevel())
                                    .apply();
                            
                            callback.onUserDataLoaded(user.getAge(), user.getFitnessLevel());
                        } else {
                            callback.onError("User data not found");
                        }
                    } else {
                        callback.onError("User document does not exist");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    public List<String> getAgeAppropriateExercises(int age) {
        if (age <= 18) {
            return new ArrayList<>(youthExercises);
        } else {
            return new ArrayList<>(adultExercises);
        }
    }
    
    public boolean isExerciseAppropriate(String exerciseName, int age) {
        List<String> appropriateExercises = getAgeAppropriateExercises(age);
        return appropriateExercises.contains(exerciseName.toLowerCase());
    }
    
    public String getAgeGroupLabel(int age) {
        if (age <= 18) {
            return "Youth Fitness (Age " + age + ")";
        } else {
            return "Adult Fitness (Age " + age + ")";
        }
    }
    
    public String getRecommendedIntensity(int age, String fitnessLevel) {
        if (age <= 18) {
            switch (fitnessLevel) {
                case "Beginner (Just Starting)":
                    return "Light intensity, focus on form and fun";
                case "Intermediate (Some Experience)":
                    return "Moderate intensity with proper supervision";
                case "Advanced (Very Active)":
                    return "Higher intensity with safety focus";
                default:
                    return "Age-appropriate moderate intensity";
            }
        } else {
            switch (fitnessLevel) {
                case "Beginner (Just Starting)":
                    return "Start slow, build gradually";
                case "Intermediate (Some Experience)":
                    return "Moderate to high intensity";
                case "Advanced (Very Active)":
                    return "High intensity training";
                case "Expert (Athletic/Professional)":
                    return "Maximum intensity with advanced techniques";
                default:
                    return "Moderate intensity";
            }
        }
    }
} 