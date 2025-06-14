package com.example.fitnessapp3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword, etAge;
    private Button btnSignup;
    private TextView tvLogin;
    private Spinner spinnerFitnessLevel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolBar);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAge = findViewById(R.id.etAge);
        spinnerFitnessLevel = findViewById(R.id.spinnerFitnessLevel);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);

        // Setup fitness level spinner
        setupFitnessLevelSpinner();

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set click listeners
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void setupFitnessLevelSpinner() {
        String[] fitnessLevels = {
            "Select Fitness Level",
            "Beginner (Just Starting)",
            "Intermediate (Some Experience)",
            "Advanced (Very Active)",
            "Expert (Athletic/Professional)"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, fitnessLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFitnessLevel.setAdapter(adapter);
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String fitnessLevel = spinnerFitnessLevel.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (fullName.length() < 2) {
            etFullName.setError("Name must be at least 2 characters");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Confirm password is required");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(ageStr)) {
            etAge.setError("Age is required");
            etAge.requestFocus();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 12 || age > 100) {
                etAge.setError("Age must be between 12 and 100");
                etAge.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAge.setError("Please enter a valid age");
            etAge.requestFocus();
            return;
        }

        if (fitnessLevel.equals("Select Fitness Level")) {
            Toast.makeText(this, "Please select your fitness level", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button during registration
        btnSignup.setEnabled(false);
        btnSignup.setText("Creating Account...");

        // Firebase registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnSignup.setEnabled(true);
                        btnSignup.setText("CREATE ACCOUNT");

                        if (task.isSuccessful()) {
                            // Registration success
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            // Update user profile with display name
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName)
                                        .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Save user data to Firestore
                                                    saveUserToFirestore(user.getUid(), fullName, email, age, fitnessLevel);
                                                } else {
                                                    Toast.makeText(SignupActivity.this, 
                                                            "Failed to update profile", 
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            // Registration failed
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("email address is already in use")) {
                                etEmail.setError("Email already registered");
                                etEmail.requestFocus();
                            } else {
                                Toast.makeText(SignupActivity.this, "Registration failed: " + 
                                        errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email, int age, String fitnessLevel) {
        User user = new User(uid, name, email);
        user.setAge(age);
        user.setFitnessLevel(fitnessLevel);
        
        firestore.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, 
                            "Account created successfully! Welcome to Fitness App!", 
                            Toast.LENGTH_SHORT).show();
                    
                    // Navigate to MainActivity
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    finishAffinity(); // Clear all previous activities
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignupActivity.this, 
                            "Failed to save user data: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 