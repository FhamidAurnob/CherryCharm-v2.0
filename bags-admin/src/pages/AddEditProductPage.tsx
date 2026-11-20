import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  doc, getDoc, setDoc, updateDoc, collection,
} from "firebase/firestore";
import {
  ref, uploadBytesResumable, getDownloadURL, deleteObject,
} from "firebase/storage";
import { CloudUpload, Delete, Add, ArrowBack } from "@mui/icons-material";
import { db, storage } from "../firebase/config";
import { Product } from "../types";
import toast from "react-hot-toast";

const EMPTY_PRODUCT: Partial<Product> = {
  name: "", description: "", price: 0, salePrice: undefined,
  category: "Tote Bags", subcategory: "", images: [], stock: 0,
  sku: "", tags: [], colors: [], isFeatured: false, isActive: true,
};

const AddEditProductPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [form, setForm] = useState<Partial<Product>>(EMPTY_PRODUCT);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [saving, setSaving] = useState(false);
  const [tagInput, setTagInput] = useState("");
  const [colorInput, setColorInput] = useState("");

  const categories = ["Tote Bags", "Clutches", "Crossbody", "Backpacks", "Wallets"];

  useEffect(() => {
    if (isEdit && id) fetchProduct(id);
  }, [id]);

  const fetchProduct = async (productId: string) => {
    const snap = await getDoc(doc(db, "products", productId));
    if (snap.exists()) setForm({ id: snap.id, ...snap.data() } as Product);
  };

  const handleChange = (key: keyof Product, value: any) => {
    setForm(prev => ({ ...prev, [key]: value }));
  };

  const uploadImage = async (file: File) => {
    setUploading(true);
    const storageRef = ref(storage, `products/${Date.now()}_${file.name}`);
    const uploadTask = uploadBytesResumable(storageRef, file);
    uploadTask.on(
      "state_changed",
      snap => setUploadProgress((snap.bytesTransferred / snap.totalBytes) * 100),
      err => { toast.error("Upload failed"); setUploading(false); },
      async () => {
        const url = await getDownloadURL(uploadTask.snapshot.ref);
        setForm(prev => ({ ...prev, images: [...(prev.images ?? []), url] }));
        setUploading(false);
        setUploadProgress(0);
      }
    );
  };

  const removeImage = (index: number) => {
    setForm(prev => ({
      ...prev,
      images: prev.images?.filter((_, i) => i !== index),
    }));
  };

  const addTag = () => {
    if (tagInput.trim() && !form.tags?.includes(tagInput.trim())) {
      handleChange("tags", [...(form.tags ?? []), tagInput.trim().toLowerCase()]);
      setTagInput("");
    }
  };

  const addColor = () => {
    if (colorInput.trim() && !form.colors?.includes(colorInput.trim())) {
      handleChange("colors", [...(form.colors ?? []), colorInput.trim()]);
      setColorInput("");
    }
  };

  const handleSave = async () => {
    if (!form.name || !form.price || !form.category) {
      toast.error("Please fill required fields");
      return;
    }
    setSaving(true);
    try {
      if (isEdit && id) {
        await updateDoc(doc(db, "products", id), {
          ...form, updatedAt: Date.now(),
        });
        toast.success("Product updated!");
      } else {
        const newRef = doc(collection(db, "products"));
        await setDoc(newRef, {
          ...form, id: newRef.id, rating: 0, reviewCount: 0,
          createdAt: Date.now(),
        });
        toast.success("Product added!");
      }
      navigate("/products");
    } catch (e) {
      toast.error("Failed to save product");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <button onClick={() => navigate("/products")} className="p-2 hover:bg-gray-100 rounded-xl">
          <ArrowBack />
        </button>
        <div>
          <h2 className="text-xl font-bold text-gray-800">
            {isEdit ? "Edit Product" : "Add New Product"}
          </h2>
          <p className="text-sm text-gray-500">Fill in the product details below</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column */}
        <div className="lg:col-span-2 space-y-4">
          {/* Basic Info */}
          <FormCard title="Basic Information">
            <FormField label="Product Name *">
              <input
                className="input-field"
                value={form.name}
                onChange={e => handleChange("name", e.target.value)}
                placeholder="e.g. Classic Brown Tote Bag"
              />
            </FormField>
            <FormField label="Description *">
              <textarea
                className="input-field h-28 resize-none"
                value={form.description}
                onChange={e => handleChange("description", e.target.value)}
                placeholder="Describe the product..."
              />
            </FormField>
            <div className="grid grid-cols-2 gap-4">
              <FormField label="SKU">
                <input
                  className="input-field"
                  value={form.sku}
                  onChange={e => handleChange("sku", e.target.value)}
                  placeholder="e.g. BAG-001"
                />
              </FormField>
              <FormField label="Stock *">
                <input
                  type="number"
                  className="input-field"
                  value={form.stock}
                  onChange={e => handleChange("stock", Number(e.target.value))}
                />
              </FormField>
            </div>
          </FormCard>

          {/* Pricing */}
          <FormCard title="Pricing">
            <div className="grid grid-cols-2 gap-4">
              <FormField label="Original Price (৳) *">
                <input
                  type="number"
                  className="input-field"
                  value={form.price}
                  onChange={e => handleChange("price", Number(e.target.value))}
                />
              </FormField>
              <FormField label="Sale Price (৳)">
                <input
                  type="number"
                  className="input-field"
                  value={form.salePrice ?? ""}
                  onChange={e => handleChange("salePrice", e.target.value ? Number(e.target.value) : undefined)}
                  placeholder="Leave empty if no sale"
                />
              </FormField>
            </div>
            {form.salePrice && form.price && form.salePrice < form.price && (
              <p className="text-green-600 text-sm font-medium">
                {((form.price - form.salePrice) / form.price * 100).toFixed(0)}% discount applied
              </p>
            )}
          </FormCard>

          {/* Images */}
          <FormCard title="Product Images">
            <div className="grid grid-cols-3 gap-3 mb-3">
              {form.images?.map((url, i) => (
                <div key={i} className="relative group aspect-square">
                  <img src={url} alt="" className="w-full h-full object-cover rounded-xl" />
                  <button
                    onClick={() => removeImage(i)}
                    className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition"
                  >
                    <Delete style={{ fontSize: 14 }} />
                  </button>
                  {i === 0 && (
                    <span className="absolute bottom-1 left-1 bg-amber-800 text-white text-xs px-1.5 py-0.5 rounded">
                      Main
                    </span>
                  )}
                </div>
              ))}

              {/* Upload Button */}
              <label className="aspect-square border-2 border-dashed border-gray-200 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:border-amber-400 transition">
                <input
                  type="file"
                  accept="image/*"
                  className="hidden"
                  onChange={e => e.target.files?.[0] && uploadImage(e.target.files[0])}
                />
                {uploading ? (
                  <div className="text-center">
                    <div className="text-xs text-gray-500">{Math.round(uploadProgress)}%</div>
                    <div className="w-16 h-1.5 bg-gray-200 rounded-full mt-1">
                      <div
                        className="h-1.5 bg-amber-600 rounded-full transition-all"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                ) : (
                  <>
                    <CloudUpload className="text-gray-300 mb-1" />
                    <p className="text-xs text-gray-400">Upload</p>
                  </>
                )}
              </label>
            </div>
          </FormCard>

          {/* Tags & Colors */}
          <FormCard title="Tags & Colors">
            {/* Tags */}
            <FormField label="Tags (for search)">
              <div className="flex gap-2">
                <input
                  className="input-field flex-1"
                  value={tagInput}
                  onChange={e => setTagInput(e.target.value)}
                  onKeyDown={e => e.key === "Enter" && addTag()}
                  placeholder="e.g. leather, summer..."
                />
                <button onClick={addTag} className="px-3 py-2 bg-amber-100 text-amber-800 rounded-xl hover:bg-amber-200">
                  <Add fontSize="small" />
                </button>
              </div>
              <div className="flex flex-wrap gap-2 mt-2">
                {form.tags?.map(tag => (
                  <span
                    key={tag}
                    className="bg-amber-100 text-amber-800 text-xs px-2 py-1 rounded-full flex items-center gap-1 cursor-pointer"
                    onClick={() => handleChange("tags", form.tags?.filter(t => t !== tag))}
                  >
                    #{tag} ×
                  </span>
                ))}
              </div>
            </FormField>

            {/* Colors */}
            <FormField label="Available Colors">
              <div className="flex gap-2">
                <input
                  className="input-field flex-1"
                  value={colorInput}
                  onChange={e => setColorInput(e.target.value)}
                  onKeyDown={e => e.key === "Enter" && addColor()}
                  placeholder="e.g. Black, Brown, Tan..."
                />
                <button onClick={addColor} className="px-3 py-2 bg-amber-100 text-amber-800 rounded-xl hover:bg-amber-200">
                  <Add fontSize="small" />
                </button>
              </div>
              <div className="flex flex-wrap gap-2 mt-2">
                {form.colors?.map(color => (
                  <span
                    key={color}
                    className="bg-gray-100 text-gray-700 text-xs px-2 py-1 rounded-full flex items-center gap-1 cursor-pointer"
                    onClick={() => handleChange("colors", form.colors?.filter(c => c !== color))}
                  >
                    {color} ×
                  </span>
                ))}
              </div>
            </FormField>
          </FormCard>
        </div>

        {/* Right Column */}
        <div className="space-y-4">
          {/* Category */}
          <FormCard title="Category">
            <FormField label="Category *">
              <select
                className="input-field"
                value={form.category}
                onChange={e => handleChange("category", e.target.value)}
              >
                {categories.map(c => <option key={c}>{c}</option>)}
              </select>
            </FormField>
            <FormField label="Subcategory">
              <input
                className="input-field"
                value={form.subcategory}
                onChange={e => handleChange("subcategory", e.target.value)}
                placeholder="e.g. Mini Tote"
              />
            </FormField>
          </FormCard>

          {/* Settings */}
          <FormCard title="Settings">
            <label className="flex items-center justify-between cursor-pointer py-2">
              <div>
                <p className="text-sm font-medium">Active</p>
                <p className="text-xs text-gray-400">Show on app</p>
              </div>
              <div
                className={`w-11 h-6 rounded-full transition-colors ${
                  form.isActive ? "bg-amber-800" : "bg-gray-300"
                } relative cursor-pointer`}
                onClick={() => handleChange("isActive", !form.isActive)}
              >
                <div className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-all ${
                  form.isActive ? "left-5" : "left-0.5"
                }`} />
              </div>
            </label>
            <label className="flex items-center justify-between cursor-pointer py-2">
              <div>
                <p className="text-sm font-medium">Featured</p>
                <p className="text-xs text-gray-400">Show on home screen</p>
              </div>
              <div
                className={`w-11 h-6 rounded-full transition-colors ${
                  form.isFeatured ? "bg-amber-800" : "bg-gray-300"
                } relative cursor-pointer`}
                onClick={() => handleChange("isFeatured", !form.isFeatured)}
              >
                <div className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-all ${
                  form.isFeatured ? "left-5" : "left-0.5"
                }`} />
              </div>
            </label>
          </FormCard>

          {/* Save */}
          <button
            onClick={handleSave}
            disabled={saving}
            className="w-full bg-amber-800 hover:bg-amber-700 text-white font-semibold py-3 rounded-xl transition disabled:opacity-50"
          >
            {saving ? "Saving..." : isEdit ? "Update Product" : "Add Product"}
          </button>
          <button
            onClick={() => navigate("/products")}
            className="w-full border border-gray-200 text-gray-600 font-medium py-3 rounded-xl hover:bg-gray-50 transition"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

// ── Form Helpers ──────────────────────────────────────────────────────────────
const FormCard = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <div className="bg-white rounded-2xl p-5 shadow-sm space-y-4">
    <h3 className="font-semibold text-gray-800">{title}</h3>
    {children}
  </div>
);

const FormField = ({ label, children }: { label: string; children: React.ReactNode }) => (
  <div>
    <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
    {children}
  </div>
);

export default AddEditProductPage;