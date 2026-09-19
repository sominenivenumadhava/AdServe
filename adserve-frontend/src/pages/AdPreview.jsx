import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  ExternalLink,
  Eye,
  MousePointerClick,
  Percent,
  CheckCircle,
  AlertTriangle,
  Sparkles,
} from 'lucide-react';
import LoadingSpinner from '../components/LoadingSpinner/LoadingSpinner';
import {
  getAdById,
  getAdAnalytics,
  recordImpression,
  recordClick,
} from '../services/api';

export default function AdPreview() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [ad, setAd] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [trackingAction, setTrackingAction] = useState(false);

  const fetchAdDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      const [adRes, analyticsRes] = await Promise.all([
        getAdById(id),
        getAdAnalytics(id).catch(() => null), // If no analytics yet
      ]);
      setAd(adRes?.data || null);
      setAnalytics(analyticsRes?.data || { impressions: 0, clicks: 0, ctr: 0 });
    } catch (err) {
      setError(err.message || 'Failed to load advertisement preview');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdDetails();
  }, [id]);

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3500);
  };

  const handleManualImpression = async () => {
    setTrackingAction(true);
    try {
      await recordImpression(id);
      showNotification('Impression beacon successfully recorded in MySQL!');
      const updatedAnalytics = await getAdAnalytics(id);
      setAnalytics(updatedAnalytics?.data);
    } catch (err) {
      showNotification(err.message || 'Failed to record impression', 'error');
    } finally {
      setTrackingAction(false);
    }
  };

  const handleAdClick = async () => {
    setTrackingAction(true);
    try {
      await recordClick(id);
      showNotification('Click event recorded in MySQL! Opening destination URL...');
      const updatedAnalytics = await getAdAnalytics(id);
      setAnalytics(updatedAnalytics?.data);

      if (ad?.targetUrl) {
        window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
      }
    } catch (err) {
      showNotification(err.message || 'Failed to record click', 'error');
    } finally {
      setTrackingAction(false);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Rendering ad creative preview..." />;
  }

  if (error || !ad) {
    return (
      <div className="p-8 max-w-lg mx-auto text-center space-y-4">
        <AlertTriangle className="w-12 h-12 text-rose-500 mx-auto" />
        <h2 className="text-xl font-bold text-slate-800">Advertisement Not Found</h2>
        <p className="text-sm text-slate-500">{error || 'Could not find ad with ID: ' + id}</p>
        <button
          onClick={() => navigate('/ads')}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700"
        >
          Return to Advertisements
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Breadcrumb */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/ads')}
          className="inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-slate-800 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Advertisements
        </button>

        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400">Ad Creative ID:</span>
          <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-800 border">
            #{ad.id}
          </span>
        </div>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div
          className={`p-4 rounded-xl border flex items-center gap-3 text-sm transition-all ${
            notification.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-800'
              : 'bg-emerald-50 border-emerald-200 text-emerald-800'
          }`}
        >
          <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
          <span>{notification.message}</span>
        </div>
      )}

      {/* Real-time Analytics Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200/80 shadow-xs grid grid-cols-3 gap-4 text-center">
        <div>
          <div className="flex items-center justify-center gap-1 text-slate-400 text-xs mb-1">
            <Eye className="w-3.5 h-3.5" />
            <span>Impressions</span>
          </div>
          <div className="text-xl font-bold text-slate-900">
            {analytics?.impressions?.toLocaleString() ?? 0}
          </div>
        </div>
        <div>
          <div className="flex items-center justify-center gap-1 text-slate-400 text-xs mb-1">
            <MousePointerClick className="w-3.5 h-3.5 text-emerald-500" />
            <span>Clicks</span>
          </div>
          <div className="text-xl font-bold text-emerald-600">
            {analytics?.clicks?.toLocaleString() ?? 0}
          </div>
        </div>
        <div>
          <div className="flex items-center justify-center gap-1 text-slate-400 text-xs mb-1">
            <Percent className="w-3.5 h-3.5 text-blue-500" />
            <span>CTR</span>
          </div>
          <div className="text-xl font-bold text-blue-600">
            {analytics?.ctr ?? 0}%
          </div>
        </div>
      </div>

      {/* Realistic Ad Container (Simulation) */}
      <div className="bg-slate-900/5 rounded-2xl p-6 sm:p-10 border border-dashed border-slate-300">
        <div className="max-w-md mx-auto bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden">
          {/* Ad Header Label */}
          <div className="px-4 py-2 bg-slate-50 border-b border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
            <span className="font-semibold uppercase tracking-wider text-slate-500 flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-blue-500" />
              Sponsored Advertisement
            </span>
            <span className="font-medium text-slate-400">Campaign: {ad.campaignName}</span>
          </div>

          {/* Ad Image Creative */}
          <div className="relative w-full h-56 bg-slate-100 overflow-hidden">
            <img
              src={ad.imageUrl}
              alt={ad.title}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.src =
                  'https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80';
              }}
            />
          </div>

          {/* Ad Body */}
          <div className="p-5 text-center space-y-4">
            <h3 className="text-xl font-bold text-slate-900 leading-tight">
              {ad.title}
            </h3>

            <p className="text-xs text-slate-500 max-w-xs mx-auto">
              Click the button below to interact with this advertisement and record a live conversion click.
            </p>

            {/* Click to Action Button */}
            <div>
              <button
                onClick={handleAdClick}
                disabled={trackingAction}
                className="w-full py-3 px-6 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-semibold rounded-xl shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition transform active:scale-98"
              >
                <span>Learn More</span>
                <ExternalLink className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Diagnostic Actions */}
      <div className="bg-white p-5 rounded-xl border border-slate-200/80 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h4 className="text-sm font-semibold text-slate-800">
            Developer / QA Diagnostic Beacon Test
          </h4>
          <p className="text-xs text-slate-500 mt-0.5">
            Manually trigger an impression event into MySQL to test real-time analytics aggregation.
          </p>
        </div>

        <button
          onClick={handleManualImpression}
          disabled={trackingAction}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold border border-slate-200 transition"
        >
          <Eye className="w-4 h-4" />
          Fire Impression Beacon
        </button>
      </div>
    </div>
  );
}
