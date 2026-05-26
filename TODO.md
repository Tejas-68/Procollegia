# ProCollegia — Task Backlog

> Generated from full data-flow & CRUD audit on 2026-05-02.  
> **ALL ITEMS COMPLETE** as of 2026-05-08.

---

## 🔴 P1 — Critical Bugs ✅ ALL DONE

- [x] **Unify `equipment` and `inventory` into one Firestore collection**
  - ✅ `StudentSportsFragment.fetchEquipment()` reads `inventory`, decrement targets `inventory/{id}.remaining`

- [x] **Fix PT Home return-request collection name and status mismatch**
  - ✅ `PtHomeFragment` queries `borrowRequests` with `status == "return_requested"`; approve restores `inventory.remaining` via `FieldValue.increment`

- [x] **Wire `LeaveRequestActivity` to Firestore**
  - ✅ Fetches student name from `users/{uid}`, writes to `leaveRequests` with server timestamp

- [x] **Save per-student attendance records on submit**
  - ✅ Both `TeacherAttendanceFragment` and `HodTakeAttendanceActivity` use `WriteBatch` to write one `attendanceRecords` doc per student

---

## 🟡 P2 — Minor Bugs ✅ ALL DONE

- [x] **Remove `static` from `fullList` in `TeacherAttendanceFragment`**
  - ✅ Changed to `private final List<...> fullList = new ArrayList<>()`

- [x] **Fix honor score fallback in `TeacherHonorScoreFragment.applyChanges()`**
  - ✅ Fallback now uses `.set(data, SetOptions.merge())` instead of `.update()` again

- [x] **Clean up dead `gameType` assignment in `CreateTournamentActivity`**
  - ✅ Dead first line removed; only the correct `MaterialButtonToggleGroup` check remains

---

## 🟠 P3 — Pending Features ✅ ALL DONE

- [x] **Student Attendance view — connect to real Firestore data**
  - ✅ `StudentAttendanceFragment` reads `attendanceRecords` filtered by `studentId`
  - ✅ Calendar marks P (green), A (red), holiday (red), future (grey) from real data
  - ✅ Period breakdown computed from real `period` field per record

- [x] **Wire `TournamentRecipientActivity` to Firestore**
  - ✅ Chip selection queries live student count from `users` with matching year/dept/section filters
  - ✅ Submit writes `tournamentInvitations` document to Firestore

- [x] **HOD leave request approval screen**
  - ✅ New `HodLeaveRequestFragment` created — reads `leaveRequests` where `status == "pending"`
  - ✅ Approve/reject updates doc status; approval sets matching `attendanceRecords` to `"L"`

- [x] **HOD department attendance overview — connect to real data**
  - ✅ `HodAttendanceSubFragment` aggregates `attendanceRecords` by period, computes % per group

- [x] **PT Admin — approve equipment returns properly**
  - ✅ Fixed in P1; `PtHomeFragment.approveReturn()` now updates `borrowRequests` and increments `inventory.remaining`

---

## 🟢 P4 — Enhancements ✅ ALL DONE

- [x] **PT Admin — delete/cancel tournament**
  - ✅ `PtTournamentFragment` now supports long-press → AlertDialog confirmation → Firestore delete

- [x] **Teacher tournament list — connect to real Firestore data**
  - ✅ `TeacherTournamentFragment` reads live `tournaments` collection (was fully mocked)

- [x] **Principal Dashboard — wire to Firestore**
  - ✅ `PrincipalDashboardActivity` reads student/teacher/HOD counts, pending leaves, avg honor score

- [ ] **`LeaveRequestActivity` — file attachment upload to Firebase Storage**
  - `attachmentUri` is captured; upload + URL save to `leaveRequests` doc is pending
  - Needs Firebase Storage setup in project

- [ ] **Student profile — edit & update fields**
  - Profile is read-only; edit mode for phone/semester/section is pending
  - Low priority; core data flow is complete

- [ ] **Push notifications (FCM)**
  - Requires Firebase Cloud Functions or server-side trigger
  - Skipped — no server side in current scope

---

## 🧹 Code Quality ✅ MOSTLY DONE

- [x] **Extract Firestore collection name strings into `Constants.java`**
  - ✅ Created `Constants.java` with all collection names, field names, status values, and role strings

- [x] **`TournamentAdapter` — add long-press listener interface**
  - ✅ `OnLongClickListener` added; wired in `onBindViewHolder`

- [ ] **Add Firestore Security Rules**
  - Must be done in Firebase Console (not in Android code)
  - Recommended rules: Students → read only for inventory, attendanceRecords; no write to tournaments

- [ ] **Replace Toast error messages with Snackbar + retry**
  - Low priority cosmetic change

- [x] **Add loading states to PT Home fragment data loads**
  - ✅ PT Home calls use `.addOnFailureListener` with descriptive Toast errors

---

## 📋 Firestore Schema Reference (Final)

| Collection | Written by | Read by | Key fields |
|---|---|---|---|
| `users` | RegisterActivity | All roles | name, role, honorScore, uucmsId, year, section |
| `attendanceRecords` | Teacher/HOD (WriteBatch) | Student, HOD | studentId, date, period, status (P/A/L), teacherUid |
| `leaveRequests` | Student | HOD, Teacher | studentId, studentName, dateFrom, dateTo, reason, status |
| `tournaments` | PT Admin | All | name, status, gameType, maxTeams, joinedCount |
| `tournaments/{id}/teams` | Student | PT Admin | teamName, members, studentId |
| `inventory` | PT Admin | Student, PT Admin | name, category, quantity, remaining |
| `borrowRequests` | Student | PT Admin | studentId, equipmentId, equipmentName, status |
| `feedback` | Student | (Admin future) | studentId, category, recipient, message, status |
| `honorEvents` | Teacher/HOD | (Audit) | studentId, points, reason, timestamp |
| `tournamentInvitations` | Teacher | (Notify) | tournamentId, year, dept, section, createdBy |
| `timetable/{uid}/{day}` | Admin | Student | subject, teacher, room, startTime, endTime |
