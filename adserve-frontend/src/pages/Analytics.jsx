import React, { useEffect, useState, useMemo } from 'react';
import {
  Eye,
  MousePointerClick,
  Percent,
  Search,
  Filter,
  BarChart3,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import StatCard from '../components/StatCard/StatCard';
import DataTable from '../components/DataTable/DataTable';
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner';
import {
  getDashboardOverview,
  getCampaignAnalytics,
  getAdAnalytics,
} from '../services/api';

export default function Analytics() {
  const [overview, setOverview] = useState(null);
  const [campaignAnalytics, setCampaignAnalytics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Per-ad Inspector
  const [lookupAdId, setLookupAdId] = useState('');
  const [adLookupResult, setAdLookupResult] = useState(null);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState(null);

  const fetchAnalytics = async () => {
    setLoading(true);
    setError(null);
    try {
      const [ovRes, campRes] = await Promise.all([
        getDashboardOverview(),
        getCampaignAnalytics(),
      ]);
      setOverview(ovRes?.data || {});
      setCampaignAnalytics(campRes?.data || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch analytics');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const handleLookupAd = async (e) => {
    e.preventDefault();
    if (!lookupAdId.trim()) return;

    setLookupLoading(true);
    setLookupError(null);
    setAdLookupResult(null);

    try {
      const res = await getAdAnalytics(lookupAdId.trim());
      setAdLookupResult(res?.data);
    } catch (err) {
      setLookupError(err.message || 'Ad not found or no analytics available');
    } finally {
      setLookupLoading(false);
    }
  };

  // Filtered campaign analytics
  const filteredCampaigns = useMemo(() => {
    return campaignAnalytics.filter((item) => {
      const matchesStatus =
        selectedStatus === 'ALL' || item.status === selectedStatus;
      const matchesSearch =
        !searchTerm.trim() ||
        item.campaignName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.advertiserName?.toLowerCase().includes(searchTerm.toLowerCase());
      return matchesStatus && matchesSearch;
    });
  }, [campaignAnalytics, selectedStatus, searchTerm]);

  const columns = [
    {
      key: 'campaignName',
      label: 'Campaign',
      render: (row) => (
        <div>
          <div className="font-semibold text-slate-900">{row.campaignName}</div>
          <div className="text-xs text-slate-400">Advertiser: {row.advertiserName}</div>
        </div>
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
      key: 'impressions',
      label: 'Total Impressions',
      align: 'right',
      render: (row) => (
        <span className="font-mono font-medium text-slate-900">
          {row.impressions?.toLocaleString() ?? 0}
        </span>
      ),
    },
    {
      key: 'clicks',
      label: 'Total Clicks',
      align: 'right',
      render: (row) => (
        <span className="font-mono font-medium text-emerald-600">
          {row.clicks?.toLocaleString() ?? 0}
        </span>
      ),
    },
    {
      key: 'ctr',
      label: 'CTR (%)',
      align: 'right',
      render: (row) => (
        <span className="font-mono font-bold text-blue-600">
          {row.ctr ?? 0}%
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Performance Analytics
          </h1>
          <p className="text-sm text-slate-500">
            Real-time conversion tracking, campaign attribution, and Click-Through Rate metrics.
          </p>
        </div>

        <button
          onClick={fetchAnalytics}
          disabled={loading}
          className="inline-flex items-center gap-2 px-3.5 py-2 bg-white border border-slate-200 rounded-lg text-xs font-semibold text-slate-700 shadow-xs hover:bg-slate-50 disabled:opacity-50 transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Data
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center gap-3 text-rose-800 text-sm">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Top 3 Summary StatCards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <StatCard
          title="Platform Impressions"
          value={overview?.totalImpressions?.toLocaleString() ?? 0}
          icon={Eye}
          color="blue"
          loading={loading}
          subtitle="Total delivered ad units"
        />
        <StatCard
          title="Platform Clicks"
          value={overview?.totalClicks?.toLocaleString() ?? 0}
          icon={MousePointerClick}
          color="green"
          loading={loading}
          subtitle="User engagement interactions"
        />
        <StatCard
          title="Overall Platform CTR"
          value={`${overview?.averageCtr ?? 0}%`}
          icon={Percent}
          color="indigo"
          loading={loading}
          subtitle="Calculated: (Clicks / Impressions) * 100"
        />
      </div>

      {/* Campaign Analytics Section with Filters */}
      <div className="bg-white rounded-xl border border-slate-200/80 shadow-sm overflow-hidden">
        {/* Filter Toolbar */}
        <div className="p-4 border-b border-slate-100 bg-slate-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-blue-600" />
            <h2 className="text-base font-semibold text-slate-900">
              Campaign-Level Attribution
            </h2>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {/* Status Filter */}
            <div className="flex items-center gap-1 text-xs">
              <Filter className="w-3.5 h-3.5 text-slate-400" />
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-2.5 py-1.5 bg-white border border-slate-200 rounded-lg text-xs text-slate-700 focus:outline-none focus:ring-1 focus:ring-blue-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="PAUSED">PAUSED</option>
                <option value="COMPLETED">COMPLETED</option>
              </select>
            </div>

            {/* Campaign Search */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Filter campaigns..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-8 pr-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs text-slate-700 placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>

        {/* Campaign Analytics Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/70 text-slate-600 font-semibold text-xs uppercase tracking-wider">
                {columns.map((col, idx) => (
                  <th
                    key={idx}
                    className={`py-3.5 px-4 ${
                      col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : 'text-left'
                    }`}
                  >
                    {col.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                <tr>
                  <td colSpan={columns.length}>
                    <LoadingSpinner text="Loading campaign analytics..." />
                  </td>
                </tr>
              ) : filteredCampaigns.length === 0 ? (
                <tr>
                  <td colSpan={columns.length} className="py-10 text-center text-slate-400 text-sm">
                    No matching campaign performance data found.
                  </td>
                </tr>
              ) : (
                filteredCampaigns.map((row) => (
                  <tr key={row.campaignId} className="hover:bg-slate-50/80 transition-colors">
                    {columns.map((col, cIdx) => (
                      <td
                        key={cIdx}
                        className={`py-3.5 px-4 align-middle ${
                          col.align === 'right'
                            ? 'text-right'
                            : col.align === 'center'
                            ? 'text-center'
                            : 'text-left'
                        }`}
                      >
                        {col.render ? col.render(row) : row[col.key]}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Diagnostic Ad-Level Inspector */}
      <div className="bg-white rounded-xl border border-slate-200/80 p-5 shadow-xs">
        <h3 className="text-sm font-semibold text-slate-900 mb-1">
          Individual Advertisement Diagnostic Inspector
        </h3>
        <p className="text-xs text-slate-500 mb-4">
          Query the live `/api/ad-server/{'{adId}'}/analytics` endpoint to inspect raw metrics for a specific creative.
        </p>

        <form onSubmit={handleLookupAd} className="flex flex-col sm:flex-row gap-2 max-w-md">
          <input
            type="number"
            placeholder="Enter Ad ID (e.g. 101)"
            value={lookupAdId}
            onChange={(e) => setLookupAdId(e.target.value)}
            className="flex-1 px-3.5 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
          <button
            type="submit"
            disabled={lookupLoading}
            className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-semibold disabled:opacity-50 transition"
          >
            {lookupLoading ? 'Querying...' : 'Lookup Metrics'}
          </button>
        </form>

        {lookupError && (
          <div className="mt-3 p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{lookupError}</span>
          </div>
        )}

        {adLookupResult && (
          <div className="mt-4 p-4 bg-slate-50 border border-slate-200 rounded-xl grid grid-cols-4 gap-4 text-center max-w-lg">
            <div>
              <div className="text-[11px] text-slate-500 font-medium uppercase">Ad ID</div>
              <div className="text-base font-bold text-slate-900">#{adLookupResult.adId}</div>
            </div>
            <div>
              <div className="text-[11px] text-slate-500 font-medium uppercase">Impressions</div>
              <div className="text-base font-bold text-slate-900">
                {adLookupResult.impressions?.toLocaleString()}
              </div>
            </div>
            <div>
              <div className="text-[11px] text-slate-500 font-medium uppercase">Clicks</div>
              <div className="text-base font-bold text-emerald-600">
                {adLookupResult.clicks?.toLocaleString()}
              </div>
            </div>
            <div>
              <div className="text-[11px] text-slate-500 font-medium uppercase">CTR</div>
              <div className="text-base font-bold text-blue-600">{adLookupResult.ctr}%</div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
