import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

async function getMessManagerTokenAndId(messId: string): Promise<{ uid: string; tokens: string[] } | null> {
  const membersSnap = await db.collection(`messes/${messId}/members`).where("role", "==", "manager").limit(1).get();
  
  if (membersSnap.empty) return null;
  
  const managerUid = membersSnap.docs[0].id;
  const userDoc = await db.collection("users").doc(managerUid).get();
  
  if (!userDoc.exists) return null;
  
  const tokens = userDoc.data()?.fcmTokens || [];
  return { uid: managerUid, tokens };
}

async function getUserTokens(uid: string): Promise<string[]> {
  const userDoc = await db.collection("users").doc(uid).get();
  if (!userDoc.exists) return [];
  return userDoc.data()?.fcmTokens || [];
}

async function sendNotification(tokens: string[], title: string, body: string, data: any) {
  if (!tokens || tokens.length === 0) return;
  
  const message = {
    notification: { title, body },
    data,
    tokens
  };
  
  try {
    const response = await admin.messaging().sendEachForMulticast(message);
    if (response.failureCount > 0) {
      console.error(`Failed to send ${response.failureCount} notifications`);
    }
  } catch (error) {
    console.error("Error sending notification:", error);
  }
}

async function getMessName(messId: string): Promise<string> {
  const doc = await db.collection("messes").doc(messId).get();
  return doc.data()?.messName || "the mess";
}

export const sendJoinRequestNotification = functions.firestore.document("messes/{messId}/joinRequests/{uid}").onCreate(async (snap, context) => {
  const { messId } = context.params;
  const data = snap.data();
  const messName = await getMessName(messId);
  const managerInfo = await getMessManagerTokenAndId(messId);
  
  if (managerInfo && managerInfo.tokens.length > 0) {
    await sendNotification(
      managerInfo.tokens,
      "Join Request",
      `${data.name || "Someone"} wants to join ${messName}`,
      { messId, type: "join_request" }
    );
  }
});

export const handleJoinRequestStatusChange = functions.firestore.document("messes/{messId}/joinRequests/{uid}").onUpdate(async (change, context) => {
  const { messId, uid } = context.params;
  const before = change.before.data();
  const after = change.after.data();
  
  if (before.status !== after.status) {
    const messName = await getMessName(messId);
    const tokens = await getUserTokens(uid);
    
    if (tokens.length > 0) {
      if (after.status === "approved") {
        await sendNotification(
          tokens,
          "Join Request Approved",
          `You've joined ${messName}`,
          { messId, type: "join_approved" }
        );
      } else if (after.status === "rejected") {
        await sendNotification(
          tokens,
          "Join Request Declined",
          `Your request to join ${messName} was declined`,
          { messId, type: "join_rejected" }
        );
      }
    }
  }
});

export const sendLeaveRequestNotification = functions.firestore.document("messes/{messId}/leaveRequests/{uid}").onCreate(async (snap, context) => {
  const { messId } = context.params;
  const data = snap.data();
  const messName = await getMessName(messId);
  const managerInfo = await getMessManagerTokenAndId(messId);
  
  if (managerInfo && managerInfo.tokens.length > 0) {
    await sendNotification(
      managerInfo.tokens,
      "Leave Request",
      `${data.name || "Someone"} wants to leave ${messName}`,
      { messId, type: "leave_request" }
    );
  }
});

export const handleLeaveRequestStatusChange = functions.firestore.document("messes/{messId}/leaveRequests/{uid}").onUpdate(async (change, context) => {
  const { messId, uid } = context.params;
  const before = change.before.data();
  const after = change.after.data();
  
  if (before.status !== after.status) {
    const messName = await getMessName(messId);
    const tokens = await getUserTokens(uid);
    
    if (tokens.length > 0) {
      if (after.status === "approved") {
        await sendNotification(
          tokens,
          "Leave Request Approved",
          `You've left ${messName}`,
          { messId, type: "leave_approved" }
        );
      } else if (after.status === "rejected") {
        await sendNotification(
          tokens,
          "Leave Request Declined",
          `Your leave request for ${messName} was declined`,
          { messId, type: "leave_rejected" }
        );
      }
    }
  }
});

export const handleManagerTransfer = functions.firestore.document("messes/{messId}/members/{memberId}").onUpdate(async (change, context) => {
  const { messId, memberId } = context.params;
  const before = change.before.data();
  const after = change.after.data();
  
  if (before.role !== "manager" && after.role === "manager") {
    const messName = await getMessName(messId);
    const tokens = await getUserTokens(memberId);
    
    if (tokens.length > 0) {
      await sendNotification(
        tokens,
        "Role Update",
        `You're now the manager of ${messName}`,
        { messId, type: "manager_assigned" }
      );
    }
  }
});
