import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { collection, getDocs, doc, updateDoc } from "firebase/firestore";
import { Search, FilterList } from "@mui/icons-material";
import { db } from "../firebase/config";
import { Order, OrderStatus } from "../types";
import toast from "react-hot-toast";

const statusOptions: OrderStatus[] = [
  "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED",
];

const statusColors: Record<OrderStatus, string> = {
  PENDING:    "bg-yellow-100 text-yellow-700",
  CONFIRMED:  "bg-blue-100 text-blue-700",
  PROCESSING: "bg-purple-100 text-purple-700",
  SHIPPED:    "bg-cyan-100 text-cyan-700",
  DELIVERED:  "bg-green-100 text-green-700",
  CANCELLED:  "bg-red-100 text-red-700",
  REFUNDED:   "bg-gray-100 text-gray-600",
};

const OrdersPage = () => {
  const [orders, setOrders]           = useState<Order[]>([]);
  const [filtered, setFiltered]       = useState<Order[]>([]);
  const [loading, setLoading]         = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<OrderStatus | "ALL">("ALL");
  const navigate = useNavigate();

  useEffect(() => { fetchOrders(); }, []);

  useEffect(() => {
    let list = [...orders];
    if (searchQuery)
      list = list.filter(o =>
        o.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
        o.address?.fullName?.toLowerCase().includes(searchQuery.toLowerCase())
      );
    if (statusFilter !== "ALL")
      list = list.filter(o => o.status === statusFilter);
    setFiltered(list);
  }, [orders, searchQuery, statusFilter]);

  const fetchOrders = async () => {
    setLoading(true);
    const snap = await getDocs(collection(db, "orders"));
    const list = snap.docs.map(d => ({ id: d.id, ...d.data() } as Order));
    setOrders(list.sort((a, b) => b.createdAt - a.createdAt));
    setLoading(false);
  };

  const updateStatus = async (orderId: string, status: OrderStatus) => {
    await updateDoc(doc(db, "orders", orderId), {
      status, updatedAt: Date.now(),
    });
    toast.success(`Order status updated to ${status}`);
    fetchOrders();
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Orders</h2>
          <p className="text-sm text-gray-500">{filtered.length} orders found</p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-2xl p-4 shadow-sm space-y-3">
        <div className="flex items-center gap-2 bg-gray-100 rounded-xl px-3 py-2">
          <Search fontSize="small" className="text-gray-400" />
          <input
            className="bg-transparent text-sm outline-none flex-1"
            placeholder="Search by order ID or customer name..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="flex gap-2 flex-wrap">
          {["ALL", ...statusOptions].map(s => (
            <button
              key={s}
              onClick={() => setStatusFilter(s as any)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                statusFilter === s
                  ? "bg-amber-800 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-800" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {["Order ID", "Customer", "Items", "Total", "Payment", "Status", "Date", "Actions"].map(h => (
                    <th key={h} className="text-left px-4 py-3 text-gray-500 font-semibold text-xs">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(order => (
                  <tr key={order.id} className="hover:bg-gray-50 transition">
                    <td className="px-4 py-3 font-mono text-xs text-gray-600">
                      #{order.id.slice(0, 8).toUpperCase()}
                    </td>
                    <td className="px-4 py-3">
                      <p className="font-medium text-gray-800">{order.address?.fullName}</p>
                      <p className="text-xs text-gray-400">{order.address?.city}</p>
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {order.items?.length ?? 0} item(s)
                    </td>
                    <td className="px-4 py-3 font-semibold text-amber-800">
                      ৳{order.totalAmount?.toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500">
                      {order.paymentMethod}
                    </td>
                    <td className="px-4 py-3">
                      <select
                        value={order.status}
                        onChange={e => updateStatus(order.id, e.target.value as OrderStatus)}
                        className={`text-xs px-2 py-1 rounded-full font-medium border-none outline-none cursor-pointer ${statusColors[order.status]}`}
                      >
                        {statusOptions.map(s => (
                          <option key={s} value={s}>{s}</option>
                        ))}
                      </select>
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-400">
                      {new Date(order.createdAt).toLocaleDateString("en-IN")}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => navigate(`/orders/${order.id}`)}
                        className="text-xs text-amber-700 hover:underline font-medium"
                      >
                        View →
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default OrdersPage;