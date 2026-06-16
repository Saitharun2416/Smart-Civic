/* ==========================================================================
   CIVICSMART - APP SIMULATOR CORE
   ========================================================================== */

// Initial Seed Data
const DEFAULT_USERS = [
    { uid: "admin_uid", name: "System Admin", email: "admin@civicsmart.gov", role: "admin", joinedAt: "2026-01-15", disabled: false },
    { uid: "citizen_uid", name: "John Doe", email: "john.doe@gmail.com", role: "citizen", joinedAt: "2026-03-20", disabled: false },
    { uid: "worker_1", name: "James Miller", email: "james.m@civicsmart.gov", role: "worker", joinedAt: "2026-02-10", disabled: false },
    { uid: "worker_2", name: "Sarah Connor", email: "sarah.c@civicsmart.gov", role: "worker", joinedAt: "2026-02-25", disabled: false }
];

const DEFAULT_WORKERS = [
    { uid: "worker_1", name: "James Miller", totalPoints: 120, issuesSolved: 12, activeTasks: 0, averageResolutionTimeMinutes: 45.0, averageRating: 4.8, rank: 1, badges: ["Fast Resolver", "Top Rated"], joinedAt: "2026-02-10" },
    { uid: "worker_2", name: "Sarah Connor", totalPoints: 95, issuesSolved: 9, activeTasks: 0, averageResolutionTimeMinutes: 62.0, averageRating: 4.5, rank: 2, badges: ["Community Hero"], joinedAt: "2026-02-25" }
];

const DEFAULT_COMPLAINTS = [
    {
        complaintId: "comp_001",
        title: "Pothole on 5th Avenue",
        description: "Huge pothole causing traffic slowdowns and damage to tires.",
        category: "Pothole",
        imageUrl: "https://images.unsplash.com/photo-1515162305285-0293e4767cc2?w=400",
        latitude: 12.971598,
        longitude: 77.594562,
        address: "5th Avenue, Sector 4, Bangalore",
        status: "Resolved",
        citizenId: "citizen_uid",
        citizenName: "John Doe",
        workerId: "worker_1",
        workerName: "James Miller",
        proofImageUrl: "https://images.unsplash.com/photo-1598514982205-f36b96d1e8d4?w=400",
        createdAt: "2026-06-15T09:00:00Z",
        acceptedAt: "2026-06-15T09:30:00Z",
        resolvedAt: "2026-06-15T11:00:00Z",
        citizenRating: 5,
        citizenFeedback: "Promptly fixed. Thank you!",
        priority: "High",
        isDuplicate: false,
        verified: true
    },
    {
        complaintId: "comp_002",
        title: "Garbage Pile near park entrance",
        description: "Large heap of unsorted trash blocking the main walking gate.",
        category: "Garbage",
        imageUrl: "https://images.unsplash.com/photo-1611284446314-60a58ac0deb9?w=400",
        latitude: 12.972500,
        longitude: 77.595000,
        address: "Park Street, Sector 3, Bangalore",
        status: "Pending",
        citizenId: "citizen_uid",
        citizenName: "John Doe",
        workerId: null,
        workerName: null,
        proofImageUrl: null,
        createdAt: "2026-06-16T06:00:00Z",
        acceptedAt: null,
        resolvedAt: null,
        citizenRating: null,
        citizenFeedback: null,
        priority: "Medium",
        isDuplicate: false,
        verified: false
    }
];

const DEFAULT_NOTIFICATIONS = [
    {
        notifId: "notif_001",
        recipientId: "citizen_uid",
        title: "Issue Resolved!",
        body: "The issue \"Pothole on 5th Avenue\" has been verified and marked as resolved by admin. Please rate their service!",
        type: "complaint_resolved",
        complaintId: "comp_001",
        isRead: false,
        createdAt: "2026-06-15T11:05:00Z"
    }
];

// Database State Controller
class DatabaseState {
    constructor() {
        this.load();
    }

    load() {
        this.users = JSON.parse(localStorage.getItem("smart_users")) || [...DEFAULT_USERS];
        this.workers = JSON.parse(localStorage.getItem("smart_workers")) || [...DEFAULT_WORKERS];
        this.complaints = JSON.parse(localStorage.getItem("smart_complaints")) || [...DEFAULT_COMPLAINTS];
        this.notifications = JSON.parse(localStorage.getItem("smart_notifications")) || [...DEFAULT_NOTIFICATIONS];
    }

    save() {
        localStorage.setItem("smart_users", JSON.stringify(this.users));
        localStorage.setItem("smart_workers", JSON.stringify(this.workers));
        localStorage.setItem("smart_complaints", JSON.stringify(this.complaints));
        localStorage.setItem("smart_notifications", JSON.stringify(this.notifications));
    }

    reset() {
        localStorage.clear();
        this.load();
    }
}

