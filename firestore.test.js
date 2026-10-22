const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 1000);
}

function validMemberPayload(uid, ts = nowTimestamp()) {
  return {
    userId: uid,
    name: "Sarah Miller",
    role: "SPOUSE",
    avatarUrl: "https://example.com/sarah.jpg",
    online: true,
    liveSharing: true,
    pinned: true,
    favorite: true,
    batteryLevel: 84,
    speedMph: 24,
    locationLabel: "Moving along Grand Ave",
    lastMessageSnippet: "Live: 84% battery • 24 mph",
    lastActivityTimeLabel: "10:42 AM",
    unreadCount: 0,
    screenTimeMinutesToday: 95,
    screenTimeLimitMinutes: 240,
    latitude: 41.8917,
    longitude: -87.6243,
    accuracyMeters: 5,
    notifyOnSchoolDeparture: true,
    ghostMode: false,
    appRestrictionsEnabled: false,
    websiteSafetyEnabled: true,
    bedtimeSchedule: "10:00 PM - 6:30 AM",
    pairingCode: "TV-8492",
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user: cannot list or read family members", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
});

test("Authenticated user: cannot read another user's family members", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("sarah")
      .set(validMemberPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("sarah")
      .get()
  );
});

test("Authenticated owner: can create, list, and update own family member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members")
    .doc("sarah");
  await assertSucceeds(ref.set(validMemberPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
  await assertSucceeds(
    ref.update({
      batteryLevel: 80,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Authenticated owner: rejects shadow update with unexpected key", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members")
    .doc("sarah");
  await assertSucceeds(ref.set(validMemberPayload(ALICE_UID)));
  await assertFails(
    ref.update({
      isAdmin: true,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Authenticated owner: rejects invalid battery level bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("members")
    .doc("sarah");
  const badPayload = { ...validMemberPayload(ALICE_UID), batteryLevel: 150 };
  await assertFails(ref.set(badPayload));
});

test("Authenticated owner: can register device and create consent session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const deviceRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("devices")
    .doc("pixel_8");
  const ts = nowTimestamp();
  await assertSucceeds(
    deviceRef.set({
      deviceId: "pixel_8",
      userId: ALICE_UID,
      name: "Pixel 8 Pro",
      model: "Google Pixel 8 Pro",
      online: true,
      batteryLevel: 92,
      latitude: 41.89,
      longitude: -87.62,
      accuracyMeters: 4,
      locationSharingApproved: true,
      cameraSessionApproved: true,
      microphoneApproved: true,
      pairingCode: "TV-8841",
      lastSeenAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("device_sessions")
    .doc("sess_camera_1");
  await assertSucceeds(
    sessionRef.set({
      sessionId: "sess_camera_1",
      userId: ALICE_UID,
      fromDeviceId: "pixel_8",
      fromDeviceName: "Pixel 8 Pro",
      toDeviceId: "samsung_s24",
      toDeviceName: "Galaxy S24",
      sessionType: "CAMERA",
      status: "REQUESTED",
      offerSdp: "v=0\r\no=mock-sdp",
      answerSdp: "",
      iceCandidatesJson: "[]",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot access Alice's device session
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("device_sessions")
      .doc("sess_camera_1")
      .get()
  );
});

