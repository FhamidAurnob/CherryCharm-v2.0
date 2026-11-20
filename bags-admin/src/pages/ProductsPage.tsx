import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  collection, getDocs, deleteDoc, doc, updateDoc,
} from "firebase/firestore";
import {
  Add, Edit, Delete, Search,
  ToggleOn, ToggleOff, Star, Inventory2,
} from "@mui/icons-material";
import { db, storage } from "../firebase/config";
import { Product } from "../types";
import toast from "react-hot-toast";

const ProductsPage = () => {
  const [products, setProducts]     = useState<Product[]>([]);
  const [filtered, setFiltered]     = useState<Product[]>([]);
  const [loading, setLoading]       = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("All");
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const navigate = useNavigate();

  const categories = ["All", "Tote Bags", "Clutches", "Crossbody", "Backpacks", "Wallets"];

  useEffect(() => { fetchProducts(); }, []);

  useEffect(() => {
    let list = [...products];
    if (searchQuery)
      list = list.filter(p =>
        p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.sku.toLowerCase().includes(searchQuery.toLowerCase())
      );
    if (categoryFilter !== "All")
      list = list.filter(p => p.category === categoryFilter);
    setFiltered(list);
  }, [products, searchQuery, categoryFilter]);

  const fetchProducts = async () => {
    setLoading(true);
    const snap = await getDocs(collection(db, "products"));
    const list = snap.docs.map(d => ({ id: d.id, ...d.data() } as Product));
    setProducts(list.sort((a, b) => b.createdAt - a.createdAt));
    setLoading(false);
  };

  const toggleActive = async (product: Product) => {
    await updateDoc(doc(db, "products", product.id), { isActive: !product.isActive });
    toast.success(`Product ${product.isActive ? "deactivated" : "activated"}`);
    fetchProducts();
  };

  const toggleFeatured = async (product: Product) => {
    await updateDoc(doc(db, "products", product.id), { isFeatured: !product.isFeatured });
    toast.success(`Product ${product.isFeatured ? "unfeatured" : "featured"}`);
    fetchProducts();
  };

  const deleteProduct = async (id: string) => {
    await deleteDoc(doc(db, "products", id));
    toast.success("Product deleted");
    setDeleteConfirm(null);
    fetchProducts();
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Products</h2>
          <p className="text-sm text-gray-500">{filtered.length} products found</p>
        </div>
        <button
          onClick={() => navigate("/products/add")}
          className="flex items-center gap-2 bg-amber-800 text-white px-4 py-2 rounded-xl hover:bg-amber-700 transition"
        >
          <Add fontSize="small" />
          Add Product
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-2xl p-4 shadow-sm flex flex-wrap gap-3 items-center">
        {/* Search */}
        <div className="flex items-center gap-2 bg-gray-100 rounded-xl px-3 py-2 flex-1 min-w-48">
          <Search fontSize="small" className="text-gray-400" />
          <input
            className="bg-transparent text-sm outline-none w-full"
            placeholder="Search by name or SKU..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
          />
        </div>
        {/* Category Filter */}
        <div className="flex gap-2 flex-wrap">
          {categories.map(cat => (
            <button
              key={cat}
              onClick={() => setCategoryFilter(cat)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                categoryFilter === cat
                  ? "bg-amber-800 text-white"
                  : "bg-gray-100 text-gray-600 hover:bg-gray-200"
              }`}
            >
              {cat}
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
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-gray-400">
            <Inventory2 fontSize="large" />
            <p className="mt-2">No products found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {["Product", "Category", "Price", "Stock", "Status", "Actions"].map(h => (
                    <th key={h} className="text-left px-4 py-3 text-gray-500 font-semibold">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(product => (
                  <tr key={product.id} className="hover:bg-gray-50 transition">
                    {/* Product Info */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <img
                          src={product.images?.[0] || "/placeholder.png"}
                          alt={product.name}
                          className="w-12 h-12 rounded-lg object-cover"
                        />
                        <div>
                          <p className="font-medium text-gray-800 line-clamp-1">{product.name}</p>
                          <p className="text-xs text-gray-400">SKU: {product.sku}</p>
                          <div className="flex items-center gap-1">
                            <Star sx={{ fontSize: 12, color: "#f59e0b" }} />
                            <span className="text-xs text-gray-500">{product.rating} ({product.reviewCount})</span>
                          </div>
                        </div>
                      </div>
                    </td>
                    {/* Category */}
                    <td className="px-4 py-3">
                      <span className="bg-amber-100 text-amber-800 text-xs px-2 py-1 rounded-full">
                        {product.category}
                      </span>
                    </td>
                    {/* Price */}
                    <td className="px-4 py-3">
                      <p className="font-semibold text-gray-800">৳{product.salePrice ?? product.price}</p>
                      {product.salePrice && (
                        <p className="text-xs text-gray-400 line-through">৳{product.price}</p>
                      )}
                    </td>
                    {/* Stock */}
                    <td className="px-4 py-3">
                      <span className={`font-medium ${product.stock < 10 ? "text-red-600" : "text-green-600"}`}>
                        {product.stock}
                      </span>
                      {product.stock < 10 && (
                        <p className="text-xs text-red-400">Low stock</p>
                      )}
                    </td>
                    {/* Status */}
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-1">
                        <span className={`text-xs px-2 py-0.5 rounded-full w-fit ${
                          product.isActive
                            ? "bg-green-100 text-green-700"
                            : "bg-gray-100 text-gray-500"
                        }`}>
                          {product.isActive ? "Active" : "Inactive"}
                        </span>
                        {product.isFeatured && (
                          <span className="text-xs px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-700 w-fit">
                            ⭐ Featured
                          </span>
                        )}
                      </div>
                    </td>
                    {/* Actions */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <button
                          title="Edit"
                          onClick={() => navigate(`/products/edit/${product.id}`)}
                          className="p-1.5 rounded-lg hover:bg-blue-50 text-blue-600"
                        >
                          <Edit fontSize="small" />
                        </button>
                        <button
                          title={product.isFeatured ? "Unfeature" : "Feature"}
                          onClick={() => toggleFeatured(product)}
                          className="p-1.5 rounded-lg hover:bg-yellow-50 text-yellow-600"
                        >
                          <Star fontSize="small" />
                        </button>
                        <button
                          title={product.isActive ? "Deactivate" : "Activate"}
                          onClick={() => toggleActive(product)}
                          className="p-1.5 rounded-lg hover:bg-green-50 text-green-600"
                        >
                          {product.isActive ? <ToggleOn /> : <ToggleOff />}
                        </button>
                        <button
                          title="Delete"
                          onClick={() => setDeleteConfirm(product.id)}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-red-500"
                        >
                          <Delete fontSize="small" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delete Dialog */}
      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 w-80 shadow-xl">
            <h3 className="font-bold text-lg text-gray-800 mb-2">Delete Product?</h3>
            <p className="text-gray-500 text-sm mb-6">This action cannot be undone.</p>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteConfirm(null)}
                className="flex-1 py-2 border rounded-xl text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteProduct(deleteConfirm)}
                className="flex-1 py-2 bg-red-500 text-white rounded-xl hover:bg-red-600"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductsPage;