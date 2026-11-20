import React, { useState, useRef } from "react";
import { CloudUpload, Close, DragIndicator } from "@mui/icons-material";
import toast from "react-hot-toast";

interface ImageUploaderProps {
  images: string[];
  onChange: (images: string[]) => void;
  maxImages?: number;
}

const ImageUploader: React.FC<ImageUploaderProps> = ({
  images,
  onChange,
  maxImages = 8,
}) => {
  const [urlInput, setUrlInput] = useState("");
  const [dragOver, setDragOver] = useState(false);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [dropIndex, setDropIndex] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ── URL add ────────────────────────────────────────────────────────────
  const addUrl = () => {
    const url = urlInput.trim();
    if (!url) return;
    if (images.includes(url)) {
      toast.error("Image already added");
      return;
    }
    if (images.length >= maxImages) {
      toast.error(`Maximum ${maxImages} images allowed`);
      return;
    }
    onChange([...images, url]);
    setUrlInput("");
  };

  // ── File upload → base64 ───────────────────────────────────────────────
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    const remaining = maxImages - images.length;
    if (files.length > remaining) {
      toast.error(`Only ${remaining} more image${remaining !== 1 ? "s" : ""} allowed`);
    }
    files.slice(0, remaining).forEach((file) => {
      if (!file.type.startsWith("image/")) {
        toast.error(`${file.name} is not an image`);
        return;
      }
      const reader = new FileReader();
      reader.onload = (ev) => {
        const result = ev.target?.result as string;
        onChange((prev: string[]) => [...prev, result]);
      };
      reader.readAsDataURL(file);
    });
    // Reset input so same file can be re-selected
    e.target.value = "";
  };

  // ── Drop zone ──────────────────────────────────────────────────────────
  const handleDropZone = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) {
      const fakeEvent = {
        target: { files: e.dataTransfer.files, value: "" },
      } as unknown as React.ChangeEvent<HTMLInputElement>;
      handleFileChange(fakeEvent);
    }
  };

  // ── Reorder drag ───────────────────────────────────────────────────────
  const handleImageDragStart = (index: number) => setDragIndex(index);
  const handleImageDragOver = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    setDropIndex(index);
  };
  const handleImageDrop = (e: React.DragEvent, index: number) => {
    e.preventDefault();
    if (dragIndex === null || dragIndex === index) return;
    const reordered = [...images];
    const [moved] = reordered.splice(dragIndex, 1);
    reordered.splice(index, 0, moved);
    onChange(reordered);
    setDragIndex(null);
    setDropIndex(null);
  };

  const removeImage = (index: number) => {
    onChange(images.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-4">
      {/* URL input row */}
      <div className="flex gap-2">
        <input
          value={urlInput}
          onChange={(e) => setUrlInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addUrl())}
          placeholder="Paste image URL..."
          className="input-field flex-1"
        />
        <button
          type="button"
          onClick={addUrl}
          className="px-4 py-2 bg-amber-800 text-white rounded-xl text-sm font-medium hover:bg-amber-900 transition-colors whitespace-nowrap"
        >
          Add URL
        </button>
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="px-4 py-2 border border-gray-200 text-gray-700 rounded-xl text-sm font-medium hover:bg-gray-50 transition-colors whitespace-nowrap flex items-center gap-1"
        >
          <CloudUpload fontSize="small" /> Upload
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          onChange={handleFileChange}
          className="hidden"
        />
      </div>

      {/* Drop zone (shown when no images or as additional area) */}
      {images.length < maxImages && (
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDropZone}
          onClick={() => fileInputRef.current?.click()}
          className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all ${
            dragOver
              ? "border-amber-500 bg-amber-50"
              : "border-gray-200 hover:border-amber-300 hover:bg-gray-50"
          }`}
        >
          <CloudUpload className={`mx-auto mb-2 ${dragOver ? "text-amber-600" : "text-gray-300"}`} />
          <p className="text-sm text-gray-400">
            Drag & drop images here or <span className="text-amber-800 font-medium">browse</span>
          </p>
          <p className="text-xs text-gray-300 mt-1">
            {images.length}/{maxImages} images added
          </p>
        </div>
      )}

      {/* Image grid with drag-to-reorder */}
      {images.length > 0 && (
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
          {images.map((url, i) => (
            <div
              key={url}
              draggable
              onDragStart={() => handleImageDragStart(i)}
              onDragOver={(e) => handleImageDragOver(e, i)}
              onDrop={(e) => handleImageDrop(e, i)}
              onDragEnd={() => { setDragIndex(null); setDropIndex(null); }}
              className={`relative group aspect-square rounded-xl overflow-hidden border-2 transition-all cursor-grab active:cursor-grabbing ${
                dropIndex === i
                  ? "border-amber-500 scale-105"
                  : dragIndex === i
                  ? "border-gray-300 opacity-50"
                  : "border-transparent"
              }`}
            >
              <img
                src={url}
                alt={`Product ${i + 1}`}
                className="w-full h-full object-cover"
                onError={(e) => {
                  (e.target as HTMLImageElement).src =
                    "https://placehold.co/100x100?text=Error";
                }}
              />

              {/* Main badge */}
              {i === 0 && (
                <span className="absolute bottom-1 left-1 bg-amber-800 text-white text-xs px-1.5 py-0.5 rounded-md font-medium">
                  Main
                </span>
              )}

              {/* Drag handle */}
              <div className="absolute top-1 left-1 hidden group-hover:flex bg-black/40 rounded-md p-0.5">
                <DragIndicator style={{ fontSize: 14, color: "white" }} />
              </div>

              {/* Remove button */}
              <button
                type="button"
                onClick={() => removeImage(i)}
                className="absolute top-1 right-1 w-5 h-5 bg-red-500 text-white rounded-full hidden group-hover:flex items-center justify-center hover:bg-red-600 transition-colors"
              >
                <Close style={{ fontSize: 12 }} />
              </button>
            </div>
          ))}
        </div>
      )}

      {images.length > 1 && (
        <p className="text-xs text-gray-400">
          💡 Drag images to reorder. First image is used as the main product photo.
        </p>
      )}
    </div>
  );
};

export default ImageUploader;
