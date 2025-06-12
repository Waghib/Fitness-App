package com.example.fitnessapp3;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressTracker {
    private static final String TAG = "ProgressTracker";
    private static final String PREFS_NAME = "ExerciseProgress";
    private static final String GUEST_PROGRESS_KEY = "guest_progress";
    
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences prefs;
    
    // Exercise categories
    public static final String YOUTH_EXERCISES = "youth_exercises";
    public static final String ADULT_EXERCISES = "adult_exercises";
    
    public interface ProgressCallback {
        void onProgressLoaded(int completedExercises, int totalExercises, List<Integer> unlockedExercises);
        void onError(String error);
    }
    
    public interface UpdateCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface UnlockCheckCallback {
        void onResult(boolean isUnlocked);
    }
    
    public ProgressTracker(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get user's exercise progress
     */
    public void getProgress(String exerciseCategory, ProgressCallback callback) {
        Log.d(TAG, "getProgress called - Category: " + exerciseCategory);
        
        FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            Log.d(TAG, "User is null, using guest progress");
            // Guest user - use SharedPreferences
            getGuestProgress(exerciseCategory, callback);
            return;
        }
        
        Log.d(TAG, "User authenticated for getProgress: " + user.getUid());
        
        // Authenticated user - use Firestore
        db.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(exerciseCategory)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                List<Long> completedIds = (List<Long>) document.get("completedExercises");
                                Long totalExercises = document.getLong("totalExercises");
                                
                                List<Integer> completed = new ArrayList<>();
                                if (completedIds != null) {
                                    for (Long id : completedIds) {
                                        completed.add(id.intValue());
                                    }
                                }
                                
                                List<Integer> unlocked = getUnlockedExercises(completed, 
                                    totalExercises != null ? totalExercises.intValue() : getTotalExercisesCount(exerciseCategory));
                                
                                callback.onProgressLoaded(completed.size(), 
                                    totalExercises != null ? totalExercises.intValue() : getTotalExercisesCount(exerciseCategory), 
                                    unlocked);
                            } else {
                                // No progress yet - unlock first exercise
                                initializeProgress(exerciseCategory, callback);
                            }
                        } else {
                            Log.e(TAG, "Error getting progress", task.getException());
                            String errorMessage = task.getException().getMessage();
                            
                            // Check if it's a permission error - fallback to guest mode
                            if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                                Log.w(TAG, "Permission denied for Firebase, falling back to guest mode");
                                getGuestProgress(exerciseCategory, callback);
                            } else {
                                callback.onError("Failed to load progress: " + errorMessage);
                            }
                        }
                    }
                });
    }
    
    /**
     * Mark an exercise as completed
     */
    public void completeExercise(String exerciseCategory, int exerciseId, UpdateCallback callback) {
        Log.d(TAG, "completeExercise called - Category: " + exerciseCategory + ", Exercise ID: " + exerciseId);
        
        FirebaseUser user = auth.getCurrentUser();
        
        if (user == null) {
            Log.d(TAG, "User is null, using guest progress");
            // Guest user
            completeGuestExercise(exerciseCategory, exerciseId, callback);
            return;
        }
        
        Log.d(TAG, "User authenticated: " + user.getUid());
        
        // Get current progress first
        getProgress(exerciseCategory, new ProgressCallback() {
            @Override
            public void onProgressLoaded(int completedExercises, int totalExercises, List<Integer> unlockedExercises) {
                // Get existing completed exercises from Firestore
                db.collection("users")
                        .document(user.getUid())
                        .collection("progress")
                        .document(exerciseCategory)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                List<Integer> completed = new ArrayList<>();
                                
                                if (task.isSuccessful() && task.getResult().exists()) {
                                    List<Long> existingCompleted = (List<Long>) task.getResult().get("completedExercises");
                                    if (existingCompleted != null) {
                                        for (Long id : existingCompleted) {
                                            completed.add(id.intValue());
                                        }
                                    }
                                } else if (!task.isSuccessful()) {
                                    String errorMessage = task.getException().getMessage();
                                    if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                                        Log.w(TAG, "Permission denied, falling back to guest mode for exercise completion");
                                        completeGuestExercise(exerciseCategory, exerciseId, callback);
                                        return;
                                    }
                                }
                                
                                // Add new exercise if not already completed
                                if (!completed.contains(exerciseId)) {
                                    completed.add(exerciseId);
                                    Log.d(TAG, "Added exercise " + exerciseId + " to completed list");
                                } else {
                                    Log.d(TAG, "Exercise " + exerciseId + " already completed");
                                }
                                
                                Log.d(TAG, "Completed exercises: " + completed.toString());
                                
                                // Update Firestore
                                updateFirestoreProgress(exerciseCategory, completed, totalExercises, callback);
                            }
                        });
            }
            
            @Override
            public void onError(String error) {
                // Check if it's a permission error - fallback to guest mode
                if (error != null && error.contains("PERMISSION_DENIED")) {
                    Log.w(TAG, "Permission denied for Firebase, falling back to guest mode for exercise completion");
                    completeGuestExercise(exerciseCategory, exerciseId, callback);
                } else {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Calculate which exercises should be unlocked based on completed exercises
     */
    private List<Integer> getUnlockedExercises(List<Integer> completedExercises, int totalExercises) {
        List<Integer> unlocked = new ArrayList<>();
        
        // Always unlock first exercise
        unlocked.add(1);
        
        // Unlock next exercise for each completed one
        for (int completedId : completedExercises) {
            int nextExercise = completedId + 1;
            if (nextExercise <= totalExercises && !unlocked.contains(nextExercise)) {
                unlocked.add(nextExercise);
            }
        }
        
        return unlocked;
    }
    
    /**
     * Initialize progress for new user
     */
    private void initializeProgress(String exerciseCategory, ProgressCallback callback) {
        List<Integer> unlocked = new ArrayList<>();
        unlocked.add(1); // First exercise unlocked
        
        int totalExercises = getTotalExercisesCount(exerciseCategory);
        
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("completedExercises", new ArrayList<Integer>());
            progressData.put("totalExercises", totalExercises);
            progressData.put("lastUpdated", new Date());
            
            db.collection("users")
                    .document(user.getUid())
                    .collection("progress")
                    .document(exerciseCategory)
                    .set(progressData)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                callback.onProgressLoaded(0, totalExercises, unlocked);
                            } else {
                                callback.onError("Failed to initialize progress");
                            }
                        }
                    });
        } else {
            callback.onProgressLoaded(0, totalExercises, unlocked);
        }
    }
    
    /**
     * Update progress in Firestore
     */
    private void updateFirestoreProgress(String exerciseCategory, List<Integer> completedExercises, 
                                       int totalExercises, UpdateCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated for Firestore update");
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "Updating Firestore progress for user: " + user.getUid());
        Log.d(TAG, "Path: users/" + user.getUid() + "/progress/" + exerciseCategory);
        
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("completedExercises", completedExercises);
        progressData.put("totalExercises", totalExercises);
        progressData.put("lastUpdated", new Date());
        
        Log.d(TAG, "Progress data to save: " + progressData.toString());
        
        // Update both the progress subcollection and the main user document
        db.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document(exerciseCategory)
                .set(progressData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Successfully updated Firestore progress");
                            
                            // Also update the main user document with total workout count
                            updateUserTotalWorkouts(user.getUid(), completedExercises.size());
                            
                            callback.onSuccess();
                        } else {
                            Log.e(TAG, "Failed to update Firestore progress", task.getException());
                            String errorMessage = task.getException().getMessage();
                            
                            // Check if it's a permission error - fallback to guest mode
                            if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                                Log.w(TAG, "Permission denied for Firestore write, falling back to guest mode");
                                // Save to local storage instead
                                saveProgressAsGuest(exerciseCategory, completedExercises, callback);
                            } else {
                                callback.onError("Failed to update progress: " + errorMessage);
                            }
                        }
                    }
                });
    }
    
    /**
     * Save progress locally when Firebase is not available
     */
    private void saveProgressAsGuest(String exerciseCategory, List<Integer> completedExercises, UpdateCallback callback) {
        String key = GUEST_PROGRESS_KEY + "_" + exerciseCategory;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < completedExercises.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(completedExercises.get(i));
        }
        
        prefs.edit().putString(key, sb.toString()).apply();
        Log.d(TAG, "Saved progress locally as guest: " + sb.toString());
        callback.onSuccess();
    }
    
    /**
     * Guest user progress management
     */
    private void getGuestProgress(String exerciseCategory, ProgressCallback callback) {
        String key = GUEST_PROGRESS_KEY + "_" + exerciseCategory;
        String progressJson = prefs.getString(key, "");
        
        List<Integer> completed = new ArrayList<>();
        if (!progressJson.isEmpty()) {
            String[] ids = progressJson.split(",");
            for (String id : ids) {
                try {
                    completed.add(Integer.parseInt(id.trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid exercise ID in guest progress: " + id);
                }
            }
        }
        
        int totalExercises = getTotalExercisesCount(exerciseCategory);
        List<Integer> unlocked = getUnlockedExercises(completed, totalExercises);
        
        callback.onProgressLoaded(completed.size(), totalExercises, unlocked);
    }
    
    private void completeGuestExercise(String exerciseCategory, int exerciseId, UpdateCallback callback) {
        getGuestProgress(exerciseCategory, new ProgressCallback() {
            @Override
            public void onProgressLoaded(int completedExercises, int totalExercises, List<Integer> unlockedExercises) {
                // Get current completed exercises
                String key = GUEST_PROGRESS_KEY + "_" + exerciseCategory;
                String progressJson = prefs.getString(key, "");
                
                List<Integer> completed = new ArrayList<>();
                if (!progressJson.isEmpty()) {
                    String[] ids = progressJson.split(",");
                    for (String id : ids) {
                        try {
                            completed.add(Integer.parseInt(id.trim()));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid exercise ID: " + id);
                        }
                    }
                }
                
                // Add new exercise if not already completed
                if (!completed.contains(exerciseId)) {
                    completed.add(exerciseId);
                }
                
                // Save back to SharedPreferences
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < completed.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(completed.get(i));
                }
                
                prefs.edit().putString(key, sb.toString()).apply();
                callback.onSuccess();
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Get total number of exercises for a category
     */
    private int getTotalExercisesCount(String exerciseCategory) {
        switch (exerciseCategory) {
            case YOUTH_EXERCISES:
                return 15; // Based on SecondActivity exercise count
            case ADULT_EXERCISES:
                return 15; // Based on SecondActivity2 exercise count
            default:
                return 10;
        }
    }
    
    /**
     * Check if an exercise is unlocked
     */
    public void isExerciseUnlocked(String exerciseCategory, int exerciseId, UnlockCheckCallback callback) {
        getProgress(exerciseCategory, new ProgressCallback() {
            @Override
            public void onProgressLoaded(int completedExercises, int totalExercises, List<Integer> unlockedExercises) {
                callback.onResult(unlockedExercises.contains(exerciseId));
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error checking if exercise is unlocked: " + error);
                callback.onResult(exerciseId == 1); // Always allow first exercise
            }
        });
    }
    
    public interface ProgressPercentageCallback {
        void onResult(int percentage);
    }
    
    /**
     * Get progress percentage for display
     */
    public void getProgressPercentage(String exerciseCategory, ProgressPercentageCallback callback) {
        getProgress(exerciseCategory, new ProgressCallback() {
            @Override
            public void onProgressLoaded(int completedExercises, int totalExercises, List<Integer> unlockedExercises) {
                int percentage = totalExercises > 0 ? (completedExercises * 100) / totalExercises : 0;
                callback.onResult(percentage);
            }
            
            @Override
            public void onError(String error) {
                callback.onResult(0);
            }
        });
    }
    
    /**
     * Update user's total workout count in main document
     */
    private void updateUserTotalWorkouts(String uid, int totalCompletedExercises) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalWorkouts", totalCompletedExercises);
        updates.put("lastActive", new Date());
        
        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Updated user total workouts: " + totalCompletedExercises);
                        } else {
                            Log.e(TAG, "Failed to update user total workouts", task.getException());
                        }
                    }
                                 });
    }
    
    /**
     * Test Firebase connectivity and permissions
     */
    public void testFirebaseConnection() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Testing Firebase connection for user: " + user.getUid());
            
            // Try to read the user document
            db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "User document exists: " + document.getData());
                            } else {
                                Log.d(TAG, "User document does not exist");
                            }
                        } else {
                            Log.e(TAG, "Error reading user document", task.getException());
                        }
                    }
                });
                
            // Try to write a test progress document
            Map<String, Object> testData = new HashMap<>();
            testData.put("test", "connection");
            testData.put("timestamp", new Date());
            
            db.collection("users")
                .document(user.getUid())
                .collection("progress")
                .document("test")
                .set(testData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Test write successful");
                        } else {
                            Log.e(TAG, "Test write failed", task.getException());
                        }
                    }
                });
        } else {
            Log.d(TAG, "No user authenticated for Firebase test");
        }
    }
} 