import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Edit, Delete, Search, FilterList } from "@mui/icons-material";

interface Product {
  id: string;
  name: string;
  category: string;
  price: number;
  salePrice?: number;
  stock: number;
  images: string[];
  isActive: boolean;
  createdAt: Date;
}

interface ProductTableProps {
  products: Product[];
  loading?: boolean;
  onDelete: (id: string) => void;
}

const ProductTable: React.FC<ProductTableProps> = ({
  products,
  loading,
  onDelete,
}) => {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const categories = ["all", ...Array.from(new Set(products.map((p) => p.category)))];

  const filtered = products.filter((p) => {
    const matchSearch =
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.category.toLowerCase().includes(search.toLowerCase());
    const matchCategory =
      categoryFilter === "all" || p.category === categoryFilter;
    return matchSearch && matchCategory;
  });

  const handleDeleteClick = (id: string) => {
    setDeleteConfirmId(id);
  };

  const handleDeleteConfirm = () => {
    if (deleteConfirmId) {
      onDelete(deleteConfirmId);
      setDeleteConfirmId(null);
    }
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      {/* Toolbar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 p-4 border-b border-gray-50">
        {/* Search */}
        <div className="flex items-center gap-2 bg-gray-100 rounded-xl px-3 py-2 flex-1 max-w-sm">
          <Search fontSize="small" className="text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search products..."
            className="bg-transparent text-sm outline-none w-full"
          />
        </div>

        {/* Category filter */}
        <div className="flex items-center gap-2">
          <FilterList fontSize="small" className="text-gray-400" />
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="text-sm border border-gray-200 rounded-xl px-3 py-2 outline-none focus:ring-2 focus:ring-amber-500 bg-white"
          >
            {categories.map((c) => (
              <option key={c} value={c}>
                {c === "all" ? "All Categories" : c}
              </option>
            ))}
          </select>
        </div>

        <span className="text-xs text-gray-400 ml-auto">
          {filtered.length} product{filtered.length !== 1 ? "s" : ""}
        </span>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="bg-gray-50 text-left">
              <th className="px-4 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wider">Product</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wider">Category</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wider">Price</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wider">Stock</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wider">Status</th>
              <th className="px-4 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wider text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {loading ? (
              [...Array(6)].map((_, i) => (
                <tr key={i} className="animate-pulse">
                  <td className="px-4 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-gray-100 rounded-xl" />
                      <div className="h-3 bg-gray-100 rounded w-32" />
                    </div>
                  </td>
                  <td className="px-4 py-4"><div className="h-3 bg-gray-100 rounded w-20" /></td>
                  <td className="px-4 py-4"><div className="h-3 bg-gray-100 rounded w-16" /></td>
                  <td className="px-4 py-4"><div className="h-3 bg-gray-100 rounded w-10" /></td>
                  <td className="px-4 py-4"><div className="h-5 bg-gray-100 rounded-full w-16" /></td>
                  <td className="px-4 py-4"><div className="h-3 bg-gray-100 rounded w-16 ml-auto" /></td>
                </tr>
              ))
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center text-gray-400 text-sm">
                  No products found
                </td>
              </tr>
            ) : (
              filtered.map((product) => (
                <tr
                  key={product.id}
                  className="hover:bg-gray-50 transition-colors"
                >
                  {/* Product */}
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl overflow-hidden bg-amber-50 flex-shrink-0">
                        {product.images[0] ? (
                          <img
                            src={product.images[0]}
                            alt={product.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-lg">👜</div>
                        )}
                      </div>
                      <span className="text-sm font-medium text-gray-800 max-w-[200px] truncate">
                        {product.name}
                      </span>
                    </div>
                  </td>

                  {/* Category */}
                  <td className="px-4 py-3">
                    <span className="text-xs bg-amber-50 text-amber-800 px-2.5 py-1 rounded-full font-medium">
                      {product.category}
                    </span>
                  </td>

                  {/* Price */}
                  <td className="px-4 py-3">
                    <div>
                      <p className="text-sm font-semibold text-gray-800">
                        ৳{(product.salePrice ?? product.price).toLocaleString()}
                      </p>
                      {product.salePrice && (
                        <p className="text-xs text-gray-400 line-through">
                          ৳{product.price.toLocaleString()}
                        </p>
                      )}
                    </div>
                  </td>

                  {/* Stock */}
                  <td className="px-4 py-3">
                    <span
                      className={`text-sm font-medium ${
                        product.stock === 0
                          ? "text-red-500"
                          : product.stock < 5
                          ? "text-amber-600"
                          : "text-gray-700"
                      }`}
                    >
                      {product.stock}
                    </span>
                  </td>

                  {/* Status */}
                  <td className="px-4 py-3">
                    <span
                      className={`text-xs font-medium px-2.5 py-1 rounded-full ${
                        product.isActive
                          ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                          : "bg-gray-100 text-gray-500 border border-gray-200"
                      }`}
                    >
                      {product.isActive ? "Active" : "Inactive"}
                    </span>
                  </td>

                  {/* Actions */}
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        onClick={() => navigate(`/products/edit/${product.id}`)}
                        className="p-1.5 rounded-lg hover:bg-amber-50 text-amber-800 transition-colors"
                        title="Edit"
                      >
                        <Edit fontSize="small" />
                      </button>
                      <button
                        onClick={() => handleDeleteClick(product.id)}
                        className="p-1.5 rounded-lg hover:bg-red-50 text-red-500 transition-colors"
                        title="Delete"
                      >
                        <Delete fontSize="small" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Delete Confirm Modal */}
      {deleteConfirmId && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 shadow-xl max-w-sm w-full mx-4">
            <h3 className="text-lg font-bold text-gray-800 mb-2">Delete Product?</h3>
            <p className="text-sm text-gray-500 mb-6">
              This action cannot be undone. The product will be permanently removed.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="flex-1 px-4 py-2.5 border border-gray-200 rounded-xl text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteConfirm}
                className="flex-1 px-4 py-2.5 bg-red-500 text-white rounded-xl text-sm font-medium hover:bg-red-600 transition-colors"
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

export default ProductTable;