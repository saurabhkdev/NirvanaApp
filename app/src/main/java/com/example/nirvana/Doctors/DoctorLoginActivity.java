package com.example.nirvana.Doctors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.nirvana.LoginByPasswordActivity;
import com.example.nirvana.Model.CountryCode;
import com.example.nirvana.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class DoctorLoginActivity extends AppCompatActivity {
    private Spinner spinner;
    ProgressBar progressBar1;
    FirebaseAuth mAuth;
    String phone1;
    ProgressBar progressBar;
    String code,Id1,flag="0";
    String phonenumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_login);
        Toolbar toolbar=findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        DoctorLoginFragment doctorLoginFragment=new DoctorLoginFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.doctor_login_layout,doctorLoginFragment);
        fragmentTransaction.commit();
    }

    public void signup_doctor(View view) {
        Intent intent =new Intent(this, DoctorSignupActivity.class);
        startActivity(intent);
    }
    public void login_doctor(View view) {
        EditText phone,Id;

        spinner = findViewById(R.id.spinnerCountries);
        progressBar=findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.VISIBLE);
        phone=findViewById(R.id.phone1_doctor);
        code = CountryCode.countryAreaCodes[spinner.getSelectedItemPosition()];
        if(TextUtils.isEmpty(phone.getText()))
        {
            phone.setError("Enter  the phone number");
            progressBar.setVisibility(View.GONE);
        }
        phone1=phone.getText().toString();
        phonenumber = "+" + code +phone1;
        if(phone1.length()<10)
        {
            phone.setError("Please enter  the valid phone number");
            progressBar.setVisibility(View.GONE);
        }
        FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance();
        DatabaseReference databaseReference=firebaseDatabase.getReference().child("Doctors");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    HashMap<String,Object> hashMap=(HashMap<String, Object>)dataSnapshot.getValue();
                    for(String key:hashMap.keySet()) {
                        Object data = hashMap.get(key);
                        HashMap<String, Object> userData = (HashMap<String, Object>) data;
                        String phone = (String) userData.get("phone");
                        if (phone.equals(phonenumber)) {
                            flag="1";
                        }
                }
                if(flag.equals("1"))
                    progressBar.setVisibility(View.VISIBLE);
                        Bundle bundle=new Bundle();
                        bundle.putString("phone",phonenumber);
                        DoctorLoginVerification doctorLoginVerification=new DoctorLoginVerification();
                        doctorLoginVerification.setArguments(bundle);
                        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.doctor_login_layout,doctorLoginVerification);
                        fragmentTransaction.commit();
                }
                else
                {
                    Toast.makeText(DoctorLoginActivity.this,"This number does not attach with any account.",Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void verify_doctor(View view) {
        mAuth=FirebaseAuth.getInstance();
        progressBar1=findViewById(R.id.progress_verify);
        progressBar1.setVisibility(View.VISIBLE);
       try {

               signInWithPhoneAuthCredential(DoctorLoginVerification.credential);
       }
           catch(Exception e)
           {
               progressBar1.setVisibility(View.GONE);
               Toast.makeText(this,"Please wait for the code or try again",Toast.LENGTH_SHORT).show();

           }

    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(DoctorLoginActivity.this,
                        new OnCompleteListener<AuthResult>() {

                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    //verification successful we will start the profile activity
                                    Id1=mAuth.getCurrentUser().getUid();
                                    progressBar1.setVisibility(View.GONE);
                                    Intent intent=new Intent(DoctorLoginActivity.this, Doctor_Welcome_Activity.class);
                                    intent.putExtra("phone",phone1);
                                    intent.putExtra("Id",Id1);
                                    startActivity(intent);
                                    DoctorLoginActivity.this.finish();

                                } else {

                                    //verification unsuccessful.. display an error message

                                    String message = "Somthing is wrong, we will fix it soon...";

                                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        message = "Invalid code entered...";
                                    }
                                    Toast.makeText(DoctorLoginActivity.this,message,Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
    }
    public void LoginByPassword(View view) {
        Intent intent=new Intent(this, LoginByPasswordActivity.class);
        intent.putExtra("phone",phonenumber);
        intent.putExtra("who","doctor");
        startActivity(intent);
    }
}
