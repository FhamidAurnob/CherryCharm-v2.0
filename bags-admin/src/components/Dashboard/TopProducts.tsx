import React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, Award } from "lucide-react";

interface TopProduct {
  id: string;
  name: string;
  image: string;
  category: string;
  sold: number;
  revenue: number;
  stock: number;
}

interface TopProductsProps {
  products: TopProduct[];
  loading?: boolean;
}

const TopProducts: React.FC<TopProductsProps> = ({ products, loading }) => {
  const navigate = useNavigate();

  const maxSold = Math.max(...products.map((p) => p.sold), 1);

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-gray-50">
        <div className="flex items-center gap-2">
          <Award className="w-4 h-4 text-amber-800" />
          <h3 className="font-semibold text-gray-800">Top Products</h3>
        </div>
        <button
          onClick={() => navigate("/products")}
          className="flex items-center gap-1 text-xs font-medium text-amber-800 hover:text-amber-900 transition-colors"
        >
          View all <ArrowRight className="w-3 h-3" />
        </button>
      </div>

      {/* List */}
      {loading ? (
        <div className="divide-y divide-gray-50">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-6 py-4 animate-pulse">
              <div className="w-10 h-10 bg-gray-100 rounded-xl flex-shrink-0" />
              <div className="flex-1 space-y-2">
                <div className="h-3 bg-gray-100 rounded w-1/2" />
                <div className="h-2 bg-gray-50 rounded w-full" />
              </div>
              <div className="h-3 bg-gray-100 rounded w-12" />
            </div>
          ))}
        </div>
      ) : products.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-400">
          <Award className="w-8 h-8 mb-2 opacity-40" />
          <p className="text-sm">No product data yet</p>
        </div>
      ) : (
        <div className="divide-y divide-gray-50">
          {products.map((product, index) => (
            <div
              key={product.id}
              onClick={() => navigate(`/products/edit/${product.id}`)}
              className="flex items-center gap-4 px-6 py-3.5 hover:bg-gray-50 cursor-pointer transition-colors group"
            >
              {/* Rank */}
              <span
                className={`w-5 text-center text-xs font-bold flex-shrink-0 ${
                  index === 0
                    ? "text-amber-500"
                    : index === 1
                    ? "text-gray-400"
                    : index === 2
                    ? "text-amber-700"
                    : "text-gray-300"
                }`}
              >
                #{index + 1}
              </span>

              {/* Product image */}
              <div className="w-10 h-10 rounded-xl overflow-hidden bg-amber-50 flex-shrink-0">
                {product.image ? (
                  <img
                    src={product.image}
                    alt={product.name}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-lg">
                    👜
                  </div>
                )}
              </div>

              {/* Info + progress bar */}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-800 truncate group-hover:text-amber-900 transition-colors">
                  {product.name}
                </p>
                <div className="flex items-center gap-2 mt-1">
                  {/* Progress bar showing relative sales */}
                  <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-amber-700 rounded-full transition-all duration-500"
                      style={{ width: `${(product.sold / maxSold) * 100}%` }}
                    />
                  </div>
                  <span className="text-xs text-gray-400 flex-shrink-0">
                    {product.sold} sold
                  </span>
                </div>
              </div>

              {/* Revenue */}
              <div className="text-right flex-shrink-0">
                <p className="text-sm font-semibold text-gray-700">
                  ৳{product.revenue.toLocaleString()}
                </p>
                <p className={`text-xs ${product.stock < 5 ? "text-red-500 font-medium" : "text-gray-400"}`}>
                  {product.stock < 5
                    ? product.stock === 0
                      ? "Out of stock"
                      : `${product.stock} left`
                    : `${product.stock} in stock`}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default TopProducts;
