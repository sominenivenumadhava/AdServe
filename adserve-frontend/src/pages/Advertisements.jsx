import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Plus,
  Edit2,
  Trash2,
  Eye,
  ExternalLink,
  Image as ImageIcon,
  AlertCircle,
  CheckCircle,
} from 'lucide-react';
import DataTable from '../components/DataTable/DataTable';
import Modal from '../components/Modal/Modal';
import {
  getAds,
  getCampaigns,
  createAd,
  updateAd,
  deleteAd,
} from '../services/api';

export default function Advertisements() {
  const navigate = useNavigate();
  const [ads, setAds] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);

  // Modal & Form state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedAd, setSelectedAd] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const initialForm = {
    title: '',
    campaignId: '',
    imageUrl: '',
    targetUrl: '',
    status: 'ACTIVE',
  };

  const [formData, setFormData] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});

  const fetchData = async () => {
    setLoading(true);
    try {
      const [adsRes, campRes] = await Promise.all([getAds(), getCampaigns()]);
      setAds(adsRes?.data || []);
      setCampaigns(campRes?.data || []);
    } catch (err) {
      showNotification(err.message || 'Failed to load advertisements', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleOpenCreate = () => {
    setSelectedAd(null);
    setFormData({
      ...initialForm,
      campaignId: campaigns[0]?.id || '',
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEdit = (ad) => {
    setSelectedAd(ad);
    setFormData({
      title: ad.title,
      campaignId: ad.campaignId,
      imageUrl: ad.imageUrl,
      targetUrl: ad.targetUrl,
      status: ad.status || 'ACTIVE',
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenDelete = (ad) => {
    setSelectedAd(ad);
    setIsDeleteModalOpen(true);
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.title.trim()) errors.title = 'Title is required';
    if (!formData.campaignId) errors.campaignId = 'Campaign must be selected';
    if (!formData.imageUrl.trim()) errors.imageUrl = 'Image URL is required';
    if (!formData.targetUrl.trim()) errors.targetUrl = 'Target URL is required';

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    try {
      const payload = {
        ...formData,
        campaignId: Number(formData.campaignId),
      };

      if (selectedAd) {
        await updateAd(selectedAd.id, payload);
        showNotification('Advertisement updated successfully');
      } else {
        await createAd(payload);
        showNotification('Advertisement created successfully');
      }

      setIsModalOpen(false);
      fetchData();
    } catch (err) {
      setFormErrors({ form: err.message });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedAd) return;
    setSubmitting(true);
    try {
      await deleteAd(selectedAd.id);
      showNotification('Advertisement deleted successfully');
      setIsDeleteModalOpen(false);
      fetchData();
    } catch (err) {
      showNotification(err.message || 'Failed to delete advertisement', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    { key: 'id', label: 'Ad ID', align: 'center' },
    {
      key: 'creative',
      label: 'Creative & Title',
      render: (row) => (
        <div className="flex items-center gap-3">
          <div className="w-12 h-10 rounded-lg overflow-hidden bg-slate-100 border border-slate-200 shrink-0 flex items-center justify-center">
            {row.imageUrl ? (
              <img
                src={row.imageUrl}
                alt={row.title}
                className="w-full h-full object-cover"
                onError={(e) => {
                  e.target.style.display = 'none';
                }}
              />
            ) : (
              <ImageIcon className="w-5 h-5 text-slate-400" />
            )}
          </div>
          <div>
            <div className="font-semibold text-slate-900">{row.title}</div>
            <div className="text-xs text-slate-400">Campaign: {row.campaignName}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'targetUrl',
      label: 'Destination URL',
      render: (row) => (
        <a
          href={row.targetUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="text-xs text-blue-600 hover:text-blue-800 hover:underline flex items-center gap-1 max-w-xs truncate"
        >
          <span className="truncate">{row.targetUrl}</span>
          <ExternalLink className="w-3 h-3 shrink-0" />
        </a>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      render: (row) => (
        <span
          className={`px-2 py-0.5 text-xs font-semibold rounded-full border ${
            row.status === 'ACTIVE'
              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
              : 'bg-slate-100 text-slate-600 border-slate-200'
          }`}
        >
          {row.status}
        </span>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          <button
            onClick={() => navigate(`/ad-preview/${row.id}`)}
            className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-lg transition"
            title="Preview Banner"
          >
            <Eye className="w-3.5 h-3.5" />
            Preview
          </button>
          <button
            onClick={() => handleOpenEdit(row)}
            className="p-1.5 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-md transition"
            title="Edit Ad"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => handleOpenDelete(row)}
            className="p-1.5 text-rose-500 hover:text-rose-700 hover:bg-rose-50 rounded-md transition"
            title="Delete Ad"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Advertisements</h1>
          <p className="text-sm text-slate-500">
            Creative banner inventory, assets, destination links, and live preview rendering.
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-sm shadow-blue-600/30 transition"
        >
          <Plus className="w-4 h-4" />
          Create Advertisement
        </button>
      </div>

      {/* Notifications */}
      {notification && (
        <div
          className={`p-4 rounded-xl border flex items-center gap-3 text-sm transition-all ${
            notification.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-800'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          {notification.type === 'error' ? (
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
          ) : (
            <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
          )}
          <span>{notification.message}</span>
        </div>
      )}

      {/* Table */}
      <DataTable
        columns={columns}
        data={ads}
        loading={loading}
        emptyMessage="No advertisements created yet. Click '+ Create Advertisement' to add one."
        searchPlaceholder="Search ads by title, campaign, URL..."
        searchFields={['title', 'campaignName', 'targetUrl']}
      />

      {/* Create / Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => !submitting && setIsModalOpen(false)}
        title={selectedAd ? 'Edit Advertisement' : 'Create Advertisement'}
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {formErrors.form && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{formErrors.form}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Ad Title <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Ultra Gaming Laptop - 40% Off"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                formErrors.title
                  ? 'border-rose-300 focus:ring-rose-200'
                  : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
              }`}
            />
            {formErrors.title && (
              <p className="mt-1 text-xs text-rose-600">{formErrors.title}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Campaign <span className="text-rose-500">*</span>
            </label>
            <select
              value={formData.campaignId}
              onChange={(e) => setFormData({ ...formData, campaignId: e.target.value })}
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
            >
              <option value="">Select Campaign...</option>
              {campaigns.map((camp) => (
                <option key={camp.id} value={camp.id}>
                  {camp.name} ({camp.targetCountry} / {camp.targetDevice} / {camp.targetCategory})
                </option>
              ))}
            </select>
            {formErrors.campaignId && (
              <p className="mt-1 text-xs text-rose-600">{formErrors.campaignId}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Image URL <span className="text-rose-500">*</span>
            </label>
            <input
              type="url"
              placeholder="https://images.unsplash.com/photo-..."
              value={formData.imageUrl}
              onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
              className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                formErrors.imageUrl
                  ? 'border-rose-300 focus:ring-rose-200'
                  : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
              }`}
            />
            {formErrors.imageUrl && (
              <p className="mt-1 text-xs text-rose-600">{formErrors.imageUrl}</p>
            )}
          </div>

          {/* Real-time Image Preview */}
          {formData.imageUrl && (
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl">
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-2">
                Live Creative Preview
              </span>
              <div className="w-full h-32 rounded-lg overflow-hidden bg-slate-200 flex items-center justify-center border border-slate-300">
                <img
                  src={formData.imageUrl}
                  alt="Preview"
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.target.style.display = 'none';
                  }}
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Target Landing URL <span className="text-rose-500">*</span>
            </label>
            <input
              type="url"
              placeholder="https://store.example.com/product"
              value={formData.targetUrl}
              onChange={(e) => setFormData({ ...formData, targetUrl: e.target.value })}
              className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                formErrors.targetUrl
                  ? 'border-rose-300 focus:ring-rose-200'
                  : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
              }`}
            />
            {formErrors.targetUrl && (
              <p className="mt-1 text-xs text-rose-600">{formErrors.targetUrl}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Status
            </label>
            <select
              value={formData.status}
              onChange={(e) => setFormData({ ...formData, status: e.target.value })}
              className="w-full px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
            </select>
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
            <button
              type="button"
              disabled={submitting}
              onClick={() => setIsModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg shadow-sm transition"
            >
              {submitting ? 'Saving...' : selectedAd ? 'Update Ad' : 'Create Advertisement'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={isDeleteModalOpen}
        onClose={() => !submitting && setIsDeleteModalOpen(false)}
        title="Confirm Deletion"
        maxWidth="max-w-md"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Are you sure you want to delete advertisement{' '}
            <strong className="text-slate-900">{selectedAd?.title}</strong>?
          </p>

          <div className="pt-2 flex items-center justify-end gap-2">
            <button
              type="button"
              disabled={submitting}
              onClick={() => setIsDeleteModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition"
            >
              Cancel
            </button>
            <button
              type="button"
              disabled={submitting}
              onClick={handleDelete}
              className="px-4 py-2 text-xs font-semibold text-white bg-rose-600 hover:bg-rose-700 disabled:opacity-50 rounded-lg shadow-sm transition"
            >
              {submitting ? 'Deleting...' : 'Confirm Delete'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
