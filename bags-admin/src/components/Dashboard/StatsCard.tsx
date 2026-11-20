import React from "react";
import { LucideIcon, TrendingUp, TrendingDown } from "lucide-react";

interface StatsCardProps {
  title: string;
  value: string;
  change: number; // percentage, positive = up, negative = down
  icon: LucideIcon;
  iconColor: string;
  iconBg: string;
  prefix?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  change,
  icon: Icon,
  iconColor,
  iconBg,
  prefix = "",
}) => {
  const isPositive = change >= 0;

  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100 hover:shadow-md transition-shadow duration-200">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1">
            {title}
          </p>
          <p className="text-2xl font-bold text-gray-800 mt-1">
            {prefix}{value}
          </p>
        </div>
        <div className={`${iconBg} p-3 rounded-xl`}>
          <Icon className={`w-5 h-5 ${iconColor}`} />
        </div>
      </div>

      <div className="flex items-center mt-4 pt-4 border-t border-gray-50">
        <div
          className={`flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full ${
            isPositive
              ? "text-emerald-700 bg-emerald-50"
              : "text-red-600 bg-red-50"
          }`}
        >
          {isPositive ? (
            <TrendingUp className="w-3 h-3" />
          ) : (
            <TrendingDown className="w-3 h-3" />
          )}
          {isPositive ? "+" : ""}{change}%
        </div>
        <span className="text-xs text-gray-400 ml-2">vs last month</span>
      </div>
    </div>
  );
};

export default StatsCard;
