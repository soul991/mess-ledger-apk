import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

function getActorUid(context: functions.EventContext): string {
  return context.auth?.uid || "system";
}

async function getActorName(uid: string, messId: string): Promise<string> {
  if (uid === "system") return "System";
  
  // Try to get from mess members first
  const memberDoc = await db.collection(`messes/${messId}/members`).doc(uid).get();
  if (memberDoc.exists && memberDoc.data()?.name) {
    return memberDoc.data()!.name;
  }
  
  // Fallback to users collection
  const userDoc = await db.collection("users").doc(uid).get();
  if (userDoc.exists && userDoc.data()?.name) {
    return userDoc.data()!.name;
  }
  
  return "Unknown User";
}

async function logActivity(messId: string, entry: any) {
  const finalEntry = {
    ...entry,
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  };
  await db.collection(`messes/${messId}/activityLog`).add(finalEntry);
}

export const onMemberWrite = functions.firestore.document("messes/{messId}/members/{memberId}").onWrite(async (change, context) => {
  const { messId, memberId } = context.params;
  const before = change.before.data();
  const after = change.after.data();
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (!change.before.exists && change.after.exists) {
    // Created
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "member_joined",
      summary: `${after?.name || "Someone"} joined the mess`,
      targetId: memberId
    });
  } else if (change.before.exists && change.after.exists) {
    // Updated
    if (!before?.deletedAt && after?.deletedAt) {
      const summary = actorUid === memberId 
        ? `${before?.name || "Someone"} left the mess`
        : `${before?.name || "Someone"} was removed from the mess`;
      
      await logActivity(messId, {
        actorUid,
        actorName,
        action: actorUid === memberId ? "member_left" : "member_removed",
        summary,
        targetId: memberId
      });
    } else if (before?.role !== after?.role && after?.role === "manager") {
      await logActivity(messId, {
        actorUid,
        actorName,
        action: "manager_assigned",
        summary: `${after?.name} was made manager by ${actorName}`,
        targetId: memberId
      });
    }
  }
});

export const onExpenseWrite = functions.firestore.document("messes/{messId}/expenses/{expenseId}").onWrite(async (change, context) => {
  const { messId, expenseId } = context.params;
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (!change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "expense_added",
      summary: `${actorName} added ₹${data.amount} expense - ${data.category}`,
      targetId: expenseId,
      amount: data.amount
    });
  } else if (change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "expense_edited",
      summary: `${actorName} edited an expense`,
      targetId: expenseId,
      amount: data.amount
    });
  } else if (change.before.exists && !change.after.exists) {
    const data = change.before.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "expense_deleted",
      summary: `${actorName} deleted ₹${data.amount} expense - ${data.category}`,
      targetId: expenseId,
      amount: data.amount
    });
  }
});

export const onContributionWrite = functions.firestore.document("messes/{messId}/contributions/{contribId}").onWrite(async (change, context) => {
  const { messId, contribId } = context.params;
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (!change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "contribution_added",
      summary: `${actorName} added ₹${data.amount} contribution`,
      targetId: contribId,
      amount: data.amount
    });
  } else if (change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "contribution_edited",
      summary: `${actorName} edited a contribution`,
      targetId: contribId,
      amount: data.amount
    });
  } else if (change.before.exists && !change.after.exists) {
    const data = change.before.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "contribution_deleted",
      summary: `${actorName} deleted ₹${data.amount} contribution`,
      targetId: contribId,
      amount: data.amount
    });
  }
});

export const onGuestMealWrite = functions.firestore.document("messes/{messId}/guestMeals/{guestMealId}").onWrite(async (change, context) => {
  const { messId, guestMealId } = context.params;
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (!change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    const hostName = await getActorName(data.hostId, messId);
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "guest_meal_added",
      summary: `${hostName} added ${data.count} guest(s) for ${data.meal}`,
      targetId: guestMealId
    });
  } else if (change.before.exists && change.after.exists) {
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "guest_meal_edited",
      summary: `${actorName} edited a guest meal`,
      targetId: guestMealId
    });
  } else if (change.before.exists && !change.after.exists) {
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "guest_meal_deleted",
      summary: `${actorName} deleted a guest meal`,
      targetId: guestMealId
    });
  }
});

export const onMealWrite = functions.firestore.document("messes/{messId}/meals/{dateStr}").onWrite(async (change, context) => {
  const { messId, dateStr } = context.params;
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (change.before.exists && change.after.exists) {
    const before = change.before.data()!;
    const after = change.after.data()!;
    
    if (JSON.stringify(before) !== JSON.stringify(after)) {
      await logActivity(messId, {
        actorUid,
        actorName,
        action: "meal_toggled",
        summary: `${actorName} updated meals for ${dateStr}`,
        targetId: dateStr
      });
    }
  }
});

export const onJoinRequestWrite = functions.firestore.document("messes/{messId}/joinRequests/{uid}").onWrite(async (change, context) => {
  const { messId, uid } = context.params;
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (!change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "join_request_submitted",
      summary: `${data.name || "Someone"} requested to join`,
      targetId: uid
    });
  } else if (change.before.exists && change.after.exists) {
    const before = change.before.data()!;
    const after = change.after.data()!;
    
    if (before.status !== after.status) {
      if (after.status === "approved") {
        await logActivity(messId, {
          actorUid,
          actorName,
          action: "join_request_approved",
          summary: `${after.name || "Someone"}'s join request was approved`,
          targetId: uid
        });
      } else if (after.status === "rejected") {
        await logActivity(messId, {
          actorUid,
          actorName,
          action: "join_request_rejected",
          summary: `${after.name || "Someone"}'s join request was rejected`,
          targetId: uid
        });
      }
    }
  }
});

export const onLeaveRequestWrite = functions.firestore.document("messes/{messId}/leaveRequests/{uid}").onWrite(async (change, context) => {
  const { messId, uid } = context.params;
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  if (!change.before.exists && change.after.exists) {
    const data = change.after.data()!;
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "leave_request_submitted",
      summary: `${data.name || "Someone"} requested to leave`,
      targetId: uid
    });
  } else if (change.before.exists && change.after.exists) {
    const before = change.before.data()!;
    const after = change.after.data()!;
    
    if (before.status !== after.status) {
      if (after.status === "approved") {
        await logActivity(messId, {
          actorUid,
          actorName,
          action: "leave_request_approved",
          summary: `${after.name || "Someone"}'s leave request was approved`,
          targetId: uid
        });
      } else if (after.status === "rejected") {
        await logActivity(messId, {
          actorUid,
          actorName,
          action: "leave_request_rejected",
          summary: `${after.name || "Someone"}'s leave request was rejected`,
          targetId: uid
        });
      }
    }
  }
});

export const onMessUpdate = functions.firestore.document("messes/{messId}").onUpdate(async (change, context) => {
  const { messId } = context.params;
  const before = change.before.data();
  const after = change.after.data();
  const actorUid = getActorUid(context);
  const actorName = await getActorName(actorUid, messId);

  const messNameChanged = before.messName !== after.messName;
  const categoriesChanged = JSON.stringify(before.categories) !== JSON.stringify(after.categories);

  if (messNameChanged || categoriesChanged) {
    await logActivity(messId, {
      actorUid,
      actorName,
      action: "mess_settings_changed",
      summary: `${actorName} changed mess settings`,
      targetId: messId
    });
  }
});
