import firebaseFunctionsTest from "firebase-functions-test";
import * as admin from "firebase-admin";

const test = firebaseFunctionsTest();

const mockSetCustomUserClaims = jest.fn().mockResolvedValue(undefined);
const mockGetUser = jest.fn().mockResolvedValue({ uid: "target-uid", customClaims: {} });
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
      getUser: mockGetUser,
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
        session_version: 1,
      });
      expect(mockFirestoreCollection).toHaveBeenCalledWith("users");
      expect(mockFirestoreDoc).toHaveBeenCalledWith("user-1");
    });

    it("should set custom claims and create firestore document for anonymous users", async () => {
      const user = test.auth.makeUserRecord({
        uid: "guest-1",
        providerData: [] as any,
      });

      const wrapped = test.wrap(myFunctions.onUserCreated);
      await wrapped(user);

      expect(mockSetCustomUserClaims).toHaveBeenCalledWith("guest-1", {
        role: "public",
        session_version: 1,
      });
      expect(mockFirestoreCollection).toHaveBeenCalledWith("users");
      expect(mockFirestoreDoc).toHaveBeenCalledWith("guest-1");
      expect(mockFirestoreSet).toHaveBeenCalledWith(
        expect.objectContaining({
          role: "public",
          is_anonymous: true,
        })
      );
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

    it("does not call the Laravel webhook when the write originated from Laravel (echo prevention)", async () => {
      const bridge = require("../bridge");
      // shouldSkipBridge is mocked module-wide to return false by default (see the jest.mock
      // above) so every other test in this file exercises the "do bridge" path - this is the
      // one place the "don't bounce a Laravel-originated write back to Laravel" path gets
      // exercised at all.
      bridge.shouldSkipBridge.mockReturnValueOnce(true);

      const beforeSnap = { exists: false } as any;
      const afterSnap = {
        exists: true,
        data: () => ({ type: "sighting", status: "open", source_system: "laravel" }),
      } as any;
      const change = { before: beforeSnap, after: afterSnap };

      const wrapped = test.wrap(myFunctions.onIncidentWritten);
      await wrapped(change, { params: { incidentId: "inc-2" } });

      expect(bridge.postToLaravelWebhook).not.toHaveBeenCalled();
    });
  });

  describe("setUserRole", () => {
    const wrap = () => test.wrap(myFunctions.setUserRole);

    it("rejects unauthenticated callers", async () => {
      await expect(
        wrap()({ targetUid: "u1", role: "ranger" }, {} as any)
      ).rejects.toThrow(/authenticated/i);
    });

    it("rejects when targetUid or role is missing", async () => {
      const context = { auth: { uid: "caller", token: { role: "uwa_official" } } } as any;
      await expect(wrap()({ role: "ranger" }, context)).rejects.toThrow(/targetUid and role/i);
    });

    it("blocks a warden from promoting a user to uwa_official", async () => {
      const context = { auth: { uid: "warden-1", token: { role: "warden" } } } as any;
      await expect(
        wrap()({ targetUid: "u1", role: "uwa_official" }, context)
      ).rejects.toThrow(/cannot promote/i);
    });

    it("blocks a warden from promoting a user to warden (self-tier escalation)", async () => {
      const context = { auth: { uid: "warden-1", token: { role: "warden" } } } as any;
      await expect(
        wrap()({ targetUid: "u1", role: "warden" }, context)
      ).rejects.toThrow(/cannot promote/i);
    });

    it("blocks a caller with no privileged role at all", async () => {
      const context = { auth: { uid: "ranger-1", token: { role: "ranger" } } } as any;
      await expect(
        wrap()({ targetUid: "u1", role: "ranger" }, context)
      ).rejects.toThrow(/Only UWA Officials or Wardens/i);
    });

    it("allows a warden to promote a user to ranger, merging claims and logging an audit entry", async () => {
      mockGetUser.mockResolvedValueOnce({ uid: "u1", customClaims: { role: "public" } });
      const context = { auth: { uid: "warden-1", token: { role: "warden" } } } as any;

      const result = await wrap()(
        { targetUid: "u1", role: "ranger", parkId: "bwindi-impenetrable" },
        context
      );

      expect(result).toEqual({ message: "Success! User u1 is now ranger." });
      expect(mockSetCustomUserClaims).toHaveBeenCalledWith("u1", {
        role: "ranger",
        park_id: "bwindi-impenetrable",
      });
      expect(mockFirestoreCollection).toHaveBeenCalledWith("role_audit");
      expect(mockFirestoreAdd).toHaveBeenCalledWith(
        expect.objectContaining({ targetUid: "u1", newRole: "ranger", changedBy: "warden-1" })
      );
    });

    it("allows a uwa_official to promote a user to warden (unrestricted)", async () => {
      mockGetUser.mockResolvedValueOnce({ uid: "u2", customClaims: {} });
      const context = { auth: { uid: "official-1", token: { role: "uwa_official" } } } as any;

      const result = await wrap()({ targetUid: "u2", role: "warden" }, context);

      expect(result).toEqual({ message: "Success! User u2 is now warden." });
      expect(mockSetCustomUserClaims).toHaveBeenCalledWith("u2", { role: "warden", park_id: null });
    });
  });
});
