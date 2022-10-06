package org.tpoly.pendingsubmissionsviewer.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import android.view.Menu;
import android.view.MenuInflater;
import androidx.appcompat.widget.SearchView;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.firebase.auth.FirebaseAuth;
import com.woxthebox.draglistview.DragListView;

import org.tpoly.pendingsubmissionsviewer.MySuggestionProvider;
import org.tpoly.pendingsubmissionsviewer.R;
import org.tpoly.pendingsubmissionsviewer.adapter.AssignmentAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SubmissionsViewer extends LoginActivity
{
    private static final String TAG = "TPCActivity";

    private DragListView assignmentListView;
    private AssignmentAdapter assignmentListAdapter;
    private SwipeRefreshLayout layout;
    private String courseID;
    private TextView mm;
    private SearchView searchView;
    private Button copy_all_btn;
    private MenuItem menuItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.assignments_menu, menu);

        menuItem = menu.findItem(R.id.search);
        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if(layout.isRefreshing()){
                    Toast.makeText(SubmissionsViewer.this, "Please wait for the data to load...", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menuItem.getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                handleSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                assignmentListAdapter.getFilter().filter(query);
                return true;
            }
        });



        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.assignments);

        String title = getIntent().getExtras().getString("courseName");
        if(title==null) title="Pending Submissions";
        setTitle(title);

        mm = findViewById(R.id.no_assignment_found);

        courseID = getIntent().getExtras().getString("courseID");

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        layout = findViewById(R.id.swipe_refresh);
        layout.setColorSchemeColors(getResources().getColor(R.color.classroom_yellow));
        layout.setRefreshing(true);

        assignmentListView = findViewById(R.id.assignmentList);

        layout.setOnRefreshListener(() -> {

            SubmissionsViewer.isFirstTime = true;

            if(!searchView.isIconified()){
                menuItem.collapseActionView();
            }

            mm.setVisibility(View.VISIBLE);
            mm.setText(R.string.loading);
            copy_all_btn.setVisibility(View.GONE);
            assignmentListView.setVisibility(View.GONE);
            Log.i("!!!!!!!!", "1");
            updateUI(mGoogleSignInClient!=null, false);
        });


        assignmentListView.setLayoutManager(new LinearLayoutManager(SubmissionsViewer.this));
        makeClassroomHelper();
        assignmentListAdapter = new AssignmentAdapter(new ArrayList<>(), R.layout.assignment, R.id.assignment, true, mClassroomServiceHelper);
        assignmentListView.setAdapter(assignmentListAdapter, true);

        copy_all_btn = findViewById(R.id.copy_all_btn);

        View.OnClickListener onClickListener = v -> {
            if(layout.isRefreshing()){
                Toast.makeText(SubmissionsViewer.this, "Please wait for the assignments to load.", Toast.LENGTH_SHORT).show();
//                return;
            }

            ArrayList<Pair<Long, CourseWork>> result =  assignmentListAdapter.getFilteredData();

            if(result.isEmpty()){
                Toast.makeText(SubmissionsViewer.this, "There are no assignments to copy!",Toast.LENGTH_SHORT).show();
            } else {

                List<Pair<Long, CourseWork>> fData =  assignmentListAdapter.getFilteredData();

                Dialog dialog = ProgressDialog.show(SubmissionsViewer.this, "Copying to clipboard...", "This might take a while depending on the number of assignments and your internet connection...");

                dialog.show();

                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        try {
                            List<Student> students = mClassroomServiceHelper.getStudents(courseID);
                            List<String> courseWorkIds = new ArrayList<>();
                            for(Pair<Long, CourseWork> data : fData){
                                final CourseWork assignment = data.second;
                                courseWorkIds.add(assignment.getId());
                            }

                            Log.i(TAG, "We are here");

                            mClassroomServiceHelper.listSubmissionsOfMultiple(courseID, courseWorkIds.toArray(new String[0])).addOnSuccessListener(
                                    pairs -> {

                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        StringBuilder builder = new StringBuilder("Status of assignments in classroom:\n\n");

                                        for(Pair<String, List<StudentSubmission>> pair : pairs){

                                            final String assignmentName = pair.first;
                                            final List<StudentSubmission> submissions = pair.second;

                                            ArrayList<String> not_submitted = new ArrayList<>();
                                            ArrayList<String> not_checked = new ArrayList<>();

                                            try {

                                                for(StudentSubmission submission: submissions){
                                                    if(submission.getState().equals("CREATED")){
                                                        for(Student student:students){
                                                            if(student.getUserId().equals(submission.getUserId())){
                                                                not_submitted.add(student.getProfile().getName().getFullName());
                                                            }
                                                        }
                                                    } else if (submission.getState().equals("TURNED_IN")){
                                                        for(Student student:students){
                                                            if(student.getUserId().equals(submission.getUserId())){
                                                                not_checked.add(student.getProfile().getName().getFullName());
                                                            }
                                                        }
                                                    }
                                                }

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            builder.append(String.format("\n\nStatus of '%s' assignment:\n\n", assignmentName));

                                            if(not_submitted.isEmpty()){
                                                builder.append("All the students have submitted their assignments.\n");
                                            } else {
                                                builder.append("\nThe following students haven't submitted their assignments:\n");
                                                for(String name : not_submitted){
                                                    builder.append("\n");
                                                    builder.append(name);
                                                }
                                            }

                                            builder.append("\n");

                                            if(not_checked.isEmpty()){
                                                builder.append("\nAll the students have submitted their assignments have received them back.\n");
                                            } else {
                                                builder.append("\nThe following students' assignment hasn't been checked yet:\n");
                                                for(String name : not_checked){
                                                    builder.append("\n");
                                                    builder.append(name);
                                                }
                                            }

                                            Log.e(TAG, "Done");
                                            runOnUiThread(()->Toast.makeText(SubmissionsViewer.this, "Copied pending submission data to clipboard!", Toast.LENGTH_LONG).show());
                                            Log.i(TAG, builder.toString());
                                        }

                                        ClipData clip = ClipData.newPlainText("Copied Text", builder.toString());
                                        clipboard.setPrimaryClip(clip);

                                        Toast.makeText(SubmissionsViewer.this, "Copied the pending submission data of all the assignments to clipboard!", Toast.LENGTH_LONG).show();

                                        dialog.cancel();
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }


        };

        copy_all_btn.setOnClickListener(onClickListener);

    }

    private void handleSearch(String query) {
        Log.d("SearchView", query);
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null);
        Log.d("SearchView", query);
    }

    @Override
    protected void afterLogout() {

    }

    private static boolean isFirstTime = true;

    @Override
    protected void updateUI(boolean login, boolean isLogout) {
        Log.i("updateUI", String.valueOf(login));
        if(login){
            if(mClassroomServiceHelper!=null) {

                Log.d("SubmissionViewer", "mClassroomServiceHelper was initialized.");

                mClassroomServiceHelper.listCourseWorks(courseID, SubmissionsViewer.isFirstTime)
                                            .addOnSuccessListener(courseWorks -> {

                                                SubmissionsViewer.isFirstTime = false;

                                                if(courseWorks==null || courseWorks.size()==0) {

                                                    Log.d(TAG, "No course work was found for " + courseID + ".");

                                                } else {

                                                    ArrayList<Pair<Long, CourseWork>> assignmentArray = new ArrayList<>();

                                                    for (int i=0; i<courseWorks.size(); ++i) {
                                                        CourseWork courseWork = courseWorks.get(i);
                                                        assignmentArray.add(new Pair<>((long) i, courseWork));
                                                    }

                                                    if(assignmentArray.isEmpty()){
                                                        mm.setText(R.string.no_assignments_found);
                                                        mm.setVisibility(View.VISIBLE);
                                                        Toast.makeText(SubmissionsViewer.this, "No assignments were found!", Toast.LENGTH_LONG).show();

                                                    } else {
                                                        mm.setVisibility(View.GONE);
                                                    }

                                                    assignmentListAdapter.setItemList(assignmentArray);

                                                    Log.d(TAG, "CourseWork size : " + courseWorks.size());
                                                    if(layout!=null) layout.setRefreshing(false);
                                                    assignmentListView.setVisibility(View.VISIBLE);
                                                    copy_all_btn.setVisibility(View.VISIBLE);

                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                SubmissionsViewer.isFirstTime = false;
                                                Log.d(TAG, "Failed to get course data for " + courseID + ".");
                                                Log.d(TAG, e.getMessage());
                                                if(layout!=null) layout.setRefreshing(false);
                                            });
                                }
            else {
                Toast.makeText(SubmissionsViewer.this,
                        "mClassroomServiceHelper was not initialized.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            // Go back to the previous activity (if the user isn't logged in)
            if(isLogout){
                Toast.makeText(SubmissionsViewer.this,
                        "You are being logged out...",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SubmissionsViewer.this,
                        "Logging out due to an unexpected error occurred.",
                        Toast.LENGTH_LONG).show();
            }
            FirebaseAuth.getInstance().signOut();
            finish();
        }
    }
}
