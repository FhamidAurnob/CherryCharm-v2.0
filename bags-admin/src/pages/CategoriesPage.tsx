import React, { useEffect, useState } from "react";
import {
  collection, getDocs, addDoc, doc, updateDoc, deleteDoc,
} from "firebase/firestore";
import { Add, Edit, Delete, Category } from "@mui/icons-material";
import { db } from "../firebase/config";
import { Category as CategoryType } from "../types";
import toast from "react-hot-toast";

const EMPTY_FORM = {
  name: "",
  image: "",
  subcategories: [] as string[],
  isActive: true,
};

const CategoriesPage = () => {
  const [categories, setCategories] = useState<CategoryType[]>([]);
  const [loading, setLoading]       = useState(true);
  const [showForm, setShowForm]     = useState(false);
  const [editId, setEditId]         = useState<string | null>(null);
  const [form, setForm]             = useState(EMPTY_FORM);
  const [subInput, setSubInput]     = useState("");
  const [saving, setSaving]         = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  useEffect(() => { fetchCategories(); }, []);

  const fetchCategories = async () => {
    setLoading(true);
    const snap = await getDocs(collection(db, "categories"));
    setCategories(snap.docs.map(d => ({ id: d.id, ...d.data() } as CategoryType)));
    setLoading(false);
  };

  const openAdd = () => {
    setEditId(null);
    setForm(EMPTY_FORM);
    setSubInput("");
    setShowForm(true);
  };

  const openEdit = (cat: CategoryType) => {
    setEditId(cat.id);
    setForm({
      name: cat.name,
      image: cat.image,
      subcategories: cat.subcategories ?? [],
      isActive: cat.isActive ?? true,
    });
    setSubInput("");
    setShowForm(true);
  };

  const addSubcategory = () => {
    const val = subInput.trim();
    if (val && !form.subcategories.includes(val)) {
      setForm(p => ({ ...p, subcategories: [...p.subcategories, val] }));
    }
    setSubInput("");
  };

  const removeSubcategory = (sub: string) => {
    setForm(p => ({ ...p, subcategories: p.subcategories.filter(s => s !== sub) }));
  };

  const handleSave = async () => {
    if (!form.name.trim()) {
      toast.error("Category name is required");
      return;
    }
    setSaving(true);
    try {
      if (editId) {
        await updateDoc(doc(db, "categories", editId), {
          ...form,
          updatedAt: Date.now(),
        });
        toast.success("Category updated!");
      } else {
        await addDoc(collection(db, "categories"), {
          ...form,
          productCount: 0,
          createdAt: Date.now(),
        });
        toast.success("Category created!");
      }
      setShowForm(false);
      fetchCategories();
    } catch {
      toast.error("Failed to save category");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteDoc(doc(db, "categories", id));
      toast.success("Category deleted");
      setDeleteConfirmId(null);
      fetchCategories();
    } catch {
      toast.error("Failed to delete");
    }
  };

  const toggleActive = async (cat: CategoryType) => {
    await updateDoc(doc(db, "categories", cat.id), { isActive: !cat.isActive });
    toast.success(`Category ${cat.isActive ? "deactivated" : "activated"}`);
    fetchCategories();
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Categories</h2>
          <p className="text-sm text-gray-500">{categories.length} categories</p>
        </div>
        <button
          onClick={openAdd}
          className="flex items-center gap-2 bg-amber-800 text-white px-4 py-2 rounded-xl hover:bg-amber-700 transition"
        >
          <Add fontSize="small" /> Add Category
        </button>
      </div>

      {/* Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-white rounded-2xl p-5 shadow-sm animate-pulse">
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 bg-gray-100 rounded-xl" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-100 rounded w-1/2" />
                  <div className="h-3 bg-gray-50 rounded w-1/3" />
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : categories.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-48 text-gray-400 bg-white rounded-2xl shadow-sm">
          <Category fontSize="large" />
          <p className="mt-2">No categories yet</p>
          <button
            onClick={openAdd}
            className="mt-3 text-sm text-amber-800 font-medium hover:underline"
          >
            Add your first category
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {categories.map(cat => (
            <div
              key={cat.id}
              className={`bg-white rounded-2xl p-5 shadow-sm border transition ${
                cat.isActive ? "border-transparent" : "border-gray-200 opacity-60"
              }`}
            >
              <div className="flex items-start gap-4">
                {/* Image / Emoji */}
                <div className="w-14 h-14 rounded-xl overflow-hidden bg-amber-50 flex-shrink-0 flex items-center justify-center">
                  {cat.image ? (
                    <img src={cat.image} alt={cat.name} className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-2xl">👜</span>
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-gray-800 truncate">{cat.name}</p>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium flex-shrink-0 ${
                      cat.isActive
                        ? "bg-emerald-50 text-emerald-700"
                        : "bg-gray-100 text-gray-500"
                    }`}>
                      {cat.isActive ? "Active" : "Inactive"}
                    </span>
                  </div>

                  <p className="text-xs text-gray-400 mt-0.5">
                    {cat.productCount ?? 0} products
                  </p>

                  {/* Subcategories */}
                  {cat.subcategories?.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-2">
                      {cat.subcategories.map(sub => (
                        <span
                          key={sub}
                          className="text-xs bg-amber-50 text-amber-700 px-2 py-0.5 rounded-full"
                        >
                          {sub}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2 mt-4 pt-4 border-t border-gray-50">
                <button
                  onClick={() => openEdit(cat)}
                  className="flex-1 flex items-center justify-center gap-1 py-1.5 text-xs font-medium text-amber-800 bg-amber-50 rounded-lg hover:bg-amber-100 transition"
                >
                  <Edit style={{ fontSize: 14 }} /> Edit
                </button>
                <button
                  onClick={() => toggleActive(cat)}
                  className="flex-1 flex items-center justify-center gap-1 py-1.5 text-xs font-medium text-gray-600 bg-gray-50 rounded-lg hover:bg-gray-100 transition"
                >
                  {cat.isActive ? "Deactivate" : "Activate"}
                </button>
                <button
                  onClick={() => setDeleteConfirmId(cat.id)}
                  className="p-1.5 text-red-400 bg-red-50 rounded-lg hover:bg-red-100 transition"
                >
                  <Delete style={{ fontSize: 16 }} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add / Edit Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl">
            <h3 className="font-bold text-lg text-gray-800 mb-5">
              {editId ? "Edit Category" : "New Category"}
            </h3>

            <div className="space-y-4">
              {/* Name */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">
                  Category Name *
                </label>
                <input
                  className="input-field"
                  value={form.name}
                  onChange={e => setForm(p => ({ ...p, name: e.target.value }))}
                  placeholder="e.g. Tote Bags"
                />
              </div>

              {/* Image URL */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">
                  Image URL
                </label>
                <input
                  className="input-field"
                  value={form.image}
                  onChange={e => setForm(p => ({ ...p, image: e.target.value }))}
                  placeholder="https://..."
                />
                {form.image && (
                  <img
                    src={form.image}
                    alt="preview"
                    className="mt-2 w-16 h-16 rounded-xl object-cover border"
                    onError={e => (e.currentTarget.style.display = "none")}
                  />
                )}
              </div>

              {/* Subcategories */}
              <div>
                <label className="text-xs font-medium text-gray-600 block mb-1">
                  Subcategories
                </label>
                <div className="flex gap-2">
                  <input
                    className="input-field flex-1"
                    value={subInput}
                    onChange={e => setSubInput(e.target.value)}
                    onKeyDown={e => e.key === "Enter" && (e.preventDefault(), addSubcategory())}
                    placeholder="e.g. Mini Tote"
                  />
                  <button
                    type="button"
                    onClick={addSubcategory}
                    className="px-3 py-2 bg-amber-100 text-amber-800 rounded-xl hover:bg-amber-200 transition"
                  >
                    <Add fontSize="small" />
                  </button>
                </div>
                <div className="flex flex-wrap gap-2 mt-2">
                  {form.subcategories.map(sub => (
                    <span
                      key={sub}
                      onClick={() => removeSubcategory(sub)}
                      className="flex items-center gap-1 text-xs bg-amber-50 text-amber-800 px-2.5 py-1 rounded-full cursor-pointer hover:bg-red-50 hover:text-red-600 transition"
                    >
                      {sub} ×
                    </span>
                  ))}
                </div>
              </div>

              {/* Active toggle */}
              <div className="flex items-center justify-between py-1">
                <div>
                  <p className="text-sm font-medium text-gray-700">Active</p>
                  <p className="text-xs text-gray-400">Show in app</p>
                </div>
                <div
                  onClick={() => setForm(p => ({ ...p, isActive: !p.isActive }))}
                  className={`w-11 h-6 rounded-full cursor-pointer transition-colors relative ${
                    form.isActive ? "bg-amber-800" : "bg-gray-300"
                  }`}
                >
                  <div className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-all ${
                    form.isActive ? "left-5" : "left-0.5"
                  }`} />
                </div>
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowForm(false)}
                className="flex-1 py-2.5 border border-gray-200 rounded-xl text-sm text-gray-600 hover:bg-gray-50 transition"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 py-2.5 bg-amber-800 text-white rounded-xl text-sm font-semibold hover:bg-amber-700 transition disabled:opacity-50"
              >
                {saving ? "Saving..." : editId ? "Update" : "Create"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirm */}
      {deleteConfirmId && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 w-80 shadow-xl">
            <h3 className="font-bold text-lg text-gray-800 mb-2">Delete Category?</h3>
            <p className="text-sm text-gray-500 mb-6">
              This will not delete the products inside it, but they will lose their category.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="flex-1 py-2.5 border border-gray-200 rounded-xl text-sm text-gray-600 hover:bg-gray-50 transition"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(deleteConfirmId)}
                className="flex-1 py-2.5 bg-red-500 text-white rounded-xl text-sm font-semibold hover:bg-red-600 transition"
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

export default CategoriesPage;
