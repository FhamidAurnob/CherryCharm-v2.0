import React, { useEffect, useState } from "react";
import {
  collection, getDocs, doc, setDoc, updateDoc, deleteDoc,
} from "firebase/firestore";
import { Add, Delete, ToggleOn, ToggleOff } from "@mui/icons-material";
import { db } from "../firebase/config";
import { PromoCode } from "../types";
import toast from "react-hot-toast";

const EMPTY_PROMO: PromoCode = {
  id: "", code: "", discountType: "percentage", discountValue: 0,
  minOrderAmount: 0, maxDiscount: 0, usageLimit: 0,
  usedCount: 0, expiryDate: Date.now() + 30 * 24 * 60 * 60 * 1000,
  isActive: true,
};

const PromoCodesPage = () => {
  const [promos, setPromos]     = useState<PromoCode[]>([]);
  const [loading, setLoading]   = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm]         = useState<PromoCode>(EMPTY_PROMO);
  const [saving, setSaving]     = useState(false);

  useEffect(() => { fetchPromos(); }, []);

  const fetchPromos = async () => {
    setLoading(true);
    const snap = await getDocs(collection(db, "promoCodes"));
    setPromos(snap.docs.map(d => d.data() as PromoCode));
    setLoading(false);
  };

  const savePromo = async () => {
    if (!form.code || !form.discountValue) {
      toast.error("Fill required fields");
      return;
    }
    setSaving(true);
    try {
      await setDoc(doc(db, "promoCodes", form.code.toUpperCase()), {
        ...form,
        code: form.code.toUpperCase(),
        usedCount: 0,
      });
      toast.success("Promo code created!");
      setShowForm(false);
      setForm(EMPTY_PROMO);
      fetchPromos();
    } catch (e) {
      toast.error("Failed to save");
    } finally {
      setSaving(false);
    }
  };

  const togglePromo = async (code: string, current: boolean) => {
    await updateDoc(doc(db, "promoCodes", code), { isActive: !current });
    toast.success(`Promo ${current ? "deactivated" : "activated"}`);
    fetchPromos();
  };

  const deletePromo = async (code: string) => {
    await deleteDoc(doc(db, "promoCodes", code));
    toast.success("Promo code deleted");
    fetchPromos();
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Promo Codes</h2>
          <p className="text-sm text-gray-500">{promos.length} promo codes</p>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 bg-amber-800 text-white px-4 py-2 rounded-xl hover:bg-amber-700"
        >
          <Add fontSize="small" />
          Add Promo Code
        </button>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                {["Code", "Discount", "Min Order", "Usage", "Expiry", "Status", "Actions"].map(h => (
                  <th key={h} className="text-left px-4 py-3 text-gray-500 font-semibold text-xs">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {promos.map(promo => {
                const isExpired = promo.expiryDate < Date.now();
                return (
                  <tr key={promo.code} className="hover:bg-gray-50 transition">
                    <td className="px-4 py-3">
                      <span className="font-mono font-bold text-amber-800 bg-amber-50 px-2 py-1 rounded">
                        {promo.code}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-semibold">
                      {promo.discountType === "percentage"
                        ? `${promo.discountValue}%`
                        : `৳${promo.discountValue}`}
                      {promo.maxDiscount > 0 && (
                        <p className="text-xs text-gray-400">Max ৳{promo.maxDiscount}</p>
                      )}
                    </td>
                    <td className="px-4 py-3 text-gray-500">৳{promo.minOrderAmount}</td>
                    <td className="px-4 py-3">
                      <p className="text-sm">{promo.usedCount} / {promo.usageLimit || "∞"}</p>
                      {promo.usageLimit > 0 && (
                        <div className="w-16 h-1 bg-gray-200 rounded-full mt-1">
                          <div
                            className="h-1 bg-amber-600 rounded-full"
                            style={{ width: `${Math.min((promo.usedCount / promo.usageLimit) * 100, 100)}%` }}
                          />
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 text-xs">
                      <p className={isExpired ? "text-red-500" : "text-gray-500"}>
                        {new Date(promo.expiryDate).toLocaleDateString("en-IN")}
                      </p>
                      {isExpired && <p className="text-red-400 font-medium">Expired</p>}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                        promo.isActive && !isExpired
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500"
                      }`}>
                        {isExpired ? "Expired" : promo.isActive ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => togglePromo(promo.code, promo.isActive)}
                          className="p-1.5 rounded-lg hover:bg-green-50 text-green-600"
                        >
                          {promo.isActive ? <ToggleOn /> : <ToggleOff />}
                        </button>
                        <button
                          onClick={() => deletePromo(promo.code)}
                          className="p-1.5 rounded-lg hover:bg-red-50 text-red-500"
                        >
                          <Delete fontSize="small" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Promo Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl">
            <h3 className="font-bold text-lg mb-4">Create Promo Code</h3>
            <div className="space-y-3">
              <div>
                <label className="text-xs font-medium text-gray-600">Code *</label>
                <input
                  className="input-field mt-1 uppercase"
                  value={form.code}
                  onChange={e => setForm(p => ({ ...p, code: e.target.value.toUpperCase() }))}
                  placeholder="e.g. SAVE20"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-gray-600">Discount Type</label>
                  <select
                    className="input-field mt-1"
                    value={form.discountType}
                    onChange={e => setForm(p => ({ ...p, discountType: e.target.value as any }))}
                  >
                    <option value="percentage">Percentage (%)</option>
                    <option value="flat">Flat Amount (৳)</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600">
                    Value * {form.discountType === "percentage" ? "(%)" : "(৳)"}
                  </label>
                  <input
                    type="number"
                    className="input-field mt-1"
                    value={form.discountValue}
                    onChange={e => setForm(p => ({ ...p, discountValue: Number(e.target.value) }))}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-gray-600">Min Order (৳)</label>
                  <input
                    type="number"
                    className="input-field mt-1"
                    value={form.minOrderAmount}
                    onChange={e => setForm(p => ({ ...p, minOrderAmount: Number(e.target.value) }))}
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600">Max Discount (৳)</label>
                  <input
                    type="number"
                    className="input-field mt-1"
                    value={form.maxDiscount}
                    onChange={e => setForm(p => ({ ...p, maxDiscount: Number(e.target.value) }))}
                    placeholder="0 = no limit"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-gray-600">Usage Limit</label>
                  <input
                    type="number"
                    className="input-field mt-1"
                    value={form.usageLimit}
                    onChange={e => setForm(p => ({ ...p, usageLimit: Number(e.target.value) }))}
                    placeholder="0 = unlimited"
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600">Expiry Date</label>
                  <input
                    type="date"
                    className="input-field mt-1"
                    value={new Date(form.expiryDate).toISOString().split("T")[0]}
                    onChange={e => setForm(p => ({ ...p, expiryDate: new Date(e.target.value).getTime() }))}
                  />
                </div>
              </div>
            </div>
            <div className="flex gap-3 mt-5">
              <button
                onClick={() => { setShowForm(false); setForm(EMPTY_PROMO); }}
                className="flex-1 py-2.5 border rounded-xl text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={savePromo}
                disabled={saving}
                className="flex-1 py-2.5 bg-amber-800 text-white rounded-xl hover:bg-amber-700 disabled:opacity-50"
              >
                {saving ? "Creating..." : "Create"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PromoCodesPage;