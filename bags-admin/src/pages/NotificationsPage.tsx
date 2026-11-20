import React, { useEffect, useState } from "react";
import {
  collection, getDocs, addDoc, doc, updateDoc,
} from "firebase/firestore";
import {
  Send, Notifications, People, ShoppingBag,
  LocalOffer, NewReleases, History,
} from "@mui/icons-material";
import { db } from "../firebase/config";
import toast from "react-hot-toast";

interface NotifForm {
  title: string;
  body: string;
  type: string;
  target: "all" | "topic" | "user";
  topic: string;
  userId: string;
  deepLink: string;
  data: Record<string, string>;
}

const EMPTY_FORM: NotifForm = {
  title: "", body: "", type: "general",
  target: "all", topic: "all_users",
  userId: "", deepLink: "", data: {},
};

const notifTemplates = [
  {
    label: "Order Shipped",
    icon: <ShoppingBag />,
    color: "bg-blue-50 text-blue-700",
    form: {
      title: "Your Order is on the Way! 🚚",
      body: "Great news! Your order has been shipped and will arrive soon.",
      type: "order",
      deepLink: "bagsapp://orders",
    },
  },
  {
    label: "Flash Sale",
    icon: <LocalOffer />,
    color: "bg-yellow-50 text-yellow-700",
    form: {
      title: "⚡ Flash Sale — Up to 50% Off!",
      body: "Hurry! Limited time offer on selected bags. Shop now before it's gone!",
      type: "promo",
      deepLink: "bagsapp://home",
    },
  },
  {
    label: "New Arrivals",
    icon: <NewReleases />,
    color: "bg-purple-50 text-purple-700",
    form: {
      title: "✨ New Collection is Here!",
      body: "Discover our latest bag collection. Fresh styles just dropped!",
      type: "new_arrival",
      deepLink: "bagsapp://home",
    },
  },
  {
    label: "Price Drop",
    icon: <Notifications />,
    color: "bg-green-50 text-green-700",
    form: {
      title: "💰 Price Drop Alert!",
      body: "An item in your wishlist just got cheaper. Grab it now!",
      type: "price_drop",
      deepLink: "bagsapp://wishlist",
    },
  },
];

