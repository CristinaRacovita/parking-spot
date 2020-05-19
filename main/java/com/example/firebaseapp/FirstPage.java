package com.example.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import javax.annotation.Nullable;

public class FirstPage extends AppCompatActivity {

    private TextView mVerify, mName, mEmail, mPhone;
    private ImageView mPhoto;
    private Button mButtonVerify;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private FirebaseFirestore firebaseFirestore;
    private String userId;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_page);

        mVerify = findViewById(R.id.verified);
        mButtonVerify = findViewById(R.id.verify_button);
        mName = findViewById(R.id.name2);
        mEmail = findViewById(R.id.email2);
        mPhone = findViewById(R.id.phone2);
        mPhoto = findViewById(R.id.account);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference(); //use for upload photos

        //ca sa ramana poza fara sa o schimbi
        StorageReference profileRef = storageReference.child("users/"+firebaseAuth.getCurrentUser().getUid()+"/profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Picasso.get().load(uri).into(mPhoto);
            }
        });

        userId = firebaseAuth.getCurrentUser().getUid();

        user = firebaseAuth.getCurrentUser();
        if (!user.isEmailVerified()) {
            mButtonVerify.setVisibility(View.VISIBLE);
            mVerify.setVisibility(View.VISIBLE);
        }

        final DocumentReference documentReference = firebaseFirestore.collection("users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() { // To “listen” for changes on a document or collection, we create a snapshot listener
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
               if(documentSnapshot != null) {
                   mPhone.setText(documentSnapshot.getString("Phone"));
                   mEmail.setText(documentSnapshot.getString("Email"));
                   mName.setText(documentSnapshot.getString("Full Name"));
               }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000){
            if(resultCode == Activity.RESULT_OK){ //daca am ceva rezultat dat
                Uri imageUri = data.getData();

                mPhoto.setImageURI(imageUri);

                uploadImageToFirebase(imageUri);
            }
        }
    }

    public void uploadImageToFirebase(Uri imageUri){
        //upload the image to Firebase storage

        final StorageReference fileRef = storageReference.child("users/"+firebaseAuth.getCurrentUser().getUid()+"/profile.jpg");
        fileRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //Toast.makeText(FirstPage.this, "Image Uploaded.", Toast.LENGTH_LONG).show();
                fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Picasso.get().load(uri).into(mPhoto);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(FirstPage.this, "Failed! "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void verify_pass(android.view.View view) { //trimiterea mail-ului pentru verificare

        user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                Toast.makeText(FirstPage.this, "Verification email has been sent.", Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(FirstPage.this, "Verification email not sent " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

    }

    public void edit_image(android.view.View view){
        // open gallery
        Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(openGalleryIntent,1000);
    }

    public void reset_pass(android.view.View view){
        String mail = mEmail.getText().toString().trim();
        AlertDialog.Builder passResetDialog = new AlertDialog.Builder(view.getContext());
        passResetDialog.setTitle("Reset Password.");
        passResetDialog.setMessage("Reset link sent to your email.");

        firebaseAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(FirstPage.this,"Reset link sent to your email.",Toast.LENGTH_LONG).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(FirstPage.this,"Error! Reset link is not sent "+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });

        passResetDialog.create().show();
    }

    public void logout(android.view.View view){
        FirebaseAuth.getInstance().signOut(); //logout
        startActivity(new Intent(getApplicationContext(),LoginActivity.class));
        //finish();
    }

    public void go_to_map(android.view.View view){
        startActivity(new Intent(getApplicationContext(),MapActivity.class));
    }
}
