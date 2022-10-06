package org.tpoly.pendingsubmissionsviewer.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.core.util.Pair;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.classroom.model.Course;
import com.google.firebase.auth.FirebaseAuth;
import com.woxthebox.draglistview.DragListView;

import org.tpoly.pendingsubmissionsviewer.R;
import org.tpoly.pendingsubmissionsviewer.adapter.CourseAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoursesViewer extends LoginActivity {
    private static final String TAG = "TPCActivity";

    DragListView courseListView;
    CourseAdapter courseListAdapter;
    SwipeRefreshLayout layout;
    TextView mm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.courses);
        setTitle("My Classrooms");

        mm = findViewById(R.id.no_courses_found);

        layout = findViewById(R.id.swipe_refresh);
        layout.setColorSchemeColors(getResources().getColor(R.color.classroom_yellow));
        layout.setRefreshing(true);

        layout.setOnRefreshListener(() -> {

            mm.setText(R.string.loading);
            Log.i("!!!!!!!!", "2");
            updateUI(mGoogleSignInClient!=null, false);

        });

        courseListView = findViewById(R.id.courseList);
        courseListView.setLayoutManager(new LinearLayoutManager(CoursesViewer.this));

        courseListAdapter = new CourseAdapter(new ArrayList<>(), R.layout.course, R.id.course, true);
        courseListView.setAdapter(courseListAdapter, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if(item.getItemId()==R.id.logout){
            thirdPartLogout();
            return true;
        }
        return false;
    }

    @Override
    protected void afterLogout() {

    }

    @Override
    protected void updateUI(boolean login, boolean isLogout) {
        Log.i("updateUI", String.valueOf(login));
        if(login){
            if(mClassroomServiceHelper!=null) {

                Log.d("Courses", "mClassroomServiceHelper was initialized.");

                mClassroomServiceHelper.listCourses()
                        .addOnSuccessListener(courses -> {

                            if(courses!=null && courses.size() > 0) {

                                ArrayList<Pair<Long, Course>> coursesArray = new ArrayList<>();

                                for (int i=0; i<courses.size(); ++i) {
                                    Course course = courses.get(i);

                                    // Only add those courses where the user is a teacher
                                    if(course.getTeacherFolder()!=null)
                                      coursesArray.add(new Pair<>((long) i, course));
                                }



                                if(coursesArray.isEmpty()){
                                    mm.setText(String.format(getString(R.string.no_courses_found), Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail()));
                                    mm.setVisibility(View.VISIBLE);
                                    Toast.makeText(CoursesViewer.this, "No courses were found!", Toast.LENGTH_LONG).show();
//                                        courseListView.
                                } else {
                                    mm.setVisibility(View.GONE);
                                }

                                courseListAdapter.setItemList(coursesArray);

                                Course course = courses.get(0);


//                                    // Get course works in selected course. show the success result?
//                                    mClassroomServiceHelper.listCourseWorks(course.getId())
//                                            .addOnSuccessListener(new OnSuccessListener<List<CourseWork>>() {
//                                                @Override
//                                                public void onSuccess(List<CourseWork> courseWorks) {
//                                                    if(courseWorks==null || courseWorks.size()==0) {
//                                                        Log.d(TAG, "No course work was found for " + course.getName() + ".");
//                                                    } else {
//                                                        Log.d(TAG, "CourseWork size : " + courseWorks.size());
//                                                    }
//                                                }
//                                            })
//                                            .addOnFailureListener(new OnFailureListener() {
//                                                @Override
//                                                public void onFailure(@NonNull Exception e) {
//                                                    Log.d(TAG, "Failed to get course data for " + course.getName() + ".");
//                                                    Log.d(TAG, e.getMessage());
//                                                }
//                                            });
//                                } else {
//
//                                    Log.d(TAG, "No courses were detected! " + String.valueOf(courses));
                            } else {
                                mm.setText(String.format(getString(R.string.no_courses_found), Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail()));
                                mm.setVisibility(View.VISIBLE);
                                Toast.makeText(CoursesViewer.this, "No courses were found!", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "No courses were detected! " + String.valueOf(courses));
                            }
                            if(layout!=null) layout.setRefreshing(false);
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CoursesViewer.this, R.string.courses_load_fail, Toast.LENGTH_LONG).show();
                                Log.d("Courses", "Failed to load courses... " + e.getMessage());
                                if(layout!=null) layout.setRefreshing(false);
                            }
                        });
            } else {
                Toast.makeText(CoursesViewer.this,
                        "mClassroomServiceHelper was not initialized.",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            // Go back to the previous activity (if the user isn't logged in)
            if(isLogout){
                Toast.makeText(CoursesViewer.this,
                        "You are being logged out...",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CoursesViewer.this,
                        "Logging out due to an unexpected error occurred.",
                        Toast.LENGTH_LONG).show();
            }
            FirebaseAuth.getInstance().signOut();
            finish();
        }
    }
}
