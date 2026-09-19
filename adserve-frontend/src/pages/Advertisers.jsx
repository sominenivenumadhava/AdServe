import React, { useEffect, useState } from 'react';
import { Plus, Edit2, Trash2, Eye, Building2, AlertCircle, CheckCircle } from 'lucide-react';
import DataTable from '../components/DataTable/DataTable';
import Modal from '../components/Modal/Modal';
import {
  getAdvertisers,
  createAdvertiser,
  updateAdvertiser,
  deleteAdvertiser,
} from '../services/api';

export default function Advertisers() {
  const [advertisers, setAdvertisers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedAdvertiser, setSelectedAdvertiser] = useState(null);
  const [formData, setFormData] = useState({ name: '', email: '' });
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const fetchAdvertisers = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getAdvertisers();
      setAdvertisers(res?.data || []);
    } catch (err) {
      setError(err.message || 'Failed to load advertisers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdvertisers();
  }, []);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleOpenCreate = () => {
    setSelectedAdvertiser(null);
    setFormData({ name: '', email: '' });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEdit = (advertiser) => {
    setSelectedAdvertiser(advertiser);
    setFormData({ name: advertiser.name, email: advertiser.email });
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenDelete = (advertiser) => {
    setSelectedAdvertiser(advertiser);
    setIsDeleteModalOpen(true);
  };

  const handleOpenView = (advertiser) => {
    setSelectedAdvertiser(advertiser);
    setIsViewModalOpen(true);
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Advertiser name is required';
    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Please provide a valid email address';
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    try {
      if (selectedAdvertiser) {
        await updateAdvertiser(selectedAdvertiser.id, formData);
        showNotification('Advertiser updated successfully');
      } else {
        await createAdvertiser(formData);
        showNotification('Advertiser created successfully');
      }
      setIsModalOpen(false);
      fetchAdvertisers();
    } catch (err) {
      if (err.status === 409) {
        setFormErrors({ email: err.message || 'This email address is already in use.' });
      } else {
        setFormErrors({ form: err.message });
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedAdvertiser) return;
    setSubmitting(true);
    try {
      await deleteAdvertiser(selectedAdvertiser.id);
      showNotification('Advertiser deleted successfully');
      setIsDeleteModalOpen(false);
      fetchAdvertisers();
    } catch (err) {
      showNotification(err.message || 'Failed to delete advertiser', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    { key: 'id', label: 'ID', align: 'center' },
    {
      key: 'name',
      label: 'Advertiser Name',
      render: (row) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center font-bold text-xs border border-blue-100">
            {row.name.charAt(0).toUpperCase()}
          </div>
          <span className="font-semibold text-slate-900">{row.name}</span>
        </div>
      ),
    },
    { key: 'email', label: 'Email Address' },
    {
      key: 'campaignCount',
      label: 'Campaigns',
      align: 'center',
      render: (row) => (
        <span className="inline-block px-2 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
          {row.campaignCount ?? 0}
        </span>
      ),
    },
    {
      key: 'createdAt',
      label: 'Created At',
      render: (row) =>
        row.createdAt ? new Date(row.createdAt).toLocaleDateString() : 'N/A',
    },
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (row) => (
        <div className="flex items-center justify-end gap-1.5">
          <button
            onClick={() => handleOpenView(row)}
            className="p-1.5 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-md transition"
            title="View Details"
          >
            <Eye className="w-4 h-4" />
          </button>
          <button
            onClick={() => handleOpenEdit(row)}
            className="p-1.5 text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded-md transition"
            title="Edit Advertiser"
          >
            <Edit2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => handleOpenDelete(row)}
            className="p-1.5 text-rose-500 hover:text-rose-700 hover:bg-rose-50 rounded-md transition"
            title="Delete Advertiser"
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
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Advertisers
          </h1>
          <p className="text-sm text-slate-500">
            Manage advertising accounts, brand profiles, and business contacts.
          </p>
        </div>

        <button
          onClick={handleOpenCreate}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold shadow-sm shadow-blue-600/30 transition"
        >
          <Plus className="w-4 h-4" />
          Create Advertiser
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

      {/* Data Table */}
      <DataTable
        columns={columns}
        data={advertisers}
        loading={loading}
        emptyMessage="No advertisers found. Click '+ Create Advertiser' to register one."
        searchPlaceholder="Search advertisers by name or email..."
        searchFields={['name', 'email']}
      />

      {/* Create / Edit Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => !submitting && setIsModalOpen(false)}
        title={selectedAdvertiser ? 'Edit Advertiser' : 'Create Advertiser'}
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
              Company / Advertiser Name <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Acme Corporation"
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
              Contact Email Address <span className="text-rose-500">*</span>
            </label>
            <input
              type="email"
              placeholder="e.g. ads@acme.com"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className={`w-full px-3.5 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:ring-2 transition ${
                formErrors.email
                  ? 'border-rose-300 focus:ring-rose-200'
                  : 'border-slate-200 focus:ring-blue-500/20 focus:border-blue-500'
              }`}
            />
            {formErrors.email && (
              <p className="mt-1 text-xs text-rose-600">{formErrors.email}</p>
            )}
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
              {submitting
                ? 'Saving...'
                : selectedAdvertiser
                ? 'Update Advertiser'
                : 'Create Advertiser'}
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
            Are you sure you want to delete advertiser{' '}
            <strong className="text-slate-900">{selectedAdvertiser?.name}</strong>?
          </p>
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
            Warning: All associated campaigns and advertisements will also be deleted due to database cascade rules.
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

      {/* View Details Modal */}
      <Modal
        isOpen={isViewModalOpen}
        onClose={() => setIsViewModalOpen(false)}
        title="Advertiser Overview"
        maxWidth="max-w-md"
      >
        {selectedAdvertiser && (
          <div className="space-y-3 text-sm">
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-500">Advertiser ID</span>
              <span className="font-semibold text-slate-900">{selectedAdvertiser.id}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-500">Name</span>
              <span className="font-semibold text-slate-900">{selectedAdvertiser.name}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-500">Email</span>
              <span className="font-mono text-xs text-slate-800">{selectedAdvertiser.email}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-500">Total Campaigns</span>
              <span className="font-semibold text-slate-900">{selectedAdvertiser.campaignCount ?? 0}</span>
            </div>
            <div className="flex justify-between py-2 border-b border-slate-100">
              <span className="text-slate-500">Registered On</span>
              <span className="text-slate-700">
                {selectedAdvertiser.createdAt
                  ? new Date(selectedAdvertiser.createdAt).toLocaleString()
                  : 'N/A'}
              </span>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setIsViewModalOpen(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
              >
                Close
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
