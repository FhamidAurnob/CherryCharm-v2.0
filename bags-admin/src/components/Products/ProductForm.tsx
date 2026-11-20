import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Add, Close, CloudUpload } from "@mui/icons-material";
import toast from "react-hot-toast";

interface ProductFormData {
  name: string;
  description: string;
  category: string;
  price: number;
  salePrice: number | "";
  stock: number;
  colors: string[];
  sizes: string[];
  images: string[];
  isActive: boolean;
  tags: string[];
}

interface ProductFormProps {
  initialData?: Partial<ProductFormData>;
  onSubmit: (data: ProductFormData) => Promise<void>;
  isEdit?: boolean;
}

const CATEGORIES = [
  "Tote Bags", "Clutches", "Crossbody", "Backpacks", "Wallets",
  "Shoulder Bags", "Mini Bags", "Travel Bags",
];

const DEFAULT_COLORS = [
  "#1A1A1A", "#FFFFFF", "#8B4513", "#B5274E", "#D4AF37",
  "#4A4A8A", "#2E7D32", "#BF360C",
];

const SIZES = ["XS", "S", "M", "L", "XL"];

const emptyForm: ProductFormData = {
  name: "",
  description: "",
  category: "",
  price: 0,
  salePrice: "",
  stock: 0,
  colors: [],
  sizes: [],
  images: [],
  isActive: true,
  tags: [],
};

