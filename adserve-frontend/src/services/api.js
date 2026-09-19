import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor to attach JWT Bearer token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to unwrap Spring Boot ApiResponse<T> and handle auth errors
api.interceptors.response.use(
  (response) => {
    // If it's a standard ApiResponse envelope, return data directly or payload
    return response.data;
  },
  (error) => {
    // If 401 Unauthorized occurs on an authenticated route, clear stored session
    if (error.response?.status === 401 && !error.config?.url?.includes('/api/auth/login')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Extract server message from standard ErrorResponse envelope
    const serverError = error.response?.data;
    const message =
      serverError?.message ||
      (serverError?.validationErrors
        ? Object.values(serverError.validationErrors).join(', ')
        : null) ||
      error.message ||
      'An unexpected network error occurred';

    const enhancedError = new Error(message);
    enhancedError.status = error.response?.status;
    enhancedError.details = serverError?.validationErrors;
    enhancedError.raw = serverError;

    return Promise.reject(enhancedError);
  }
);

// ==============================================================================
// Authentication API
// ==============================================================================
export const loginUser = (credentials) => api.post('/api/auth/login', credentials);
export const registerUser = (userData) => api.post('/api/auth/register', userData);
export const getCurrentUser = () => api.get('/api/auth/me');

// ==============================================================================
// Health Check
// ==============================================================================
export const getHealth = () => api.get('/api/v1/health');

// ==============================================================================
// Advertisers API
// ==============================================================================
export const getAdvertisers = () => api.get('/api/advertisers');
export const getAdvertiserById = (id) => api.get(`/api/advertisers/${id}`);
export const createAdvertiser = (data) => api.post('/api/advertisers', data);
export const updateAdvertiser = (id, data) => api.put(`/api/advertisers/${id}`, data);
export const deleteAdvertiser = (id) => api.delete(`/api/advertisers/${id}`);

// ==============================================================================
// Campaigns API
// ==============================================================================
export const getCampaigns = () => api.get('/api/campaigns');
export const getCampaignById = (id) => api.get(`/api/campaigns/${id}`);
export const createCampaign = (data) => api.post('/api/campaigns', data);
export const updateCampaign = (id, data) => api.put(`/api/campaigns/${id}`, data);
export const deleteCampaign = (id) => api.delete(`/api/campaigns/${id}`);

// ==============================================================================
// Advertisements API
// ==============================================================================
export const getAds = () => api.get('/api/ads');
export const getAdById = (id) => api.get(`/api/ads/${id}`);
export const createAd = (data) => api.post('/api/ads', data);
export const updateAd = (id, data) => api.put(`/api/ads/${id}`, data);
export const deleteAd = (id) => api.delete(`/api/ads/${id}`);

// ==============================================================================
// Ad Server & Analytics API
// ==============================================================================
export const serveAd = ({ country, device, category }) =>
  api.get('/api/ad-server/serve', {
    params: { country, device, category },
  });

export const recordImpression = (adId) => api.post(`/api/ad-server/${adId}/impression`);
export const recordClick = (adId) => api.post(`/api/ad-server/${adId}/click`);
export const getAdAnalytics = (adId) => api.get(`/api/ad-server/${adId}/analytics`);
export const getDashboardOverview = () => api.get('/api/ad-server/analytics/overview');
export const getCampaignAnalytics = () => api.get('/api/ad-server/analytics/campaigns');
export const clearCache = () => api.post('/api/ad-server/cache/clear');
export const getCacheMetrics = () => api.get('/api/ad-server/cache/metrics');
export const getKafkaMetrics = () => api.get('/api/ad-server/kafka/metrics');

export default api;
