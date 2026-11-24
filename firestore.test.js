const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_001";
const ADMIN_EMAIL = "cm776773934@gmail.com";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowMinus(ms = 2000) {
  return new Date(Date.now() - ms);
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

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("academies").get());
  await assertFails(unauthDb.collection("courses").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("lessonContents").doc("lesson_1").get());
});

test("Email spoofing attack against admin rule is blocked when email_verified is false", async () => {
  const spoofDb = testEnv.authenticatedContext("attacker_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  const ts = nowMinus();
  await assertFails(
    spoofDb.collection("academies").doc("ai_academy").set({
      name: "Fake Academy",
      shortName: "Fake",
      description: "Spoofed admin write",
      iconKey: "ai",
      accentHex: "#2563EB",
      displayOrder: 1,
      courseCount: 0,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("User profile PII isolation, role escalation prevention, and safe updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = nowMinus();

  // Regular user cannot self-assign ADMIN role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Hacker",
      email: "alice@example.com",
      role: "ADMIN",
      careerInterests: ["Data"],
      learningInterests: ["SQL"],
      learningGoal: "Learn SQL",
      weeklyTargetHours: 5,
      streakDays: 0,
      totalLearningMinutes: 0,
      remindersEnabled: true,
      courseUpdatesEnabled: true,
      announcementsEnabled: true,
      achievementsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular user creates valid STUDENT profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Namukasa",
      email: "alice@example.com",
      role: "STUDENT",
      careerInterests: ["Data Analytics"],
      learningInterests: ["Excel", "SQL"],
      learningGoal: "Become a Data Analyst",
      weeklyTargetHours: 6,
      streakDays: 0,
      totalLearningMinutes: 0,
      remindersEnabled: true,
      courseUpdatesEnabled: true,
      announcementsEnabled: true,
      achievementsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update of allowed fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      learningGoal: "Master Power BI & SQL",
      streakDays: 3,
      totalLearningMinutes: 90,
      updatedAt: nowMinus(500),
    })
  );

  // Role escalation on update is blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: nowMinus(400),
    })
  );

  // Bob cannot read Alice's PII profile or list users
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Admin catalog CRUD, free vs premium lessonContent entitlement, and enrollment atomicity", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = nowMinus(5000);

  // Admin creates academy, instructor, courses (1 free, 1 premium), lessons, and lessonContents
  await assertSucceeds(
    adminDb.collection("academies").doc("data_analytics").set({
      name: "Data & Analytics Academy",
      shortName: "Data & Analytics",
      description: "Practical analytics, SQL, Excel, Power BI, and Python skills.",
      iconKey: "analytics",
      accentHex: "#0284C7",
      displayOrder: 2,
      courseCount: 18,
      published: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("courses").doc("course_sql_free").set({
      academyId: "data_analytics",
      academyName: "Data & Analytics Academy",
      title: "SQL for Beginners",
      subtitle: "Query databases with confidence",
      description: "Hands-on SQL querying for real-world datasets.",
      instructorId: "inst_1",
      instructorName: "Grace Okello",
      level: "BEGINNER",
      durationMinutes: 180,
      lessonCount: 1,
      accessTier: "FREE",
      priceUsd: 0.0,
      priceUgx: 0,
      certificateAvailable: true,
      published: true,
      featured: true,
      enrollmentCount: 0,
      popularityScore: 0,
      whatYouWillLearn: ["SELECT queries", "WHERE filtering"],
      prerequisites: ["Basic computer skills"],
      formats: ["VIDEO", "WRITTEN", "QUIZ"],
      tags: ["SQL", "Data"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("courses").doc("course_powerbi_pro").set({
      academyId: "data_analytics",
      academyName: "Data & Analytics Academy",
      title: "Power BI",
      subtitle: "Executive dashboards and DAX",
      description: "Build interactive BI dashboards.",
      instructorId: "inst_1",
      instructorName: "Grace Okello",
      level: "INTERMEDIATE",
      durationMinutes: 300,
      lessonCount: 1,
      accessTier: "PREMIUM",
      priceUsd: 19.0,
      priceUgx: 70000,
      certificateAvailable: true,
      published: true,
      featured: true,
      enrollmentCount: 0,
      popularityScore: 0,
      whatYouWillLearn: ["Data modeling", "DAX measures"],
      prerequisites: ["Excel basics"],
      formats: ["VIDEO", "PROJECT"],
      tags: ["Power BI", "BI"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("lessonContents").doc("lesson_free_1").set({
      courseId: "course_sql_free",
      freePreview: true,
      premiumOnly: false,
      notesMarkdown: "# Welcome to SQL",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("lessonContents").doc("lesson_pro_1").set({
      courseId: "course_powerbi_pro",
      freePreview: false,
      premiumOnly: true,
      notesMarkdown: "# Secret Premium DAX Masterclass",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular user cannot write courses or list lessonContents
  await assertFails(
    aliceDb.collection("courses").doc("hacked_course").set({
      academyId: "data_analytics",
      academyName: "Data & Analytics Academy",
      title: "Hacked",
      subtitle: "Hacked",
      description: "Hacked",
      instructorId: "inst_1",
      instructorName: "Hacker",
      level: "BEGINNER",
      durationMinutes: 60,
      lessonCount: 1,
      accessTier: "FREE",
      priceUsd: 0.0,
      priceUgx: 0,
      certificateAvailable: false,
      published: true,
      featured: false,
      enrollmentCount: 0,
      popularityScore: 0,
      whatYouWillLearn: ["None"],
      prerequisites: ["None"],
      formats: ["WRITTEN"],
      tags: ["SQL"],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(aliceDb.collection("lessonContents").get());

  // Unsubscribed Alice can read free lessonContent, CANNOT read premium lessonContent
  await assertSucceeds(aliceDb.collection("lessonContents").doc("lesson_free_1").get());
  await assertFails(aliceDb.collection("lessonContents").doc("lesson_pro_1").get());

  // Unsubscribed Alice CANNOT enroll in premium course, CAN enroll in free course with atomic batch
  await assertFails(
    aliceDb.collection("enrollments").doc(`${ALICE_UID}_course_powerbi_pro`).set({
      userId: ALICE_UID,
      courseId: "course_powerbi_pro",
      academyId: "data_analytics",
      courseTitle: "Power BI",
      status: "ACTIVE",
      progressPercent: 0,
      completedLessonsCount: 0,
      totalLessonsCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("enrollments").doc(`${ALICE_UID}_course_sql_free`), {
    userId: ALICE_UID,
    courseId: "course_sql_free",
    academyId: "data_analytics",
    courseTitle: "SQL for Beginners",
    status: "ACTIVE",
    progressPercent: 0,
    completedLessonsCount: 0,
    totalLessonsCount: 1,
    createdAt: ts,
    updatedAt: ts,
  });
  batch.update(aliceDb.collection("courses").doc("course_sql_free"), {
    enrollmentCount: 1,
    popularityScore: 1,
    updatedAt: nowMinus(1000),
  });
  await assertSucceeds(batch.commit());

  // Alice cannot forge ACTIVE subscription herself
  await assertFails(
    aliceDb.collection("subscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planId: "plan_monthly",
      planName: "Pro Monthly",
      billingCycle: "MONTHLY",
      currency: "UGX",
      amount: 35000,
      status: "ACTIVE",
      paymentProvider: "MTN_MOMO",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin activates Alice's subscription -> now Alice CAN read premium lessonContent and enroll!
  await assertSucceeds(
    adminDb.collection("subscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planId: "plan_monthly",
      planName: "Pro Monthly",
      billingCycle: "MONTHLY",
      currency: "UGX",
      amount: 35000,
      status: "ACTIVE",
      paymentProvider: "ADMIN_GRANT",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("lessonContents").doc("lesson_pro_1").get());
  await assertSucceeds(
    aliceDb.collection("enrollments").doc(`${ALICE_UID}_course_powerbi_pro`).set({
      userId: ALICE_UID,
      courseId: "course_powerbi_pro",
      academyId: "data_analytics",
      courseTitle: "Power BI",
      status: "ACTIVE",
      progressPercent: 0,
      completedLessonsCount: 0,
      totalLessonsCount: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Certificates require completed enrollment and can be verified by exact ID", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = nowMinus(5000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("courses").doc("course_excel").set({
      academyId: "data_analytics",
      academyName: "Data & Analytics Academy",
      title: "Excel for Beginners",
      subtitle: "Spreadsheets",
      description: "Excel basics",
      instructorId: "inst_1",
      instructorName: "Grace Okello",
      level: "BEGINNER",
      durationMinutes: 120,
      lessonCount: 1,
      accessTier: "FREE",
      priceUsd: 0.0,
      priceUgx: 0,
      certificateAvailable: true,
      published: true,
      featured: false,
      enrollmentCount: 1,
      popularityScore: 1,
      whatYouWillLearn: ["Formulas"],
      prerequisites: ["None"],
      formats: ["WRITTEN"],
      tags: ["Excel"],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("enrollments").doc(`${ALICE_UID}_course_excel`).set({
      userId: ALICE_UID,
      courseId: "course_excel",
      academyId: "data_analytics",
      courseTitle: "Excel for Beginners",
      status: "ACTIVE",
      progressPercent: 50,
      completedLessonsCount: 1,
      totalLessonsCount: 2,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const certId = `${ALICE_UID}_course_excel`;
  const certPayload = {
    certificateCode: "DLH-2026-EXCEL1",
    userId: ALICE_UID,
    studentName: "Alice Namukasa",
    courseId: "course_excel",
    courseTitle: "Excel for Beginners",
    academyName: "Data & Analytics Academy",
    issuedDateText: "Nov 21, 2026",
    verificationUrl: "https://datalabs.academy/verify/DLH-2026-EXCEL1",
    status: "VALID",
    issuedBy: "SYSTEM_COMPLETION",
    createdAt: ts,
  };

  // Fails while enrollment is ACTIVE (not COMPLETED)
  await assertFails(aliceDb.collection("certificates").doc(certId).set(certPayload));

  // Alice completes the course
  await assertSucceeds(
    aliceDb.collection("enrollments").doc(`${ALICE_UID}_course_excel`).update({
      status: "COMPLETED",
      progressPercent: 100,
      completedLessonsCount: 2,
      completedAt: nowMinus(1000),
      updatedAt: nowMinus(1000),
    })
  );

  // Now certificate creation succeeds
  await assertSucceeds(aliceDb.collection("certificates").doc(certId).set(certPayload));

  // Bob can verify by exact ID, cannot list Alice's certificates, and cannot revoke/delete
  await assertSucceeds(bobDb.collection("certificates").doc(certId).get());
  await assertFails(bobDb.collection("certificates").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("certificates").doc(certId).update({ status: "REVOKED" }));
  await assertSucceeds(aliceDb.collection("certificates").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(adminDb.collection("certificates").get());
});

test("Scoped queries for enrollments, lessonProgress, savedCourses, and notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const ts = nowMinus(3000);

  await assertSucceeds(
    aliceDb.collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "Study Reminder",
      body: "Complete your SQL module today.",
      type: "REMINDER",
      targetRoute: "explore",
      read: false,
      createdAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("notifications").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("notifications").get());
  await assertFails(bobDb.collection("notifications").where("userId", "==", ALICE_UID).get());
});

test("AI Academy Specialization Certificate requires all 5 AI Academy courses COMPLETED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const ts = nowMinus(4000);
  const aiCourseIds = [
    "ai_artificial_intelligence_for_beginners",
    "ai_ai_for_productivity",
    "ai_prompt_engineering",
    "ai_ai_for_business_and_entrepreneurs",
    "ai_building_apps_with_ai",
  ];

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (let i = 0; i < aiCourseIds.length; i++) {
      const cid = aiCourseIds[i];
      await db.collection("enrollments").doc(`${ALICE_UID}_${cid}`).set({
        userId: ALICE_UID,
        courseId: cid,
        academyId: "academy_ai",
        courseTitle: cid,
        status: i < 4 ? "COMPLETED" : "ACTIVE",
        progressPercent: i < 4 ? 100 : 50,
        completedLessonsCount: i < 4 ? 4 : 2,
        totalLessonsCount: 4,
        createdAt: ts,
        updatedAt: ts,
      });
    }
  });

  const specCertId = `${ALICE_UID}_spec_academy_ai`;
  const specPayload = {
    certificateCode: "DLH-SPEC-AI2026",
    userId: ALICE_UID,
    studentName: "Alice Namukasa",
    courseId: "spec_academy_ai",
    courseTitle: "AI Academy Specialization: Practical AI & App Engineering",
    academyName: "AI Academy",
    issuedDateText: "Nov 23, 2026",
    verificationUrl: "https://datalabs.academy/verify/DLH-SPEC-AI2026",
    status: "VALID",
    issuedBy: "SYSTEM_COMPLETION",
    createdAt: ts,
  };

  // Fails when 5th course is still ACTIVE
  await assertFails(aliceDb.collection("certificates").doc(specCertId).set(specPayload));

  // Complete 5th course
  await assertSucceeds(
    aliceDb.collection("enrollments").doc(`${ALICE_UID}_ai_building_apps_with_ai`).update({
      status: "COMPLETED",
      progressPercent: 100,
      completedLessonsCount: 4,
      completedAt: nowMinus(1000),
      updatedAt: nowMinus(1000),
    })
  );

  // Now specialization certificate creation succeeds
  await assertSucceeds(aliceDb.collection("certificates").doc(specCertId).set(specPayload));
});
