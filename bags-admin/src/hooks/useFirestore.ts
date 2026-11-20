import { useState } from "react";
import {
  collection,
  doc,
  getDocs,
  getDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  limit,
  Query,
  DocumentData,
  WhereFilterOp,
  OrderByDirection,
  serverTimestamp,
  QueryConstraint,
} from "firebase/firestore";
import { db } from "../firebase/config";

interface QueryOptions {
  where?: [string, WhereFilterOp, any][];
  orderBy?: [string, OrderByDirection?];
  limit?: number;
}

export const useFirestore = (collectionName: string) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = () => setError(null);

  // ── Get all documents (with optional filters) ──────────────────────────
  const getAll = async <T = DocumentData>(
    options?: QueryOptions
  ): Promise<T[]> => {
    setLoading(true);
    setError(null);
    try {
      const constraints: QueryConstraint[] = [];

      if (options?.where) {
        options.where.forEach(([field, op, value]) => {
          constraints.push(where(field, op, value));
        });
      }
      if (options?.orderBy) {
        const [field, dir = "asc"] = options.orderBy;
        constraints.push(orderBy(field, dir));
      }
      if (options?.limit) {
        constraints.push(limit(options.limit));
      }

      const ref = collection(db, collectionName);
      const q = constraints.length > 0 ? query(ref, ...constraints) : ref;
      const snapshot = await getDocs(q as Query<DocumentData>);

      return snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as T[];
    } catch (e: any) {
      setError(e.message);
      return [];
    } finally {
      setLoading(false);
    }
  };

  // ── Get single document by ID ──────────────────────────────────────────
  const getById = async <T = DocumentData>(id: string): Promise<T | null> => {
    setLoading(true);
    setError(null);
    try {
      const ref = doc(db, collectionName, id);
      const snapshot = await getDoc(ref);
      if (!snapshot.exists()) return null;
      return { id: snapshot.id, ...snapshot.data() } as T;
    } catch (e: any) {
      setError(e.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  // ── Add new document ───────────────────────────────────────────────────
  const add = async <T extends DocumentData>(data: T): Promise<string | null> => {
    setLoading(true);
    setError(null);
    try {
      const ref = await addDoc(collection(db, collectionName), {
        ...data,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      return ref.id;
    } catch (e: any) {
      setError(e.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  // ── Update existing document ───────────────────────────────────────────
  const update = async <T extends Partial<DocumentData>>(
    id: string,
    data: T
  ): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      await updateDoc(doc(db, collectionName, id), {
        ...data,
        updatedAt: serverTimestamp(),
      });
      return true;
    } catch (e: any) {
      setError(e.message);
      return false;
    } finally {
      setLoading(false);
    }
  };

  // ── Delete document ────────────────────────────────────────────────────
  const remove = async (id: string): Promise<boolean> => {
    setLoading(true);
    setError(null);
    try {
      await deleteDoc(doc(db, collectionName, id));
      return true;
    } catch (e: any) {
      setError(e.message);
      return false;
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    error,
    clearError,
    getAll,
    getById,
    add,
    update,
    remove,
  };
};