// app.js - connects the web page to the Java backend.
// Each button sends a request to the Java server (ApiHandler) and shows the reply.

const $ = (id) => document.getElementById(id);

// Code of the course currently being edited, or null when adding a new one.
let editingCode = null;

// ---------- Talking to the server ----------

/** Sends a request to the Java API and returns the JSON reply.
 *  Throws an Error with a readable message if anything goes wrong. */
async function api(method, path, data) {
    const options = { method };
    if (data) {
        options.headers = { "Content-Type": "application/x-www-form-urlencoded" };
        options.body = new URLSearchParams(data).toString();
    }

    let response;
    try {
        response = await fetch(path, options);
    } catch (networkError) {
        throw new Error("Cannot reach the server. Is CourseServer running?");
    }

    let reply;
    try {
        reply = await response.json();
    } catch (parseError) {
        throw new Error("The server sent an unexpected reply (status " + response.status + ").");
    }

    if (!response.ok || !reply.success) {
        const error = new Error(reply.message || "Something went wrong.");
        error.status = response.status;
        throw error;
    }
    return reply;
}

// ---------- Showing results ----------

let noticeTimer;
function showNotice(message, type) {
    const box = $("notice");
    box.textContent = message;
    box.className = "notice " + type;
    clearTimeout(noticeTimer);
    noticeTimer = setTimeout(() => box.classList.add("hidden"), 6000);
}

/** Fills the table. textContent is used (not innerHTML) so course text
 *  can never be run as code in the page. */
function renderCourses(courses, totalUnits, highlightCode) {
    const body = $("courseTable");
    body.innerHTML = "";

    if (!courses || courses.length === 0) {
        const row = body.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 5;
        cell.className = "empty";
        cell.textContent = "No courses recorded yet. Add one using the form.";
        return;
    }

    courses.forEach((course, index) => {
        const row = body.insertRow();
        if (course.code === highlightCode) row.className = "highlight";
        row.insertCell().textContent = index + 1;
        row.insertCell().textContent = course.code;
        row.insertCell().textContent = course.title;
        const units = row.insertCell();
        units.textContent = course.units;
        units.className = "num";

        const actions = row.insertCell();
        actions.className = "actions";
        const editBtn = document.createElement("button");
        editBtn.type = "button";
        editBtn.className = "icon-btn";
        editBtn.title = "Edit " + course.code;
        editBtn.setAttribute("aria-label", "Edit " + course.code);
        editBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
            'stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"></path>' +
            '<path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4Z"></path></svg>';
        editBtn.addEventListener("click", () => startEdit(course));
        const deleteBtn = document.createElement("button");
        deleteBtn.type = "button";
        deleteBtn.className = "icon-btn icon-danger";
        deleteBtn.title = "Delete " + course.code;
        deleteBtn.setAttribute("aria-label", "Delete " + course.code);
        deleteBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" ' +
            'stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"></path>' +
            '<path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>' +
            '<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path>' +
            '<line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>';
        deleteBtn.addEventListener("click", () => removeCourse(course.code));
        actions.appendChild(editBtn);
        actions.appendChild(deleteBtn);
    });

    const footer = body.insertRow();
    const label = footer.insertCell();
    label.colSpan = 3;
    label.textContent = "Total (" + courses.length + " course" + (courses.length === 1 ? "" : "s") + ")";
    label.style.fontWeight = "700";
    const total = footer.insertCell();
    total.textContent = totalUnits;
    total.className = "num";
    total.style.fontWeight = "700";
    footer.insertCell();   // keeps the footer aligned with the Actions column
}

function updateStats(count, totalUnits) {
    $("courseCount").textContent = count;
    $("totalUnits").textContent = totalUnits;
}

// ---------- Front-end validation (the server checks again) ----------

function cleanCode(text) {
    return text.trim().replace(/\s+/g, "").toUpperCase();
}

function markInvalid(input, isInvalid) {
    input.classList.toggle("invalid", isInvalid);
}

// ---------- 1. Add Course / Edit Course ----------

function startEdit(course) {
    editingCode = course.code;
    $("code").value = course.code;
    $("title").value = course.title;
    $("units").value = course.units;
    markInvalid($("code"), false);
    markInvalid($("title"), false);
    markInvalid($("units"), false);
    $("formTitle").textContent = "Edit Course";
    $("submitBtn").textContent = "Save Changes";
    $("cancelEditBtn").classList.remove("hidden");
    $("code").focus();
}

function endEdit() {
    editingCode = null;
    $("formTitle").textContent = "Add Course";
    $("submitBtn").textContent = "Add Course";
    $("cancelEditBtn").classList.add("hidden");
    $("addForm").reset();
}

$("cancelEditBtn").addEventListener("click", endEdit);

