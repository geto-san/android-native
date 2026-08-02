"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const firebase_functions_test_1 = __importDefault(require("firebase-functions-test"));
const test = (0, firebase_functions_test_1.default)();
const mockSetCustomUserClaims = jest.fn().mockResolvedValue(undefined);
const mockFirestoreSet = jest.fn().mockResolvedValue(undefined);
const mockFirestoreUpdate = jest.fn().mockResolvedValue(undefined);
const mockFirestoreAdd = jest.fn().mockResolvedValue({ id: "new-doc-id" });
const mockFirestoreCollection = jest.fn().mockReturnThis();
const mockFirestoreDoc = jest.fn().mockReturnThis();
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
const myFunctions = __importStar(require("../index"));
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
                providerData: [{ providerId: "google.com", uid: "google-uid" }],
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
                providerData: [],
            });
            const wrapped = test.wrap(myFunctions.onUserCreated);
            await wrapped(user);
            expect(mockSetCustomUserClaims).toHaveBeenCalledWith("guest-1", {
                role: "public",
                session_version: 1,
            });
            expect(mockFirestoreCollection).toHaveBeenCalledWith("users");
            expect(mockFirestoreDoc).toHaveBeenCalledWith("guest-1");
            expect(mockFirestoreSet).toHaveBeenCalledWith(expect.objectContaining({
                role: "public",
                is_anonymous: true,
            }));
        });
    });
    describe("onIncidentWritten", () => {
        it("should call Laravel webhook and handle notifications on creation", async () => {
            const beforeSnap = { exists: false };
            const afterSnap = {
                exists: true,
                data: () => ({ type: "sighting", status: "open" }),
            };
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
//# sourceMappingURL=index.test.js.map