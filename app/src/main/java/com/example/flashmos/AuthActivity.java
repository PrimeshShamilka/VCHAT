package com.example.flashmos;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthActivity extends AppCompatActivity {
    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN= 9001;
    private FirebaseAuth mAuth;
    private Button signin_btn;
    private static final String TAG = "GoogleActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        signin_btn = findViewById(R.id.auth_btn);
        signin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultLauncher.launch(new Intent(mGoogleSignInClient.getSignInIntent()));
            }
        });
        createRequest();
        mAuth = FirebaseAuth.getInstance();
    }
    private void createRequest(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("787983473122-289q6f1ipi7fmvdiugbakhmpgflel05m.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this,gso);

    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d(TAG, "onActivityResult: ");
//        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
//        if (requestCode == RC_SIGN_IN) {
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            try {
//                // Google Sign In was successful, authenticate with Firebase
//                GoogleSignInAccount account = task.getResult(ApiException.class);
//                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
//                firebaseAuthWithGoogle(account.getIdToken());
//            } catch (ApiException e) {
//                // Google Sign In failed, update UI appropriately
//                Log.w(TAG, "Google sign in failed", e);
//            }
//        }
   // }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }
    private void updateUI(FirebaseUser user) {

        if(user!=null){
            startActivity(new Intent(AuthActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }
    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if(result.getResultCode()==Activity.RESULT_OK) {
                Intent intent = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        // Google Sign In failed, update UI appropriately
                        Log.w(TAG, "Google sign in failed", e);
                    }
                }


        }
    });

}