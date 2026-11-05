const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- USER PROFILE SECURITY TESTS ---

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Bob cannot read Alice's private user profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "+1234567890",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Alice can read and update her own user profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      phone: "+1234567890",
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    name: "Alice Updated",
  }));
});

// --- EMERGENCY ALERT SECURITY TESTS ---

test("Unauthenticated user cannot create emergency alert", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("emergency_alerts").doc("SOS-100").set({
    id: "SOS-100",
    userId: ALICE_UID,
    status: "active",
    crisisMode: "KIDNAP_SILENT",
  }));
});

test("Alice can create her own emergency alert", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("emergency_alerts").doc("SOS-100").set({
    id: "SOS-100",
    userId: ALICE_UID,
    status: "active",
    crisisMode: "KIDNAP_SILENT",
    battery: 85,
    latitude: 6.4281,
    longitude: 3.4219,
    address: "Victoria Island",
  }));
});

test("Bob cannot spoof an emergency alert under Alice's ID", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("emergency_alerts").doc("SOS-100").set({
    id: "SOS-100",
    userId: ALICE_UID,
    status: "active",
    crisisMode: "KIDNAP_SILENT",
  }));
});

test("Bob can view Alice's active alert as a community safe circle helper", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("emergency_alerts").doc("SOS-100").set({
      id: "SOS-100",
      userId: ALICE_UID,
      status: "active",
      crisisMode: "ACCIDENT",
      latitude: 6.4281,
      longitude: 3.4219,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("emergency_alerts").doc("SOS-100").get());
});

test("Bob cannot update or resolve Alice's emergency alert", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("emergency_alerts").doc("SOS-100").set({
      id: "SOS-100",
      userId: ALICE_UID,
      status: "active",
      crisisMode: "ACCIDENT",
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("emergency_alerts").doc("SOS-100").update({
    status: "resolved",
  }));
});