$("addForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const codeInput = $("code"), titleInput = $("title"), unitsInput = $("units");

    const code = cleanCode(codeInput.value);
    const title = titleInput.value.trim().replace(/\s+/g, " ");
    const units = unitsInput.value.trim();

    const problems = [];
    const codeBad = !/^[A-Z]{3}[0-9]{3}$/.test(code);
    const titleBad = title === "";
    const unitsBad = !/^[1-6]$/.test(units);
    markInvalid(codeInput, codeBad);
    markInvalid(titleInput, titleBad);
    markInvalid(unitsInput, unitsBad);
    if (codeBad) problems.push("Course code must be 3 letters followed by 3 digits (e.g. COS201).");
    if (titleBad) problems.push("Course title cannot be empty.");
    if (unitsBad) problems.push("Units must be a whole number between 1 and 6.");
    if (problems.length > 0) {
        showNotice(problems.join(" "), "error");
        return;
    }

    try {
        const reply = editingCode
            ? await api("PUT", "/api/courses/" + encodeURIComponent(editingCode), { code, title, units })
            : await api("POST", "/api/courses", { code, title, units });
        renderCourses(reply.courses, reply.totalUnits, code);
        updateStats(reply.count, reply.totalUnits);
        showNotice(reply.message, "success");
        endEdit();
        codeInput.focus();
    } catch (error) {
        showNotice(error.message, "error");
    }
});

async function removeCourse(code) {
    if (!confirm("Delete course " + code + "? This cannot be undone.")) return;
    try {
        const reply = await api("DELETE", "/api/courses/" + encodeURIComponent(code));
        renderCourses(reply.courses, reply.totalUnits);
        updateStats(reply.count, reply.totalUnits);
        showNotice(reply.message, "success");
        if (editingCode === code) endEdit();
    } catch (error) {
        showNotice(error.message, "error");
    }
}

// ---------- 2. View All Courses ----------

async function viewAllCourses(showMessage) {
    try {
        const reply = await api("GET", "/api/courses");
        renderCourses(reply.courses, reply.totalUnits);
        updateStats(reply.count, reply.totalUnits);
        if (showMessage) showNotice(reply.message, "success");
    } catch (error) {
        renderCourses([], 0);
        showNotice(error.message, "error");
    }
}
$("refreshBtn").addEventListener("click", () => viewAllCourses(true));

// ---------- 3. Search Course by Code ----------

$("searchForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const code = cleanCode($("searchCode").value);
    const result = $("searchResult");

    if (code === "") {
        showNotice("Please enter a course code to search for.", "error");
        return;
    }

    try {
        const reply = await api("GET", "/api/courses/search?code=" + encodeURIComponent(code));
        const c = reply.course;
        result.textContent = "Found: " + c.code + " | " + c.title + " | " + c.units + " units";
        result.className = "result found";
        highlightRow(c.code);
    } catch (error) {
        result.textContent = error.message;
        result.className = "result missing";
        if (error.status !== 404) showNotice(error.message, "error");
    }
});

function highlightRow(code) {
    document.querySelectorAll("#courseTable tr").forEach((row) => {
        const codeCell = row.cells[1];
        row.classList.toggle("highlight", !!codeCell && codeCell.textContent === code);
    });
}

// ---------- 4. Compute Total Units ----------

$("totalBtn").addEventListener("click", async () => {
    try {
        const reply = await api("GET", "/api/total");
        updateStats(reply.count, reply.totalUnits);
        showNotice(reply.message + " across " + reply.count + " course(s).", "success");
    } catch (error) {
        showNotice(error.message, "error");
    }
});

// ---------- 5. Save to File ----------

$("saveBtn").addEventListener("click", async () => {
    try {
        const reply = await api("POST", "/api/save", { filename: $("filename").value.trim() });
        showNotice(reply.message, "success");
    } catch (error) {
        showNotice(error.message, "error");
    }
});

// ---------- 6. Load from File ----------

$("loadBtn").addEventListener("click", async () => {
    const rows = document.querySelectorAll("#courseTable td.num").length;
    if (rows > 0 && !confirm("Loading will replace the courses currently shown. Continue?")) {
        showNotice("Load cancelled.", "success");
        return;
    }
    try {
        const reply = await api("POST", "/api/load", { filename: $("filename").value.trim() });
        renderCourses(reply.courses, reply.totalUnits);
        updateStats(reply.count, reply.totalUnits);
        let message = reply.message;
        if (reply.skipped && reply.skipped.length > 0) {
            message += " Skipped: " + reply.skipped.join("; ");
        }
        showNotice(message, reply.skipped && reply.skipped.length > 0 ? "error" : "success");
        $("searchResult").classList.add("hidden");
    } catch (error) {
        showNotice(error.message, "error");
    }
});

// ---------- 7. Exit Program ----------

$("exitBtn").addEventListener("click", async () => {
    if (!confirm("Exit the program? Unsaved courses will be lost.")) return;
    try {
        await api("POST", "/api/exit");
    } catch (error) {
        // The server may already be stopped - show the closed screen anyway
    }
    $("stoppedScreen").classList.remove("hidden");
});

// Show the current list when the page opens
viewAllCourses(false);
