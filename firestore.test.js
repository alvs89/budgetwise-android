const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'demo-no-project';
const ALICE_UID = 'user_alice_123';
const BOB_UID = 'user_bob_456';

let testEnv;

before(async () => {
  const rules = fs.readFileSync('firestore.rules', 'utf8');
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: '127.0.0.1',
      port: 8085,
      rules,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

function pastDate(secondsAgo = 5) {
  return new Date(Date.now() - secondsAgo * 1000);
}

function futureDate(secondsAhead = 600) {
  return new Date(Date.now() + secondsAhead * 1000);
}

function validProfile(uid = ALICE_UID) {
  const ts = pastDate(5);
  return {
    userId: uid,
    displayName: 'Alex Rivera',
    preferredCurrency: 'PHP',
    onboardingCompleted: false,
    initializedData: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTransaction(uid = ALICE_UID) {
  const ts = pastDate(5);
  return {
    userId: uid,
    title: 'Weekly Groceries',
    amountCentavos: 215000,
    type: 'EXPENSE',
    category: 'Food and Dining',
    dateEpochDay: 20400,
    paymentMethod: 'GCASH',
    notes: 'Produce and pantry staples',
    sampleData: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validBudget(uid = ALICE_UID) {
  const ts = pastDate(5);
  return {
    userId: uid,
    category: 'Food and Dining',
    limitCentavos: 450000,
    year: 2026,
    month: 11,
    sampleData: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validGoal(uid = ALICE_UID) {
  const ts = pastDate(5);
  return {
    userId: uid,
    name: 'Emergency Fund',
    targetCentavos: 3000000,
    currentSavedCentavos: 500000,
    targetDateEpochDay: 21000,
    createdDateEpochDay: 20400,
    status: 'ACTIVE',
    sampleData: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validContribution(uid = ALICE_UID) {
  return {
    userId: uid,
    goalDocId: 'goal_emergency_1',
    amountCentavos: 500000,
    withdrawal: false,
    note: 'Initial deposit',
    dateEpochDay: 20400,
    createdAt: pastDate(5),
  };
}

function validCategory(uid = ALICE_UID) {
  const ts = pastDate(5);
  return {
    userId: uid,
    name: 'Coffee & Snacks',
    type: 'EXPENSE',
    iconKey: 'coffee',
    colorHex: 0xFFEA580C,
    defaultCategory: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

describe('BudgetWise Firestore Security Rules', () => {
  it('denies unauthenticated read and write across user profile and subcollections', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await db.doc(`users/${ALICE_UID}`).set(validProfile());
      await db.doc(`users/${ALICE_UID}/transactions/tx_1`).set(validTransaction());
    });

    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(anonDb.doc(`users/${ALICE_UID}`).get());
    await assertFails(anonDb.doc(`users/${ALICE_UID}`).set(validProfile()));
    await assertFails(anonDb.collection(`users/${ALICE_UID}/transactions`).get());
    await assertFails(anonDb.doc(`users/${ALICE_UID}/transactions/tx_2`).set(validTransaction()));
  });

  it('prevents listing /users root collection and blocks cross-user reads/writes', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore();
      await db.doc(`users/${ALICE_UID}`).set(validProfile(ALICE_UID));
      await db.doc(`users/${ALICE_UID}/transactions/tx_1`).set(validTransaction(ALICE_UID));
      await db.doc(`users/${ALICE_UID}/budgets/b_1`).set(validBudget(ALICE_UID));
      await db.doc(`users/${ALICE_UID}/goals/g_1`).set(validGoal(ALICE_UID));
      await db.doc(`users/${ALICE_UID}/contributions/c_1`).set(validContribution(ALICE_UID));
      await db.doc(`users/${ALICE_UID}/categories/cat_1`).set(validCategory(ALICE_UID));
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    await assertFails(bobDb.collection('users').get());
    await assertFails(bobDb.doc(`users/${ALICE_UID}`).get());
    await assertFails(bobDb.doc(`users/${ALICE_UID}`).update({ displayName: 'Hacked', updatedAt: pastDate(1) }));
    await assertFails(bobDb.collection(`users/${ALICE_UID}/transactions`).get());
    await assertFails(bobDb.doc(`users/${ALICE_UID}/transactions/tx_1`).delete());
    await assertFails(bobDb.collection(`users/${ALICE_UID}/budgets`).get());
    await assertFails(bobDb.collection(`users/${ALICE_UID}/goals`).get());
    await assertFails(bobDb.collection(`users/${ALICE_UID}/contributions`).get());
    await assertFails(bobDb.collection(`users/${ALICE_UID}/categories`).get());
  });

  it('allows owner profile create, partial update, and rejects immutable or invalid writes', async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    const profileRef = aliceDb.doc(`users/${ALICE_UID}`);

    await assertSucceeds(profileRef.set(validProfile()));
    await assertSucceeds(profileRef.get());

    // Valid partial update (displayName, currency, onboardingCompleted)
    await assertSucceeds(
      profileRef.update({
        displayName: 'Alex Santos',
        preferredCurrency: 'USD',
        onboardingCompleted: true,
        initializedData: true,
        updatedAt: pastDate(1),
      })
    );

    // Reject mutating immutable createdAt
    await assertFails(
      profileRef.update({
        createdAt: pastDate(1),
        updatedAt: pastDate(1),
      })
    );

    // Reject invalid currency code
    await assertFails(
      profileRef.update({
        preferredCurrency: 'XYZ',
        updatedAt: pastDate(1),
      })
    );

    // Reject extra undocumented "id" key on create
    const badProfile = { ...validProfile(BOB_UID), id: BOB_UID };
    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    await assertFails(bobDb.doc(`users/${BOB_UID}`).set(badProfile));
  });

  it('validates transactions CRUD, partial updates, enums, bounds, and timestamps', async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    const txRef = aliceDb.doc(`users/${ALICE_UID}/transactions/tx_100`);

    await assertSucceeds(txRef.set(validTransaction()));
    await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/transactions`).get());

    // Partial update matching FirestoreSyncRepository
    await assertSucceeds(
      txRef.update({
        title: 'Updated Groceries',
        amountCentavos: 250000,
        category: 'Groceries',
        updatedAt: pastDate(1),
      })
    );

    // Reject negative/zero amount
    await assertFails(
      txRef.update({
        amountCentavos: 0,
        updatedAt: pastDate(1),
      })
    );

    // Reject invalid paymentMethod
    await assertFails(
      txRef.update({
        paymentMethod: 'CRYPTO',
        updatedAt: pastDate(1),
      })
    );

    // Reject future timestamp
    await assertFails(
      txRef.update({
        title: 'Future Tx',
        updatedAt: futureDate(600),
      })
    );

    // Reject mutating userId
    await assertFails(
      txRef.update({
        userId: BOB_UID,
        updatedAt: pastDate(1),
      })
    );

    await assertSucceeds(txRef.delete());
  });

  it('validates budgets, goals, contributions, and categories CRUD rules', async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

    // Budget
    const budgetRef = aliceDb.doc(`users/${ALICE_UID}/budgets/budget_2026_11_food`);
    await assertSucceeds(budgetRef.set(validBudget()));
    await assertSucceeds(budgetRef.update({ limitCentavos: 600000, updatedAt: pastDate(1) }));
    await assertFails(budgetRef.update({ month: 13, updatedAt: pastDate(1) }));
    await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/budgets`).get());
    await assertSucceeds(budgetRef.delete());

    // Goal
    const goalRef = aliceDb.doc(`users/${ALICE_UID}/goals/goal_emergency_1`);
    await assertSucceeds(goalRef.set(validGoal()));
    await assertSucceeds(
      goalRef.update({
        currentSavedCentavos: 1000000,
        status: 'COMPLETED',
        updatedAt: pastDate(1),
      })
    );
    await assertFails(goalRef.update({ status: 'ARCHIVED', updatedAt: pastDate(1) }));
    await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/goals`).get());

    // Contribution (create + read + delete allowed, update forbidden)
    const contribRef = aliceDb.doc(`users/${ALICE_UID}/contributions/contrib_1`);
    await assertSucceeds(contribRef.set(validContribution()));
    await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/contributions`).get());
    await assertFails(contribRef.update({ amountCentavos: 999999 }));
    await assertSucceeds(contribRef.delete());
    await assertSucceeds(goalRef.delete());

    // Category
    const catRef = aliceDb.doc(`users/${ALICE_UID}/categories/cat_expense_coffee`);
    await assertSucceeds(catRef.set(validCategory()));
    await assertSucceeds(
      catRef.update({
        name: 'Specialty Coffee',
        iconKey: 'local_cafe',
        colorHex: 0xFF10B981,
        updatedAt: pastDate(1),
      })
    );
    // Cannot change immutable category type or defaultCategory flag
    await assertFails(catRef.update({ type: 'INCOME', updatedAt: pastDate(1) }));
    await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/categories`).get());
    await assertSucceeds(catRef.delete());
  });

  it('denies access to any unknown root or nested collections', async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(aliceDb.doc('secrets/global').get());
    await assertFails(aliceDb.doc(`users/${ALICE_UID}/unknown/doc1`).set({ foo: 'bar' }));
  });
});
