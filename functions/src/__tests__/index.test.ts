import firebaseFunctionsTest from "firebase-functions-test";
import * as admin from "firebase-admin";

const test = firebaseFunctionsTest();

const mockSetCustomUserClaims = jest.fn().mockResolvedValue(undefined);
const mockFirestoreSet = jest.fn().mockResolvedValue(undefined);
const mockFirestoreUpdate = jest.fn().mockResolvedValue(undefined);
const mockFirestoreAdd = jest.fn().mockResolvedValue({ id: "new-doc-id" });
const mockFirestoreCollection = jest.fn().mockReturnThis();
const mockFirestoreDoc = jest.fn().mockReturnThis();

// Mock dependencies before importing functions
jest.mock("firebase-admin", () => {
  return {
    initializeApp: jest.fn(),
    auth: jest.fn(() => ({
      setCustomUserClaims: mockSetCustomUserClaims,
    })),
    firestore: Object.assign(jest.fn(() => ({
      collection: mockFirestoreCollection,
      doc: mockFirestoreDoc,
      set: mockFirestoreSet,
      update: mockFirestoreUpdate,
      add: mockFirestoreAdd,
    })), {
      FieldValue: {
        serverTimestamp: jest.fn().mockReturnValue("mock-timestamp"),
      },
    }),
  };
});

jest.mock("../bridge", () => ({
  postToLaravelWebhook: jest.fn().mockResolvedValue(undefined),
  shouldSkipBridge: jest.fn().mockReturnValue(false),
}));

jest.mock("../notifications", () => ({
  handleIncidentCreateNotifications: jest.fn().mockResolvedValue(undefined),
  handleSightingApprovalNotifications: jest.fn().mockResolvedValue(undefined),
  handleSosAlertCreateNotifications: jest.fn().mockResolvedValue(undefined),
  handleFeedArticleCreateNotifications: jest.fn().mockResolvedValue(undefined),
}));

import * as myFunctions from "../index";

describe("Cloud Functions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterAll(() => {
    test.cleanup();
  });

  describe("onUserCreated", () => {
    it("should set custom claims and create firestore document for non-guest users", async () => {
      const user = test.auth.makeUserRecord({
        uid: "user-1",
        email: "test@example.com",
        displayName: "Test User",
        providerData: [{ providerId: "google.com", uid: "google-uid" }] as any,
      });

      const wrapped = test.wrap(myFunctions.onUserCreated);
      await wrapped(user);

      expect(mockSetCustomUserClaims).toHaveBeenCalledWith("user-1", {
        role: "public",
      });
      expect(mockFirestoreCollection).toHaveBeenCalledWith("users");
      expect(mockFirestoreDoc).toHaveBeenCalledWith("user-1");
    });

    it("should skip for guest users (no provider data)", async () => {
      const user = test.auth.makeUserRecord({
        uid: "guest-1",
        providerData: [] as any,
      });

      const wrapped = test.wrap(myFunctions.onUserCreated);
      await wrapped(user);

      expect(mockSetCustomUserClaims).not.toHaveBeenCalled();
    });
  });

  describe("onIncidentWritten", () => {
    it("should call Laravel webhook and handle notifications on creation", async () => {
      const beforeSnap = { exists: false } as any;
      const afterSnap = {
        exists: true,
        data: () => ({ type: "sighting", status: "open" }),
      } as any;
      const change = { before: beforeSnap, after: afterSnap };

      const wrapped = test.wrap(myFunctions.onIncidentWritten);
      await wrapped(change, { params: { incidentId: "inc-1" } });

      const bridge = require("../bridge");
      const notifications = require("../notifications");

      expect(bridge.postToLaravelWebhook).toHaveBeenCalledWith("incidents", expect.objectContaining({
        docId: "inc-1",
        eventType: "create"
      }));
      expect(notifications.handleIncidentCreateNotifications).toHaveBeenCalled();
    });
  });
});
