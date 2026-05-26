package com.procollegia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.procollegia.utils.InputValidator;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextView tabPersonal, tabAcademic;
    private ViewFlipper viewFlipper;
    private Spinner spinnerRole, spinnerYear;
    private Button btnNext, btnPrev, btnSubmit;
    private TextView tvBackToLogin;
    
    private EditText etName, etPhone, etRegEmail, etRegPassword;
    private EditText etUucms, etCourse, etSem, etLoginCode;
    private ProgressBar progressRegister;

    // Temporary institution login code — will be fetched from Firestore once Principal panel is built
    private static final String INSTITUTION_LOGIN_CODE = "Login";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tabPersonal = findViewById(R.id.tabPersonal);
        tabAcademic = findViewById(R.id.tabAcademic);
        viewFlipper = findViewById(R.id.viewFlipper);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerYear = findViewById(R.id.spinnerYear);
        
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etUucms = findViewById(R.id.etUucms);
        etCourse = findViewById(R.id.etCourse);
        etSem = findViewById(R.id.etSem);
        etLoginCode = findViewById(R.id.etLoginCode);
        progressRegister = findViewById(R.id.progressRegister);


        // Smooth animations
        viewFlipper.setInAnimation(this, android.R.anim.slide_in_left);
        viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right);

        tabPersonal.setOnClickListener(v -> switchToPersonal());
        tabAcademic.setOnClickListener(v -> switchToAcademic());

        btnNext.setOnClickListener(v -> switchToAcademic());
        btnPrev.setOnClickListener(v -> switchToPersonal());
        
        tvBackToLogin.setOnClickListener(v -> finish());
        
        btnSubmit.setOnClickListener(v -> {
            String selectedRole = spinnerRole.getSelectedItem().toString();
            String selectedYear = spinnerYear.getSelectedItem().toString();
            String name     = etName.getText().toString().trim();
            String phone    = etPhone.getText().toString().trim();
            String email    = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            String uucms    = etUucms.getText().toString().trim();
            String course   = etCourse.getText().toString().trim();
            String sem      = etSem.getText().toString().trim();
            String loginCode = etLoginCode.getText().toString().trim();

            // ── Validations ──
            if (!InputValidator.arePersonalDetailsValid(name, phone, email, password)) {
                Toast.makeText(this, "Please fill in all personal details correctly", Toast.LENGTH_SHORT).show();
                switchToPersonal();
                return;
            }

            // Students must supply their UUCMS ID
            if (selectedRole.equals("Student") && !InputValidator.isValidUucmsId(uucms)) {
                Toast.makeText(this, "UUCMS ID is mandatory for students", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate institution login code
            if (!InputValidator.isValidLoginCode(loginCode, INSTITUTION_LOGIN_CODE)) {
                if (loginCode.isEmpty()) {
                    Toast.makeText(this, "Please enter the institution login code", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid login code. Contact your institution.", Toast.LENGTH_LONG).show();
                    etLoginCode.setError("Invalid code");
                }
                return;
            }

            progressRegister.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);
            btnPrev.setEnabled(false);

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", uid);
                        userMap.put("name", name);
                        userMap.put("phone", phone);
                        userMap.put("email", email);
                        userMap.put("role", selectedRole);
                        userMap.put("uucmsId", uucms);
                        userMap.put("department", course);
                        userMap.put("year", selectedYear);
                        userMap.put("semester", sem);
                        userMap.put("section", "Sec A");

                        if (selectedRole.equals("Student")) {
                            userMap.put("honorScore", 500);}

                        FirebaseFirestore.getInstance().collection("users").document(uid).set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                progressRegister.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                                
                                Intent intent;
                                if (selectedRole.equals("Teacher")) {
                                    intent = new Intent(RegisterActivity.this, TeacherDashboardActivity.class);
                                } else if (selectedRole.equals("HOD")) {
                                    intent = new Intent(RegisterActivity.this, HodDashboardActivity.class);
                                } else if (selectedRole.equals("PT Admin")) {
                                    intent = new Intent(RegisterActivity.this, PtAdminDashboardActivity.class);
                                } else if (selectedRole.equals("Principal")) {
                                    intent = new Intent(RegisterActivity.this, PrincipalDashboardActivity.class);
                                } else {
                                    intent = new Intent(RegisterActivity.this, StudentDashboardActivity.class);
                                }
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressRegister.setVisibility(View.GONE);
                                btnSubmit.setEnabled(true);
                                btnPrev.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Error saving details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                    } else {
                        progressRegister.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        btnPrev.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });
    }

    private void switchToPersonal() {
        if (viewFlipper.getDisplayedChild() != 0) {
            viewFlipper.setInAnimation(this, android.R.anim.slide_in_left);
            viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
            viewFlipper.setDisplayedChild(0);
            updateTabs(true);
        }
    }

    private void switchToAcademic() {
        if (viewFlipper.getDisplayedChild() != 1) {
            // Slide academic in from right
            viewFlipper.setInAnimation(this, R.anim.slide_in_right);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
            viewFlipper.setDisplayedChild(1);
            updateTabs(false);
        }
    }

    private void updateTabs(boolean isPersonalActive) {
        if (isPersonalActive) {
            tabPersonal.setBackgroundResource(R.drawable.bg_neumorph_tab_active);
            tabPersonal.setTextColor(ContextCompat.getColor(this, R.color.text_on_accent));
            
            tabAcademic.setBackgroundResource(android.R.color.transparent);
            tabAcademic.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            tabAcademic.setBackgroundResource(R.drawable.bg_neumorph_tab_active);
            tabAcademic.setTextColor(ContextCompat.getColor(this, R.color.text_on_accent));
            
            tabPersonal.setBackgroundResource(android.R.color.transparent);
            tabPersonal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }
}