const db = new DatabaseState();

// Active Session Context
let currentUserId = "worker_1";
let currentUserRole = "worker";

// DOM Elements cache
const els = {
    roleSelector: document.getElementById("role-selector"),
    headerUsername: document.getElementById("header-username"),
    headerUserRole: document.getElementById("header-user-role"),
    headerAvatar: document.getElementById("header-avatar"),
    
    // Portal Views
    viewCitizen: document.getElementById("view-citizen"),
    viewWorker: document.getElementById("view-worker"),
    viewAdmin: document.getElementById("view-admin"),
    
    // Lists
    citizenComplaints: document.getElementById("citizen-complaints-list"),
    workerActiveTasks: document.getElementById("worker-active-tasks"),
    workerAvailableTasks: document.getElementById("worker-available-tasks"),
    leaderboardBody: document.getElementById("leaderboard-body"),
    notificationsList: document.getElementById("notifications-list"),
    unreadCount: document.getElementById("unread-count"),
    
    // Forms
    reportForm: document.getElementById("report-complaint-form"),
    submitProofForm: document.getElementById("submit-proof-form"),
    submitRatingForm: document.getElementById("submit-rating-form"),
    
    // Modals
    proofModal: document.getElementById("submit-proof-modal"),
    ratingModal: document.getElementById("rating-modal"),
    btnCloseModal: document.getElementById("btn-close-modal"),
    btnCloseRatingModal: document.getElementById("btn-close-rating-modal"),
    
    // Worker Stats
    workerRank: document.getElementById("worker-rank"),
    workerPoints: document.getElementById("worker-points"),
    workerSolved: document.getElementById("worker-solved"),
    workerAvgTime: document.getElementById("worker-avg-time"),
    
    // Admin Panels
    adminTotalComplaints: document.getElementById("admin-total-complaints"),
    adminPendingVerifications: document.getElementById("admin-pending-verifications"),
    adminDuplicates: document.getElementById("admin-duplicates"),
    adminVerificationList: document.getElementById("admin-verification-list"),
    adminAllComplaints: document.getElementById("admin-all-complaints-body"),
    adminUsers: document.getElementById("admin-users-body"),
    adminDuplicateList: document.getElementById("admin-duplicate-list")
};

// ==========================================================================
// SESSION CONTROLLER
// ==========================================================================
function switchRole(role) {
    currentUserRole = role;
    
    // Select appropriate default user ID
    if (role === "citizen") currentUserId = "citizen_uid";
    else if (role === "worker") currentUserId = "worker_1";
    else if (role === "admin") currentUserId = "admin_uid";
    
    const user = db.users.find(u => u.uid === currentUserId);
    if (!user) return;
    
    els.headerUsername.textContent = user.name;
    els.headerUserRole.textContent = user.role.toUpperCase();
    els.headerAvatar.textContent = user.name.charAt(0);
    
    // Hide all views
    els.viewCitizen.classList.add("hidden");
    els.viewWorker.classList.add("hidden");
    els.viewAdmin.classList.add("hidden");
    
    // Show selected view
    if (role === "citizen") els.viewCitizen.classList.remove("hidden");
    else if (role === "worker") els.viewWorker.classList.remove("hidden");
    else if (role === "admin") els.viewAdmin.classList.remove("hidden");
    
    // Sync view specific data
    renderAll();
}

// ==========================================================================
// RENDERING CONTROLLER
// ==========================================================================
function renderAll() {
    renderNotifications();
    renderLeaderboard();
    
    if (currentUserRole === "citizen") {
        renderCitizenView();
    } else if (currentUserRole === "worker") {
        renderWorkerView();
    } else if (currentUserRole === "admin") {
        renderAdminView();
    }
}

