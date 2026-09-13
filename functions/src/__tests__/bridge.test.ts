import * as crypto from "crypto";

// bridge.ts is entirely jest.mock()'d in index.test.ts (postToLaravelWebhook/shouldSkipBridge
// stubbed to isolate the trigger-dispatch tests), so its own logic - the HMAC signing that the
// Laravel side's VerifyWebhookSignature middleware actually verifies, and the echo-prevention
// check that stops a Laravel-originated write from bouncing back through Firestore - has never
// been directly exercised. This file tests the real implementation, unmocked.
import {
  getBridgeSecret,
  getLaravelWebhookBaseUrl,
  shouldSkipBridge,
  signPayload,
} from "../bridge";

describe("signPayload", () => {
  it("produces a sha256=-prefixed hex HMAC matching Node's own hash_hmac equivalent", () => {
    const body = JSON.stringify({ docId: "inc-1", eventType: "create" });
    const secret = "test-secret";

    const signature = signPayload(body, secret);

    const expectedDigest = crypto.createHmac("sha256", secret).update(body).digest("hex");
    expect(signature).toBe(`sha256=${expectedDigest}`);
  });

  it("is deterministic for the same body and secret", () => {
    const body = JSON.stringify({ a: 1 });
    expect(signPayload(body, "secret-a")).toBe(signPayload(body, "secret-a"));
  });

  it("produces a different signature for a different secret", () => {
    const body = JSON.stringify({ a: 1 });
    expect(signPayload(body, "secret-a")).not.toBe(signPayload(body, "secret-b"));
  });

  it("produces a different signature for a different body", () => {
    const secret = "same-secret";
    expect(signPayload(JSON.stringify({ a: 1 }), secret)).not.toBe(
      signPayload(JSON.stringify({ a: 2 }), secret)
    );
  });
});

describe("shouldSkipBridge", () => {
  it("skips when the document's source_system is laravel (echo prevention)", () => {
    expect(shouldSkipBridge({ source_system: "laravel" })).toBe(true);
  });

  it("does not skip when source_system is firestore", () => {
    expect(shouldSkipBridge({ source_system: "firestore" })).toBe(false);
  });

  it("does not skip when source_system is absent (mobile-authored documents)", () => {
    expect(shouldSkipBridge({ type: "sighting" })).toBe(false);
  });

  it("does not skip for null or undefined data", () => {
    expect(shouldSkipBridge(null)).toBe(false);
    expect(shouldSkipBridge(undefined)).toBe(false);
  });
});

describe("getBridgeSecret / getLaravelWebhookBaseUrl", () => {
  const originalSecret = process.env.FIREBASE_BRIDGE_SECRET;
  const originalUrl = process.env.LARAVEL_WEBHOOK_BASE_URL;

  afterEach(() => {
    process.env.FIREBASE_BRIDGE_SECRET = originalSecret;
    process.env.LARAVEL_WEBHOOK_BASE_URL = originalUrl;
  });

  it("reads the secret from FIREBASE_BRIDGE_SECRET when set", () => {
    process.env.FIREBASE_BRIDGE_SECRET = "env-secret";
    expect(getBridgeSecret()).toBe("env-secret");
  });

  it("falls back to an empty string when unset (postToLaravelWebhook's caller treats this as unconfigured)", () => {
    delete process.env.FIREBASE_BRIDGE_SECRET;
    expect(getBridgeSecret()).toBe("");
  });

  it("reads the webhook base URL from LARAVEL_WEBHOOK_BASE_URL when set", () => {
    process.env.LARAVEL_WEBHOOK_BASE_URL = "http://example.test/api/webhooks";
    expect(getLaravelWebhookBaseUrl()).toBe("http://example.test/api/webhooks");
  });

  it("falls back to the local docker-compose default when unset", () => {
    delete process.env.LARAVEL_WEBHOOK_BASE_URL;
    expect(getLaravelWebhookBaseUrl()).toBe("http://laravel:8000/api/webhooks");
  });
});
