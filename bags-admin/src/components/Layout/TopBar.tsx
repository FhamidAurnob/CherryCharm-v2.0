import React from "react";
import { Notifications, Search, AccountCircle } from "@mui/icons-material";
import { useAuth } from "../../hooks/useAuth";
import { useLocation } from "react-router-dom";

const pageTitles: Record<string, string> = {
  "/dashboard":   "Dashboard",
  "/products":    "Products",
  "/orders":      "Orders",
  "/customers":   "Customers",
  "/categories":  "Categories",
  "/promo-codes": "Promo Codes",
  "/notifications": "Notifications",
};

const TopBar = () => {
  const { user } = useAuth();
  const location = useLocation();
  const title =
    Object.entries(pageTitles).find(([key]) =>
      location.pathname.startsWith(key)
    )?.[1] ?? "Admin";

  return (
    <div className="bg-white border-b px-6 py-3 flex items-center justify-between">
      <h1 className="text-xl font-bold text-gray-800">{title}</h1>
      <div className="flex items-center gap-4">
        {/* Search */}
        <div className="flex items-center gap-2 bg-gray-100 rounded-lg px-3 py-2">
          <Search fontSize="small" className="text-gray-400" />
          <input
            className="bg-transparent text-sm outline-none w-48"
            placeholder="Search..."
          />
        </div>
        {/* Notifications */}
        <button className="relative p-2 rounded-lg hover:bg-gray-100">
          <Notifications fontSize="small" className="text-gray-600" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
        </button>
        {/* User */}
        <div className="flex items-center gap-2">
          <AccountCircle className="text-amber-800" />
          <span className="text-sm font-medium text-gray-700">
            {user?.email?.split("@")[0]}
          </span>
        </div>
      </div>
    </div>
  );
};

export default TopBar;