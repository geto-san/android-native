import * as crypto from "crypto";
import * as functions from "firebase-functions";

export interface BridgePayload {
  docId: string;
  before: Record<string, unknown> | null;
  after: Record<string, unknown> | null;
  eventType: "create" | "update" | "delete";
}

export function getBridgeSecret(): string {
  return (
    process.env.FIREBASE_BRIDGE_SECRET ||
    functions.config().bridge?.secret ||
    ""
  );
}

export function getLaravelWebhookBaseUrl(): string {
  return (
    process.env.LARAVEL_WEBHOOK_BASE_URL ||
    functions.config().bridge?.laravel_url ||
    "http://laravel:8000/api/webhooks"
  );
}

export function signPayload(body: string, secret: string): string {
  const digest = crypto.createHmac("sha256", secret).update(body).digest("hex");
  return `sha256=${digest}`;
}

export function shouldSkipBridge(data: Record<string, unknown> | null | undefined): boolean {
  return data?.source_system === "laravel";
}

export async function postToLaravelWebhook(
  resource: "incidents" | "sightings" | "sos-alerts",
  payload: BridgePayload
): Promise<void> {
  const secret = getBridgeSecret();
  if (!secret) {
    console.warn(`Bridge secret not configured; skipping ${resource} webhook for ${payload.docId}`);
    return;
  }

  const baseUrl = getLaravelWebhookBaseUrl().replace(/\/$/, "");
  const url = `${baseUrl}/${resource}`;
  const body = JSON.stringify(payload);
  const signature = signPayload(body, secret);

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-WildWatch-Signature": signature,
    },
    body,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Laravel webhook ${url} failed (${response.status}): ${text}`);
  }
}