// Notifications Panel
function renderNotifications() {
    const list = els.notificationsList;
    list.innerHTML = "";
    
    const userNotifs = db.notifications
        .filter(n => n.recipientId === currentUserId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        
    const unread = userNotifs.filter(n => !n.isRead).length;
    els.unreadCount.textContent = `${unread} New`;
    
    if (userNotifs.length === 0) {
        list.innerHTML = '<div class="empty-state">No alerts. You are up to date!</div>';
        return;
    }
    
    userNotifs.forEach(n => {
        const div = document.createElement("div");
        div.className = `notif-item ${n.isRead ? "" : "unread"}`;
        div.onclick = () => {
            n.isRead = true;
            db.save();
            renderNotifications();
        };
        
        const date = new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        div.innerHTML = `
            <div class="notif-header">
                <span>${n.title}</span>
                <span class="notif-time">${date}</span>
            </div>
            <div class="notif-body">${n.body}</div>
        `;
        list.appendChild(div);
    });
}

// Global Leaderboard
function renderLeaderboard() {
    const tbody = els.leaderboardBody;
    tbody.innerHTML = "";
    
    // Sort workers by points descending
    const sortedWorkers = [...db.workers].sort((a, b) => b.totalPoints - a.totalPoints);
    
    // Recalculate ranks in-memory
    sortedWorkers.forEach((w, index) => {
        w.rank = index + 1;
        
        // Dynamic badges allocation
        w.badges = [];
        if (w.totalPoints >= 100) w.badges.push("Veteran");
        if (w.averageRating >= 4.7) w.badges.push("Top Rated");
        if (w.issuesSolved >= 10) w.badges.push("Master Resolver");
        if (w.averageResolutionTimeMinutes <= 50.0 && w.issuesSolved > 0) w.badges.push("Speedy");
    });
    
    // Save updated ranks
    db.workers.forEach(original => {
        const updated = sortedWorkers.find(sw => sw.uid === original.uid);
        if (updated) {
            original.rank = updated.rank;
            original.badges = updated.badges;
        }
    });
    db.save();

    sortedWorkers.forEach(w => {
        const tr = document.createElement("tr");
        const rankClass = w.rank <= 3 ? `rank-column rank-${w.rank}` : "rank-column";
        const rankText = w.rank === 1 ? `<i class="fa-solid fa-crown rank-1"></i> 1` : w.rank;
        
        const badgesHtml = w.badges.map(b => `<span class="badge-pill">${b}</span>`).join("");
        
        tr.innerHTML = `
            <td class="${rankClass}">${rankText}</td>
            <td style="font-weight: 600; color: #fff;">${w.name}</td>
            <td>${w.totalPoints} PTS</td>
            <td>${w.issuesSolved} Solved</td>
            <td><i class="fa-solid fa-star" style="color: #f59e0b;"></i> ${w.averageRating.toFixed(1)}</td>
            <td>${badgesHtml || '<span class="text-muted" style="font-size: 0.75rem;">None</span>'}</td>
        `;
        tbody.appendChild(tr);
    });
}

// CITIZEN VIEW
function renderCitizenView() {
    const list = els.citizenComplaints;
    list.innerHTML = "";
    
    const myComplaints = db.complaints
        .filter(c => c.citizenId === currentUserId)
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        
    if (myComplaints.length === 0) {
        list.innerHTML = '<div class="empty-state">No complaints reported yet.</div>';
        return;
    }
    
    myComplaints.forEach(c => {
        const div = document.createElement("div");
        div.className = "complaint-item";
        
        const statusBadge = `badge-${c.status.toLowerCase().replace(" ", "-")}`;
        const priorityBadge = `badge-${c.priority.toLowerCase()}`;
        const duplicateBadge = c.isDuplicate ? '<span class="badge badge-duplicate"><i class="fa-solid fa-clone"></i> Duplicate</span>' : '';
        
        let footerAction = "";
        if (c.status === "Resolved" && c.citizenRating === null) {
            footerAction = `<button class="action-btn btn-resolve" onclick="openRatingModal('${c.complaintId}', '${c.title.replace(/'/g, "\\'")}')"><i class="fa-solid fa-star"></i> Rate Resolution</button>`;
        } else if (c.citizenRating !== null) {
            footerAction = `<span>Rated: ${"★".repeat(c.citizenRating)}${"☆".repeat(5-c.citizenRating)}</span>`;
        } else if (c.status === "Verification Pending") {
            footerAction = `<span style="color:#a855f7;"><i class="fa-solid fa-clock-rotate-left"></i> Awaiting Verification</span>`;
        } else if (c.status === "In Progress") {
            footerAction = `<span style="color:#3b82f6;"><i class="fa-solid fa-person-digging"></i> Assigned to ${c.workerName}</span>`;
        } else {
            footerAction = `<span>Reported on ${new Date(c.createdAt).toLocaleDateString()}</span>`;
        }

        div.innerHTML = `
            <div class="item-header">
                <span class="item-title">${c.title}</span>
                <div class="item-meta">
                    <span class="badge ${statusBadge}">${c.status}</span>
                    <span class="badge ${priorityBadge}">${c.priority}</span>
                    ${duplicateBadge}
                </div>
            </div>
            <div class="item-body">
                <img src="${c.imageUrl}" class="item-image" onerror="this.src='https://picsum.photos/100'">
                <div class="item-text">
                    <p>${c.description}</p>
                    <p style="margin-top: 0.5rem; font-size: 0.75rem; color: #64748b;"><i class="fa-solid fa-location-dot"></i> ${c.address}</p>
                </div>
            </div>
            <div class="item-footer">
                <span>Category: ${c.category}</span>
                ${footerAction}
            </div>
        `;
        list.appendChild(div);
    });
}

// WORKER VIEW
function renderWorkerView() {
    const stats = db.workers.find(w => w.uid === currentUserId);
    if (stats) {
        els.workerRank.textContent = `#${stats.rank}`;
        els.workerPoints.textContent = `${stats.totalPoints} PTS`;
        els.workerSolved.textContent = stats.issuesSolved;
        
        // Calculate dynamic active tasks
        const myActive = db.complaints.filter(c => c.workerId === currentUserId && c.status === "In Progress").length;
        stats.activeTasks = myActive;
        db.save();

        els.workerAvgTime.textContent = stats.averageResolutionTimeMinutes > 0 ? `${Math.round(stats.averageResolutionTimeMinutes)}m` : 'N/A';
    }
    
    // Render Active Tasks
    const activeList = els.workerActiveTasks;
    activeList.innerHTML = "";
    
    const activeTasks = db.complaints.filter(c => c.workerId === currentUserId && c.status === "In Progress");
    if (activeTasks.length === 0) {
        activeList.innerHTML = '<div class="empty-state">No active tasks in progress.</div>';
    } else {
        activeTasks.forEach(c => {
            const div = document.createElement("div");
            div.className = "task-item";
            
            const priorityBadge = `badge-${c.priority.toLowerCase()}`;
            
            div.innerHTML = `
                <div class="item-header">
                    <span class="item-title">${c.title}</span>
                    <span class="badge ${priorityBadge}">${c.priority}</span>
                </div>
                <div class="item-body">
                    <img src="${c.imageUrl}" class="item-image" onerror="this.src='https://picsum.photos/100'">
                    <div class="item-text">
                        <p>${c.description}</p>
                        <p style="margin-top: 0.35rem; font-size: 0.75rem; color: #64748b;"><i class="fa-solid fa-location-dot"></i> ${c.address}</p>
                    </div>
                </div>
                <div class="item-footer">
                    <span>Category: ${c.category}</span>
                    <button class="action-btn btn-resolve" onclick="openSubmitProofModal('${c.complaintId}', '${c.title.replace(/'/g, "\\'")}')"><i class="fa-solid fa-camera"></i> Resolve Task</button>
                </div>
            `;
            activeList.appendChild(div);
        });
    }
    
    // Render Available Alerts (Status: Pending)
    const availList = els.workerAvailableTasks;
    availList.innerHTML = "";
    
    const availTasks = db.complaints.filter(c => c.status === "Pending");
    if (availTasks.length === 0) {
        availList.innerHTML = '<div class="empty-state">No pending complaints. All clean!</div>';
    } else {
        availTasks.forEach(c => {
            const div = document.createElement("div");
            div.className = "task-item";
            
            const priorityBadge = `badge-${c.priority.toLowerCase()}`;
            
            div.innerHTML = `
                <div class="item-header">
                    <span class="item-title">${c.title}</span>
                    <span class="badge ${priorityBadge}">${c.priority}</span>
                </div>
                <div class="item-body">
                    <img src="${c.imageUrl}" class="item-image" onerror="this.src='https://picsum.photos/100'">
                    <div class="item-text">
                        <p>${c.description}</p>
                        <p style="margin-top: 0.35rem; font-size: 0.75rem; color: #64748b;"><i class="fa-solid fa-location-dot"></i> ${c.address}</p>
                    </div>
                </div>
                <div class="item-footer">
                    <span>Category: ${c.category}</span>
                    <button class="action-btn btn-accept" onclick="acceptTask('${c.complaintId}')"><i class="fa-solid fa-check"></i> Accept Task</button>
                </div>
            `;
            availList.appendChild(div);
        });
    }
}

// ADMIN VIEW
function renderAdminView() {
    // Total numbers
    els.adminTotalComplaints.textContent = db.complaints.length;
    els.adminPendingVerifications.textContent = db.complaints.filter(c => c.status === "Verification Pending").length;
    els.adminDuplicates.textContent = db.complaints.filter(c => c.isDuplicate).length;
    
    // Render Verification queue
    const queue = els.adminVerificationList;
    queue.innerHTML = "";
    
    const pendingVerifs = db.complaints.filter(c => c.status === "Verification Pending");
    if (pendingVerifs.length === 0) {
        queue.innerHTML = '<div class="empty-state">No resolutions pending verification.</div>';
    } else {
        pendingVerifs.forEach(c => {
            const div = document.createElement("div");
            div.className = "verification-card";
            
            div.innerHTML = `
                <div class="item-header" style="border-bottom: 1px solid var(--border-color); padding-bottom: 0.5rem;">
                    <div>
                        <h4 style="font-size: 1.1rem; color: #fff;">${c.title}</h4>
                        <p style="font-size: 0.8rem; color: var(--text-secondary);">Worker: <strong>${c.workerName}</strong> | Category: ${c.category}</p>
                    </div>
                    <div>
                        <button class="action-btn btn-approve" onclick="verifyResolution('${c.complaintId}', true)"><i class="fa-solid fa-check"></i> Approve</button>
                        <button class="action-btn btn-reject" onclick="verifyResolution('${c.complaintId}', false)"><i class="fa-solid fa-xmark"></i> Reject</button>
                    </div>
                </div>
                <div style="font-size: 0.85rem; color: var(--text-secondary);">
                    <p><strong>Citizen Description:</strong> ${c.description}</p>
                    <p style="margin-top: 0.5rem;"><strong>Worker Notes:</strong> <em>"${c.workerNotes || 'No notes provided'}"</em></p>
                </div>
                <div class="verification-images">
                    <div class="verification-image-box">
                        <span>Original Issue</span>
                        <img src="${c.imageUrl}">
                    </div>
                    <div class="verification-image-box">
                        <span>Resolution Proof</span>
                        <img src="${c.proofImageUrl}">
                    </div>
                </div>
            `;
            queue.appendChild(div);
        });
    }
    
    // Render All Complaints Table
    const tableBody = els.adminAllComplaints;
    tableBody.innerHTML = "";
    
    db.complaints.forEach(c => {
        const tr = document.createElement("tr");
        
        const statusBadge = `badge-${c.status.toLowerCase().replace(" ", "-")}`;
        const priorityBadge = `badge-${c.priority.toLowerCase()}`;
        
        let workerAssignCell = "";
        if (c.workerName) {
            workerAssignCell = c.workerName;
        } else if (c.status === "Pending") {
            workerAssignCell = `
                <select onchange="assignWorker('${c.complaintId}', this.value)" style="font-size: 0.75rem; padding: 0.15rem; background: var(--bg-input); color: white; border: 1px solid var(--border-color); border-radius: 4px;">
                    <option value="">-- Assign --</option>
                    ${db.workers.map(w => `<option value="${w.uid}">${w.name}</option>`).join("")}
                </select>
            `;
        } else {
            workerAssignCell = '<span class="text-muted">N/A</span>';
        }
        
        tr.innerHTML = `
            <td style="font-family: monospace; font-size: 0.75rem;">${c.complaintId}</td>
            <td style="color:#fff; font-weight:600;">${c.title}</td>
            <td>${c.category}</td>
            <td><span class="badge ${priorityBadge}">${c.priority}</span></td>
            <td>${c.citizenName}</td>
            <td>${workerAssignCell}</td>
            <td><span class="badge ${statusBadge}">${c.status}</span></td>
            <td>
                <button class="action-btn btn-delete" onclick="deleteComplaint('${c.complaintId}')"><i class="fa-solid fa-trash"></i></button>
            </td>
        `;
        tableBody.appendChild(tr);
    });
    
    // Render Users Table
    const usersBody = els.adminUsers;
    usersBody.innerHTML = "";
    
    db.users.forEach(u => {
        const tr = document.createElement("tr");
        
        tr.innerHTML = `
            <td><div class="avatar-cell">${u.name.charAt(0)}</div></td>
            <td style="color:#fff; font-weight:600;">${u.name}</td>
            <td>${u.email}</td>
            <td><span class="badge badge-pill" style="background:rgba(255,255,255,0.05); color:#cbd5e1;">${u.role.toUpperCase()}</span></td>
            <td>${new Date(u.joinedAt).toLocaleDateString()}</td>
            <td>
                <span id="user-status-${u.uid}" style="font-weight: 600; color: ${u.disabled ? '#ef4444' : '#10b981'}">
                    ${u.disabled ? 'Disabled' : 'Active'}
                </span>
            </td>
            <td>
                <label class="switch">
                    <input type="checkbox" ${u.disabled ? 'checked' : ''} onchange="toggleUserStatus('${u.uid}', this.checked)">
                    <span class="slider"></span>
                </label>
            </td>
        `;
        usersBody.appendChild(tr);
    });
    
    // Render Duplicate Detection pane
    const dupList = els.adminDuplicateList;
    dupList.innerHTML = "";
    
    const duplicates = db.complaints.filter(c => c.isDuplicate);
    if (duplicates.length === 0) {
        dupList.innerHTML = '<div class="empty-state">No duplicate complaints flagged by system.</div>';
    } else {
        duplicates.forEach(c => {
            const div = document.createElement("div");
            div.className = "complaint-item";
            
            // Find potential original (same category, within 100m, reported prior)
            const original = db.complaints.find(o => 
                o.complaintId !== c.complaintId && 
                o.category === c.category &&
                !o.isDuplicate &&
                new Date(o.createdAt) < new Date(c.createdAt)
            );
            
            const origTitle = original ? original.title : "Unknown Original Alert";
            const origId = original ? original.complaintId : "N/A";
            
            div.innerHTML = `
                <div class="item-header">
                    <span class="item-title" style="color: #fca5a5;"><i class="fa-solid fa-clone"></i> Flagged Duplicate: "${c.title}"</span>
                    <span class="badge badge-rejected">Duplicate</span>
                </div>
                <div style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.25rem;">
                    <p>This complaint is within 100m of the existing complaint: <strong>"${origTitle}"</strong> (ID: ${origId})</p>
                    <p style="margin-top: 0.25rem; font-size: 0.75rem; color: var(--text-muted);"><i class="fa-solid fa-location-dot"></i> Address: ${c.address}</p>
                </div>
                <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem; border-top: 1px solid rgba(255,255,255,0.05); padding-top: 0.5rem;">
                    <button class="action-btn btn-reject" onclick="deleteComplaint('${c.complaintId}')"><i class="fa-solid fa-trash"></i> Dismiss Duplicate</button>
                </div>
            `;
            dupList.appendChild(div);
        });
    }
}

// ==========================================================================
// BUSINESS LOGIC & EVENT HANDLERS
// ==========================================================================

// 1. Citizen reports a complaint
els.reportForm.onsubmit = function(e) {
    e.preventDefault();
    
    const title = document.getElementById("complaint-title").value.trim();
    const description = document.getElementById("complaint-desc").value.trim();
    const category = document.getElementById("complaint-category").value;
    const priority = document.getElementById("complaint-priority").value;
    const latitude = parseFloat(document.getElementById("complaint-lat").value);
    const longitude = parseFloat(document.getElementById("complaint-lng").value);
    const address = document.getElementById("complaint-address").value.trim();
    const imageUrl = document.getElementById("complaint-image").value.trim();
    
    // Duplicate Detection Logic (within 100m, same category, last 24 hours)
    // For local simulation, we check if coordinates are within ~0.001 degrees (~110 meters)
    const matchesCategory = db.complaints.filter(c => c.category === category);
    const isDup = matchesCategory.some(c => {
        const latDiff = Math.abs(c.latitude - latitude);
        const lngDiff = Math.abs(c.longitude - longitude);
        const timeDiff = Math.abs(new Date() - new Date(c.createdAt)) / 3600000; // hours
        return latDiff < 0.001 && lngDiff < 0.001 && timeDiff < 24;
    });

    const newComplaint = {
        complaintId: "comp_" + Math.random().toString(36).substr(2, 9),
        title,
        description,
        category,
        imageUrl: imageUrl || "https://picsum.photos/400/300",
        latitude,
        longitude,
        address,
        status: "Pending",
        citizenId: currentUserId,
        citizenName: els.headerUsername.textContent,
        workerId: null,
        workerName: null,
        proofImageUrl: null,
        createdAt: new Date().toISOString(),
        acceptedAt: null,
        resolvedAt: null,
        citizenRating: null,
        citizenFeedback: null,
        priority,
        isDuplicate: isDup,
        verified: false
    };
    
    db.complaints.push(newComplaint);
    
    // Create notification if it's a duplicate
    if (isDup) {
        db.notifications.push({
            notifId: "notif_" + Math.random().toString(36).substr(2, 9),
            recipientId: "admin_uid",
            title: "Duplicate Flagged",
            body: `Complaint "${title}" flagged as duplicate of another active alert.`,
            type: "complaint_duplicate",
            complaintId: newComplaint.complaintId,
            isRead: false,
            createdAt: new Date().toISOString()
        });
    }

    db.save();
    els.reportForm.reset();
    document.getElementById("complaint-lat").value = "12.971598";
    document.getElementById("complaint-lng").value = "77.594562";
    
    alert("Complaint reported successfully!");
    renderAll();
};

// 2. Worker accepts a task
function acceptTask(complaintId) {
    const complaint = db.complaints.find(c => c.complaintId === complaintId);
    const worker = db.workers.find(w => w.uid === currentUserId);
    
    if (!complaint || !worker) return;
    
    complaint.status = "In Progress";
    complaint.workerId = currentUserId;
    complaint.workerName = worker.name;
    complaint.acceptedAt = new Date().toISOString();
    
    // Increment activeTasks
    worker.activeTasks = (worker.activeTasks || 0) + 1;
    
    // Notification to citizen
    db.notifications.push({
        notifId: "notif_" + Math.random().toString(36).substr(2, 9),
        recipientId: complaint.citizenId,
        title: "Complaint In Progress",
        body: `Your complaint "${complaint.title}" has been accepted by ${worker.name}.`,
        type: "complaint_accepted",
        complaintId,
        isRead: false,
        createdAt: new Date().toISOString()
    });

    db.save();
    alert(`Task "${complaint.title}" accepted successfully!`);
    renderAll();
}

// 3. Worker submits resolution proof
function openSubmitProofModal(complaintId, title) {
    document.getElementById("modal-complaint-id").value = complaintId;
    document.getElementById("proof-complaint-title").value = title;
    els.proofModal.classList.remove("hidden");
}

els.btnCloseModal.onclick = () => els.proofModal.classList.add("hidden");

els.submitProofForm.onsubmit = function(e) {
    e.preventDefault();
    
    const complaintId = document.getElementById("modal-complaint-id").value;
    const proofUrl = document.getElementById("proof-image-url").value;
    const notes = document.getElementById("proof-notes").value;
    
    const complaint = db.complaints.find(c => c.complaintId === complaintId);
    if (!complaint) return;
    
    complaint.status = "Verification Pending";
    complaint.proofImageUrl = proofUrl;
    complaint.workerNotes = notes;
    complaint.resolvedAt = new Date().toISOString();
    
    // Decrement worker activeTasks
    const worker = db.workers.find(w => w.uid === complaint.workerId);
    if (worker) {
        worker.activeTasks = Math.max(0, (worker.activeTasks || 0) - 1);
    }
    
    // Notify Admin
    db.notifications.push({
        notifId: "notif_" + Math.random().toString(36).substr(2, 9),
        recipientId: "admin_uid",
        title: "Proof Submitted",
        body: `Worker ${complaint.workerName} submitted proof for "${complaint.title}".`,
        type: "complaint_verification_pending",
        complaintId,
        isRead: false,
        createdAt: new Date().toISOString()
    });

    db.save();
    els.proofModal.classList.add("hidden");
    els.submitProofForm.reset();
    alert("Resolution proof submitted successfully. Awaiting Admin verification!");
    renderAll();
};

// 4. Admin verifies resolution
function verifyResolution(complaintId, approve) {
    const complaint = db.complaints.find(c => c.complaintId === complaintId);
    if (!complaint) return;
    
    const worker = db.workers.find(w => w.uid === complaint.workerId);
    
    if (approve) {
        complaint.status = "Resolved";
        complaint.verified = true;
        
        if (worker) {
            // Base Points
            let earnedPoints = 10;
            worker.issuesSolved += 1;
            
            // Calculate time taken for resolution
            let hasFastBonus = false;
            if (complaint.acceptedAt && complaint.resolvedAt) {
                const accepted = new Date(complaint.acceptedAt);
                const resolved = new Date(complaint.resolvedAt);
                const diffMinutes = (resolved - accepted) / 60000;
                
                if (diffMinutes > 0) {
                    if (diffMinutes < 120.0) { // Resolve in < 2 hours
                        earnedPoints += 5;
                        hasFastBonus = true;
                    }
                    
                    // Update average resolution time
                    if (worker.averageResolutionTimeMinutes === 0 || worker.issuesSolved === 1) {
                        worker.averageResolutionTimeMinutes = diffMinutes;
                    } else {
                        worker.averageResolutionTimeMinutes = (worker.averageResolutionTimeMinutes * (worker.issuesSolved - 1) + diffMinutes) / worker.issuesSolved;
                    }
                }
            }
            
            worker.totalPoints += earnedPoints;
            
            // Worker Notification
            const pointsText = hasFastBonus ? "+15 points (includes fast resolution bonus)" : "+10 points";
            db.notifications.push({
                notifId: "notif_" + Math.random().toString(36).substr(2, 9),
                recipientId: complaint.workerId,
                title: "Points Earned!",
                body: `You earned ${pointsText} for resolving "${complaint.title}"!`,
                type: "points_earned",
                complaintId,
                isRead: false,
                createdAt: new Date().toISOString()
            });
        }
        
        // Notify Citizen
        db.notifications.push({
            notifId: "notif_" + Math.random().toString(36).substr(2, 9),
            recipientId: complaint.citizenId,
            title: "Issue Resolved!",
            body: `Your complaint "${complaint.title}" has been verified and marked as resolved by admin. Please rate their service!`,
            type: "complaint_resolved",
            complaintId,
            isRead: false,
            createdAt: new Date().toISOString()
        });
        
        alert("Complaint resolution approved and resolved!");
    } else {
        // Rejected
        complaint.status = "Rejected";
        complaint.verified = false;
        
        if (worker) {
            worker.totalPoints = Math.max(0, worker.totalPoints - 10);
            
            // Notify Worker
            db.notifications.push({
                notifId: "notif_" + Math.random().toString(36).substr(2, 9),
                recipientId: complaint.workerId,
                title: "Task Rejected",
                body: `Your proof for "${complaint.title}" was rejected by Admin. -10 points penalty.`,
                type: "complaint_rejected",
                complaintId,
                isRead: false,
                createdAt: new Date().toISOString()
            });
        }
        alert("Resolution proof rejected. Worker penalized -10 points.");
    }
    
    db.save();
    renderAll();
}

// 5. Citizen submits rating
function openRatingModal(complaintId, title) {
    document.getElementById("modal-rating-complaint-id").value = complaintId;
    document.getElementById("rating-complaint-title").value = title;
    els.ratingModal.classList.remove("hidden");
}

els.btnCloseRatingModal.onclick = () => els.ratingModal.classList.add("hidden");

els.submitRatingForm.onsubmit = function(e) {
    e.preventDefault();
    
    const complaintId = document.getElementById("modal-rating-complaint-id").value;
    const ratingEl = document.querySelector('input[name="stars"]:checked');
    const feedback = document.getElementById("rating-feedback").value;
    
    if (!ratingEl) {
        alert("Please select a rating!");
        return;
    }
    
    const rating = parseInt(ratingEl.value);
    
    const complaint = db.complaints.find(c => c.complaintId === complaintId);
    if (!complaint) return;
    
    complaint.citizenRating = rating;
    complaint.citizenFeedback = feedback;
    
    // Recalculate average worker rating
    const worker = db.workers.find(w => w.uid === complaint.workerId);
    if (worker) {
        // Query all complaints resolved by this worker that have citizenRating
        const ratedComplaints = db.complaints.filter(c => c.workerId === worker.uid && c.citizenRating !== null);
        const totalRating = ratedComplaints.reduce((sum, c) => sum + c.citizenRating, 0);
        
        worker.averageRating = ratedComplaints.length > 0 ? totalRating / ratedComplaints.length : rating;
        
        // Bonus points for 4-5 stars
        if (rating >= 4) {
            worker.totalPoints += 5;
            
            // Notify worker of bonus
            db.notifications.push({
                notifId: "notif_" + Math.random().toString(36).substr(2, 9),
                recipientId: worker.uid,
                title: "Rating Bonus!",
                body: `Citizen rated you ${rating} stars for "${complaint.title}"! You earned +5 points.`,
                type: "points_earned",
                complaintId,
                isRead: false,
                createdAt: new Date().toISOString()
            });
        }
    }
    
    db.save();
    els.ratingModal.classList.add("hidden");
    els.submitRatingForm.reset();
    alert("Thank you for your rating!");
    renderAll();
};

// Admin utilities
function assignWorker(complaintId, workerId) {
    const complaint = db.complaints.find(c => c.complaintId === complaintId);
    const worker = db.workers.find(w => w.uid === workerId);
    
    if (!complaint || !worker) return;
    
    complaint.status = "In Progress";
    complaint.workerId = workerId;
    complaint.workerName = worker.name;
    complaint.acceptedAt = new Date().toISOString();
    
    worker.activeTasks = (worker.activeTasks || 0) + 1;
    
    // Notify worker
    db.notifications.push({
        notifId: "notif_" + Math.random().toString(36).substr(2, 9),
        recipientId: workerId,
        title: "New Task Assigned",
        body: `Admin assigned you the task: "${complaint.title}"`,
        type: "task_assigned",
        complaintId,
        isRead: false,
        createdAt: new Date().toISOString()
    });

    db.save();
    alert(`Complaint assigned to ${worker.name}.`);
    renderAll();
}

function deleteComplaint(complaintId) {
    if (!confirm("Are you sure you want to delete this complaint?")) return;
    
    const index = db.complaints.findIndex(c => c.complaintId === complaintId);
    if (index === -1) return;
    
    const complaint = db.complaints[index];
    
    // Decrement active tasks if it was in progress
    if (complaint.status === "In Progress" && complaint.workerId) {
        const worker = db.workers.find(w => w.uid === complaint.workerId);
        if (worker) {
            worker.activeTasks = Math.max(0, (worker.activeTasks || 0) - 1);
        }
    }
    
    db.complaints.splice(index, 1);
    db.save();
    alert("Complaint deleted.");
    renderAll();
}

function toggleUserStatus(uid, disabled) {
    const user = db.users.find(u => u.uid === uid);
    if (!user) return;
    
    user.disabled = disabled;
    db.save();
    
    const statusText = document.getElementById(`user-status-${uid}`);
    if (statusText) {
        statusText.textContent = disabled ? 'Disabled' : 'Active';
        statusText.style.color = disabled ? '#ef4444' : '#10b981';
    }
    alert(`User ${user.name} has been ${disabled ? 'Disabled' : 'Enabled'}.`);
    renderAll();
}

// Admin Tab Controller
document.querySelectorAll(".tab-btn").forEach(btn => {
    btn.onclick = function() {
        document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
        document.querySelectorAll(".tab-pane").forEach(p => p.classList.remove("active"));
        
        btn.classList.add("active");
        const tabId = btn.getAttribute("data-tab");
        document.getElementById(tabId).classList.add("active");
    };
});

// Role Switcher hook
els.roleSelector.onchange = function(e) {
    switchRole(e.target.value);
};

// Initialize App on load
window.onload = function() {
    switchRole("worker"); // Starts as worker
};
