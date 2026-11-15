STUDENT COURSE MANAGEMENT SYSTEM - COS201 Programming I
Java backend + HTML front end

HOW TO RUN
1. Open a terminal in this project folder.
2. Compile:   javac -d out src/*.java
3. Start:     java -cp out CourseServer
4. Open http://localhost:8080 in a web browser.
5. To stop, click "Exit Program" on the page, or press Ctrl+C in the terminal.

If port 8080 is busy, start on another port:  java -cp out CourseServer 9090
and open http://localhost:9090 instead.

Run the commands from this folder (not from inside src), so the server can
find the web folder.

FILES
src/Course.java             One course + validation (trim, upper case, format checks)
src/CourseManager.java      Course list, add/edit/delete, recursive search and total,
                            save/load (JSON)
src/CourseServer.java       Starts the web server (contains main)
src/ApiHandler.java         Handles /api/... requests from the page
src/StaticFileHandler.java  Sends the HTML/CSS/JS files to the browser
src/JsonUtil.java           Builds JSON replies and reads saved JSON files
web/index.html              Page layout
web/style.css               Styling
web/app.js                  Button actions; talks to the Java API

FEATURES
1. Add Course   2. View All Courses   3. Search Course by Code
4. Compute Total Units   5. Save to File   6. Load from File   7. Exit Program
Extra: Edit and Delete buttons on each row of the course table.

Courses are saved to courses.json in the project folder (default name).
