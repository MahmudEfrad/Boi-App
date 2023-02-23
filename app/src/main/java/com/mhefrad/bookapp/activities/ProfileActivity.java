package com.mhefrad.bookapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mhefrad.bookapp.MyApplication;
import com.mhefrad.bookapp.R;
import com.mhefrad.bookapp.adapter.AdapterPdfFavorite;
import com.mhefrad.bookapp.databinding.ActivityProfileBinding;
import com.mhefrad.bookapp.models.ModelPdf;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    //view binding
    private ActivityProfileBinding binding;
    
    //firebase auth, loading uer data using user uid
    private FirebaseAuth firebaseAuth;

    //firebase current user
    private FirebaseUser firebaseUser;

    //arraylist to hold the books
    private ArrayList<ModelPdf> pdfArrayList;
    //adapter to set in recyclerview
    private AdapterPdfFavorite adapterPdfFavorite;
    
    private static final String TAG =  "PROFILE_TAG";

    //progress dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //reset data of user info
        binding.accountTypeTv.setText("N/A");
        binding.memberDateTv.setText("N/A");
        binding.favoriteBookCountTv.setText("N/A");
        binding.accountStatusTv.setText("N/A");

        //setup firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        //get current user
        firebaseUser = firebaseAuth.getCurrentUser();

        //init/setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        loadUserInfo();
        loadFavoriteBooks();

        //handle click, start profile edit page
        binding.profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, ProfileEditActivity.class));
            }
        });

        //handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, verify user if not
        binding.accountStatusTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseUser.isEmailVerified()){
                    //already verified
                    Toast.makeText(ProfileActivity.this, "Already verified..", Toast.LENGTH_SHORT).show();
                }
                else {
                    emailVerificationDialog();
                }
            }
        });

    }

    private void emailVerificationDialog() {
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Email")
                .setMessage("Are you sure you want to send email verification instructions to your email "+firebaseUser.getEmail())
                .setPositiveButton("SEND", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendEmailVerification();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void sendEmailVerification() {
        //show progress
        progressDialog.setMessage("Sending email verification instructions to your email "+firebaseUser.getEmail());
        progressDialog.show();

        firebaseUser.sendEmailVerification()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //successfully sent
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Instructions sent, check your email "+firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to send
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Failed due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserInfo() {
        Log.d(TAG, "loadUserInfo: Loading user info...");

        //get email verification status, after verification you have to re login to get changes
        if (firebaseUser.isEmailVerified()){
            binding.accountStatusTv.setText("Verified");
        } else {
            binding.accountStatusTv.setText("Not verified");
        }

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        reference.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get all info of user here from snapshot
                        String email = ""+snapshot.child("email").getValue();
                        String name = ""+snapshot.child("name").getValue();
                        String profileImage = ""+snapshot.child("profileImage").getValue();
                        String timeStamp = ""+snapshot.child("timeStamp").getValue();
                        String uId = ""+snapshot.child("uId").getValue();
                        String userType = ""+snapshot.child("userType").getValue();

                        //format date to dd/MM/yyyy
                        String formattedDate = MyApplication.formatTimestamp(Long.parseLong(timeStamp));

                        //set data to UI
                        binding.emailTv.setText(email);
                        binding.nameTv.setText(name);
                        binding.memberDateTv.setText(formattedDate);
                        binding.accountTypeTv.setText(userType);

                        //set image, using glide
                        Glide.with(ProfileActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.ic_person_gray)
                                .into(binding.profileIv);


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: failed to load user info "+error.getMessage());
                    }
                });
        
    }

    private void loadFavoriteBooks() {
        //init list
        pdfArrayList = new ArrayList<>();

        //load favorite books from database
        //users > userId > Favorites
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before starting adding data
                        pdfArrayList.clear();
                        
                        for (DataSnapshot ds: snapshot.getChildren()){
                            //we will only get the bookId here, and we got other details in adapter using that bookId
                            String bookId = ""+ds.child("bookId").getValue();

                            //set id to model
                            ModelPdf modelPdf = new ModelPdf();
                            modelPdf.setId(bookId);

                            //add model to list
                            pdfArrayList.add(modelPdf);
                        }

                        //set number of favorite books
                        binding.favoriteBookCountTv.setText(""+pdfArrayList.size()); //cant set int/long to Textview so concatenate with string
                        //setup adapter
                        adapterPdfFavorite = new AdapterPdfFavorite(ProfileActivity.this,pdfArrayList);
                        //set adapter to recyclerview
                        binding.booksRv.setAdapter(adapterPdfFavorite);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: failed to load favorite books from db "+error.getMessage());
                    }
                });
    }
}