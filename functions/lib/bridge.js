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
Object.defineProperty(exports, "__esModule", { value: true });
exports.getBridgeSecret = getBridgeSecret;
exports.getLaravelWebhookBaseUrl = getLaravelWebhookBaseUrl;
exports.signPayload = signPayload;
exports.shouldSkipBridge = shouldSkipBridge;
exports.postToLaravelWebhook = postToLaravelWebhook;
const crypto = __importStar(require("crypto"));
const functions = __importStar(require("firebase-functions"));
function getBridgeSecret() {
    return (process.env.FIREBASE_BRIDGE_SECRET ||
        functions.config().bridge?.secret ||
        "");
}
function getLaravelWebhookBaseUrl() {
    return (process.env.LARAVEL_WEBHOOK_BASE_URL ||
        functions.config().bridge?.laravel_url ||
        "http://laravel:8000/api/webhooks");
}
function signPayload(body, secret) {
    const digest = crypto.createHmac("sha256", secret).update(body).digest("hex");
    return `sha256=${digest}`;
}
function shouldSkipBridge(data) {
    return data?.source_system === "laravel";
}
async function postToLaravelWebhook(resource, payload) {
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
//# sourceMappingURL=bridge.js.map