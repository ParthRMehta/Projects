package org.tpoly.pendingsubmissionsviewer.adapter;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.classroom.model.Course;
import com.woxthebox.draglistview.DragItemAdapter;

import org.tpoly.pendingsubmissionsviewer.R;
import org.tpoly.pendingsubmissionsviewer.activities.SubmissionsViewer;

import java.util.ArrayList;

public class CourseAdapter extends DragItemAdapter<Pair<Long, Course>, CourseAdapter.ViewHolder> {

    private final int mLayoutId;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;

    public CourseAdapter(ArrayList<Pair<Long, Course>> list, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
        setItemList(list);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.i("Parent", String.valueOf(parent.getChildCount()));

        View view = LayoutInflater.from(parent.getContext()).inflate(this.mLayoutId, parent, false); //this is invalid layout file
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Course course = mItemList.get(position).second;

        TextView courseNameView = holder.courseLayout.findViewById(R.id.courseName);
        courseNameView.setText(course.getName());

        TextView courseIDView = holder.courseLayout.findViewById(R.id.course_id);
        courseIDView.setText(course.getId());

        holder.itemView.setTag(mItemList.get(position));
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).first;
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        RelativeLayout courseLayout;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            courseLayout = itemView.findViewById(R.id.course);
        }

        @Override
        public void onItemClicked(View view) {

            Intent intent = new Intent(view.getContext(), SubmissionsViewer.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            TextView courseIDView = view.findViewById(R.id.course_id);
            String courseID = courseIDView.getText().toString();
            intent.putExtra("courseID", courseID);

            TextView courseNameView = view.findViewById(R.id.courseName);
            String courseName = courseNameView.getText().toString();
            intent.putExtra("courseName", courseName);

//            Toast.makeText(view.getContext(), courseID, Toast.LENGTH_SHORT).show();
            view.getContext().startActivity(intent);
        }

        @Override
        public boolean onItemLongClicked(View view) {
            Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}