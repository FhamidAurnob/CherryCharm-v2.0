/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

// ── bKash Config ──────────────────────────────────────────────────────────────
const BKASH_BASE_URL = "https://tokenized.sandbox.bka.sh/v1.2.0-beta"; // sandbox
const BKASH_APP_KEY = functions.config().bkash.app_key;
const BKASH_APP_SECRET = functions.config().bkash.app_secret;
const BKASH_USERNAME = functions.config().bkash.username;
const BKASH_PASSWORD = functions.config().bkash.password;

// ── Nagad Config ──────────────────────────────────────────────────────────────
const NAGAD_BASE_URL = "https://sandbox.mynagad.com:10080/remote-payment-gateway-1.0";
const NAGAD_MERCHANT_ID = functions.config().nagad.merchant_id;
const NAGAD_MERCHANT_PRIVATE_KEY = functions.config().nagad.private_key;

// ── SSL Commerz Config ────────────────────────────────────────────────────────
const SSL_STORE_ID = functions.config().ssl.store_id;
const SSL_STORE_PASSWD = functions.config().ssl.store_password;
const SSL_BASE_URL = "https://sandbox.sslcommerz.com"; // sandbox

// ── Get bKash Token ───────────────────────────────────────────────────────────
async function getBkashToken() {
    const response = await axios.post(
        `${BKASH_BASE_URL}/tokenized/checkout/token/grant`,
        {
            app_key: BKASH_APP_KEY,
            app_secret: BKASH_APP_SECRET,
        },
        {
            headers: {
                username: BKASH_USERNAME,
                password: BKASH_PASSWORD,
                "Content-Type": "application/json",
            },
        }
    );
    return response.data.id_token;
}

// ── Initiate bKash Payment ────────────────────────────────────────────────────
exports.initiateBkashPayment = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

    const { amount, orderId } = data;
    const token = await getBkashToken();

    const response = await axios.post(
        `${BKASH_BASE_URL}/tokenized/checkout/create`,
        {
            mode: "0011",
            payerReference: context.auth.uid,
            callbackURL: `https://your-app.web.app/bkash/callback`,
            amount: amount,
            currency: "BDT",
            intent: "sale",
            merchantInvoiceNumber: orderId,
        },
        {
            headers: {
                Authorization: token,
                "X-APP-Key": BKASH_APP_KEY,
            },
        }
    );
    return response.data;
});

// ── Execute bKash Payment ─────────────────────────────────────────────────────
exports.executeBkashPayment = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

    const { paymentId, orderId } = data;
    const token = await getBkashToken();

    const response = await axios.post(
        `${BKASH_BASE_URL}/tokenized/checkout/execute`,
        { paymentID: paymentId },
        {
            headers: {
                Authorization: token,
                "X-APP-Key": BKASH_APP_KEY,
            },
        }
    );

    // Update order status in Firestore
    if (response.data.transactionStatus === "Completed") {
        await admin.firestore().collection("orders").doc(orderId).update({
            status: "CONFIRMED",
            paymentStatus: "SUCCESS",
            paymentId: response.data.trxID,
            updatedAt: Date.now(),
        });
    }
    return response.data;
});

// ── Initiate Nagad Payment ────────────────────────────────────────────────────
exports.initiateNagadPayment = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

    const { amount, orderId } = data;

    const response = await axios.post(
        `${NAGAD_BASE_URL}/api/dfs/check-out/initialize/${NAGAD_MERCHANT_ID}/${orderId}`,
        {
            dateTime: new Date().toISOString(),
            sensitiveData: amount, // encrypted in production
            signature: "your-signature", // RSA signed in production
        }
    );
    return response.data;
});

// ── Initiate SSL Commerz Payment ──────────────────────────────────────────────
exports.initiateSSLCommerzPayment = functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");

    const {
        amount, orderId, customerName,
        customerEmail, customerPhone, customerAddress
    } = data;

    const params = new URLSearchParams({
        store_id: SSL_STORE_ID,
        store_passwd: SSL_STORE_PASSWD,
        total_amount: amount,
        currency: "BDT",
        tran_id: orderId,
        success_url: `https://your-app.web.app/ssl/success`,
        fail_url: `https://your-app.web.app/ssl/fail`,
        cancel_url: `https://your-app.web.app/ssl/cancel`,
        cus_name: customerName,
        cus_email: customerEmail,
        cus_phone: customerPhone,
        cus_add1: customerAddress,
        cus_city: "Dhaka",
        cus_country: "Bangladesh",
        shipping_method: "NO",
        product_name: "Bags",
        product_category: "Fashion",
        product_profile: "general",
    });

    const response = await axios.post(
        `${SSL_BASE_URL}/gwprocess/apissl.pl`,
        params.toString(),
        { headers: { "Content-Type": "application/x-www-form-urlencoded" } }
    );
    return response.data;
});