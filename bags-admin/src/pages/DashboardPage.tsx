import React, { useEffect, useState } from "react";
import {
  collection, getDocs, query, where,
  orderBy, limit, Timestamp,
} from "firebase/firestore";
import {
  AttachMoney, ShoppingBag, People,
  Inventory, TrendingUp, Warning,
} from "@mui/icons-material";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, BarChart, Bar, Legend,
} from "recharts";
import { db } from "../firebase/config";
import { Order, Product, Customer } from "../types";

const DashboardPage = () => {
  const [stats, setStats]             = useState<any>(null);
  const [revenueData, setRevenueData] = useState<any[]>([]);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [topProducts, setTopProducts] = useState<any[]>([]);
  const [loading, setLoading]         = useState(true);

  useEffect(() => { fetchDashboardData(); }, []);

  const fetchDashboardData = async () => {
    try {
      const [ordersSnap, productsSnap, customersSnap] = await Promise.all([
        getDocs(collection(db, "orders")),
        getDocs(collection(db, "products")),
        getDocs(collection(db, "users")),
      ]);

      const orders   = ordersSnap.docs.map(d => ({ id: d.id, ...d.data() } as Order));
      const products = productsSnap.docs.map(d => ({ id: d.id, ...d.data() } as Product));
      const customers = customersSnap.docs.length;

      const delivered = orders.filter(o => o.status === "DELIVERED");
      const totalRevenue = delivered.reduce((sum, o) => sum + o.totalAmount, 0);
      const pending = orders.filter(o => o.status === "PENDING").length;
      const lowStock = products.filter(p => p.stock < 10).length;

      setStats({
        totalRevenue,
        totalOrders: orders.length,
        totalCustomers: customers,
        totalProducts: products.length,
        pendingOrders: pending,
        lowStockProducts: lowStock,
      });

      // Revenue by last 7 days
      const last7 = Array.from({ length: 7 }, (_, i) => {
        const d = new Date();
        d.setDate(d.getDate() - (6 - i));
        const dayOrders = orders.filter(o => {
          const od = new Date(o.createdAt);
          return od.toDateString() === d.toDateString() && o.status === "DELIVERED";
        });
        return {
          day: d.toLocaleDateString("en", { weekday: "short" }),
          revenue: dayOrders.reduce((s, o) => s + o.totalAmount, 0),
          orders: dayOrders.length,
        };
      });
      setRevenueData(last7);

      // Recent orders
      const sorted = [...orders].sort((a, b) => b.createdAt - a.createdAt);
      setRecentOrders(sorted.slice(0, 5));

      // Top products by order count
      const productCount: Record<string, { name: string; count: number; revenue: number }> = {};
      orders.forEach(o => o.items?.forEach(item => {
        if (!productCount[item.productId]) {
          productCount[item.productId] = { name: item.productName, count: 0, revenue: 0 };
        }
        productCount[item.productId].count += item.quantity;
        productCount[item.productId].revenue += item.price * item.quantity;
      }));
      const top = Object.values(productCount)
        .sort((a, b) => b.count - a.count)
        .slice(0, 5);
      setTopProducts(top);

    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-amber-800" />
    </div>
  );

  return (
    <div className="space-y-6">
      {/* ── Stats Cards ───────────────────────────────────────────── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={<AttachMoney />}
          label="Total Revenue"
          value={`৳${stats?.totalRevenue?.toLocaleString("en-IN", { maximumFractionDigits: 0 })}`}
          color="bg-green-500"
          sub="From delivered orders"
        />
        <StatCard
          icon={<ShoppingBag />}
          label="Total Orders"
          value={stats?.totalOrders}
          color="bg-blue-500"
          sub={`${stats?.pendingOrders} pending`}
          subColor="text-yellow-600"
        />
        <StatCard
          icon={<People />}
          label="Customers"
          value={stats?.totalCustomers}
          color="bg-purple-500"
          sub="Registered users"
        />
        <StatCard
          icon={<Inventory />}
          label="Products"
          value={stats?.totalProducts}
          color="bg-amber-500"
          sub={`${stats?.lowStockProducts} low stock`}
          subColor="text-red-600"
        />
      </div>

      {/* ── Alerts ────────────────────────────────────────────────── */}
      {(stats?.pendingOrders > 0 || stats?.lowStockProducts > 0) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {stats?.pendingOrders > 0 && (
            <AlertCard
              icon={<ShoppingBag className="text-yellow-600" />}
              message={`${stats.pendingOrders} orders waiting for confirmation`}
              color="bg-yellow-50 border-yellow-200"
              linkTo="/orders"
            />
          )}
          {stats?.lowStockProducts > 0 && (
            <AlertCard
              icon={<Warning className="text-red-600" />}
              message={`${stats.lowStockProducts} products running low on stock`}
              color="bg-red-50 border-red-200"
              linkTo="/products"
            />
          )}
        </div>
      )}

      {/* ── Charts ────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Revenue Chart */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-gray-800 mb-4">Revenue (Last 7 Days)</h2>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={revenueData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="day" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v: any) => [`৳${v}`, "Revenue"]} />
              <Line
                type="monotone"
                dataKey="revenue"
                stroke="#92400e"
                strokeWidth={2}
                dot={{ fill: "#92400e" }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Orders Chart */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-gray-800 mb-4">Orders (Last 7 Days)</h2>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={revenueData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="day" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="orders" fill="#d97706" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ── Bottom Row ────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Orders */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <div className="flex justify-between items-center mb-4">
            <h2 className="font-semibold text-gray-800">Recent Orders</h2>
            <a href="/orders" className="text-amber-700 text-sm hover:underline">View all</a>
          </div>
          <div className="space-y-3">
            {recentOrders.map(order => (
              <div key={order.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                <div>
                  <p className="text-sm font-medium">#{order.id.slice(0, 8).toUpperCase()}</p>
                  <p className="text-xs text-gray-500">{order.items?.length} item(s)</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-amber-800">৳{order.totalAmount.toLocaleString()}</p>
                  <StatusBadge status={order.status} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Products */}
        <div className="bg-white rounded-2xl p-6 shadow-sm">
          <h2 className="font-semibold text-gray-800 mb-4">Top Products</h2>
          <div className="space-y-3">
            {topProducts.map((p, i) => (
              <div key={p.name} className="flex items-center gap-3">
                <span className="w-6 h-6 rounded-full bg-amber-100 text-amber-800 text-xs font-bold flex items-center justify-center">
                  {i + 1}
                </span>
                <div className="flex-1">
                  <p className="text-sm font-medium truncate">{p.name}</p>
                  <div className="w-full bg-gray-100 rounded-full h-1.5 mt-1">
                    <div
                      className="bg-amber-600 h-1.5 rounded-full"
                      style={{ width: `${(p.count / topProducts[0]?.count) * 100}%` }}
                    />
                  </div>
                </div>
                <span className="text-sm font-semibold text-gray-600">{p.count} sold</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

// ── Helpers ───────────────────────────────────────────────────────────────────
const StatCard = ({ icon, label, value, color, sub, subColor = "text-gray-400" }: any) => (
  <div className="bg-white rounded-2xl p-5 shadow-sm">
    <div className="flex items-center justify-between mb-3">
      <p className="text-sm text-gray-500">{label}</p>
      <div className={`${color} text-white p-2 rounded-xl`}>{icon}</div>
    </div>
    <p className="text-2xl font-bold text-gray-800">{value}</p>
    <p className={`text-xs mt-1 ${subColor}`}>{sub}</p>
  </div>
);

const AlertCard = ({ icon, message, color, linkTo }: any) => (

    href={linkTo}
    className={`flex items-center gap-3 p-4 rounded-xl border ${color} hover:opacity-80 transition`}
  >
    {icon}
    <p className="text-sm font-medium text-gray-700">{message}</p>
  </a>
);

const StatusBadge = ({ status }: { status: string }) => {
  const styles: Record<string, string> = {
    PENDING:    "bg-yellow-100 text-yellow-700",
    CONFIRMED:  "bg-blue-100 text-blue-700",
    PROCESSING: "bg-purple-100 text-purple-700",
    SHIPPED:    "bg-cyan-100 text-cyan-700",
    DELIVERED:  "bg-green-100 text-green-700",
    CANCELLED:  "bg-red-100 text-red-700",
    REFUNDED:   "bg-gray-100 text-gray-700",
  };
  return (
    <span className={`text-xs px-2 py-1 rounded-full font-medium ${styles[status] || styles.PENDING}`}>
      {status}
    </span>
  );
};

export default DashboardPage;