const NotificationsPage = () => {
  const [form, setForm]         = useState<NotifForm>(EMPTY_FORM);
  const [sending, setSending]   = useState(false);
  const [history, setHistory]   = useState<any[]>([]);
  const [customers, setCustomers] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<"compose" | "history">("compose");

  useEffect(() => {
    fetchHistory();
    fetchCustomers();
  }, []);

  const fetchHistory = async () => {
    const snap = await getDocs(collection(db, "notificationHistory"));
    const list = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    setHistory(list.sort((a: any, b: any) => b.sentAt - a.sentAt));
  };

  const fetchCustomers = async () => {
    const snap = await getDocs(collection(db, "users"));
    setCustomers(snap.docs.map(d => ({ id: d.id, ...d.data() })));
  };

  const applyTemplate = (template: any) => {
    setForm(prev => ({ ...prev, ...template.form }));
  };

  const sendNotification = async () => {
    if (!form.title || !form.body) {
      toast.error("Title and message are required");
      return;
    }
    setSending(true);
    try {
      // Save to notificationHistory
      await addDoc(collection(db, "notificationHistory"), {
        ...form,
        sentAt: Date.now(),
        sentBy: "admin",
        status: "sent",
      });

      // If targeting specific user — save to their notifications subcollection
      if (form.target === "user" && form.userId) {
        const newNotifRef = doc(
          collection(db, "users", form.userId, "notifications")
        );
        await updateDoc(newNotifRef, {
          id: newNotifRef.id,
          title: form.title,
          body: form.body,
          type: form.type,
          deepLink: form.deepLink,
          isRead: false,
          createdAt: Date.now(),
        }).catch(async () => {
          // Document might not exist, use setDoc instead
          const { setDoc } = await import("firebase/firestore");
          await setDoc(newNotifRef, {
            id: newNotifRef.id,
            title: form.title,
            body: form.body,
            type: form.type,
            deepLink: form.deepLink,
            isRead: false,
            createdAt: Date.now(),
          });
        });
      }

      // If targeting all — save to all users' notifications
      if (form.target === "all") {
        const batch = (await import("firebase/firestore")).writeBatch(db);
        customers.forEach(customer => {
          const ref = doc(
            collection(db, "users", customer.id, "notifications")
          );
          batch.set(ref, {
            id: ref.id,
            title: form.title,
            body: form.body,
            type: form.type,
            deepLink: form.deepLink,
            isRead: false,
            createdAt: Date.now(),
          });
        });
        await batch.commit();
      }

      toast.success(`Notification sent to ${
        form.target === "all"
          ? `${customers.length} users`
          : form.target === "user"
          ? "1 user"
          : `topic: ${form.topic}`
      }!`);

      setForm(EMPTY_FORM);
      fetchHistory();
    } catch (e) {
      console.error(e);
      toast.error("Failed to send notification");
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Push Notifications</h2>
          <p className="text-sm text-gray-500">Send notifications to your customers</p>
        </div>
        <div className="flex gap-2">
          {(["compose", "history"] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-2 rounded-xl text-sm font-medium capitalize transition ${
                activeTab === tab
                  ? "bg-amber-800 text-white"
                  : "bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              {tab === "compose" ? "✏️ Compose" : "📋 History"}
            </button>
          ))}
        </div>
      </div>

      {activeTab === "compose" ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left — Form */}
          <div className="lg:col-span-2 space-y-4">
            {/* Templates */}
            <div className="bg-white rounded-2xl p-5 shadow-sm">
              <p className="font-semibold text-gray-700 mb-3">Quick Templates</p>
              <div className="grid grid-cols-2 gap-3">
                {notifTemplates.map(t => (
                  <button
                    key={t.label}
                    onClick={() => applyTemplate(t)}
                    className={`flex items-center gap-2 p-3 rounded-xl border ${t.color} hover:opacity-80 transition text-left`}
                  >
                    {t.icon}
                    <span className="text-sm font-medium">{t.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Compose Form */}
            <div className="bg-white rounded-2xl p-5 shadow-sm space-y-4">
              <p className="font-semibold text-gray-700">Compose Notification</p>

              {/* Target */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">Send To</label>
                <div className="flex gap-2">
                  {(["all", "topic", "user"] as const).map(t => (
                    <button
                      key={t}
                      onClick={() => setForm(p => ({ ...p, target: t }))}
                      className={`px-3 py-1.5 rounded-lg text-sm font-medium transition capitalize ${
                        form.target === t
                          ? "bg-amber-800 text-white"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {t === "all" ? `👥 All Users (${customers.length})` : t === "topic" ? "📌 Topic" : "👤 User"}
                    </button>
                  ))}
                </div>
              </div>

              {/* Topic Selector */}
              {form.target === "topic" && (
                <div>
                  <label className="text-xs font-medium text-gray-600 block mb-1">Topic</label>
                  <select
                    className="input-field"
                    value={form.topic}
                    onChange={e => setForm(p => ({ ...p, topic: e.target.value }))}
                  >
                    <option value="all_users">All Users</option>
                    <option value="order_updates">Order Updates Subscribers</option>
                    <option value="promotions">Promotions Subscribers</option>
                    <option value="new_arrivals">New Arrivals Subscribers</option>
                    <option value="price_drops">Price Drop Subscribers</option>
                  </select>
                </div>
              )}

              {/* User Selector */}
              {form.target === "user" && (
                <div>
                  <label className="text-xs font-medium text-gray-600 block mb-1">Select User</label>
                  <select
                    className="input-field"
                    value={form.userId}
                    onChange={e => setForm(p => ({ ...p, userId: e.target.value }))}
                  >
                    <option value="">-- Select a customer --</option>
                    {customers.map(c => (
                      <option key={c.id} value={c.id}>
                        {c.name || "No Name"} — {c.email}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Notification Type */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">Type</label>
                <select
                  className="input-field"
                  value={form.type}
                  onChange={e => setForm(p => ({ ...p, type: e.target.value }))}
                >
                  <option value="general">General</option>
                  <option value="order">Order Update</option>
                  <option value="promo">Promotion</option>
                  <option value="price_drop">Price Drop</option>
                  <option value="new_arrival">New Arrival</option>
                </select>
              </div>

              {/* Title */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">
                  Title * <span className="text-gray-400">({form.title.length}/65)</span>
                </label>
                <input
                  className="input-field"
                  value={form.title}
                  maxLength={65}
                  onChange={e => setForm(p => ({ ...p, title: e.target.value }))}
                  placeholder="Notification title..."
                />
              </div>

              {/* Body */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">
                  Message * <span className="text-gray-400">({form.body.length}/200)</span>
                </label>
                <textarea
                  className="input-field h-24 resize-none"
                  value={form.body}
                  maxLength={200}
                  onChange={e => setForm(p => ({ ...p, body: e.target.value }))}
                  placeholder="Write your notification message..."
                />
              </div>

              {/* Deep Link */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">Deep Link (optional)</label>
                <input
                  className="input-field"
                  value={form.deepLink}
                  onChange={e => setForm(p => ({ ...p, deepLink: e.target.value }))}
                  placeholder="e.g. bagsapp://orders or bagsapp://product/id"
                />
                <p className="text-xs text-gray-400 mt-1">
                  Valid links: bagsapp://home, bagsapp://orders, bagsapp://wishlist, bagsapp://product/&#123;id&#125;
                </p>
              </div>

              {/* Send Button */}
              <button
                onClick={sendNotification}
                disabled={sending || !form.title || !form.body}
                className="w-full flex items-center justify-center gap-2 bg-amber-800 text-white py-3 rounded-xl hover:bg-amber-700 disabled:opacity-50 transition font-semibold"
              >
                {sending ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
                    Sending...
                  </>
                ) : (
                  <>
                    <Send fontSize="small" />
                    Send Notification
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Right — Preview */}
          <div>
            <div className="bg-white rounded-2xl p-5 shadow-sm">
              <p className="font-semibold text-gray-700 mb-4">📱 Preview</p>
              {/* Phone mockup */}
              <div className="border-2 border-gray-800 rounded-3xl p-3 bg-gray-900 mx-auto w-56">
                <div className="bg-white rounded-2xl p-3 min-h-48">
                  {/* Status bar */}
                  <div className="flex justify-between text-xs text-gray-400 mb-3">
                    <span>9:41</span>
                    <span>●●●</span>
                  </div>
                  {/* Notification bubble */}
                  <div className="bg-gray-100 rounded-xl p-3 shadow-sm">
                    <div className="flex items-center gap-2 mb-1">
                      <div className="w-5 h-5 bg-amber-800 rounded-md flex items-center justify-center">
                        <span className="text-white text-xs">👜</span>
                      </div>
                      <span className="text-xs font-semibold text-gray-600">BagsApp</span>
                      <span className="text-xs text-gray-400 ml-auto">now</span>
                    </div>
                    <p className="text-xs font-semibold text-gray-800 mb-0.5">
                      {form.title || "Notification Title"}
                    </p>
                    <p className="text-xs text-gray-500 line-clamp-2">
                      {form.body || "Your notification message will appear here..."}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* History Tab */
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
          <div className="p-4 border-b flex items-center gap-2">
            <History className="text-gray-400" fontSize="small" />
            <p className="font-semibold text-gray-700">Sent Notifications</p>
          </div>
          {history.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400">
              <Notifications fontSize="large" />
              <p className="mt-2">No notifications sent yet</p>
            </div>
          ) : (
            <div className="divide-y">
              {history.map((item: any) => (
                <div key={item.id} className="p-4 hover:bg-gray-50 transition">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                          item.type === "order"      ? "bg-blue-100 text-blue-700"   :
                          item.type === "promo"      ? "bg-yellow-100 text-yellow-700" :
                          item.type === "price_drop" ? "bg-green-100 text-green-700"  :
                          "bg-gray-100 text-gray-600"
                        }`}>
                          {item.type}
                        </span>
                        <span className="text-xs text-gray-400">
                          → {item.target === "all" ? "All Users" : item.target === "topic" ? item.topic : "1 User"}
                        </span>
                      </div>
                      <p className="font-semibold text-sm text-gray-800">{item.title}</p>
                      <p className="text-sm text-gray-500 mt-0.5">{item.body}</p>
                    </div>
                    <p className="text-xs text-gray-400 ml-4 whitespace-nowrap">
                      {new Date(item.sentAt).toLocaleDateString("en-IN", {
                        day: "numeric", month: "short", hour: "2-digit", minute: "2-digit"
                      })}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationsPage;