package org.tpoly.pendingsubmissionsviewer.adapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.woxthebox.draglistview.DragItemAdapter;

import org.tpoly.pendingsubmissionsviewer.ClassroomServiceHelper;
import org.tpoly.pendingsubmissionsviewer.R;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class AssignmentAdapter extends DragItemAdapter<Pair<Long, CourseWork>, AssignmentAdapter.ViewHolder> implements Filterable {

    private final int mLayoutId;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;
    private final ClassroomServiceHelper classroomServiceHelper;
    private ArrayList<Pair<Long, CourseWork>> originalData;
    private ArrayList<Pair<Long, CourseWork>> filteredData;
    private final AssignmentFilter aFilter = new AssignmentFilter();

    public AssignmentAdapter(ArrayList<Pair<Long, CourseWork>> list, int layoutId, int grabHandleId, boolean dragOnLongPress, final ClassroomServiceHelper classroomServiceHelper) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        this.classroomServiceHelper = classroomServiceHelper;
        originalData = list;
        filteredData = list;
        Log.i("Test1", String.valueOf(this.classroomServiceHelper));

        setItemList(list);
    }

    @Override
    public void setItemList(List<Pair<Long, CourseWork>> itemList) {
        originalData = (ArrayList<Pair<Long, CourseWork>>)itemList;
        filteredData = (ArrayList<Pair<Long, CourseWork>>)itemList;
        super.setItemList(itemList);
    }

    public ArrayList<Pair<Long, CourseWork>> getFilteredData() {
        return filteredData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.i("Parent", String.valueOf(parent.getChildCount()));

        View view = LayoutInflater.from(parent.getContext()).inflate(this.mLayoutId, parent, false); // this is invalid layout file
        return new ViewHolder(view, this.classroomServiceHelper);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        CourseWork courseWork = filteredData.get(position).second;

        TextView assignmentNameView = holder.assignmentLayout.findViewById(R.id.assignment_name);
        assignmentNameView.setText(courseWork.getTitle());

        TextView assignmentDescView = holder.assignmentLayout.findViewById(R.id.assignment_desc);
        assignmentDescView.setText(courseWork.getDescription());

        TextView courseIDView = holder.assignmentLayout.findViewById(R.id.courseID);
        courseIDView.setText(courseWork.getCourseId());

        TextView courseWorkView = holder.assignmentLayout.findViewById(R.id.courseWorkID);
        courseWorkView.setText(courseWork.getId());

        holder.itemView.setTag(filteredData.get(position));
    }

    @Override
    public long getUniqueItemId(int position) {
        return filteredData.get(position).first;
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    @Override
    public Filter getFilter() {
        return aFilter;
    }

    private class AssignmentFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final ArrayList<Pair<Long, CourseWork>> list = originalData;

            int count = list.size();
            final ArrayList<Pair<Long, CourseWork>> nList = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                final Pair<Long, CourseWork> data = list.get(i);

                final Long position = data.first;
                final CourseWork courseWork = data.second;

                final String title = courseWork.getTitle().toLowerCase();
                final String description = courseWork.getDescription().toLowerCase();

                if(title.contains(filterString) || description.contains(filterString)){
                    nList.add(new Pair<>(position, courseWork));
                }
            }

            results.values = nList;
            results.count = nList.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<Pair<Long, CourseWork>>) results.values;
            notifyDataSetChanged();
        }

    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        ConstraintLayout assignmentLayout;
        private final ClassroomServiceHelper classroomServiceHelper;

        @SuppressLint("NonConstantResourceId")
        ViewHolder(final View itemView, final ClassroomServiceHelper classroomServiceHelper) {
            super(itemView, mGrabHandleId, mDragOnLongPress);

            assignmentLayout = itemView.findViewById(R.id.assignment);
            this.classroomServiceHelper = classroomServiceHelper;

            Log.i("Test3", String.valueOf(this.classroomServiceHelper));

            ConstraintLayout expandableView = itemView.findViewById(R.id.expandableView);
            Button moBtn = itemView.findViewById(R.id.more_options);
            CardView cardView = itemView.findViewById(R.id.assignment_card);

            itemView.setOnClickListener(v -> {
                if (expandableView.getVisibility()==View.GONE){
                    TransitionManager.beginDelayedTransition(cardView, new AutoTransition());
                    expandableView.setVisibility(View.VISIBLE);
                    onItemClicked(itemView);
                } else {
                    TransitionManager.beginDelayedTransition(cardView, new AutoTransition());
                    expandableView.setVisibility(View.GONE);
                }
            });

            moBtn.setOnClickListener(view -> {

                //creating a popup menu
                PopupMenu popup = new PopupMenu(itemView.getContext(), moBtn);
                //inflating menu from xml resource
                popup.inflate(R.menu.item_mo);
                //adding click listener

                popup.setOnMenuItemClickListener(item -> {

                    ProgressDialog dialog;

                    switch (item.getItemId()) {
                        case R.id.item_mo_option_1:
                            dialog = ProgressDialog.show(itemView.getContext(), "Copying to clipboard...",
                                    "Fetching the required data...", true);
                            loadSubmissions(itemView, 1, dialog);
                            return true;
                        case R.id.item_mo_option_2:
                            dialog = ProgressDialog.show(itemView.getContext(), "Sharing text...",
                                    "Fetching the required data...", true);
                            loadSubmissions(itemView, 2, dialog);
                            return true;
                        default:
                            return false;
                    }
                });
                //displaying the popup
                popup.show();
            });

//            AppCompatActivity activity = (AppCompatActivity )itemView.getContext();

        }

        List<StudentSubmission> submissions;

        boolean isSubmissionLoading = false;

        @Override
        public void onItemClicked(View view) {

            Log.i(TAG, String.valueOf(view));

            if(!this.isSubmissionLoading){

                if(this.submissions==null){
                    loadSubmissions(view, 0, null);
                }
            }
        }

        Dialog d;
        ClipboardManager clipboard;


        private void loadSubmissions(View view, final int action, Dialog dialog){

            isSubmissionLoading = true;

            String courseId = ((TextView)view.findViewById(R.id.courseID)).getText().toString();
            String courseWorkId = ((TextView)view.findViewById(R.id.courseWorkID)).getText().toString();

            clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);

            d = dialog;

            this.classroomServiceHelper.listSubmissions(courseId, courseWorkId)
                    .addOnSuccessListener(submissions -> {

                        if (submissions == null || submissions.size() == 0) {
                            Log.d(TAG, "No submissions for " + submissions + ".");
                        } else {
                            ViewHolder.this.submissions = submissions;
                            ViewHolder.this.processSubmissions(view, action, courseId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to get student data");
                        Log.d(TAG, e.getMessage());

                        isSubmissionLoading = false;
                    });
        }

        private void processSubmissions(View view, int action, String courseId){

            Log.d(TAG, "Submissions size : " + this.submissions.size());
            ProgressBar progressBar = view.findViewById(R.id.indeterminateBar);
            progressBar.setVisibility(View.GONE);

            ArrayList<String> not_submitted = new ArrayList<>();
            ArrayList<String> not_checked = new ArrayList<>();

            Thread thread = new Thread(){
                public void run(){

                    Log.e(TAG, "Running thread...");

                    String assignmentName = ((TextView) itemView.findViewById(R.id.assignment_name))
                            .getText().toString();

                    try {
                        List<Student> students = classroomServiceHelper.getStudents(courseId);

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

                    StringBuilder builder = new StringBuilder(
                            String.format("Status of '%s' assignment:\n", assignmentName)
                    );

                    builder.append("\n");

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

                    if(action==1){
                        ClipData clip = ClipData.newPlainText("Copied Text", builder.toString());
                        clipboard.setPrimaryClip(clip);

                        ((Activity)itemView.getContext()).runOnUiThread(()->Toast.makeText(view.getContext(), "Copied pending submission data to clipboard!", Toast.LENGTH_LONG).show());
                    } else if (action==2){

                        Intent intent2 = new Intent(); intent2.setAction(Intent.ACTION_SEND);
                        intent2.setType("text/plain");
                        intent2.putExtra(Intent.EXTRA_TEXT, builder.toString() );
                        itemView.getContext().startActivity(Intent.createChooser(intent2, "Share via"));

                        Log.i("Sharer", "Sharing to another app");
                    }

                    if(d!=null){
                        d.cancel();
                    } else {
                        Log.e("!!!!!!!!!!!!!!!!!!!!!!!", "Dialog was null");
                    }
                }


            };


              Log.e(TAG, "Starting thread...");
              thread.start();
              Log.e(TAG, "Started thread...");

              isSubmissionLoading = false;

          }

        @Override
        public boolean onItemLongClicked(View view) {
            Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}