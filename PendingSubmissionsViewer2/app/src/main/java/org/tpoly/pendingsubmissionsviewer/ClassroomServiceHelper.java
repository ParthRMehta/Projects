package org.tpoly.pendingsubmissionsviewer;

import android.util.Log;

import androidx.core.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.UserProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClassroomServiceHelper
{
    // If you don't want to multiThread, use 'newSingleThreadExecutor' method.
    private final Executor mExecutor = Executors.newFixedThreadPool(5);
    private final Classroom mClassroomService;

    private final static HashMap<String, MPair<List<Student>, HashMap<String, MPair<CourseWork, List<StudentSubmission>>>>> cache = new HashMap<>();

    public ClassroomServiceHelper(Classroom classroomService) {
        mClassroomService = classroomService;
    }

    public Task<List<Course>> listCourses() {
        return Tasks.call(mExecutor, new Callable<List<Course>>() {
            @Override
            public List<Course> call() throws Exception {
                return mClassroomService.courses().list().execute().getCourses();
            }
        });
    }

    public Task<List<CourseWork>> listCourseWorks(final String courseId, boolean refresh) {
        return Tasks.call(mExecutor, new Callable<List<CourseWork>>() {
            @Override
            public List<CourseWork> call() throws Exception {

                if(!refresh && cache.containsKey(courseId)){

                    ArrayList<CourseWork> result = new ArrayList<>();

                    Set<Map.Entry<String, MPair<CourseWork, List<StudentSubmission>>>> resultset =
                            cache.get(courseId).second.entrySet();

                    Log.i("Nice Games", String.valueOf(resultset.size()));

                    for(Map.Entry<String, MPair<CourseWork, List<StudentSubmission>>> pair : resultset)
                    {
                        result.add(pair.getValue().first);
                    }

                    return result;
                }

                List<CourseWork> response = mClassroomService.courses().courseWork()
                        .list(courseId).execute().getCourseWork();

                HashMap<String, MPair<CourseWork, List<StudentSubmission>>> cData = new HashMap<>();

                for(CourseWork r : response){
                    if(!cData.containsKey(r.getCourseId()))
                        cData.put(r.getId(), new MPair<>(r, null));
                }

                if(!cache.containsKey(courseId)) cache.put(courseId, new MPair<>(null, cData));

                return response;
            }
        });
    }

    Map<String, UserProfile> userProfiles = new HashMap<>();

    public List<Student> getStudents(String courseId) throws IOException {

        if(Objects.requireNonNull(cache.get(courseId)).first!=null){
            Log.d("TAG", "Fetching students from cache...");
            return Objects.requireNonNull(cache.get(courseId)).first;
        }

        Log.d("TAG", "Fetching students from network...");

        List<Student> result = mClassroomService.courses().students().list(courseId).execute().getStudents();

        Objects.requireNonNull(cache.get(courseId)).first = result;

        return result;
    }


    public Task<List<StudentSubmission>> listSubmissions(final String courseId,
                                                         final String courseWorkId) {
        return Tasks.call(mExecutor, new Callable<List<StudentSubmission>>() {
            @Override
            public List<StudentSubmission> call() throws Exception {
                return getSubmissions(courseId, courseWorkId);
            }
        });
    }

    private List<StudentSubmission> getSubmissions(final String courseId, final String courseWorkId) throws IOException {
        if(Objects.requireNonNull(Objects.requireNonNull(cache.get(courseId)).second.get(courseWorkId)).second!=null){
            return Objects.requireNonNull(Objects.requireNonNull(cache.get(courseId)).second.get(courseWorkId)).second;
        }

        List<StudentSubmission> submissions =
                mClassroomService.courses().courseWork().studentSubmissions().list(courseId, courseWorkId).execute().getStudentSubmissions();

        Objects.requireNonNull(Objects.requireNonNull(cache.get(courseId)).second.get(courseWorkId)).second = submissions;

        return submissions;
    }

    public Task<List<Pair<String, List<StudentSubmission>>>> listSubmissionsOfMultiple(final String courseId, final String[] courseWorkIds)
    {
        return Tasks.call(mExecutor, () -> {

            ArrayList<Pair<String, List<StudentSubmission>>> arrayList = new ArrayList<>();

            for(final String courseWorkId : courseWorkIds){
                Log.i(courseWorkId, "Done");
                final List<StudentSubmission> submissions = getSubmissions(courseId, courseWorkId);
                final String title = Objects.requireNonNull(Objects.requireNonNull(cache.get(courseId))
                        .second.get(courseWorkId)).first.getTitle();
                arrayList.add(new Pair<>(title, submissions));
            }

            return arrayList;
        });
    }

    public Task<List<Pair<String, List<StudentSubmission>>>> listAllSubmissionsOfCourse(final String courseId){
        Set<String> courseWorkIdSet = Objects.requireNonNull(cache.get(courseId)).second.keySet();
        return listSubmissionsOfMultiple(courseId, courseWorkIdSet.toArray(new String[courseWorkIdSet.size()]));
    }


    public void clearCache(){
        cache.clear();
    }
}

class MPair<A, B>
{
    MPair(A first, B second){
        this.first = first;
        this.second = second;
    }

    public A first;
    public B second;
}