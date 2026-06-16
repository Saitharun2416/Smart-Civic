const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

// Helper to send FCM notifications and save to notifications collection
async function sendNotification(recipientId, title, body, type, complaintId) {
    try {
        // Create notification doc
        const notifRef = db.collection('notifications').doc();
        await notifRef.set({
            recipientId,
            title,
            body,
            type,
            complaintId,
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        // Fetch user FCM token
        const userDoc = await db.collection('users').doc(recipientId).get();
        if (userDoc.exists) {
            const userData = userDoc.data();
            const fcmToken = userData.fcmToken;
            if (fcmToken) {
                const message = {
                    notification: { title, body },
                    data: { type, complaintId },
                    token: fcmToken
                };
                await admin.messaging().send(message);
                console.log(`Notification sent to user ${recipientId}`);
            }
        }
    } catch (error) {
        console.error("Error sending notification:", error);
    }
}

// Helper to recalculate ranks for all workers
async function recalculateRanks() {
    try {
        const workersSnapshot = await db.collection('workers').orderBy('totalPoints', 'desc').get();
        const batch = db.batch();
        workersSnapshot.docs.forEach((doc, index) => {
            batch.update(doc.ref, { rank: index + 1 });
        });
        await batch.commit();
        console.log("Worker ranks successfully recalculated.");
    } catch (error) {
        console.error("Error recalculating ranks:", error);
    }
}

// Firestore trigger for complaint updates
exports.onComplaintUpdated = functions.firestore
    .document('complaints/{complaintId}')
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const complaintId = context.params.complaintId;

        // 1. Status transition: Pending -> In Progress (Task accepted by worker)
        if (before.status === 'Pending' && after.status === 'In Progress') {
            if (after.workerId) {
                // Notify Citizen
                await sendNotification(
                    after.citizenId,
                    "Complaint In Progress",
                    `Your complaint "${after.title}" has been accepted by ${after.workerName}.`,
                    "complaint_accepted",
                    complaintId
                );

                // Notify Worker (FCM only or doc as well)
                await sendNotification(
                    after.workerId,
                    "Task Assigned",
                    `You have accepted the task "${after.title}".`,
                    "task_assigned",
                    complaintId
                );

                // Increment worker's active tasks
                const workerRef = db.collection('workers').doc(after.workerId);
                await db.runTransaction(async (transaction) => {
                    const workerDoc = await transaction.get(workerRef);
                    if (workerDoc.exists) {
                        const currentActive = workerDoc.data().activeTasks || 0;
                        transaction.update(workerRef, { activeTasks: currentActive + 1 });
                    }
                });
            }
        }

        // 2. Admin explicitly assigns worker to a Pending complaint
        if (before.workerId !== after.workerId && after.workerId && before.status === 'Pending') {
            await sendNotification(
                after.workerId,
                "New Task Assigned",
                `Admin has assigned you the task: "${after.title}"`,
                "task_assigned",
                complaintId
            );
        }

        // 3a. Status transition: In Progress -> Verification Pending (Worker submits proof)
        if (before.status === 'In Progress' && after.status === 'Verification Pending') {
            if (after.workerId) {
                // Notify Citizen that work is finished and pending verification
                await sendNotification(
                    after.citizenId,
                    "Work Finished",
                    `The issue "${after.title}" has been finished by ${after.workerName} and is pending admin verification.`,
                    "complaint_verification_pending",
                    complaintId
                );

                // Decrement worker's active tasks
                const workerRef = db.collection('workers').doc(after.workerId);
                await db.runTransaction(async (transaction) => {
                    const workerDoc = await transaction.get(workerRef);
                    if (workerDoc.exists) {
                        const currentActive = workerDoc.data().activeTasks || 0;
                        transaction.update(workerRef, { activeTasks: Math.max(0, currentActive - 1) });
                    }
                });
            }
        }

        // 3b. Status transition: Verification Pending -> Resolved (Admin approves completion)
        if (before.status === 'Verification Pending' && after.status === 'Resolved') {
            if (after.workerId) {
                // Notify Citizen
                await sendNotification(
                    after.citizenId,
                    "Issue Resolved!",
                    `The issue "${after.title}" has been marked as resolved by ${after.workerName}. Please rate their service!`,
                    "complaint_resolved",
                    complaintId
                );

                const workerRef = db.collection('workers').doc(after.workerId);
                await db.runTransaction(async (transaction) => {
                    const workerDoc = await transaction.get(workerRef);
                    if (workerDoc.exists) {
                        const data = workerDoc.data();
                        let points = data.totalPoints || 0;
                        let solved = data.issuesSolved || 0;
                        let active = data.activeTasks || 0;

                        // Base points
                        points += 10;
                        solved += 1;

                        // Time taken calculations
                        let newAvgTime = data.averageResolutionTimeMinutes || 0.0;
                        if (after.acceptedAt && after.resolvedAt) {
                            const acceptedMillis = after.acceptedAt.toMillis();
                            const resolvedMillis = after.resolvedAt.toMillis();
                            const diffMinutes = (resolvedMillis - acceptedMillis) / 60000;

                            if (diffMinutes > 0) {
                                // Fast bonus (+5 points if resolved in < 120 minutes)
                                if (diffMinutes < 120) {
                                    points += 5;
                                    console.log("Fast resolution bonus awarded!");
                                }

                                // Update average resolution time (protect division by zero)
                                if (newAvgTime === 0 || solved === 1) {
                                    newAvgTime = diffMinutes;
                                } else {
                                    newAvgTime = (newAvgTime * (solved - 1) + diffMinutes) / solved;
                                }
                            }
                        }

                        transaction.update(workerRef, {
                            totalPoints: points,
                            issuesSolved: solved,
                            averageResolutionTimeMinutes: newAvgTime
                        });
                    }
                });

                // Notify Worker of points earned
                await sendNotification(
                    after.workerId,
                    "Points Earned",
                    "You earned +10 points for resolving the task!",
                    "points_earned",
                    complaintId
                );

                // Recalculate ranks after transaction
                await recalculateRanks();
            }
        }

        // 4. Status transition: Verification Pending -> Rejected (Admin rejects completion)
        if (before.status === 'Verification Pending' && after.status === 'Rejected') {
            if (after.workerId) {
                // Notify Worker of rejection
                await sendNotification(
                    after.workerId,
                    "Task Rejected",
                    `Your completion for "${after.title}" was rejected. 10 points deducted.`,
                    "complaint_rejected",
                    complaintId
                );

                const workerRef = db.collection('workers').doc(after.workerId);
                await db.runTransaction(async (transaction) => {
                    const workerDoc = await transaction.get(workerRef);
                    if (workerDoc.exists) {
                        const data = workerDoc.data();
                        const currentPoints = data.totalPoints || 0;
                        transaction.update(workerRef, {
                            totalPoints: Math.max(0, currentPoints - 10)
                        });
                    }
                });

                await recalculateRanks();
            }
        }

        // 5. Citizen submits rating (Rating changes from null to a value)
        if (before.citizenRating === null && after.citizenRating !== null && after.citizenRating !== undefined) {
            if (after.workerId) {
                const workerRef = db.collection('workers').doc(after.workerId);
                
                await db.runTransaction(async (transaction) => {
                    const workerDoc = await transaction.get(workerRef);
                    if (workerDoc.exists) {
                        const data = workerDoc.data();
                        let points = data.totalPoints || 0;
                        let currentAvgRating = data.averageRating || 0.0;
                        let solved = data.issuesSolved || 1;

                        // Recalculate average rating with division check
                        const divider = Math.max(1, solved);
                        if (currentAvgRating === 0) {
                            currentAvgRating = after.citizenRating;
                        } else {
                            currentAvgRating = (currentAvgRating * (divider - 1) + after.citizenRating) / divider;
                        }

                        // High rating bonus
                        if (after.citizenRating >= 4) {
                            points += 5;
                            console.log("High rating bonus (+5 points) awarded!");
                            
                            transaction.update(workerRef, {
                                totalPoints: points,
                                averageRating: currentAvgRating
                            });
                        } else {
                            transaction.update(workerRef, {
                                averageRating: currentAvgRating
                            });
                        }
                    }
                });

                if (after.citizenRating >= 4) {
                    await sendNotification(
                        after.workerId,
                        "Rating Bonus!",
                        `Citizen rated you ${after.citizenRating} stars! You earned +5 points.`,
                        "points_earned",
                        complaintId
                    );
                }

                await recalculateRanks();
            }
        }

        return null;
    });