const ProductForm: React.FC<ProductFormProps> = ({
  initialData,
  onSubmit,
  isEdit = false,
}) => {
  const navigate = useNavigate();
  const [form, setForm] = useState<ProductFormData>({
    ...emptyForm,
    ...initialData,
  });
  const [loading, setLoading] = useState(false);
  const [tagInput, setTagInput] = useState("");
  const [imageUrlInput, setImageUrlInput] = useState("");
  const [errors, setErrors] = useState<Partial<Record<keyof ProductFormData, string>>>({});

  useEffect(() => {
    if (initialData) {
      setForm({ ...emptyForm, ...initialData });
    }
  }, [initialData]);

  const set = <K extends keyof ProductFormData>(key: K, value: ProductFormData[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    setErrors((prev) => ({ ...prev, [key]: undefined }));
  };

  const validate = (): boolean => {
    const newErrors: typeof errors = {};
    if (!form.name.trim()) newErrors.name = "Product name is required";
    if (!form.category) newErrors.category = "Category is required";
    if (!form.price || form.price <= 0) newErrors.price = "Price must be greater than 0";
    if (form.salePrice !== "" && Number(form.salePrice) >= form.price)
      newErrors.salePrice = "Sale price must be less than original price";
    if (form.stock < 0) newErrors.stock = "Stock cannot be negative";
    if (form.images.length === 0) newErrors.images = "At least one image is required";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) {
      toast.error("Please fix the errors before submitting");
      return;
    }
    setLoading(true);
    try {
      await onSubmit(form);
      toast.success(isEdit ? "Product updated!" : "Product created!");
      navigate("/products");
    } catch (err: any) {
      toast.error(err.message ?? "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  const toggleColor = (color: string) => {
    set(
      "colors",
      form.colors.includes(color)
        ? form.colors.filter((c) => c !== color)
        : [...form.colors, color]
    );
  };

  const toggleSize = (size: string) => {
    set(
      "sizes",
      form.sizes.includes(size)
        ? form.sizes.filter((s) => s !== size)
        : [...form.sizes, size]
    );
  };

  const addTag = () => {
    const tag = tagInput.trim().toLowerCase();
    if (tag && !form.tags.includes(tag)) {
      set("tags", [...form.tags, tag]);
    }
    setTagInput("");
  };

  const removeTag = (tag: string) => {
    set("tags", form.tags.filter((t) => t !== tag));
  };

  const addImageUrl = () => {
    const url = imageUrlInput.trim();
    if (url && !form.images.includes(url)) {
      set("images", [...form.images, url]);
    }
    setImageUrlInput("");
  };

  const removeImage = (url: string) => {
    set("images", form.images.filter((i) => i !== url));
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6 max-w-4xl">

      {/* ── Basic Info ────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
          Basic Information
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

          {/* Name */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Product Name <span className="text-red-500">*</span>
            </label>
            <input
              value={form.name}
              onChange={(e) => set("name", e.target.value)}
              placeholder="e.g. Leather Tote Bag"
              className={`input-field ${errors.name ? "border-red-400 focus:ring-red-400" : ""}`}
            />
            {errors.name && <p className="text-xs text-red-500 mt-1">{errors.name}</p>}
          </div>

          {/* Description */}
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
              placeholder="Describe the product..."
              rows={4}
              className="input-field resize-none"
            />
          </div>

          {/* Category */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Category <span className="text-red-500">*</span>
            </label>
            <select
              value={form.category}
              onChange={(e) => set("category", e.target.value)}
              className={`input-field ${errors.category ? "border-red-400" : ""}`}
            >
              <option value="">Select category</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
            {errors.category && <p className="text-xs text-red-500 mt-1">{errors.category}</p>}
          </div>

          {/* Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status
            </label>
            <div className="flex items-center gap-3 h-10">
              <button
                type="button"
                onClick={() => set("isActive", !form.isActive)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  form.isActive ? "bg-amber-800" : "bg-gray-300"
                }`}
              >
                <span
                  className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                    form.isActive ? "translate-x-6" : "translate-x-1"
                  }`}
                />
              </button>
              <span className="text-sm text-gray-600">
                {form.isActive ? "Active (visible to customers)" : "Inactive (hidden)"}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ── Pricing & Stock ───────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
          Pricing & Stock
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">

          {/* Price */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Original Price (৳) <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              min="0"
              value={form.price || ""}
              onChange={(e) => set("price", Number(e.target.value))}
              placeholder="0"
              className={`input-field ${errors.price ? "border-red-400" : ""}`}
            />
            {errors.price && <p className="text-xs text-red-500 mt-1">{errors.price}</p>}
          </div>

          {/* Sale Price */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Sale Price (৳) <span className="text-gray-400 font-normal">optional</span>
            </label>
            <input
              type="number"
              min="0"
              value={form.salePrice}
              onChange={(e) =>
                set("salePrice", e.target.value === "" ? "" : Number(e.target.value))
              }
              placeholder="Leave empty for no sale"
              className={`input-field ${errors.salePrice ? "border-red-400" : ""}`}
            />
            {errors.salePrice && <p className="text-xs text-red-500 mt-1">{errors.salePrice}</p>}
            {form.salePrice !== "" && form.price > 0 && Number(form.salePrice) < form.price && (
              <p className="text-xs text-emerald-600 mt-1">
                {Math.round(((form.price - Number(form.salePrice)) / form.price) * 100)}% off
              </p>
            )}
          </div>

          {/* Stock */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Stock Quantity <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              min="0"
              value={form.stock || ""}
              onChange={(e) => set("stock", Number(e.target.value))}
              placeholder="0"
              className={`input-field ${errors.stock ? "border-red-400" : ""}`}
            />
            {errors.stock && <p className="text-xs text-red-500 mt-1">{errors.stock}</p>}
          </div>
        </div>
      </div>

      {/* ── Images ────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
          Product Images <span className="text-red-500">*</span>
        </h2>

        {/* URL Input */}
        <div className="flex gap-2 mb-4">
          <input
            value={imageUrlInput}
            onChange={(e) => setImageUrlInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addImageUrl())}
            placeholder="Paste image URL and press Enter or click Add"
            className="input-field flex-1"
          />
          <button
            type="button"
            onClick={addImageUrl}
            className="px-4 py-2 bg-amber-800 text-white rounded-xl text-sm font-medium hover:bg-amber-900 transition-colors flex items-center gap-1"
          >
            <Add fontSize="small" /> Add
          </button>
        </div>

        {errors.images && (
          <p className="text-xs text-red-500 mb-3">{errors.images}</p>
        )}

        {/* Image previews */}
        {form.images.length > 0 ? (
          <div className="grid grid-cols-3 sm:grid-cols-5 gap-3">
            {form.images.map((url, i) => (
              <div key={i} className="relative group aspect-square">
                <img
                  src={url}
                  alt={`Product ${i + 1}`}
                  className="w-full h-full object-cover rounded-xl border border-gray-100"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src =
                      "https://via.placeholder.com/100x100?text=Error";
                  }}
                />
                {i === 0 && (
                  <span className="absolute bottom-1 left-1 bg-amber-800 text-white text-xs px-1.5 py-0.5 rounded-md">
                    Main
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => removeImage(url)}
                  className="absolute top-1 right-1 w-5 h-5 bg-red-500 text-white rounded-full items-center justify-center hidden group-hover:flex"
                >
                  <Close style={{ fontSize: 12 }} />
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="border-2 border-dashed border-gray-200 rounded-xl p-8 text-center text-gray-400">
            <CloudUpload className="mx-auto mb-2 opacity-40" />
            <p className="text-sm">Add image URLs above to preview them here</p>
          </div>
        )}
      </div>

      {/* ── Variants ──────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
          Variants
        </h2>

        {/* Colors */}
        <div className="mb-5">
          <label className="block text-sm font-medium text-gray-700 mb-2">Colors</label>
          <div className="flex flex-wrap gap-2">
            {DEFAULT_COLORS.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => toggleColor(color)}
                className={`w-8 h-8 rounded-full border-2 transition-all ${
                  form.colors.includes(color)
                    ? "border-amber-800 scale-110 shadow-md"
                    : "border-transparent hover:border-gray-300"
                }`}
                style={{ backgroundColor: color }}
                title={color}
              />
            ))}
          </div>
          {form.colors.length > 0 && (
            <p className="text-xs text-gray-400 mt-2">
              {form.colors.length} color{form.colors.length !== 1 ? "s" : ""} selected
            </p>
          )}
        </div>

        {/* Sizes */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Sizes</label>
          <div className="flex flex-wrap gap-2">
            {SIZES.map((size) => (
              <button
                key={size}
                type="button"
                onClick={() => toggleSize(size)}
                className={`px-4 py-1.5 rounded-xl text-sm font-medium border transition-all ${
                  form.sizes.includes(size)
                    ? "bg-amber-800 text-white border-amber-800"
                    : "bg-white text-gray-600 border-gray-200 hover:border-amber-400"
                }`}
              >
                {size}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ── Tags ──────────────────────────────────────────────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">
          Tags
        </h2>
        <div className="flex gap-2 mb-3">
          <input
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addTag())}
            placeholder="Type a tag and press Enter"
            className="input-field flex-1"
          />
          <button
            type="button"
            onClick={addTag}
            className="px-4 py-2 bg-gray-100 text-gray-700 rounded-xl text-sm font-medium hover:bg-gray-200 transition-colors"
          >
            Add
          </button>
        </div>
        <div className="flex flex-wrap gap-2">
          {form.tags.map((tag) => (
            <span
              key={tag}
              className="flex items-center gap-1 bg-amber-50 text-amber-800 px-3 py-1 rounded-full text-xs font-medium"
            >
              #{tag}
              <button type="button" onClick={() => removeTag(tag)}>
                <Close style={{ fontSize: 12 }} />
              </button>
            </span>
          ))}
        </div>
      </div>

      {/* ── Actions ───────────────────────────────────────────────── */}
      <div className="flex items-center justify-end gap-3 pb-8">
        <button
          type="button"
          onClick={() => navigate("/products")}
          className="px-6 py-2.5 border border-gray-200 rounded-xl text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-6 py-2.5 bg-amber-800 text-white rounded-xl text-sm font-semibold hover:bg-amber-900 transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2"
        >
          {loading ? (
            <>
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              {isEdit ? "Updating..." : "Creating..."}
            </>
          ) : (
            isEdit ? "Update Product" : "Create Product"
          )}
        </button>
      </div>
    </form>
  );
};

export default ProductForm;
