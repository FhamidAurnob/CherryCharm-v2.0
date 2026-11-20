import React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, Package } from "lucide-react";

interface Order {
  id: string;
  customerName: string;
  customerEmail: string;
  total: number;
  status: "pending" | "confirmed" | "shipped" | "delivered" | "cancelled";
  createdAt: Date;
  itemCount: number;
}

interface RecentOrdersProps {
  orders: Order[];
  loading?: boolean;
}

const STATUS_STYLES: Record<Order["status"], { label: string; classes: string }> = {
  pending:   { label: "Pending",   classes: "bg-amber-50 text-amber-700 border border-amber-200" },
  confirmed: { label: "Confirmed", classes: "bg-blue-50 text-blue-700 border border-blue-200" },
  shipped:   { label: "Shipped",   classes: "bg-purple-50 text-purple-700 border border-purple-200" },
  delivered: { label: "Delivered", classes: "bg-emerald-50 text-emerald-700 border border-emerald-200" },
  cancelled: { label: "Cancelled", classes: "bg-red-50 text-red-600 border border-red-200" },
};

const RecentOrders: React.FC<RecentOrdersProps> = ({ orders, loading }) => {
  const navigate = useNavigate();

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-gray-50">
        <div className="flex items-center gap-2">
          <Package className="w-4 h-4 text-amber-800" />
          <h3 className="font-semibold text-gray-800">Recent Orders</h3>
        </div>
        <button
          onClick={() => navigate("/orders")}
          className="flex items-center gap-1 text-xs font-medium text-amber-800 hover:text-amber-900 transition-colors"
        >
          View all <ArrowRight className="w-3 h-3" />
        </button>
      </div>

      {/* Table */}
      {loading ? (
        <div className="divide-y divide-gray-50">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-6 py-4 animate-pulse">
              <div className="w-8 h-8 bg-gray-100 rounded-full" />
              <div className="flex-1 space-y-2">
                <div className="h-3 bg-gray-100 rounded w-1/3" />
                <div className="h-2 bg-gray-50 rounded w-1/4" />
              </div>
              <div className="h-5 bg-gray-100 rounded-full w-16" />
              <div className="h-3 bg-gray-100 rounded w-12" />
            </div>
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <Package className="w-8 h-8 mb-2 opacity-40" />
          <p className="text-sm">No orders yet</p>
        </div>
      ) : (
        <div className="divide-y divide-gray-50">
          {orders.map((order) => {
            const style = STATUS_STYLES[order.status];
            const initials = order.customerName
              .split(" ")
              .map((n) => n[0])
              .join("")
              .toUpperCase()
              .slice(0, 2);

            return (
              <div
                key={order.id}
                onClick={() => navigate(`/orders/${order.id}`)}
                className="flex items-center gap-4 px-6 py-3.5 hover:bg-gray-50 cursor-pointer transition-colors group"
              >
                {/* Avatar */}
                <div className="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center text-xs font-bold text-amber-800 flex-shrink-0">
                  {initials}
                </div>

                {/* Customer & order info */}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate group-hover:text-amber-900 transition-colors">
                    {order.customerName}
                  </p>
                  <p className="text-xs text-gray-400 truncate">
                    #{order.id.slice(-6).toUpperCase()} · {order.itemCount} item{order.itemCount !== 1 ? "s" : ""}
                  </p>
                </div>

                {/* Status badge */}
                <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full flex-shrink-0 ${style.classes}`}>
                  {style.label}
                </span>

                {/* Amount */}
                <p className="text-sm font-semibold text-gray-700 flex-shrink-0 w-20 text-right">
                  ৳{order.total.toLocaleString()}
                </p>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default RecentOrders;
