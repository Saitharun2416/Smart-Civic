import os
import sys
import time
import logging
import pytest
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from pages import PortalPage

# Ensure parent directory is in path for imports
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from report_generator import TestReporter

# Configure Logging
log_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "Test Results", "Logs")
os.makedirs(log_dir, exist_ok=True)
log_file = os.path.join(log_dir, "automation_execution.log")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.FileHandler(log_file, mode="w", encoding="utf-8"),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("SeleniumE2E")

# Directories for screenshots
screenshots_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "Test Results", "Screenshots")
os.makedirs(screenshots_dir, exist_ok=True)

# Test Status Tracker for Report Generation
test_results_summary = []

@pytest.fixture(scope="class")
def driver():
    # Read BASE_URL from env, fallback to local file path
    base_url = os.environ.get("BASE_URL")
    if not base_url:
        # Resolve path to local web/index.html
        local_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "web", "index.html")
        base_url = f"file:///{local_path.replace(os.sep, '/')}"
        logger.warning(f"BASE_URL env var not set. Falling back to local index: {base_url}")
    else:
        logger.info(f"Running E2E tests against BASE_URL: {base_url}")

    chrome_options = Options()
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--window-size=1920,1080")
    
    driver = webdriver.Chrome(options=chrome_options)
    driver.implicitly_wait(5)
    
    yield driver, base_url
    
    driver.quit()
    logger.info("Browser session closed.")


