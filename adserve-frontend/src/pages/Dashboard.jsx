import React, { useEffect, useState } from 'react';
import {
  Users,
  Megaphone,
  Image as ImageIcon,
  Eye,
  MousePointerClick,
  Percent,
  TrendingUp,
  RefreshCw,
  AlertTriangle,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import StatCard from '../components/StatCard/StatCard';
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner';
import { getDashboardOverview, getCampaignAnalytics } from '../services/api';

export default function Dashboard() {
  const [overview, setOverview] = useState(null);
  const [campaignData, setCampaignData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeMetric, setActiveMetric] = useState('impressions'); // 'impressions' | 'clicks' | 'ctr'

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [overviewRes, campaignRes] = await Promise.all([
        getDashboardOverview(),
        getCampaignAnalytics(),
      ]);

      setOverview(overviewRes?.data || {});
      setCampaignData(campaignRes?.data || []);
    } catch (err) {
      setError(err.message || 'Failed to load dashboard data from backend');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const metricConfig = {
    impressions: {
      label: 'Impressions',
      key: 'impressions',
      color: '#3B82F6',
      unit: '',
    },
    clicks: {
      label: 'Clicks',
      key: 'clicks',
      color: '#10B981',
      unit: '',
    },
    ctr: {
      label: 'CTR (%)',
      key: 'ctr',
      color: '#8B5CF6',
      unit: '%',
    },
  };

  const currentConfig = metricConfig[activeMetric];

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Executive Dashboard
          </h1>
          <p className="text-sm text-slate-500">
            Real-time platform overview, campaign serving metrics, and engagement analytics.
          </p>
        </div>

        <button
          onClick={fetchData}
          disabled={loading}
          className="inline-flex items-center gap-2 px-3.5 py-2 bg-white border border-slate-200 rounded-lg text-xs font-semibold text-slate-700 shadow-xs hover:bg-slate-50 disabled:opacity-50 transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Metrics
        </button>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-3 text-rose-800 text-sm">
          <AlertTriangle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
          <div>
            <div className="font-semibold">Backend Connection Issue</div>
            <div>{error}</div>
            <p className="text-xs text-rose-600 mt-1">
              Ensure Spring Boot backend is running on http://localhost:8080.
            </p>
          </div>
        </div>
      )}

      {/* 6 Key StatCards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <StatCard
          title="Total Advertisers"
          value={overview?.totalAdvertisers?.toLocaleString() ?? 0}
          icon={Users}
          color="blue"
          loading={loading}
          subtitle="Registered accounts"
        />
        <StatCard
          title="Total Campaigns"
          value={overview?.totalCampaigns?.toLocaleString() ?? 0}
          icon={Megaphone}
          color="indigo"
          loading={loading}
          subtitle="Active & paused"
        />
        <StatCard
          title="Total Creatives"
          value={overview?.totalAdvertisements?.toLocaleString() ?? 0}
          icon={ImageIcon}
          color="purple"
          loading={loading}
          subtitle="Targeted ad creatives"
        />
        <StatCard
          title="Total Impressions"
          value={overview?.totalImpressions?.toLocaleString() ?? 0}
          icon={Eye}
          color="cyan"
          loading={loading}
          subtitle="Served impressions"
        />
        <StatCard
          title="Total Clicks"
          value={overview?.totalClicks?.toLocaleString() ?? 0}
          icon={MousePointerClick}
          color="green"
          loading={loading}
          subtitle="User interactions"
        />
        <StatCard
          title="Average CTR"
          value={`${overview?.averageCtr ?? 0}%`}
          icon={Percent}
          color="amber"
          loading={loading}
          subtitle="Overall conversion"
        />
      </div>

      {/* Performance Chart Card */}
      <div className="bg-white rounded-xl border border-slate-200/80 p-5 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4 mb-5">
          <div>
            <div className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-blue-600" />
              <h2 className="text-base font-semibold text-slate-900">
                Campaign Performance Comparison
              </h2>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Compare volume and interaction rate across all targeted campaigns
            </p>
          </div>

          {/* Metric Switcher */}
          <div className="inline-flex bg-slate-100 p-1 rounded-lg border border-slate-200 text-xs">
            {Object.keys(metricConfig).map((key) => {
              const cfg = metricConfig[key];
              const isSelected = activeMetric === key;
              return (
                <button
                  key={key}
                  onClick={() => setActiveMetric(key)}
                  className={`px-3 py-1.5 rounded-md font-medium transition ${
                    isSelected
                      ? 'bg-white text-slate-900 shadow-xs font-semibold'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {cfg.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Recharts Container */}
        {loading ? (
          <LoadingSpinner text="Aggregating campaign analytics..." />
        ) : campaignData.length === 0 ? (
          <div className="h-64 flex flex-col items-center justify-center text-slate-400 text-sm">
            <Megaphone className="w-10 h-10 stroke-1 text-slate-300 mb-2" />
            <p>No campaign performance records found.</p>
            <p className="text-xs text-slate-500 mt-1">
              Create campaigns and serve ads to see visual performance charts.
            </p>
          </div>
        ) : (
          <div className="h-80 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={campaignData}
                margin={{ top: 10, right: 20, left: 0, bottom: 25 }}
              >
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis
                  dataKey="campaignName"
                  tick={{ fill: '#64748B', fontSize: 12 }}
                  tickLine={false}
                  axisLine={{ stroke: '#CBD5E1' }}
                  interval={0}
                  angle={-15}
                  textAnchor="end"
                />
                <YAxis
                  tick={{ fill: '#64748B', fontSize: 12 }}
                  tickLine={false}
                  axisLine={{ stroke: '#CBD5E1' }}
                  unit={currentConfig.unit}
                />
                <Tooltip
                  formatter={(value) => [`${value}${currentConfig.unit}`, currentConfig.label]}
                  contentStyle={{
                    backgroundColor: '#0F172A',
                    borderRadius: '8px',
                    color: '#FFF',
                    border: 'none',
                    fontSize: '12px',
                  }}
                  itemStyle={{ color: '#FFF' }}
                />
                <Bar
                  dataKey={currentConfig.key}
                  fill={currentConfig.color}
                  radius={[6, 6, 0, 0]}
                  barSize={40}
                >
                  {campaignData.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={
                        index % 2 === 0
                          ? currentConfig.color
                          : `${currentConfig.color}CC`
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}
