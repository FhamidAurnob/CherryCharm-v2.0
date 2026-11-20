import React from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { useAuth } from "./hooks/useAuth";
import AdminLayout from "./components/Layout/AdminLayout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import ProductsPage from "./pages/ProductsPage";
import AddEditProductPage from "./pages/AddEditProductPage";
import OrdersPage from "./pages/OrdersPage";
import OrderDetailPage from "./pages/OrderDetailPage";
import CustomersPage from "./pages/CustomersPage";
import CategoriesPage from "./pages/CategoriesPage";
import PromoCodesPage from "./pages/PromoCodesPage";
import NotificationsPage from "./pages/NotificationsPage";

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, isAdmin, loading } = useAuth();
  if (loading) return (
    <div className="flex items-center justify-center h-screen">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-amber-800" />
    </div>
  );
  return user && isAdmin ? <>{children}</> : <Navigate to="/login" />;
};

const App = () => {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" />} />
          <Route path="dashboard"          element={<DashboardPage />} />
          <Route path="products"           element={<ProductsPage />} />
          <Route path="products/add"       element={<AddEditProductPage />} />
          <Route path="products/edit/:id"  element={<AddEditProductPage />} />
          <Route path="orders"             element={<OrdersPage />} />
          <Route path="orders/:id"         element={<OrderDetailPage />} />
          <Route path="customers"          element={<CustomersPage />} />
          <Route path="categories"         element={<CategoriesPage />} />
          <Route path="promo-codes"        element={<PromoCodesPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;