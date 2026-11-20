import React, { useEffect, useState } from "react";
import { collection, getDocs } from "firebase/firestore";
import { Search, People } from "@mui/icons-material";
import { db } from "../firebase/config";
import { Customer } from "../types";

const CustomersPage = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [filtered, setFiltered]   = useState<Customer[]>([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState("");

  useEffect(() => { fetchCustomers(); }, []);

  useEffect(() => {
    setFiltered(
      customers.filter(c =>
        c.name.toLowerCase().includes(search.toLowerCase()) ||
        c.email.toLowerCase().includes(search.toLowerCase())
      )
    );
  }, [customers, search]);

  const fetchCustomers = async () => {
    setLoading(true);
    const [usersSnap, ordersSnap] = await Promise.all([
      getDocs(collection(db, "users")),
      getDocs(collection(db, "orders")),
    ]);
    const orders = ordersSnap.docs.map(d => d.data());
    const users = usersSnap.docs.map(d => {
      const user = { id: d.id, ...d.data() } as Customer;
      const userOrders = orders.filter((o: any) => o.userId === user.id);
      return {
        ...user,
        totalOrders: userOrders.length,
        totalSpent: userOrders
          .filter((o: any) => o.status === "DELIVERED")
          .reduce((sum: number, o: any) => sum + o.totalAmount, 0),
      };
    });
    setCustomers(users.sort((a, b) => (b.totalSpent ?? 0) - (a.totalSpent ?? 0)));
    setLoading(false);
  };

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-xl font-bold text-gray-800">Customers</h2>
        <p className="text-sm text-gray-500">{filtered.length} customers</p>
      </div>

      <div className="bg-white rounded-2xl p-4 shadow-sm">
        <div className="flex items-center gap-2 bg-gray-100 rounded-xl px-3 py-2">
          <Search fontSize="small" className="text-gray-400" />
          <input
            className="bg-transparent text-sm outline-none flex-1"
            placeholder="Search by name or email..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-800" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-gray-400">
            <People fontSize="large" />
            <p className="mt-2">No customers found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {["Customer", "Email", "Phone", "Orders", "Total Spent", "Joined", "Wishlist"].map(h => (
                    <th key={h} className="text-left px-4 py-3 text-gray-500 font-semibold text-xs">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(customer => (
                  <tr key={customer.id} className="hover:bg-gray-50 transition">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-amber-100 flex items-center justify-center text-amber-800 font-bold text-sm">
                          {customer.profileImage
                            ? <img src={customer.profileImage} className="w-9 h-9 rounded-full object-cover" alt="" />
                            : customer.name?.[0]?.toUpperCase() ?? "?"}
                        </div>
                        <p className="font-medium text-gray-800">{customer.name || "No Name"}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{customer.email}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{customer.phone || "—"}</td>
                    <td className="px-4 py-3">
                      <span className="bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded-full font-medium">
                        {customer.totalOrders} orders
                      </span>
                    </td>
                    <td className="px-4 py-3 font-semibold text-amber-800">
                      ৳{customer.totalSpent?.toLocaleString("en-BD", { maximumFractionDigits: 0 }) ?? 0}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-400">
                      {new Date(customer.createdAt).toLocaleDateString("en-BD")}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-500">
                      {customer.wishlist?.length ?? 0} items
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

export default CustomersPage;