class TestSmartCivicGovernanceFlow:

    def take_screenshot(self, driver, filename):
        path = os.path.join(screenshots_dir, filename)
        driver.save_screenshot(path)
        logger.info(f"Screenshot captured: {path}")
        return path

    def test_full_e2e_governance_flow(self, driver):
        driver_instance, base_url = driver
        portal = PortalPage(driver_instance, base_url)
        reporter = TestReporter()
        
        test_run_success = True
        steps_executed = []

        try:
            # ----------------------------------------------------
            # STEP 1: OPEN WEB DASHBOARD
            # ----------------------------------------------------
            step_name = "1. Open Web Portal"
            logger.info(f"Starting step: {step_name}")
            portal.open()
            self.take_screenshot(driver_instance, "01_portal_opened.png")
            steps_executed.append((step_name, "Passed", "Opened Portal successfully"))

            # ----------------------------------------------------
            # STEP 2: SWITCH TO CITIZEN ROLE & REPORT COMPLAINT
            # ----------------------------------------------------
            step_name = "2. Switch to Citizen & Report Pothole Complaint"
            logger.info(f"Starting step: {step_name}")
            portal.switch_role("citizen")
            
            # Fill form and submit
            complaint_title = "Pothole on 5th Avenue E2E"
            complaint_desc = "Deep crater in center lane. Cars swerving dangerously."
            portal.report_complaint(
                title=complaint_title,
                desc=complaint_desc,
                category="Pothole",
                priority="High",
                address="5th Avenue, Sector 4, Bangalore",
                image_url="https://images.unsplash.com/photo-1515162305285-0293e4767cc2?w=400"
            )
            
            self.take_screenshot(driver_instance, "02_complaint_submitted.png")
            
            # Verify complaint is visible in Citizen list
            complaints = portal.get_citizen_complaints()
            found = any(complaint_title in c["title"] for c in complaints)
            assert found, "Reported complaint not found in Citizen list!"
            
            logger.info("Complaint submitted and verified in Citizen list.")
            steps_executed.append((step_name, "Passed", "Complaint reported and verified in Citizen list"))

            # ----------------------------------------------------
            # STEP 3: SWITCH TO WORKER ROLE & ACCEPT TASK
            # ----------------------------------------------------
            step_name = "3. Switch to Worker & Accept Assigned Task"
            logger.info(f"Starting step: {step_name}")
            portal.switch_role("worker")
            self.take_screenshot(driver_instance, "03_worker_view.png")
            
            # Get stats before accepting
            stats_before = portal.get_worker_stats()
            logger.info(f"Worker Stats Before: {stats_before}")
            
            # Accept task
            accepted = portal.accept_task(complaint_title)
            assert accepted, "Failed to accept task under available alerts!"
            
            # Verify task moved to active tasks
            active_tasks = portal.get_active_tasks()
            found_active = any(complaint_title in t["title"] for t in active_tasks)
            assert found_active, "Accepted task not visible in active tasks!"
            
            self.take_screenshot(driver_instance, "04_task_accepted.png")
            logger.info("Task accepted and moved to active tasks.")
            steps_executed.append((step_name, "Passed", "Worker accepted task and verified state transition"))

            # ----------------------------------------------------
            # STEP 4: WORKER SUBMITS RESOLUTION PROOF
            # ----------------------------------------------------
            step_name = "4. Worker Submits Resolution Proof"
            logger.info(f"Starting step: {step_name}")
            
            proof_url = "https://images.unsplash.com/photo-1598514982205-f36b96d1e8d4?w=400"
            notes = "Pothole filled with cold mix asphalt and compacted. Clear for traffic."
            resolved = portal.submit_resolution_proof(complaint_title, proof_url, notes)
            assert resolved, "Failed to submit resolution proof!"
            
            self.take_screenshot(driver_instance, "05_proof_submitted.png")
            logger.info("Worker submitted resolution proof successfully.")
            steps_executed.append((step_name, "Passed", "Resolution proof submitted and status transitioned to Verification Pending"))

            # ----------------------------------------------------
            # STEP 5: SWITCH TO ADMIN ROLE & VERIFY RESOLUTION
            # ----------------------------------------------------
            step_name = "5. Switch to Admin & Approve Resolution"
            logger.info(f"Starting step: {step_name}")
            portal.switch_role("admin")
            self.take_screenshot(driver_instance, "06_admin_view.png")
            
            # Verify in queue
            admin_stats = portal.get_admin_dashboard_stats()
            logger.info(f"Admin Dashboard Stats: {admin_stats}")
            
            # Approve resolution
            approved = portal.verify_resolution(complaint_title, approve=True)
            assert approved, "Failed to approve resolution in Admin queue!"
            
            self.take_screenshot(driver_instance, "07_resolution_approved.png")
            logger.info("Admin approved worker proof successfully.")
            steps_executed.append((step_name, "Passed", "Admin verified and approved proof. Status transitioned to Resolved"))

            # ----------------------------------------------------
            # STEP 6: VERIFY POINTS ADDED & LEADERBOARD RANKINGS
            # ----------------------------------------------------
            step_name = "6. Verify Leaderboard & Worker Points"
            logger.info(f"Starting step: {step_name}")
            
            # Switch back to worker view to verify points
            portal.switch_role("worker")
            stats_after = portal.get_worker_stats()
            logger.info(f"Worker Stats After: {stats_after}")
            
            # Verify points increased (from 120 base, should add +10 points = 130 PTS or +15 with speed bonus)
            assert "130" in stats_after["points"] or "135" in stats_after["points"], "Worker points did not increase correctly!"
            
            # Verify Leaderboard updates
            leaderboard = portal.get_leaderboard()
            found_leaderboard = any(w["name"] == "James Miller" for w in leaderboard)
            assert found_leaderboard, "Worker not visible on global leaderboard!"
            
            self.take_screenshot(driver_instance, "08_leaderboard_verified.png")
            logger.info("Points and leaderboard rank verified.")
            steps_executed.append((step_name, "Passed", "Worker points incremented and verified on Leaderboard"))

            # ----------------------------------------------------
            # STEP 7: CITIZEN RATES WORKER RESOLUTION
            # ----------------------------------------------------
            step_name = "7. Citizen Rates Resolution"
            logger.info(f"Starting step: {step_name}")
            portal.switch_role("citizen")
            
            rated = portal.rate_resolution(complaint_title, 5, "Amazing speedy work. Very clean job.")
            assert rated, "Failed to submit citizen rating!"
            
            self.take_screenshot(driver_instance, "09_rating_submitted.png")
            logger.info("Citizen rated the worker resolution 5 stars.")
            steps_executed.append((step_name, "Passed", "Citizen rated resolution and submitted review"))

        except Exception as e:
            logger.error(f"E2E test failed on step {step_name}: {str(e)}", exc_info=True)
            self.take_screenshot(driver_instance, "99_execution_error.png")
            steps_executed.append((step_name, "Failed", str(e)))
            test_run_success = False
            raise e
            
        finally:
            # Generate Execution Reports
            logger.info("Compiling Test Results and generating reports...")
            reporter.generate_reports(steps_executed, test_run_success)
            logger.info("Reports successfully written to disk.")
