Pending Submissions Viewer:

In order to run/install the app as a project, you'll need to setup your Firebase/GCP project and follow the procedure and make the required changes in the code files accordingly (updating the google-services.json file, generating a new keystore file, setting it's SHA key in the server, enabling Google sign-in via Firebase, the Google Classroom API via GCP, etc.)

If you'll just want to run the app without building it using Android Studio, then the debug apk present in the *PendingSubmissionsViewer2* directory of this folder can be used.

php-fs-and-ide:

Just as any other PHP script, you'll need to host/place the *index.php* and *translation.json* file in the public folder of your test server and access the server, which should be the localhost as the file system does not have any proper security measure that has been explicitly or implicitly taken by the developer and the system is purely intended for testing and debugging purposes, specially made for students and teachers.

msbte-report-generator:

The MSBTE report generator can be open by simple running the *ReportGenerator.vbs* file or if the user does not have those permission, then the *run.bat* file could be run instead which basically contains the required command required to run the main Java file while specifying all the libraries required to successfully run the code.

Please make sure you run the latest version of Java in order to avoid any unexpected errors.

In order to view or debug those errors, one could use the *run.bat* file instead of the .vbs file specified above. The vbs file is basically used to silence that terminal.