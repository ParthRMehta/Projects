package org.tpoly.pendingsubmissionsviewer.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.firebase.auth.FirebaseAuth;

import org.tpoly.pendingsubmissionsviewer.ClassroomServiceHelper;
import org.tpoly.pendingsubmissionsviewer.R;

import java.util.HashSet;
import java.util.Set;

public abstract class LoginActivity extends AppCompatActivity {
    private static final String TAG = "TPLActivity";
    private static final int REQUEST_CODE_SIGN_IN = 9001;

    protected GoogleSignInClient mGoogleSignInClient;
    protected ClassroomServiceHelper mClassroomServiceHelper;

    //This is for cancel task.
    protected String account;
    protected ProgressDialog dialog;

    boolean isFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeSignInClient();
        if(isFirstTime){
            isFirstTime = false;
            checkAlreadyLogin(true);
        } else {
            checkAlreadyLogin(false);
        }
    }

    public void checkAlreadyLogin(boolean firstTime) {
        Task<GoogleSignInAccount> task = mGoogleSignInClient.silentSignIn();
        if (task.isSuccessful()) {
//            GoogleSignInAccount signInAccount = task.getResult();
            makeClassroomHelper();
            Log.i("updateUI", String.valueOf(1));
            if(firstTime) updateUI(true, false);
        } else {
            task.addOnSuccessListener(googleSignInAccount -> {
                Log.i("updateUI", String.valueOf(2));
                makeClassroomHelper();
                updateUI(true, false);
            }).addOnFailureListener(e -> updateUI(false, false));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SIGN_IN) {
            Log.d(TAG, String.valueOf(data));
            Bundle bundle = data.getExtras();
            if (bundle != null) {
                Log.d("123", String.valueOf(bundle.keySet().size()));
                for (String key : bundle.keySet()) {
                    Log.e(TAG, key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
                }
            }
            if (resultCode == Activity.RESULT_OK) {
                handleSignInResult(data);
            }
            else {
                if(dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Log.d(TAG, "Sign-in wasn't successful.");
            }
        }
    }

    // can we do it using firebase...I need to implement a concept asap
    // Could we try debugging this issue instead? okay.

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleSignInAccount -> {
                    // Login Success!
                    if(dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    account = googleSignInAccount.getEmail();
                    Log.d(TAG, "account : " + account);
                    Log.i("updateUI", String.valueOf(3));
                    updateUI(true, false);
                    makeClassroomHelper();
                })
                .addOnFailureListener(e -> {
                    // Login Fail!
                    updateUI(false, false);
                });
    }

    protected void thirdPartLogin() {
        dialog.setMessage("Login....");
        dialog.show();
        makeSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }



    protected void thirdPartLogout() {
        dialog.setMessage("Logout....");
        dialog.show();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            if(dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            FirebaseAuth.getInstance().signOut();
            updateUI(false, true);
            mGoogleSignInClient = null;
            mClassroomServiceHelper = null;
            afterLogout();
        });

    }

    protected abstract void afterLogout();
    protected abstract void updateUI(boolean login, boolean islogout);

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

    }


    protected void makeSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .requestScopes(
                                new Scope(ClassroomScopes.CLASSROOM_COURSES_READONLY),
                                new Scope(ClassroomScopes.CLASSROOM_GUARDIANLINKS_STUDENTS_READONLY),
                                new Scope(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY),
                                new Scope(ClassroomScopes.CLASSROOM_ROSTERS_READONLY)
                        )
                        .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    protected void makeClassroomHelper() {
        Set<String> scopes = new HashSet<>();
        scopes.add(ClassroomScopes.CLASSROOM_COURSES_READONLY);
        scopes.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY);
        scopes.add(ClassroomScopes.CLASSROOM_GUARDIANLINKS_STUDENTS_READONLY);
        scopes.add(ClassroomScopes.CLASSROOM_ROSTERS_READONLY);

        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(
                this);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this,  scopes);
        assert googleSignInAccount != null;
        credential.setSelectedAccount(googleSignInAccount.getAccount());
        Classroom service = new Classroom.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
                .setApplicationName("GRExample")
                .build();

        mClassroomServiceHelper = new ClassroomServiceHelper(service);
    }

}
