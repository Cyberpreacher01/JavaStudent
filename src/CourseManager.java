import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * CourseManager.java
 * Stores the list of Course objects and performs every operation on them:
 * add, display, search, total units, save and load.
 */
public class CourseManager {

    private ArrayList<Course> courses = new ArrayList<>();   // internal structure

    public int getCourseCount() {
        return courses.size();
    }

    /** Read-only view of the courses, used by the API to send the list to the browser. */
    public List<Course> getCourses() {
        return Collections.unmodifiableList(courses);
    }

    // ---------- 1. Add Course ----------
    public Course addCourse(String code, String title, String units) {
        Course course = new Course(code, title, units);   // may throw IllegalArgumentException
        if (searchCourse(course.getCode()) != null) {
            throw new IllegalArgumentException("Course " + course.getCode() + " has already been added.");
        }
        courses.add(course);
        return course;
    }

    // ---------- 2. View All Courses ----------
    // Handled by getCourses() above: the API sends this list to the browser,
    // which displays it as a table.

    // ---------- Edit Course ----------
    /** Finds the course with the given code and replaces it with a new
     *  code/title/units. The new code may be the same course or a different,
     *  still-unused one. */
    public Course updateCourse(String code, String newCode, String newTitle, String newUnits) {
        int index = indexOfCode(code);
        if (index < 0) {
            throw new IllegalArgumentException(
                "No course with code '" + code.trim().toUpperCase() + "' was found.");
        }
        Course updated = new Course(newCode, newTitle, newUnits);   // may throw IllegalArgumentException
        if (!updated.getCode().equals(courses.get(index).getCode()) && searchCourse(updated.getCode()) != null) {
            throw new IllegalArgumentException("Course " + updated.getCode() + " has already been added.");
        }
        courses.set(index, updated);
        return updated;
    }

    // ---------- Delete Course ----------
    public Course deleteCourse(String code) {
        int index = indexOfCode(code);
        if (index < 0) {
            throw new IllegalArgumentException(
                "No course with code '" + code.trim().toUpperCase() + "' was found.");
        }
        return courses.remove(index);
    }

    /** Finds the list position of a course by code, or -1 if not present. */
    private int indexOfCode(String code) {
        String cleaned = code.trim().replace(" ", "").toUpperCase();
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).getCode().equals(cleaned)) {
                return i;
            }
        }
        return -1;
    }

    // ---------- 3. Search Course by Code (RECURSIVE) ----------
    public Course searchCourse(String code) {
        String cleaned = code.trim().replace(" ", "").toUpperCase();
        return searchCourse(cleaned, 0);
    }

    /** Base cases: end of list (not found) or a match.
     *  Recursive case: check the next position. */
    private Course searchCourse(String code, int index) {
        if (index >= courses.size()) {
            return null;
        }
        if (courses.get(index).getCode().equals(code)) {
            return courses.get(index);
        }
        return searchCourse(code, index + 1);
    }

    // ---------- 4. Compute Total Units (RECURSIVE) ----------
    public int totalUnits() {
        return totalUnits(0);
    }

    /** Base case: past the last course, return 0.
     *  Recursive case: this course's units + total of the remaining courses. */
    private int totalUnits(int index) {
        if (index >= courses.size()) {
            return 0;
        }
        return courses.get(index).getUnits() + totalUnits(index + 1);
    }

    // ---------- 5. Save to File (JSON) ----------
    public int saveToFile(String filename) throws IOException {
        String json = JsonUtil.courseListToJson(courses);
        Files.write(Paths.get(filename), json.getBytes(StandardCharsets.UTF_8));
        return courses.size();
    }

    // ---------- 6. Load from File (JSON) ----------
    /** Loads courses from a JSON file written by saveToFile. Bad entries are
     *  skipped and reported so that one damaged entry does not stop the whole
     *  file from loading. Returns the list of skipped-entry messages. */
    public List<String> loadFromFile(String filename) throws IOException {
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new IOException("File '" + filename + "' was not found or cannot be opened.");
        }

        List<Map<String, String>> rawItems;
        try {
            rawItems = JsonUtil.parseObjectArray(content);
        } catch (RuntimeException e) {
            throw new IOException("File '" + filename + "' is not valid JSON: " + e.getMessage());
        }

        ArrayList<Course> loaded = new ArrayList<>();
        ArrayList<String> skipped = new ArrayList<>();
        int itemNumber = 0;
        for (Map<String, String> item : rawItems) {
            itemNumber++;
            try {
                Course course = new Course(item.get("code"), item.get("title"), item.get("units"));
                for (Course c : loaded) {
                    if (c.getCode().equals(course.getCode())) {
                        throw new IllegalArgumentException("Duplicate code " + course.getCode());
                    }
                }
                loaded.add(course);
            } catch (IllegalArgumentException e) {
                skipped.add("Item " + itemNumber + ": " + e.getMessage());
            }
        }

        courses = loaded;     // replace the current list only after a successful read
        return skipped;
    }
}
