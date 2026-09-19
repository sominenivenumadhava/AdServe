import React, { useEffect, useState } from 'react';
import { Plus, Edit2, Trash2, Megaphone, AlertCircle, CheckCircle } from 'lucide-react';
import DataTable from '../components/DataTable/DataTable';
import Modal from '../components/Modal/Modal';
import { useAuth } from '../context/AuthContext';
import {
  getCampaigns,
  getAdvertisers,
  createCampaign,
  updateCampaign,
  deleteCampaign,
} from '../services/api';

export default function Campaigns() {
  const { user, isAdmin } = useAuth();
  const [campaigns, setCampaigns] = useState([]);
  const [advertisers, setAdvertisers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);

  // Modals & Form
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const initialForm = {
    name: '',
    advertiserId: '',
    budget: '',
    startDate: '',
    endDate: '',
    status: 'ACTIVE',
    targetCountry: 'IN',
    targetDevice: 'ANDROID',
    targetCategory: 'GAMING',
  };

  const [formData, setFormData] = useState(initialForm);
  const [formErrors, setFormErrors] = useState({});

  const fetchData = async () => {
    setLoading(true);
    try {
      if (isAdmin) {
        const [campRes, advRes] = await Promise.all([
          getCampaigns(),
          getAdvertisers(),
        ]);
        setCampaigns(campRes?.data || []);
        setAdvertisers(advRes?.data || []);
      } else {
        const campRes = await getCampaigns();
        setCampaigns(campRes?.data || []);
        if (user?.advertiserId) {
          setAdvertisers([{ id: user.advertiserId, name: user.name, email: user.email }]);
        }
      }
    } catch (err) {
      showNotification(err.message || 'Failed to load campaigns', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [isAdmin, user?.advertiserId]);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleOpenCreate = () => {
    setSelectedCampaign(null);
    setFormData({
      ...initialForm,
      advertiserId: !isAdmin && user?.advertiserId ? user.advertiserId : (advertisers[0]?.id || ''),
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEdit = (camp) => {
    setSelectedCampaign(camp);
    setFormData({
      name: camp.name,
      advertiserId: camp.advertiserId,
      budget: camp.budget,
      startDate: camp.startDate ? camp.startDate.substring(0, 16) : '',
      endDate: camp.endDate ? camp.endDate.substring(0, 16) : '',
      status: camp.status || 'ACTIVE',
      targetCountry: camp.targetCountry,
      targetDevice: camp.targetDevice,
      targetCategory: camp.targetCategory,
    });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenDelete = (camp) => {
    setSelectedCampaign(camp);
    setIsDeleteModalOpen(true);
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Campaign name is required';
    if (!formData.advertiserId) errors.advertiserId = 'Advertiser must be selected';

    const numBudget = parseFloat(formData.budget);
    if (!formData.budget || isNaN(numBudget) || numBudget <= 0) {
      errors.budget = 'Budget must be greater than zero';
    }

    if (!formData.startDate) errors.startDate = 'Start date is required';
    if (!formData.endDate) errors.endDate = 'End date is required';

    if (formData.startDate && formData.endDate) {
      const start = new Date(formData.startDate);
      const end = new Date(formData.endDate);
      if (end < start) {
        errors.endDate = 'End date cannot be before start date';
      }
    }

    if (!formData.targetCountry.trim()) errors.targetCountry = 'Target country is required';
    if (!formData.targetDevice.trim()) errors.targetDevice = 'Target device is required';
    if (!formData.targetCategory.trim()) errors.targetCategory = 'Target category is required';

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
        advertiserId: Number(formData.advertiserId),
        budget: parseFloat(formData.budget),
        startDate: formData.startDate.length === 16 ? `${formData.startDate}:00` : formData.startDate,
        endDate: formData.endDate.length === 16 ? `${formData.endDate}:00` : formData.endDate,
      };

      if (selectedCampaign) {
        await updateCampaign(selectedCampaign.id, payload);
        showNotification('Campaign updated successfully');
      } else {
        await createCampaign(payload);
        showNotification('Campaign created successfully');
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
    if (!selectedCampaign) return;
    setSubmitting(true);
    try {
      await deleteCampaign(selectedCampaign.id);
      showNotification('Campaign deleted successfully');
      setIsDeleteModalOpen(false);
      fetchData();
    } catch (err) {
      showNotification(err.message || 'Failed to delete campaign', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    const map = {
      ACTIVE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      PAUSED: 'bg-amber-50 text-amber-700 border-amber-200',
      COMPLETED: 'bg-slate-100 text-slate-700 border-slate-200',
    };
    return (
      <span
        className={`px-2 py-0.5 text-xs font-semibold rounded-full border ${
          map[status] || map.ACTIVE
        }`}
      >
        {status}
      </span>
    );
  };

  const columns = [
    {
      key: 'name',
      label: 'Campaign Name',
      render: (row) => (
        <div>
          <div className="font-semibold text-slate-900">{row.name}</div>
          <div className="text-xs text-slate-400">ID: {row.id}</div>
        </div>
      ),
    },
    { key: 'advertiserName', label: 'Advertiser' },
    {
      key: 'budget',
      label: 'Budget',
      render: (row) => (
        <span className="font-mono font-medium text-slate-900">
          ${parseFloat(row.budget).toLocaleString(undefined, { minimumFractionDigits: 2 })}
        </span>
      ),
    },
    {
      key: 'dates',
      label: 'Schedule (Start - End)',
      render: (row) => (
        <div className="text-xs text-slate-600">
          <div>{row.startDate ? new Date(row.startDate).toLocaleDateString() : 'N/A'}</div>
          <div className="text-slate-400 text-[11px]">to {row.endDate ? new Date(row.endDate).toLocaleDateString() : 'N/A'}</div>
        </div>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      align: 'center',
      render: (row) => getStatusBadge(row.status),
    },
    {
      key: 'targeting',
      label: 'Targeting Context',
      render: (row) => (
        <div className="flex flex-wrap gap-1">
          <span className="px-1.5 py-0.5 rounded bg-blue-50 text-blue-700 font-mono text-[10px] font-semibold">
            {row.targetCountry}
          </span>
          <span className="px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700 font-mono text-[10px] font-semibold">
            {row.targetDevice}
          </span>
          <span className="px-1.5 py-0.5 rounded bg-purple-50 text-purple-700 font-mono text-[10px] font-semibold">
            {row.targetCategory}
          </span>
        </div>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          <button
            onClick={() => handleOpenEdit(row)}
            className="p-1.5 text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded-md transition"
            title="Edit Campaign"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => handleOpenDelete(row)}
            className="p-1.5 text-rose-500 hover:text-rose-700 hover:bg-rose-50 rounded-md transition"
            title="Delete Campaign"
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
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Campaigns</h1>
          <p className="text-sm text-slate-500">
            Create and manage targeted campaigns, budgets, and scheduling rules.
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-sm shadow-blue-600/30 transition"
        >
          <Plus className="w-4 h-4" />
          Create Campaign
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
        data={campaigns}
        loading={loading}
        emptyMessage="No campaigns configured yet. Click '+ Create Campaign' to create one."
        searchPlaceholder="Search campaigns by name, country, device..."
        searchFields={['name', 'advertiserName', 'targetCountry', 'targetDevice', 'targetCategory']}
      />

      {/* Create / Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => !submitting && setIsModalOpen(false)}
        title={selectedCampaign ? 'Edit Campaign' : 'Create Campaign'}
        maxWidth="max-w-xl"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {formErrors.form && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{formErrors.form}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Campaign Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                placeholder="e.g. Q4 High-Performance Gaming Sale"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                  formErrors.name
                    ? 'border-rose-300 focus:ring-rose-200'
                    : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
                }`}
              />
              {formErrors.name && (
                <p className="mt-1 text-xs text-rose-600">{formErrors.name}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Advertiser Account <span className="text-rose-500">*</span>
              </label>
              <select
                disabled={!isAdmin}
                value={formData.advertiserId}
                onChange={(e) => setFormData({ ...formData, advertiserId: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                  formErrors.advertiserId
                    ? 'border-rose-300 focus:ring-rose-200'
                    : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
                } ${!isAdmin ? 'bg-slate-100 text-slate-500 cursor-not-allowed' : ''}`}
              >
                {isAdmin && <option value="">Select Advertiser...</option>}
                {advertisers.map((adv) => (
                  <option key={adv.id} value={adv.id}>
                    {adv.name} ({adv.email})
                  </option>
                ))}
              </select>
              {!isAdmin && (
                <p className="mt-1 text-[11px] text-slate-400">
                  Locked to your registered advertiser organization.
                </p>
              )}
              {formErrors.advertiserId && (
                <p className="mt-1 text-xs text-rose-600">{formErrors.advertiserId}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Budget (USD $) <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                placeholder="e.g. 5000.00"
                value={formData.budget}
                onChange={(e) => setFormData({ ...formData, budget: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                  formErrors.budget
                    ? 'border-rose-300 focus:ring-rose-200'
                    : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
                }`}
              />
              {formErrors.budget && (
                <p className="mt-1 text-xs text-rose-600">{formErrors.budget}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Start Date & Time <span className="text-rose-500">*</span>
              </label>
              <input
                type="datetime-local"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                  formErrors.startDate
                    ? 'border-rose-300 focus:ring-rose-200'
                    : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
                }`}
              />
              {formErrors.startDate && (
                <p className="mt-1 text-xs text-rose-600">{formErrors.startDate}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                End Date & Time <span className="text-rose-500">*</span>
              </label>
              <input
                type="datetime-local"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                  formErrors.endDate
                    ? 'border-rose-300 focus:ring-rose-200'
                    : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
                }`}
              />
              {formErrors.endDate && (
                <p className="mt-1 text-xs text-rose-600">{formErrors.endDate}</p>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Campaign Status
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="PAUSED">PAUSED</option>
                <option value="COMPLETED">COMPLETED</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Country (ISO) <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                placeholder="e.g. IN, US, GB"
                value={formData.targetCountry}
                onChange={(e) =>
                  setFormData({ ...formData, targetCountry: e.target.value.toUpperCase() })
                }
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Device <span className="text-rose-500">*</span>
              </label>
              <select
                value={formData.targetDevice}
                onChange={(e) => setFormData({ ...formData, targetDevice: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                <option value="ANDROID">ANDROID</option>
                <option value="IOS">IOS</option>
                <option value="DESKTOP">DESKTOP</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Category <span className="text-rose-500">*</span>
              </label>
              <select
                value={formData.targetCategory}
                onChange={(e) => setFormData({ ...formData, targetCategory: e.target.value })}
                className="w-full px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                <option value="GAMING">GAMING</option>
                <option value="FINANCE">FINANCE</option>
                <option value="TECH">TECH</option>
                <option value="ECOMMERCE">ECOMMERCE</option>
                <option value="ENTERTAINMENT">ENTERTAINMENT</option>
              </select>
            </div>
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
              {submitting ? 'Saving...' : selectedCampaign ? 'Update Campaign' : 'Create Campaign'}
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
            Are you sure you want to delete campaign{' '}
            <strong className="text-slate-900">{selectedCampaign?.name}</strong>?
          </p>
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
            All linked advertisements and impressions will be removed permanently.
          </div>

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
