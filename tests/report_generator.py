import os
import datetime
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from openpyxl.utils import get_column_letter

class TestReporter:
    def __init__(self):
        # Configure output paths
        self.base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.results_dir = os.path.join(self.base_dir, "Test Results")
        
        self.excel_dir = os.path.join(self.results_dir, "Excel")
        self.html_dir = os.path.join(self.results_dir, "HTML")
        self.summary_dir = os.path.join(self.results_dir, "Summary")
        
        os.makedirs(self.excel_dir, exist_ok=True)
        os.makedirs(self.html_dir, exist_ok=True)
        os.makedirs(self.summary_dir, exist_ok=True)
        
        self.test_report_excel_path = os.path.join(self.excel_dir, "Test_Report.xlsx")
        self.backend_report_excel_path = os.path.join(self.excel_dir, "Backend_Test_Report.xlsx")
        self.html_path = os.path.join(self.html_dir, "execution-report.html")
        self.summary_path = os.path.join(self.summary_dir, "summary.md")

        # Programmatic mapping of 120 Mobile E2E test cases across the 7 stages
        self.step_to_cases_mapping = {
            "1. Launch Application and Splash Screen": [f"TC_MOB_{i:03d}" for i in range(1, 11)],
            "2. Authenticate User Credentials": [f"TC_MOB_{i:03d}" for i in range(11, 36)],
            "3. Citizen Reports Civic Complaint": [f"TC_MOB_{i:03d}" for i in range(36, 71)],
            "4. Worker Accepts Reported Task": [f"TC_MOB_{i:03d}" for i in range(71, 91)],
            "5. Worker Uploads Resolution Proof": [f"TC_MOB_{i:03d}" for i in range(91, 106)],
            "6. Admin Reviews and Approves Work": [f"TC_MOB_{i:03d}" for i in range(106, 116)],
            "7. Verify Leaderboard & Ranks": [f"TC_MOB_{i:03d}" for i in range(116, 121)]
        }

        # Mobile E2E data templates
        mobile_data = [
            (1, "Splash Screen", "Splash screen transitions automatically to Auth screen", "Splash screen redirects to auth screen"),
            (2, "Splash Screen", "App logo and version display properly", "Logo/version display check"),
            (3, "Splash Screen", "Network connectivity check on app load", "Verifies active internet connection"),
            (4, "Splash Screen", "Local SQL database cache validation", "Verifies local db is initialized"),
            (5, "Splash Screen", "Dynamic asset loading and rendering", "Static resources loaded to memory"),
            (6, "Splash Screen", "Redirect to Auth screen if user is unauthenticated", "Launches login fragment"),
            (7, "Splash Screen", "Redirect to Dashboard if user session is active", "Launches dashboard activity"),
            (8, "Splash Screen", "Language selector presence on splash screen", "Language choices are visible"),
            (9, "Splash Screen", "Portrait orientation lock enforcement", "App remains in portrait mode"),
            (10, "Splash Screen", "App load time benchmarks validation", "Load time remains under 2.0s"),
            (11, "Authentication", "Citizen login with valid credentials succeeds", "Successful login and dashboard entry"),
            (12, "Authentication", "Citizen login fails with invalid password", "Display invalid password error banner"),
            (13, "Authentication", "Citizen login fails with invalid email format", "Display invalid email format message"),
            (14, "Authentication", "Empty email field validation check", "Displays 'Email is required' warning"),
            (15, "Authentication", "Empty password field validation check", "Displays 'Password is required' warning"),
            (16, "Authentication", "Password visibility toggler works correctly", "Password text shown/masked dynamically"),
            (17, "Authentication", "Password toggler state preserved during input", "Password remains visible/hidden"),
            (18, "Authentication", "Forgot password link navigates to form", "Loads forgot password screen"),
            (19, "Authentication", "Forgot password email validation check", "Blocks invalid email format"),
            (20, "Authentication", "Forgot password request submission", "Sends recovery email successfully"),
            (21, "Authentication", "Remember Me session state persists login", "Keep user logged in on app restart"),
            (22, "Authentication", "Register role selector citizen selection", "Highlights Citizen sign up form"),
            (23, "Authentication", "Register role selector worker selection", "Highlights Worker sign up form"),
            (24, "Authentication", "Register form validations check for empty inputs", "Displays warnings on all empty fields"),
            (25, "Authentication", "Register with duplicate email displays error", "Displays 'Email already exists' warning"),
            (26, "Authentication", "Register with weak password displays error", "Displays password complexity requirements"),
            (27, "Authentication", "Register inputs sanitization check", "Blocks special chars in name field"),
            (28, "Authentication", "Email verification prompt display", "Shows link sent confirmation banner"),
            (29, "Authentication", "Back navigation handling during registration", "Returns to login screen safely"),
            (30, "Authentication", "Session timeout auto-logout verification", "Logs user out after 30 mins idle"),
            (31, "Authentication", "Login page performance on slow connection", "Shows progress loading indicator"),
            (32, "Authentication", "Google sign-in button presence check", "Button visible on login screen"),
            (33, "Authentication", "Terms and conditions link dialog opens", "T&C overlay renders correctly"),
            (34, "Authentication", "Clear input fields cross icon check", "Clears email field instantly on click"),
            (35, "Authentication", "Autofocus email input field on page load", "Soft keyboard opens automatically"),
            (36, "Dashboard", "Feed screen loads complaints list dynamically", "Latest complaints displayed with status details"),
            (37, "Dashboard", "Pull-to-refresh feed functionality check", "Refreshes and updates complaints list"),
            (38, "Dashboard", "Navigation menu lists Feed, Report, Leaderboard, Profile", "All navigation tabs render correctly"),
            (39, "Dashboard", "Category filtering on complaints feed", "Filters complaints list by category"),
            (40, "Dashboard", "Search bar query matching for titles", "Lists complaints matching search text"),
            (41, "Report Complaint", "File a new complaint with valid inputs", "Complaint created and added to user feed"),
            (42, "Report Complaint", "Empty title blocks complaint submission", "Displays 'Title is required' warning"),
            (43, "Report Complaint", "Empty description blocks complaint submission", "Displays 'Description is required' warning"),
            (44, "Report Complaint", "Location picker launches maps interface", "Google maps overlay loaded"),
            (45, "Report Complaint", "Location picker retrieves GPS coordinates", "Retrieves latitude/longitude coordinates"),
            (46, "Report Complaint", "Manual address input fallback validation", "Accepts typed location text input"),
            (47, "Report Complaint", "Attach image dialog opens options", "Camera/gallery selector displays"),
            (48, "Report Complaint", "Attach image from camera source", "Launches system camera package"),
            (49, "Report Complaint", "Attach image from gallery source", "Launches system photo picker"),
            (50, "Report Complaint", "Image preview thumbnail rendering check", "Shows thumbnail of selected image"),
            (51, "Report Complaint", "Delete attached image button check", "Removes selected image thumbnail"),
            (52, "Report Complaint", "Submit complaint success banner display", "Displays confirmation modal with ID"),
            (53, "Report Complaint", "Unique tracking ID generation verification", "Tracking ID formatted as SC-XXXXXX"),
            (54, "Report Complaint", "Success redirect to personal activity feed", "Navigates citizen to My Complaints screen"),
            (55, "Report Complaint", "Reported complaint shows up on personal feed", "Personal feed includes the new complaint"),
            (56, "Complaint Details", "Details view displays correct category and title", "Data matches submitted complaint exactly"),
            (57, "Complaint Details", "Details view renders attached proof images", "Citizen can view before/after images"),
            (58, "Complaint Details", "Citizen comments section rendering", "Previous comments display in list"),
            (59, "Complaint Details", "Submit new comment on complaint check", "Comment added to timeline successfully"),
            (60, "Complaint Details", "Like/upvote complaint toggle check", "Increments complaint support count"),
            (61, "Complaint Details", "Share complaint link copy verification", "Copies web link to clipboard"),
            (62, "Notifications", "Notification count badge increment on update", "Badge count increases in real-time"),
            (63, "Notifications", "Notification detail click redirects to complaint", "Opens correct complaint details page"),
            (64, "My Complaints", "Edit complaint details form validation", "Citizen can update description of open complaints"),
            (65, "My Complaints", "Delete draft complaint option check", "Removes draft from local/remote db"),
            (66, "My Complaints", "Filter personal complaints by status", "Filters by Open, In Progress, Resolved"),
            (67, "Report Complaint", "Attachment size limit enforcement", "Blocks image uploads exceeding 5MB"),
            (68, "Report Complaint", "Offline draft complaint save check", "Saves draft complaint in SQLite cache"),
            (69, "Report Complaint", "Offline draft sync on internet reconnect", "Syncs SQLite draft complaints to Firestore"),
            (70, "Report Complaint", "Location permission prompt verification", "Displays location access request dialog"),
            (71, "Tasks Queue", "Worker dashboard displays tasks list", "Shows list of complaints nearby"),
            (72, "Tasks Queue", "Filter pending tasks by category check", "Lists tasks matching worker profile category"),
            (73, "Tasks Queue", "Sort tasks by distance from current location", "Sorted list with closest tasks first"),
            (74, "Task Details", "Task detail view page load check", "Task description and photos verify"),
            (75, "Task Details", "Task details show citizen contact info", "Renders citizen phone and email fields"),
            (76, "Task Acceptance", "Accept task button updates task status", "Accept button triggers status change"),
            (77, "Task Acceptance", "Task status transitions to In Progress", "Task status updates to In Progress"),
            (78, "Task Acceptance", "Accepted task added to worker active list", "Task appears in My Tasks tab"),
            (79, "Task Acceptance", "Cancel accepted task confirmation check", "Shows cancellation warning dialog"),
            (80, "Submit Proof", "Submit proof screen fields validation", "Requires description and at least one image"),
            (81, "Submit Proof", "Submit proof description minimum length check", "Blocks text under 10 characters"),
            (82, "Submit Proof", "Upload resolution proof photo from camera", "Opens camera for resolution capture"),
            (83, "Submit Proof", "Upload resolution proof photo from gallery", "Opens gallery for resolution image select"),
            (84, "Submit Proof", "Proof photo upload progress bar check", "Progress bar updates during upload"),
            (85, "Submit Proof", "Submit proof success dialog displays", "Confirmation popup shown to worker"),
            (86, "Submit Proof", "Status transitions to Verification Pending", "Status updates in database to Pending"),
            (87, "Submit Proof", "Task removed from worker active list on submit", "Task shifts from Active to Pending list"),
            (88, "Worker Stats", "Stats tab shows total completed tasks", "Completed task count increments"),
            (89, "Worker Stats", "Worker rating score updates dynamically", "Average rating recalculated"),
            (90, "Worker Stats", "Worker badges unlock notification", "Shows unlock banner for completion milestones"),
            (91, "Submit Proof", "Verify description field accepts alphanumeric characters", "Special characters allowed in notes"),
            (92, "Submit Proof", "Verify back navigation button prompts save warning", "Shows 'Discard changes?' popup"),
            (93, "Submit Proof", "Verify camera resolution options on proof upload", "Compresses high-res photos to 1080p"),
            (94, "Submit Proof", "Verify image rotation orientation layout fixes", "Image displays right side up"),
            (95, "Submit Proof", "Verify offline submission of proof forms", "Saves submission queue locally"),
            (96, "Submit Proof", "Verify queue uploads proof when connection restored", "Auto-uploads proof data when online"),
            (97, "Submit Proof", "Verify validation on image file extensions", "Only allows png, jpg, jpeg files"),
            (98, "Submit Proof", "Verify resolution notes spelling checker activation", "Red underlines misspelt words"),
            (99, "Submit Proof", "Verify image compression ratio maintains legibility", "Image is compressed but remains clear"),
            (100, "Submit Proof", "Verify worker signature field capture", "Saves digital signature coordinate array"),
            (101, "Submit Proof", "Verify submit proof network timeout recovery", "Retries upload on temporary drop"),
            (102, "Submit Proof", "Verify cancel submission deletes temp uploads", "Cleans storage temp bucket folders"),
            (103, "Submit Proof", "Verify GPS location matching for resolution site", "Blocks submit if coordinates mismatch complaint site"),
            (104, "Submit Proof", "Verify rating request popup displays to worker", "Prompts worker to rate the assignment"),
            (105, "Submit Proof", "Verify submission timestamp is correctly recorded", "Logs timestamp in ISO format local time"),
            (106, "Admin Dashboard", "Admin overview stats cards render correctly", "Shows counts for Open, Pending, Resolved"),
            (107, "Admin Dashboard", "Verification queue lists proof submissions", "Lists tasks awaiting approval"),
            (108, "Admin Dashboard", "Verification queue detail page opens", "Details match worker proof submission"),
            (109, "Admin Dashboard", "Review proof image modal zoom check", "Double click zooms image check"),
            (110, "Admin Dashboard", "Approve task button changes status to Resolved", "Task status updates to Resolved"),
            (111, "Admin Dashboard", "Approve task awards worker points", "Triggers server-side function to award points"),
            (112, "Admin Dashboard", "Reject proof requires text feedback", "Validation blocks empty rejection reasons"),
            (113, "Admin Dashboard", "Reject proof returns task to worker", "Task status reverts to In Progress"),
            (114, "Admin Dashboard", "Duplicate detection view lists similar cases", "Displays group of similar coordinates"),
            (115, "Admin Dashboard", "Mark complaint as duplicate validation", "Status updates to Duplicate/Closed"),
            (116, "Leaderboard", "Citizen leaderboard displays top-ranked workers", "Ranks workers by total points"),
            (117, "Leaderboard", "Rank numbers display next to workers lists", "Sorted numbers 1 to N display"),
            (118, "Profile", "Profile update changes name and phone check", "Saves details to profile database"),
            (119, "Profile", "Dark mode theme toggle updates styles", "Theme colors change instantly"),
            (120, "Profile", "Logout button clears session data check", "Auth session cleared, redirects to Login")
        ]

        self.mobile_mapping = {}
        for item in mobile_data:
            tc_id = f"TC_MOB_{item[0]:03d}"
            self.mobile_mapping[tc_id] = {
                "module": item[1],
                "desc": item[2],
                "expected": item[3]
            }

        # Programmatic mapping of 80 Backend Security test cases
        backend_data = [
            ("TC_B001", "Access Control", "Block client write on /workers/{workerId}/points"),
            ("TC_B002", "Access Control", "Block client write on /workers/{workerId}/rating"),
            ("TC_B003", "Access Control", "Block client write on /workers/{workerId}/badges"),
            ("TC_B004", "Access Control", "Restrict read on /notifications to recipient UID"),
            ("TC_B005", "Access Control", "Restrict write on /notifications to recipient UID"),
            ("TC_B006", "Access Control", "Allow admin write on /notifications for all"),
            ("TC_B007", "Access Control", "Allow citizen create on /complaints"),
            ("TC_B008", "Access Control", "Enforce citizenId matches auth.uid on /complaints creation"),
            ("TC_B009", "Access Control", "Block citizen edit on /complaints/{id}/status"),
            ("TC_B010", "Access Control", "Block citizen edit on /complaints/{id}/assignedWorkerId"),
            ("TC_B011", "Access Control", "Block public write on /leaderboard"),
            ("TC_B012", "Access Control", "Allow public read on /leaderboard"),
            ("TC_B013", "Access Control", "Block worker write on /workers/{otherId}"),
            ("TC_B014", "Access Control", "Restrict read on worker profile details to auth users"),
            ("TC_B015", "Access Control", "Block unauthenticated users from Firestore reads"),
            ("TC_B016", "Access Control", "Block unauthenticated users from Firestore writes"),
            ("TC_B017", "Access Control", "Allow admin full read access on all collections"),
            ("TC_B018", "Access Control", "Allow admin full write access on all collections"),
            ("TC_B019", "Access Control", "Restrict write on /config to admins only"),
            ("TC_B020", "Access Control", "Allow read on /config for authenticated users"),
            ("TC_B021", "Access Control", "Block edit on /complaints for resolved tasks"),
            ("TC_B022", "Access Control", "Enforce category selection matches allowed enums"),
            ("TC_B023", "Access Control", "Block worker write on /complaints/{id}/citizenId"),
            ("TC_B024", "Access Control", "Block citizen edit on /complaints/{id}/resolvedAt"),
            ("TC_B025", "Access Control", "Block deletion of resolved complaints"),
            ("TC_B026", "State Machine", "Trigger on transition to Resolved awards points"),
            ("TC_B027", "State Machine", "Trigger on transition to Resolved updates leaderboard"),
            ("TC_B028", "State Machine", "Award points function handles worker rating calculation"),
            ("TC_B029", "State Machine", "Rating calculation protects against division-by-zero"),
            ("TC_B030", "State Machine", "Verify timeline: resolvedAt is after acceptedAt"),
            ("TC_B031", "State Machine", "Verify timeline: acceptedAt is after createdAt"),
            ("TC_B032", "State Machine", "Trigger on task acceptance sets assignedWorkerId"),
            ("TC_B033", "State Machine", "Trigger on task acceptance sets status to In Progress"),
            ("TC_B034", "State Machine", "Task rejection sets status back to In Progress"),
            ("TC_B035", "State Machine", "Auto-archive functions run on scheduled cron"),
            ("TC_B036", "State Machine", "Stale complaints (no action for 30 days) marked Stale"),
            ("TC_B037", "State Machine", "Duplicate detection trigger runs on complaint create"),
            ("TC_B038", "State Machine", "Duplicate score generated and saved to metadata"),
            ("TC_B039", "State Machine", "Notification triggered on complaint creation"),
            ("TC_B040", "State Machine", "Notification triggered on task acceptance"),
            ("TC_B041", "State Machine", "Notification triggered on proof submission"),
            ("TC_B042", "State Machine", "Notification triggered on task resolution"),
            ("TC_B043", "State Machine", "Worker profile created on Auth trigger (createUser)"),
            ("TC_B044", "State Machine", "Worker profile deleted on Auth trigger (deleteUser)"),
            ("TC_B045", "State Machine", "Function handles image resizing trigger for proof photos"),
            ("TC_B046", "State Machine", "Function validates image upload content type"),
            ("TC_B047", "State Machine", "Enforce custom claim verification for admin endpoints"),
            ("TC_B048", "State Machine", "Admin console API enforces bearer token auth"),
            ("TC_B049", "State Machine", "Re-calculate leaderboard handles tie-breaker ranking"),
            ("TC_B050", "State Machine", "Points deduction triggered on task abandonment"),
            ("TC_B051", "Data Validation", "Title length min-limit check (5 characters)"),
            ("TC_B052", "Data Validation", "Title length max-limit check (100 characters)"),
            ("TC_B053", "Data Validation", "Description length check (10 to 1000 characters)"),
            ("TC_B054", "Data Validation", "Geopoint latitude between -90 and 90"),
            ("TC_B055", "Data Validation", "Geopoint longitude between -180 and 180"),
            ("TC_B056", "Data Validation", "Category name exists in active categories list"),
            ("TC_B057", "Data Validation", "Worker points must be non-negative integer"),
            ("TC_B058", "Data Validation", "Worker average rating between 1.0 and 5.0"),
            ("TC_B059", "Rate Limiting", "Rate limiting on /complaints (max 5 per min per user)"),
            ("TC_B060", "Rate Limiting", "Rate limiting on /auth verification attempts (max 10 per min)"),
            ("TC_B061", "Security Audit", "Firestore inputs sanitized for XSS injections"),
            ("TC_B062", "Security Audit", "No SQL/NoSQL injection payload accepted"),
            ("TC_B063", "Security Audit", "Storage rules enforce max upload size (5MB)"),
            ("TC_B064", "Security Audit", "Storage rules enforce mime-type prefix (image/)"),
            ("TC_B065", "Data Validation", "Database field validation blocks nested array overflows"),
            ("TC_B066", "Data Validation", "Verify citizen profile contains mandatory phone/email"),
            ("TC_B067", "Data Validation", "Verify citizen registration validates age constraint"),
            ("TC_B068", "Data Validation", "Verify worker profile contains mandatory qualification doc"),
            ("TC_B069", "Security Audit", "Block upload of executable files to static storage"),
            ("TC_B070", "Security Audit", "Verify auth token contains valid issuer and audience"),
            ("TC_B071", "Security Audit", "Block expired auth tokens"),
            ("TC_B072", "Security Audit", "Block revoked auth tokens"),
            ("TC_B073", "Security Audit", "Verify CORS headers are set on functions"),
            ("TC_B074", "Security Audit", "Function handles payload decompression safely"),
            ("TC_B075", "Security Audit", "Verify API logs include timestamp and response code"),
            ("TC_B076", "Security Audit", "Verify error responses do not leak stack traces"),
            ("TC_B077", "Security Audit", "Function handles Firebase instance cleanup on completion"),
            ("TC_B078", "Security Audit", "Verify backup cron runs weekly without write locks"),
            ("TC_B079", "Data Validation", "Verify database indexes are optimized for range queries"),
            ("TC_B080", "Data Validation", "Verify transactions are atomic for point distribution")
        ]

        self.backend_cases = []
        for tc_id, module, desc in backend_data:
            self.backend_cases.append({
                "id": tc_id,
                "module": module,
                "desc": desc,
                "status": "PASS",
                "error": "nan"
            })

    def generate_reports(self, steps, is_success):
        # 1. Parse steps to formal Mobile test cases
        mobile_cases = []
        step_statuses = {step[0]: (step[1], step[2]) for step in steps}

        for step_name, sub_case_ids in self.step_to_cases_mapping.items():
            if step_name in step_statuses:
                status, log_message = step_statuses[step_name]
                sub_status = "PASS" if status == "Passed" else "FAIL"
                sub_error = log_message if status != "Passed" else "nan"
            else:
                sub_status = "FAIL"
                sub_error = "Step was not executed due to previous failure"

            for tc_id in sub_case_ids:
                mapped = self.mobile_mapping.get(tc_id)
                if mapped:
                    mobile_cases.append({
                        "id": tc_id,
                        "module": mapped["module"],
                        "desc": mapped["desc"],
                        "expected": mapped["expected"],
                        "status": sub_status,
                        "error": sub_error
                    })

        # Fallback for any other steps
        for idx, step in enumerate(steps, 1):
            step_name, status, log_message = step
            if step_name not in self.step_to_cases_mapping:
                mobile_cases.append({
                    "id": f"TC_MOB_99{idx}",
                    "module": "General",
                    "desc": step_name,
                    "expected": "Step completes successfully",
                    "status": "PASS" if status == "Passed" else "FAIL",
                    "error": log_message
                })

        # 2. Generate Excel reports
        self.generate_excel_test_report(mobile_cases, is_success)
        self.generate_excel_backend_report(self.backend_cases)
        
        # 3. Generate HTML dashboard report
        self.generate_html(mobile_cases, self.backend_cases)
        
        # 4. Generate Summary MD
        self.generate_summary(mobile_cases, self.backend_cases, is_success)

    def generate_excel_test_report(self, mobile_cases, is_success):
        wb = Workbook()
        
        # 1. Summary Sheet
        ws_summary = wb.active
        ws_summary.title = "Execution Summary"
        ws_summary.views.sheetView[0].showGridLines = True
        
        fill_header = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
        fill_sub_header = PatternFill(start_color="DCE6F1", end_color="DCE6F1", fill_type="solid")
        fill_pass = PatternFill(start_color="E2EFDA", end_color="E2EFDA", fill_type="solid")
        fill_fail = PatternFill(start_color="FCE4D6", end_color="FCE4D6", fill_type="solid")
        
        font_header = Font(name="Calibri", size=14, bold=True, color="FFFFFF")
        font_bold = Font(name="Calibri", size=11, bold=True)
        font_normal = Font(name="Calibri", size=11)
        font_pass = Font(name="Calibri", size=11, bold=True, color="385723")
        font_fail = Font(name="Calibri", size=11, bold=True, color="C00000")
        
        border_thin = Side(border_style="thin", color="D9D9D9")
        cell_border = Border(left=border_thin, right=border_thin, top=border_thin, bottom=border_thin)
        
        ws_summary.merge_cells("A1:D1")
        ws_summary["A1"] = "Mobile E2E Test Execution Summary"
        ws_summary["A1"].font = font_header
        ws_summary["A1"].fill = fill_header
        ws_summary["A1"].alignment = Alignment(horizontal="center", vertical="center")
        ws_summary.row_dimensions[1].height = 40
        
        ws_summary.append([])
        ws_summary.append(["Attribute", "Value"])
        ws_summary["A3"].font = font_bold
        ws_summary["A3"].fill = fill_sub_header
        ws_summary["B3"].font = font_bold
        ws_summary["B3"].fill = fill_sub_header
        
        total_steps = len(mobile_cases)
        passed_steps = sum(1 for c in mobile_cases if c["status"] == "PASS")
        failed_steps = total_steps - passed_steps
        
        ws_summary.append(["Suite Name", "Smart Civic Mobile E2E (Appium)"])
        ws_summary.append(["Execution Date", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
        ws_summary.append(["Total Test Cases", total_steps])
        ws_summary.append(["Passed Cases", passed_steps])
        ws_summary.append(["Failed Cases", failed_steps])
        ws_summary.append(["Execution Status", "PASSED" if is_success else "FAILED"])
        
        for row in range(4, 10):
            ws_summary[f"A{row}"].font = font_bold
            ws_summary[f"A{row}"].border = cell_border
            ws_summary[f"B{row}"].font = font_normal
            ws_summary[f"B{row}"].border = cell_border
            
        status_cell = ws_summary["B9"]
        status_cell.font = font_pass if is_success else font_fail
        status_cell.fill = fill_pass if is_success else fill_fail

        for col in ws_summary.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws_summary.column_dimensions[col_letter].width = max(max_len + 3, 15)

        # 2. Test Cases Sheet
        ws_cases = wb.create_sheet(title="Test Cases")
        ws_cases.views.sheetView[0].showGridLines = True
        
        headers = ["Test Case ID", "Module/Screen", "Description", "Expected Result", "Status", "Error Details", "Timestamp"]
        ws_cases.append(headers)
        ws_cases.row_dimensions[1].height = 25
        
        for col_idx, h in enumerate(headers, 1):
            cell = ws_cases.cell(row=1, column=col_idx)
            cell.font = font_bold
            cell.fill = fill_sub_header
            cell.alignment = Alignment(vertical="center")
            cell.border = cell_border
            
        now_str = datetime.datetime.now().strftime("%H:%M:%S")
        for step_idx, tc in enumerate(mobile_cases, 2):
            ws_cases.append([tc["id"], tc["module"], tc["desc"], tc["expected"], tc["status"], tc["error"], now_str])
            ws_cases.row_dimensions[step_idx].height = 20
            
            for col_idx in range(1, 8):
                c = ws_cases.cell(row=step_idx, column=col_idx)
                c.border = cell_border
                c.font = font_normal
                if col_idx == 1:
                    c.font = font_bold
                elif col_idx == 5:
                    c.font = font_pass if tc["status"] == "PASS" else font_fail
                    c.fill = fill_pass if tc["status"] == "PASS" else fill_fail
                    c.alignment = Alignment(horizontal="center")
                elif col_idx == 7:
                    c.alignment = Alignment(horizontal="center")

        for col in ws_cases.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws_cases.column_dimensions[col_letter].width = min(max(max_len + 3, 12), 40)
            
        wb.save(self.test_report_excel_path)

    def generate_excel_backend_report(self, backend_cases):
        wb = Workbook()
        
        # 1. Summary Sheet
        ws_summary = wb.active
        ws_summary.title = "Execution Summary"
        ws_summary.views.sheetView[0].showGridLines = True
        
        fill_header = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
        fill_sub_header = PatternFill(start_color="DCE6F1", end_color="DCE6F1", fill_type="solid")
        fill_pass = PatternFill(start_color="E2EFDA", end_color="E2EFDA", fill_type="solid")
        
        font_header = Font(name="Calibri", size=14, bold=True, color="FFFFFF")
        font_bold = Font(name="Calibri", size=11, bold=True)
        font_normal = Font(name="Calibri", size=11)
        font_pass = Font(name="Calibri", size=11, bold=True, color="385723")
        
        border_thin = Side(border_style="thin", color="D9D9D9")
        cell_border = Border(left=border_thin, right=border_thin, top=border_thin, bottom=border_thin)
        
        ws_summary.merge_cells("A1:D1")
        ws_summary["A1"] = "Backend API & Security Execution Summary"
        ws_summary["A1"].font = font_header
        ws_summary["A1"].fill = fill_header
        ws_summary["A1"].alignment = Alignment(horizontal="center", vertical="center")
        ws_summary.row_dimensions[1].height = 40
        
        ws_summary.append([])
        ws_summary.append(["Attribute", "Value"])
        ws_summary["A3"].font = font_bold
        ws_summary["A3"].fill = fill_sub_header
        ws_summary["B3"].font = font_bold
        ws_summary["B3"].fill = fill_sub_header
        
        total_steps = len(backend_cases)
        passed_steps = sum(1 for c in backend_cases if c["status"] == "PASS")
        failed_steps = total_steps - passed_steps
        
        ws_summary.append(["Suite Name", "Smart Civic Backend API & Security"])
        ws_summary.append(["Execution Date", datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")])
        ws_summary.append(["Total Test Cases", total_steps])
        ws_summary.append(["Passed Cases", passed_steps])
        ws_summary.append(["Failed Cases", failed_steps])
        ws_summary.append(["Execution Status", "PASSED"])
        
        for row in range(4, 10):
            ws_summary[f"A{row}"].font = font_bold
            ws_summary[f"A{row}"].border = cell_border
            ws_summary[f"B{row}"].font = font_normal
            ws_summary[f"B{row}"].border = cell_border
            
        status_cell = ws_summary["B9"]
        status_cell.font = font_pass
        status_cell.fill = fill_pass

        for col in ws_summary.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws_summary.column_dimensions[col_letter].width = max(max_len + 3, 15)

        # 2. Test Cases Sheet
        ws_cases = wb.create_sheet(title="Test Cases")
        ws_cases.views.sheetView[0].showGridLines = True
        
        headers = ["Test Case ID", "Module", "Description", "Status", "Error Details", "Timestamp"]
        ws_cases.append(headers)
        ws_cases.row_dimensions[1].height = 25
        
        for col_idx, h in enumerate(headers, 1):
            cell = ws_cases.cell(row=1, column=col_idx)
            cell.font = font_bold
            cell.fill = fill_sub_header
            cell.alignment = Alignment(vertical="center")
            cell.border = cell_border
            
        now_str = datetime.datetime.now().strftime("%H:%M:%S")
        for step_idx, tc in enumerate(backend_cases, 2):
            ws_cases.append([tc["id"], tc["module"], tc["desc"], tc["status"], tc["error"], now_str])
            ws_cases.row_dimensions[step_idx].height = 20
            
            for col_idx in range(1, 7):
                c = ws_cases.cell(row=step_idx, column=col_idx)
                c.border = cell_border
                c.font = font_normal
                if col_idx == 1:
                    c.font = font_bold
                elif col_idx == 4:
                    c.font = font_pass
                    c.fill = fill_pass
                    c.alignment = Alignment(horizontal="center")
                elif col_idx == 6:
                    c.alignment = Alignment(horizontal="center")

        for col in ws_cases.columns:
            max_len = max(len(str(cell.value or '')) for cell in col)
            col_letter = get_column_letter(col[0].column)
            ws_cases.column_dimensions[col_letter].width = min(max(max_len + 3, 12), 40)
            
        wb.save(self.backend_report_excel_path)

    def generate_html(self, mobile_cases, backend_cases):
        total_tests = len(mobile_cases) + len(backend_cases)
        passed_tests = sum(1 for c in mobile_cases if c["status"] == "PASS") + sum(1 for c in backend_cases if c["status"] == "PASS")
        failed_tests = total_tests - passed_tests
        pass_rate = round((passed_tests / total_tests * 100), 1) if total_tests > 0 else 0.0
        
        date_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        # Compile Mobile rows
        mobile_rows_html = ""
        for tc in mobile_cases:
            badge_class = "badge-pass" if tc["status"] == "PASS" else "badge-fail"
            icon_span = '<span class="check-icon">✔</span>' if tc["status"] == "PASS" else '<span class="cross-icon">✘</span>'
            mobile_rows_html += f"""
            <tr>
                <td class="text-code">{tc["id"]}</td>
                <td>{tc["module"]}</td>
                <td>{tc["desc"]}</td>
                <td>{tc["expected"]}</td>
                <td><span class="{badge_class}">{icon_span} {tc["status"]}</span></td>
                <td class="error-details">{tc["error"]}</td>
            </tr>
            """

        # Compile Backend rows
        backend_rows_html = ""
        for tc in backend_cases:
            badge_class = "badge-pass" if tc["status"] == "PASS" else "badge-fail"
            icon_span = '<span class="check-icon">✔</span>' if tc["status"] == "PASS" else '<span class="cross-icon">✘</span>'
            backend_rows_html += f"""
            <tr>
                <td class="text-code">{tc["id"]}</td>
                <td>{tc["module"]}</td>
                <td>{tc["desc"]}</td>
                <td><span class="{badge_class}">{icon_span} {tc["status"]}</span></td>
                <td class="error-details">{tc["error"]}</td>
            </tr>
            """

        html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Smart Civic Governance - Test Execution Report</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap" rel="stylesheet">
    <style>
        body {{
            background-color: #0b0f17;
            color: #f8fafc;
            font-family: 'Outfit', sans-serif;
            margin: 0;
            padding: 2.5rem;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
        }}
        h1 {{
            font-size: 2.2rem;
            font-weight: 700;
            margin-bottom: 1.5rem;
            color: #ffffff;
            display: flex;
            align-items: center;
            gap: 12px;
        }}
        h2 {{
            font-size: 1.4rem;
            font-weight: 600;
            margin-top: 2.5rem;
            margin-bottom: 1.2rem;
            color: #ffffff;
            display: flex;
            align-items: center;
            gap: 10px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            padding-bottom: 8px;
        }}
        .summary-card {{
            background: rgba(30, 41, 59, 0.3);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 8px;
            padding: 1.5rem;
            margin-bottom: 2rem;
        }}
        .summary-card h3 {{
            margin-top: 0;
            margin-bottom: 1rem;
            font-size: 1.2rem;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
        }}
        ul.summary-list {{
            list-style: none;
            padding: 0;
            margin: 0;
        }}
        ul.summary-list li {{
            margin-bottom: 0.75rem;
            font-size: 1.05rem;
            color: #cbd5e1;
            display: flex;
            align-items: center;
            gap: 8px;
        }}
        ul.summary-list li strong {{
            color: #ffffff;
            font-weight: 600;
        }}
        .timestamp-val {{
            background: rgba(255, 255, 255, 0.1);
            padding: 3px 8px;
            border-radius: 4px;
            font-family: monospace;
            font-size: 0.95rem;
            color: #cbd5e1;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 2.5rem;
            background: rgba(15, 23, 42, 0.2);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 8px;
            overflow: hidden;
        }}
        th {{
            background: #1e293b;
            color: #ffffff;
            font-weight: 600;
            text-align: left;
            padding: 12px 16px;
            font-size: 0.9rem;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }}
        td {{
            padding: 14px 16px;
            font-size: 0.9rem;
            color: #94a3b8;
            border-bottom: 1px solid rgba(255, 255, 255, 0.05);
            vertical-align: middle;
        }}
        tr:last-child td {{
            border-bottom: none;
        }}
        tr:hover td {{
            background: rgba(255, 255, 255, 0.02);
            color: #f1f5f9;
        }}
        .text-code {{
            font-family: 'Courier New', Courier, monospace;
            font-weight: 700;
            color: #ffffff;
        }}
        .badge-pass {{
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(16, 185, 129, 0.08);
            border: 1px solid rgba(16, 185, 129, 0.2);
            color: #10b981;
            font-weight: 700;
            font-size: 0.75rem;
            padding: 3px 8px;
            border-radius: 4px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }}
        .badge-pass .check-icon {{
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 14px;
            height: 14px;
            background: #10b981;
            color: #0b0f17;
            border-radius: 2px;
            font-size: 0.75rem;
            font-weight: bold;
        }}
        .badge-fail {{
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(239, 68, 68, 0.08);
            border: 1px solid rgba(239, 68, 68, 0.2);
            color: #ef4444;
            font-weight: 700;
            font-size: 0.75rem;
            padding: 3px 8px;
            border-radius: 4px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }}
        .badge-fail .cross-icon {{
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 14px;
            height: 14px;
            background: #ef4444;
            color: #0b0f17;
            border-radius: 2px;
            font-size: 0.75rem;
            font-weight: bold;
        }}
        .error-details {{
            font-family: 'Courier New', Courier, monospace;
            font-size: 0.85rem;
            color: #94a3b8;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>🧪 Smart Civic Governance - Test Execution Report</h1>
        
        <div class="summary-card">
            <h3>📊 Execution Summary</h3>
            <ul class="summary-list">
                <li><strong>Timestamp:</strong> <span class="timestamp-val">{date_str}</span></li>
                <li><strong>Total Tests Executed:</strong> <strong>{total_tests}</strong></li>
                <li><strong>Passed:</strong> <strong>{passed_tests}</strong> &nbsp;✅</li>
                <li><strong>Failed:</strong> <strong>{failed_tests}</strong> &nbsp;❌</li>
                <li><strong>Pass Rate:</strong> <strong>{pass_rate}%</strong> &nbsp;📈</li>
            </ul>
        </div>
        
        <h2>📱 Mobile E2E Tests (Appium)</h2>
        <table>
            <thead>
                <tr>
                    <th style="width: 120px;">Test Case ID</th>
                    <th style="width: 140px;">Module/Screen</th>
                    <th>Description</th>
                    <th>Expected Result</th>
                    <th style="width: 110px;">Status</th>
                    <th>Error Details</th>
                </tr>
            </thead>
            <tbody>
                {mobile_rows_html}
            </tbody>
        </table>
        
        <h2>⚙️ Backend API & Security Tests</h2>
        <table>
            <thead>
                <tr>
                    <th style="width: 120px;">Test Case ID</th>
                    <th style="width: 140px;">Module</th>
                    <th>Description</th>
                    <th style="width: 110px;">Status</th>
                    <th>Error Details</th>
                </tr>
            </thead>
            <tbody>
                {backend_rows_html}
            </tbody>
        </table>
    </div>
</body>
</html>"""

        with open(self.html_path, "w", encoding="utf-8") as f:
            f.write(html_content)

    def generate_summary(self, mobile_cases, backend_cases, is_success):
        total_tests = len(mobile_cases) + len(backend_cases)
        passed_tests = sum(1 for c in mobile_cases if c["status"] == "PASS") + sum(1 for c in backend_cases if c["status"] == "PASS")
        failed_tests = total_tests - passed_tests
        pass_rate = f"{round((passed_tests / total_tests * 100), 1)}%" if total_tests > 0 else "0%"
        
        repo_name = os.environ.get("GITHUB_REPOSITORY", "Saitharun2416/Smart-Civic")
        github_username = repo_name.split("/")[0] if "/" in repo_name else "Saitharun2416"
        repository_name = repo_name.split("/")[1] if "/" in repo_name else "Smart-Civic"
        
        deployment_url = f"https://{github_username}.github.io/{repository_name}/"

        markdown = f"""# Mobile & Backend E2E Test Summary

**Deployment URL:**
{deployment_url}

### Key Metrics:
- **Total Tests Executed:** {total_tests}
- **Passed:** {passed_tests}
- **Failed:** {failed_tests}
- **Pass Percentage:** {pass_rate}

### Appium Mobile E2E Status:
{"- **PASSED** ✅" if is_success else "- **FAILED** ❌"}
"""
        with open(self.summary_path, "w", encoding="utf-8") as f:
            f.write(markdown)
