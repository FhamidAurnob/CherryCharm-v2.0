import React, { useState } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { TrendingUp } from "lucide-react";

interface RevenueDataPoint {
  month: string;
  revenue: number;
  orders: number;
}

interface RevenueChartProps {
  data: RevenueDataPoint[];
  loading?: boolean;
}

type ViewMode = "revenue" | "orders";

const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    const isRevenue = payload[0].dataKey === "revenue";
    return (
      <div className="bg-gray-900 text-white px-3 py-2 rounded-xl shadow-xl text-xs">
        <p className="font-semibold mb-1">{label}</p>
        <p className="text-amber-300">
          {isRevenue
            ? `৳${payload[0].value.toLocaleString()}`
            : `${payload[0].value} orders`}
        </p>
      </div>
    );
  }
  return null;
};

const RevenueChart: React.FC<RevenueChartProps> = ({ data, loading }) => {
  const [view, setView] = useState<ViewMode>("revenue");

  const totalRevenue = data.reduce((sum, d) => sum + d.revenue, 0);
  const totalOrders = data.reduce((sum, d) => sum + d.orders, 0);

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <TrendingUp className="w-4 h-4 text-amber-800" />
            <h3 className="font-semibold text-gray-800">Revenue Overview</h3>
          </div>
          <p className="text-2xl font-bold text-gray-900">
            {view === "revenue"
              ? `৳${totalRevenue.toLocaleString()}`
              : `${totalOrders.toLocaleString()} orders`}
          </p>
          <p className="text-xs text-gray-400 mt-0.5">Last 6 months total</p>
        </div>

        {/* Toggle */}
        <div className="flex bg-gray-100 rounded-xl p-1 gap-1">
          {(["revenue", "orders"] as ViewMode[]).map((v) => (
            <button
              key={v}
              onClick={() => setView(v)}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all ${
                view === v
                  ? "bg-white text-amber-900 shadow-sm"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              {v.charAt(0).toUpperCase() + v.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Chart */}
      {loading ? (
        <div className="h-48 flex items-center justify-center">
          <div className="animate-pulse w-full h-full bg-gray-50 rounded-xl" />
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={200}>
          <AreaChart data={data} margin={{ top: 5, right: 5, left: -20, bottom: 0 }}>
            <defs>
              <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#92400e" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#92400e" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="colorOrders" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#b45309" stopOpacity={0.15} />
                <stop offset="95%" stopColor="#b45309" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
            <XAxis
              dataKey="month"
              tick={{ fontSize: 11, fill: "#9ca3af" }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tick={{ fontSize: 11, fill: "#9ca3af" }}
              axisLine={false}
              tickLine={false}
              tickFormatter={(v) =>
                view === "revenue"
                  ? v >= 1000 ? `৳${(v / 1000).toFixed(0)}k` : `৳${v}`
                  : `${v}`
              }
            />
            <Tooltip content={<CustomTooltip />} />
            <Area
              type="monotone"
              dataKey={view}
              stroke="#92400e"
              strokeWidth={2.5}
              fill={`url(#color${view.charAt(0).toUpperCase() + view.slice(1)})`}
              dot={{ fill: "#92400e", strokeWidth: 0, r: 3 }}
              activeDot={{ r: 5, fill: "#92400e" }}
            />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </div>
  );
};

export default RevenueChart;
