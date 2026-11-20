import React, { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import {
  Dashboard, Inventory, ShoppingBag, People,
  Category, LocalOffer, Menu, Close, Logout,
  Store, Notifications,
} from "@mui/icons-material";
import { useAuth } from "../../hooks/useAuth";

const navItems = [
  { icon: Dashboard,   label: "Dashboard",   path: "/dashboard" },
  { icon: Inventory,   label: "Products",    path: "/products" },
  { icon: ShoppingBag, label: "Orders",      path: "/orders" },
  { icon: People,      label: "Customers",   path: "/customers" },
  { icon: Category,    label: "Categories",  path: "/categories" },
  { icon: LocalOffer,  label: "Promo Codes", path: "/promo-codes" },
  { icon: Notifications, label: "Notifications", path: "/notifications" },
];

const Sidebar = () => {
  const [collapsed, setCollapsed] = useState(false);
  const { logout } = useAuth();
  const location = useLocation();

  return (
    <div
      className={`${
        collapsed ? "w-16" : "w-64"
      } min-h-screen bg-amber-900 text-white flex flex-col transition-all duration-300`}
    >
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-amber-700">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <Store className="text-amber-300" />
            <span className="font-bold text-lg">CherryCharm</span>
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="p-1 rounded hover:bg-amber-700 transition-colors"
        >
          {collapsed ? <Menu /> : <Close />}
        </button>
      </div>

      {/* Nav Items */}
      <nav className="flex-1 py-4">
        {navItems.map(({ icon: Icon, label, path }) => {
          const isActive = location.pathname.startsWith(path);
          return (
            <NavLink
              key={path}
              to={path}
              className={`flex items-center gap-3 px-4 py-3 mx-2 rounded-lg mb-1 transition-all ${
                isActive
                  ? "bg-amber-700 text-white"
                  : "text-amber-200 hover:bg-amber-800"
              }`}
            >
              <Icon fontSize="small" />
              {!collapsed && (
                <span className="text-sm font-medium">{label}</span>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* Logout */}
      <button
        onClick={logout}
        className="flex items-center gap-3 px-4 py-3 mx-2 mb-4 rounded-lg text-amber-200 hover:bg-amber-800 transition-all"
      >
        <Logout fontSize="small" />
        {!collapsed && <span className="text-sm font-medium">Logout</span>}
      </button>
    </div>
  );
};

export default Sidebar;