import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();

// Generate an 8+ char ID using allowed charset
function generateNewId(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789#*-_";
  let result = "";
  for (let i = 0; i < 9; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

// Copy a collection
async function copyCollection(sourceRef: FirebaseFirestore.CollectionReference, destRef: FirebaseFirestore.CollectionReference) {
  const docs = await sourceRef.get();
  for (const doc of docs.docs) {
    await destRef.doc(doc.id).set(doc.data());
  }
}

export const runMessIdMigration = functions.https.onCall(async (data, context) => {
  // Ensure only authenticated users, or maybe just specific admins
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in to run migration");
  }

  const dryRun = data.dryRun === true;
  const results: string[] = [];
  
  try {
    const messesSnapshot = await db.collection("messes").get();
    
    for (const messDoc of messesSnapshot.docs) {
      const oldId = messDoc.id;
      const messData = messDoc.data();
      
      // Skip already migrated (tombstones)
      if (messData.redirectTo) {
        results.push(`Skipping ${oldId} - already migrated to ${messData.redirectTo}`);
        continue;
      }
      
      const newId = generateNewId();
      
      if (dryRun) {
        results.push(`[DRY RUN] Would migrate mess ${oldId} -> ${newId}`);
      } else {
        await db.runTransaction(async (transaction) => {
          // Verify mess still exists and isn't migrated
          const latestDoc = await transaction.get(messDoc.ref);
          if (!latestDoc.exists || latestDoc.data()?.redirectTo) {
            return;
          }
          
          const newMessRef = db.collection("messes").doc(newId);
          transaction.set(newMessRef, messData);
        });
        
        // Copy subcollections (outside transaction as it may exceed limits)
        const subcollections = ["members", "meals", "guestMeals", "contributions", "expenses", "joinRequests", "leaveRequests", "activityLog"];
        for (const sub of subcollections) {
          await copyCollection(db.collection(`messes/${oldId}/${sub}`), db.collection(`messes/${newId}/${sub}`));
        }
        
        // Update user memberships
        const membersSnapshot = await db.collection(`messes/${oldId}/members`).get();
        for (const memberDoc of membersSnapshot.docs) {
          const uid = memberDoc.id;
          const userRef = db.collection("users").doc(uid);
          
          await db.runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);
            if (userDoc.exists) {
              const userData = userDoc.data()!;
              const memberships: string[] = userData.messMemberships || [];
              if (memberships.includes(oldId)) {
                const newMemberships = memberships.map(id => id === oldId ? newId : id);
                transaction.update(userRef, { messMemberships: newMemberships });
              }
            }
          });
        }
        
        // Create tombstone
        await db.collection("messes").doc(oldId).set({
          redirectTo: newId,
          migratedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        results.push(`Successfully migrated ${oldId} to ${newId}`);
      }
    }
    
    return { success: true, results, message: "Migration completed" };
  } catch (error: any) {
    console.error("Migration error:", error);
    throw new functions.https.HttpsError("internal", error.message || "An error occurred during migration");
  }
});

// For running as a standalone script (e.g. npx ts-node migration.ts)
if (require.main === module) {
  (async () => {
    // Only run this if we are running the file directly via node/ts-node
    try {
      if (!admin.apps.length) {
        // You can set process.env.GOOGLE_APPLICATION_CREDENTIALS before running
        admin.initializeApp();
      }
      
      console.log("Starting migration (DRY RUN)...");
      const result = await runMessIdMigration({ dryRun: true }, { auth: { uid: 'admin' } } as any);
      console.log(result);
      
      console.log("To run actual migration, modify the script call.");
      process.exit(0);
    } catch (e) {
      console.error(e);
      process.exit(1);
    }
  })();
}
