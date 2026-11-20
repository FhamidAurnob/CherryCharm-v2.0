import React from "react";

type OrderStatus = "pending" | "confirmed" | "shipped" | "delivered" | "cancelled";
type ProductStatus = "active" | "inactive" | "out_of_stock";
type GeneralStatus = OrderStatus | ProductStatus | string;

interface StatusBadgeProps {
  status: GeneralStatus;
  size?: "sm" | "md";
}

const STATUS_MAP: Record<string, { label: string; classes: string; dot: string }> = {
  // Order statuses
  pending: {
    label: "Pending",
    classes: "bg-amber-50 text-amber-700 border border-amber-200",
    dot: "bg-amber-500",
  },
  confirmed: {
    label: "Confirmed",
    classes: "bg-blue-50 text-blue-700 border border-blue-200",
    dot: "bg-blue-500",
  },
  shipped: {
    label: "Shipped",
    classes: "bg-purple-50 text-purple-700 border border-purple-200",
    dot: "bg-purple-500",
  },
  delivered: {
    label: "Delivered",
    classes: "bg-emerald-50 text-emerald-700 border border-emerald-200",
    dot: "bg-emerald-500",
  },
  cancelled: {
    label: "Cancelled",
    classes: "bg-red-50 text-red-600 border border-red-200",
    dot: "bg-red-500",
  },
  // Product statuses
  active: {
    label: "Active",
    classes: "bg-emerald-50 text-emerald-700 border border-emerald-200",
    dot: "bg-emerald-500",
  },
  inactive: {
    label: "Inactive",
    classes: "bg-gray-100 text-gray-500 border border-gray-200",
    dot: "bg-gray-400",
  },
  out_of_stock: {
    label: "Out of Stock",
    classes: "bg-red-50 text-red-600 border border-red-200",
    dot: "bg-red-500",
  },
};

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = "md" }) => {
  const normalized = status.toLowerCase().replace(/\s+/g, "_");
  const config = STATUS_MAP[normalized] ?? {
    label: status,
    classes: "bg-gray-100 text-gray-600 border border-gray-200",
    dot: "bg-gray-400",
  };

  const sizeClasses =
    size === "sm"
      ? "text-xs px-2 py-0.5 gap-1"
      : "text-xs px-2.5 py-1 gap-1.5";

  const dotSize = size === "sm" ? "w-1.5 h-1.5" : "w-2 h-2";

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full ${sizeClasses} ${config.classes}`}
    >
      <span className={`${dotSize} rounded-full flex-shrink-0 ${config.dot}`} />
      {config.label}
    </span>
  );
};

export default StatusBadge;
