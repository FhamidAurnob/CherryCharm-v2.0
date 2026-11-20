import { Timestamp } from "firebase/firestore";

// ── Product ───────────────────────────────────────────────────────────────────
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  salePrice?: number;
  category: string;
  subcategory: string;
  images: string[];
  stock: number;
  sku: string;
  rating: number;
  reviewCount: number;
  tags: string[];
  colors: string[];
  sizes: string[];
  isFeatured: boolean;
  isActive: boolean;
  createdAt: Timestamp | number;
  updatedAt: Timestamp | number;
}

// ── Order ─────────────────────────────────────────────────────────────────────
export interface Order {
  id: string;
  userId: string;
  customerName: string;
  customerEmail: string;
  items: OrderItem[];
  totalAmount: number;
  discountAmount: number;
  deliveryFee: number;
  status: OrderStatus;
  address: Address;
  paymentMethod: string;
  paymentId: string;
  promoCode: string;
  notes?: string;
  createdAt: Timestamp | number;
  updatedAt: Timestamp | number;
}

export interface OrderItem {
  productId: string;
  productName: string;
  productImage: string;
  price: number;
  quantity: number;
  color: string;
  size?: string;
}

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "REFUNDED";

// ── Address ───────────────────────────────────────────────────────────────────
export interface Address {
  id: string;
  label: string;
  fullName: string;
  phone: string;
  line1: string;
  line2: string;
  city: string;
  district: string;   // Bangladesh-specific (was "state")
  postCode: string;   // Bangladesh-specific (was "pincode")
  isDefault: boolean;
}

// ── Customer ──────────────────────────────────────────────────────────────────
export interface Customer {
  id: string;
  name: string;
  email: string;
  phone: string;
  profileImage: string;
  wishlist: string[];
  addresses?: Address[];
  createdAt: Timestamp | number;
  totalOrders?: number;
  totalSpent?: number;
}

// ── Category ──────────────────────────────────────────────────────────────────
export interface Category {
  id: string;
  name: string;
  image: string;
  subcategories: string[];
  productCount?: number;
  isActive: boolean;
  createdAt: Timestamp | number;
}

// ── Promo Code ────────────────────────────────────────────────────────────────
export interface PromoCode {
  id: string;                              // ← was missing
  code: string;
  discountType: "percentage" | "flat";
  discountValue: number;
  minOrderAmount: number;
  maxDiscount: number;
  usageLimit: number;
  usedCount: number;
  expiryDate: Timestamp | number;
  isActive: boolean;
  createdAt: Timestamp | number;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export interface DashboardStats {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
  revenueGrowth: number;
  ordersGrowth: number;
  customersGrowth: number;
  pendingOrders: number;
  lowStockProducts: number;
}

export interface RevenueDataPoint {
  month: string;
  revenue: number;
  orders: number;
}

// ── Notification ──────────────────────────────────────────────────────────────
export interface Notification {
  id: string;
  title: string;
  body: string;
  type: "order" | "promo" | "system" | "restock";
  targetType: "all" | "specific";
  targetUserId?: string;
  isRead: boolean;
  createdAt: Timestamp | number;
}

// ── Review ────────────────────────────────────────────────────────────────────
export interface Review {
  id: string;
  productId: string;
  userId: string;
  userName: string;
  userImage: string;
  rating: number;
  comment: string;
  images?: string[];
  isVerifiedPurchase: boolean;
  createdAt: Timestamp | number;
}

// ── Helper types ──────────────────────────────────────────────────────────────
export type FirestoreTimestamp = Timestamp | number;

/** Converts Firestore Timestamp or number to JS Date */
export const toDate = (ts: FirestoreTimestamp): Date => {
  if (ts instanceof Timestamp) return ts.toDate();
  return new Date(ts);
};

/** Converts Firestore Timestamp or number to milliseconds */
export const toMillis = (ts: FirestoreTimestamp): number => {
  if (ts instanceof Timestamp) return ts.toMillis();
  return ts;
};