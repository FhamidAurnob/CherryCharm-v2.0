import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import {
  ArrowBack, LocalShipping, CheckCircle,
  Cancel, Print, Person, LocationOn, Payment,
} from "@mui/icons-material";
import { db } from "../firebase/config";
import { Order, OrderStatus } from "../types";
import { toDate } from "../types";
import toast from "react-hot-toast";

const STATUS_FLOW: OrderStatus[] = [
  "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED",
];

const STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING:    "bg-amber-50 text-amber-700 border border-amber-200",
  CONFIRMED:  "bg-blue-50 text-blue-700 border border-blue-200",
  PROCESSING: "bg-purple-50 text-purple-700 border border-purple-200",
  SHIPPED:    "bg-cyan-50 text-cyan-700 border border-cyan-200",
  DELIVERED:  "bg-emerald-50 text-emerald-700 border border-emerald-200",
  CANCELLED:  "bg-red-50 text-red-600 border border-red-200",
  REFUNDED:   "bg-gray-100 text-gray-600 border border-gray-200",
};

const statusOptions: OrderStatus[] = [
  "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED",
];

const OrderDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    if (id) fetchOrder(id);
  }, [id]);

  const fetchOrder = async (orderId: string) => {
    setLoading(true);
    try {
      const snap = await getDoc(doc(db, "orders", orderId));
      if (snap.exists()) {
        setOrder({ id: snap.id, ...snap.data() } as Order);
      } else {
        toast.error("Order not found");
        navigate("/orders");
      }
    } catch {
      toast.error("Failed to load order");
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (status: OrderStatus) => {
    if (!order) return;
    setUpdating(true);
    try {
      await updateDoc(doc(db, "orders", order.id), {
        status,
        updatedAt: Date.now(),
      });
      setOrder(prev => prev ? { ...prev, status } : prev);
      toast.success(`Status updated to ${status}`);
    } catch {
      toast.error("Failed to update status");
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-amber-800" />
      </div>
    );
  }

  if (!order) return null;

  const isCancelled = order.status === "CANCELLED" || order.status === "REFUNDED";
  const currentStep = STATUS_FLOW.indexOf(order.status as OrderStatus);
  const orderDate = toDate(order.createdAt);

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate("/orders")}
            className="p-2 hover:bg-gray-100 rounded-xl transition"
          >
            <ArrowBack />
          </button>
          <div>
            <h2 className="text-xl font-bold text-gray-800">
              Order #{order.id.slice(0, 8).toUpperCase()}
            </h2>
            <p className="text-sm text-gray-500">
              Placed on {orderDate.toLocaleDateString("en-BD", {
                day: "numeric", month: "long", year: "numeric",
                hour: "2-digit", minute: "2-digit",
              })}
            </p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => window.print()}
            className="flex items-center gap-1 px-3 py-2 border border-gray-200 rounded-xl text-sm text-gray-600 hover:bg-gray-50 transition"
          >
            <Print fontSize="small" /> Print
          </button>
          {!isCancelled && order.status !== "DELIVERED" && (
            <button
              onClick={() => updateStatus("CANCELLED")}
              disabled={updating}
              className="flex items-center gap-1 px-3 py-2 bg-red-50 text-red-600 border border-red-200 rounded-xl text-sm hover:bg-red-100 transition disabled:opacity-50"
            >
              <Cancel fontSize="small" /> Cancel Order
            </button>
          )}
        </div>
      </div>

      {/* Status Tracker */}
      {!isCancelled && (
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <div className="flex items-center justify-between relative">
            {/* Progress line */}
            <div className="absolute top-5 left-0 right-0 h-0.5 bg-gray-100 mx-10 z-0" />
            <div
              className="absolute top-5 left-0 h-0.5 bg-amber-700 mx-10 z-0 transition-all duration-500"
              style={{
                width: currentStep >= 0
                  ? `${(currentStep / (STATUS_FLOW.length - 1)) * (100 - 20)}%`
                  : "0%",
              }}
            />

            {STATUS_FLOW.map((status, i) => {
              const isCompleted = i < currentStep;
              const isCurrent = i === currentStep;
              return (
                <div key={status} className="flex flex-col items-center gap-2 z-10">
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center transition-all ${
                      isCompleted
                        ? "bg-amber-700 text-white"
                        : isCurrent
                        ? "bg-amber-700 text-white ring-4 ring-amber-100"
                        : "bg-gray-100 text-gray-400"
                    }`}
                  >
                    {isCompleted ? (
                      <CheckCircle style={{ fontSize: 20 }} />
                    ) : (
                      <span className="text-xs font-bold">{i + 1}</span>
                    )}
                  </div>
                  <span className={`text-xs font-medium text-center max-w-16 leading-tight ${
                    isCurrent ? "text-amber-800" : isCompleted ? "text-gray-700" : "text-gray-400"
                  }`}>
                    {status.charAt(0) + status.slice(1).toLowerCase()}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Cancelled / Refunded Banner */}
      {isCancelled && (
        <div className="bg-red-50 border border-red-200 rounded-2xl p-4 flex items-center gap-3">
          <Cancel className="text-red-500" />
          <div>
            <p className="font-semibold text-red-700">
              Order {order.status === "REFUNDED" ? "Refunded" : "Cancelled"}
            </p>
            <p className="text-sm text-red-500">
              This order has been {order.status.toLowerCase()}.
            </p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left — Items + Status */}
        <div className="lg:col-span-2 space-y-4">

          {/* Order Items */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-50">
              <h3 className="font-semibold text-gray-800">
                Order Items ({order.items?.length ?? 0})
              </h3>
            </div>
            <div className="divide-y divide-gray-50">
              {order.items?.map((item, i) => (
                <div key={i} className="flex items-center gap-4 px-6 py-4">
                  <div className="w-14 h-14 rounded-xl overflow-hidden bg-amber-50 flex-shrink-0">
                    {item.productImage ? (
                      <img
                        src={item.productImage}
                        alt={item.productName}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-xl">👜</div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-800 truncate">{item.productName}</p>
                    <div className="flex items-center gap-2 mt-0.5">
                      {item.color && (
                        <span className="text-xs text-gray-400">Color: {item.color}</span>
                      )}
                      {item.size && (
                        <span className="text-xs text-gray-400">· Size: {item.size}</span>
                      )}
                    </div>
                    <p className="text-xs text-gray-400 mt-0.5">Qty: {item.quantity}</p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="font-semibold text-gray-800">
                      ৳{(item.price * item.quantity).toLocaleString()}
                    </p>
                    <p className="text-xs text-gray-400">৳{item.price.toLocaleString()} each</p>
                  </div>
                </div>
              ))}
            </div>

            {/* Totals */}
            <div className="px-6 py-4 bg-gray-50 space-y-2">
              <div className="flex justify-between text-sm text-gray-600">
                <span>Subtotal</span>
                <span>৳{(order.totalAmount - order.deliveryFee + order.discountAmount).toLocaleString()}</span>
              </div>
              {order.discountAmount > 0 && (
                <div className="flex justify-between text-sm text-emerald-600">
                  <span>Discount {order.promoCode && `(${order.promoCode})`}</span>
                  <span>-৳{order.discountAmount.toLocaleString()}</span>
                </div>
              )}
              <div className="flex justify-between text-sm text-gray-600">
                <span>Delivery Fee</span>
                <span>
                  {order.deliveryFee === 0
                    ? <span className="text-emerald-600">Free</span>
                    : `৳${order.deliveryFee.toLocaleString()}`}
                </span>
              </div>
              <div className="flex justify-between font-bold text-gray-800 text-base pt-2 border-t border-gray-200">
                <span>Total</span>
                <span className="text-amber-800">৳{order.totalAmount.toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* Update Status */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <LocalShipping className="text-amber-700" fontSize="small" />
              Update Status
            </h3>
            <div className="flex flex-wrap gap-2">
              {statusOptions.map(status => (
                <button
                  key={status}
                  onClick={() => updateStatus(status)}
                  disabled={updating || order.status === status}
                  className={`px-3 py-1.5 rounded-xl text-xs font-medium transition disabled:cursor-not-allowed ${
                    order.status === status
                      ? STATUS_STYLES[status] + " cursor-default"
                      : "bg-gray-100 text-gray-600 hover:bg-gray-200 disabled:opacity-40"
                  }`}
                >
                  {order.status === status && "✓ "}{status}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Right — Customer, Address, Payment */}
        <div className="space-y-4">
          {/* Customer */}
          <div className="bg-white rounded-2xl shadow-sm p-5">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <Person className="text-amber-700" fontSize="small" />
              Customer
            </h3>
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center text-amber-800 font-bold flex-shrink-0">
                {order.address?.fullName?.[0]?.toUpperCase() ?? "?"}
              </div>
              <div>
                <p className="font-medium text-gray-800">{order.address?.fullName ?? "—"}</p>
                <p className="text-xs text-gray-400">{order.address?.phone ?? "—"}</p>
              </div>
            </div>
          </div>

          {/* Delivery Address */}
          <div className="bg-white rounded-2xl shadow-sm p-5">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <LocationOn className="text-amber-700" fontSize="small" />
              Delivery Address
            </h3>
            {order.address ? (
              <div className="text-sm text-gray-600 space-y-1">
                {order.address.label && (
                  <span className="text-xs bg-amber-50 text-amber-700 px-2 py-0.5 rounded-full font-medium">
                    {order.address.label}
                  </span>
                )}
                <p className="mt-1">{order.address.line1}</p>
                {order.address.line2 && <p>{order.address.line2}</p>}
                <p>{order.address.city}, {order.address.district}</p>
                <p>{order.address.postCode}</p>
              </div>
            ) : (
              <p className="text-sm text-gray-400">No address info</p>
            )}
          </div>

          {/* Payment */}
          <div className="bg-white rounded-2xl shadow-sm p-5">
            <h3 className="font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <Payment className="text-amber-700" fontSize="small" />
              Payment
            </h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Method</span>
                <span className="font-medium text-gray-800">{order.paymentMethod || "—"}</span>
              </div>
              {order.paymentId && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Transaction ID</span>
                  <span className="font-mono text-xs text-gray-600">{order.paymentId}</span>
                </div>
              )}
              {order.promoCode && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Promo Code</span>
                  <span className="font-mono text-xs bg-amber-50 text-amber-700 px-2 py-0.5 rounded">
                    {order.promoCode}
                  </span>
                </div>
              )}
              <div className="flex justify-between pt-2 border-t border-gray-50">
                <span className="text-gray-500">Total Paid</span>
                <span className="font-bold text-amber-800">৳{order.totalAmount.toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* Notes */}
          {order.notes && (
            <div className="bg-amber-50 rounded-2xl p-5">
              <p className="text-xs font-semibold text-amber-700 mb-1">Customer Note</p>
              <p className="text-sm text-amber-900">{order.notes}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default OrderDetailPage;
