import time
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select

class BasePage:
    def __init__(self, driver, base_url):
        self.driver = driver
        self.base_url = base_url
        self.wait = WebDriverWait(self.driver, 10)

    def open(self):
        self.driver.get(self.base_url)
        # Small sleep to ensure page loads fully
        time.sleep(1)

    def find(self, locator):
        return self.wait.until(EC.presence_of_element_located(locator))

    def find_all(self, locator):
        return self.wait.until(EC.presence_of_all_elements_located(locator))

    def click(self, locator):
        element = self.wait.until(EC.element_to_be_clickable(locator))
        element.click()

    def type(self, locator, text):
        element = self.find(locator)
        element.clear()
        element.send_keys(text)


class PortalPage(BasePage):
    # Locators
    ROLE_SELECTOR = (By.ID, "role-selector")
    HEADER_USERNAME = (By.ID, "header-username")
    HEADER_ROLE = (By.ID, "header-user-role")
    
    # Citizen Locators
    COMPLAINT_TITLE = (By.ID, "complaint-title")
    COMPLAINT_DESC = (By.ID, "complaint-desc")
    COMPLAINT_CATEGORY = (By.ID, "complaint-category")
    COMPLAINT_PRIORITY = (By.ID, "complaint-priority")
    COMPLAINT_LAT = (By.ID, "complaint-lat")
    COMPLAINT_LNG = (By.ID, "complaint-lng")
    COMPLAINT_ADDRESS = (By.ID, "complaint-address")
    COMPLAINT_IMAGE = (By.ID, "complaint-image")
    BTN_SUBMIT_COMPLAINT = (By.ID, "btn-submit-complaint")
    CITIZEN_COMPLAINTS_LIST = (By.ID, "citizen-complaints-list")
    
    # Worker Locators
    WORKER_RANK = (By.ID, "worker-rank")
    WORKER_POINTS = (By.ID, "worker-points")
    WORKER_SOLVED = (By.ID, "worker-solved")
    WORKER_AVG_TIME = (By.ID, "worker-avg-time")
    WORKER_ACTIVE_TASKS = (By.ID, "worker-active-tasks")
    WORKER_AVAILABLE_TASKS = (By.ID, "worker-available-tasks")
    
    # Admin Locators
    ADMIN_TOTAL_COMPLAINTS = (By.ID, "admin-total-complaints")
    ADMIN_PENDING_VERIFICATIONS = (By.ID, "admin-pending-verifications")
    ADMIN_DUPLICATES = (By.ID, "admin-duplicates")
    ADMIN_VERIFICATION_LIST = (By.ID, "admin-verification-list")
    ADMIN_ALL_COMPLAINTS = (By.ID, "admin-all-complaints-body")
    ADMIN_USERS_BODY = (By.ID, "admin-users-body")
    
    # Modals
    PROOF_COMPLAINT_ID = (By.ID, "modal-complaint-id")
    PROOF_IMAGE_URL = (By.ID, "proof-image-url")
    PROOF_NOTES = (By.ID, "proof-notes")
    BTN_SUBMIT_PROOF = (By.CSS_SELECTOR, "#submit-proof-form button[type='submit']")
    
    RATING_COMPLAINT_ID = (By.ID, "modal-rating-complaint-id")
    RATING_FEEDBACK = (By.ID, "rating-feedback")
    BTN_SUBMIT_RATING = (By.CSS_SELECTOR, "#submit-rating-form button[type='submit']")
    
    # Leaderboard & Notifications
    LEADERBOARD_BODY = (By.ID, "leaderboard-body")
    NOTIFICATIONS_LIST = (By.ID, "notifications-list")

    def switch_role(self, role):
        """Switches active role: citizen, worker, admin"""
        selector_element = self.find(self.ROLE_SELECTOR)
        select = Select(selector_element)
        select.select_by_value(role)
        # Give role switch animation time to complete
        time.sleep(0.5)

    def get_active_user(self):
        username = self.find(self.HEADER_USERNAME).text
        role = self.find(self.HEADER_ROLE).text
        return username, role

    def report_complaint(self, title, desc, category, priority, address, lat=12.971598, lng=77.594562, image_url=""):
        """Reports a new complaint from Citizen View"""
        self.type(self.COMPLAINT_TITLE, title)
        self.type(self.COMPLAINT_DESC, desc)
        
        # Select category
        category_select = Select(self.find(self.COMPLAINT_CATEGORY))
        category_select.select_by_value(category)
        
        # Select priority
        priority_select = Select(self.find(self.COMPLAINT_PRIORITY))
        priority_select.select_by_value(priority)
        
        # Lat/Lng & Address
        self.type(self.COMPLAINT_LAT, str(lat))
        self.type(self.COMPLAINT_LNG, str(lng))
        self.type(self.COMPLAINT_ADDRESS, address)
        
        if image_url:
            self.type(self.COMPLAINT_IMAGE, image_url)
            
        self.click(self.BTN_SUBMIT_COMPLAINT)
        
        time.sleep(0.5)

    def get_citizen_complaints(self):
        """Returns list of complaints visible in Citizen View"""
        container = self.find(self.CITIZEN_COMPLAINTS_LIST)
        items = container.find_elements(By.CLASS_NAME, "complaint-item")
        complaints = []
        for item in items:
            title = item.find_element(By.CLASS_NAME, "item-title").text
            status = item.find_element(By.CSS_SELECTOR, ".badge[class*='badge-']").text
            complaints.append({"title": title, "status": status, "element": item})
        return complaints

    def get_available_tasks(self):
        """Returns list of pending tasks in Worker View"""
        container = self.find(self.WORKER_AVAILABLE_TASKS)
        items = container.find_elements(By.CLASS_NAME, "task-item")
        tasks = []
        for item in items:
            title = item.find_element(By.CLASS_NAME, "item-title").text
            btn = item.find_element(By.CLASS_NAME, "btn-accept")
            tasks.append({"title": title, "accept_button": btn})
        return tasks

    def get_active_tasks(self):
        """Returns list of in-progress tasks in Worker View"""
        container = self.find(self.WORKER_ACTIVE_TASKS)
        items = container.find_elements(By.CLASS_NAME, "task-item")
        tasks = []
        for item in items:
            title = item.find_element(By.CLASS_NAME, "item-title").text
            btn = item.find_element(By.CLASS_NAME, "btn-resolve")
            tasks.append({"title": title, "resolve_button": btn})
        return tasks

    def accept_task(self, complaint_title):
        """Accepts a task in Worker View"""
        tasks = self.get_available_tasks()
        for t in tasks:
            if complaint_title in t["title"]:
                t["accept_button"].click()
                time.sleep(0.5)
                return True
        return False

    def submit_resolution_proof(self, complaint_title, proof_url, notes):
        """Submits resolution proof for a task in Worker View"""
        tasks = self.get_active_tasks()
        for t in tasks:
            if complaint_title in t["title"]:
                t["resolve_button"].click()
                time.sleep(0.5)
                self.type(self.PROOF_IMAGE_URL, proof_url)
                self.type(self.PROOF_NOTES, notes)
                self.click(self.BTN_SUBMIT_PROOF)
                time.sleep(0.5)
                return True
        return False

    def get_worker_stats(self):
        """Returns worker statistics"""
        rank = self.find(self.WORKER_RANK).text
        points = self.find(self.WORKER_POINTS).text
        solved = self.find(self.WORKER_SOLVED).text
        avg_time = self.find(self.WORKER_AVG_TIME).text
        return {
            "rank": rank,
            "points": points,
            "solved": solved,
            "average_resolution_time": avg_time
        }

    def get_admin_dashboard_stats(self):
        total = self.find(self.ADMIN_TOTAL_COMPLAINTS).text
        verifs = self.find(self.ADMIN_PENDING_VERIFICATIONS).text
        dups = self.find(self.ADMIN_DUPLICATES).text
        return {"total": total, "verifications": verifs, "duplicates": dups}

    def get_pending_verifications(self):
        """Returns verification pending queue items in Admin View"""
        container = self.find(self.ADMIN_VERIFICATION_LIST)
        items = container.find_elements(By.CLASS_NAME, "verification-card")
        cards = []
        for item in items:
            title = item.find_element(By.CSS_SELECTOR, "h4").text
            btn_approve = item.find_element(By.CLASS_NAME, "btn-approve")
            btn_reject = item.find_element(By.CLASS_NAME, "btn-reject")
            cards.append({
                "title": title,
                "approve_button": btn_approve,
                "reject_button": btn_reject
            })
        return cards

    def verify_resolution(self, complaint_title, approve=True):
        """Verifies resolution proof in Admin View"""
        cards = self.get_pending_verifications()
        for c in cards:
            if complaint_title in c["title"]:
                if approve:
                    c["approve_button"].click()
                else:
                    c["reject_button"].click()
                time.sleep(0.5)
                return True
        return False

    def rate_resolution(self, complaint_title, rating_stars, feedback):
        """Rates a resolved complaint from Citizen View"""
        complaints = self.get_citizen_complaints()
        for c in complaints:
            if complaint_title in c["title"] and "RESOLVED" in c["status"]:
                btn = c["element"].find_element(By.CLASS_NAME, "btn-resolve")
                btn.click()
                time.sleep(0.5)
                
                # Select star rating (star-1 to star-5)
                star_locator = (By.ID, f"star-{rating_stars}")
                self.click(star_locator)
                
                # Fill feedback
                self.type(self.RATING_FEEDBACK, feedback)
                self.click(self.BTN_SUBMIT_RATING)
                time.sleep(0.5)
                return True
        return False

    def get_leaderboard(self):
        """Reads worker ranking leaderboard table"""
        container = self.find(self.LEADERBOARD_BODY)
        rows = container.find_elements(By.TAG_NAME, "tr")
        workers = []
        for r in rows:
            cols = r.find_elements(By.TAG_NAME, "td")
            if len(cols) >= 5:
                workers.append({
                    "rank": cols[0].text,
                    "name": cols[1].text,
                    "points": cols[2].text,
                    "solved": cols[3].text,
                    "rating": cols[4].text
                })
        return workers
