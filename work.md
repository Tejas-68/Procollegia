# Phase 2 Implementation Plan

## 1. Authentication & Profile
- [x] **Login with UUCMS ID:** Modify `LoginActivity` to allow login using UUCMS ID. If the input doesn't contain `@`, query Firestore to find the associated email before calling Firebase Auth.
- [x] **Student Profile details:** Ensure department, section, year, sem are stored and displayed. (RegisterActivity already saves department, year, semester).

## 2. Attendance & Scanner
- [x] **Attendance Scanner improvements:** QR code returns UUCMS ID. `AttendanceScannerActivity` should match scanned UUCMS ID to student name, show the name on success, and keep the camera active for the next student.
- [x] **PDF Attendance Report:** Change the CSV export in `TeacherAttendanceFragment` to generate and share a formatted PDF document.

## 3. Academics & Internals (New Module)
- [x] **Teacher Internals Management:** New screen for teachers to input, view, and modify internal marks for students.
- [x] **Student Internals View & Reconsideration:** Students can view their internal marks and raise a "reconsideration" request.
- [x] **Teacher Reconsideration Handling:** Teachers can view reconsiderations and update marks.
- [x] **HOD Subject Assignment:** HOD dashboard screen to assign a specific subject to a specific teacher. Enforce that only assigned teachers can give attendance/internals for that subject.
- [x] **HOD Internals Override:** HOD can view and modify any student's internal marks.

## 4. Feedback & Complaints
- [x] **Teacher Feedback View:** Allow teachers to view complaints and feedback (currently only students can submit).

## 5. Timetable Validation
- [x] **Teacher Timetable:** Ensure teachers can see their assigned timetable.
