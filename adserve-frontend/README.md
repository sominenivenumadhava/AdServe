# AdServe Frontend — React Admin Dashboard

Professional, responsive SaaS admin dashboard for the **AdServe** Mini Ad Serving & Campaign Analytics Platform.

Built with **React 18**, **Vite**, **Tailwind CSS**, **Recharts**, and **Axios**, communicating in real time with the Spring Boot backend REST APIs.

---

## Technology Stack

- **Framework**: React 18 & Vite
- **Routing**: React Router DOM v6
- **Styling**: Tailwind CSS (Navy / Slate enterprise design system)
- **Charts**: Recharts (Dynamic Bar Chart with metric switching)
- **HTTP Client**: Axios with centralized error and response unwrapping
- **Icons**: Lucide React

---

## Available Application Routes

| Route | Page Component | Description |
| :--- | :--- | :--- |
| `/dashboard` | `Dashboard.jsx` | 6 KPI StatCards, Recharts performance chart (Impressions / Clicks / CTR), recent campaign overview. |
| `/advertisers` | `Advertisers.jsx` | Advertisers table, creation modal with email validation, editing, and deletion confirmation. |
| `/campaigns` | `Campaigns.jsx` | Campaign table with status pills, targeting chips, and modal form enforcing budget > 0 & date rules. |
| `/ads` | `Advertisements.jsx` | Ad creative inventory, destination URLs, status badges, and live creative thumbnail preview. |
| `/ad-preview/:id` | `AdPreview.jsx` | Realistic banner ad simulation, "Learn More" click tracker, manual impression beacon dispatcher. |
| `/analytics` | `Analytics.jsx` | Platform-wide totals, campaign attribution table with filters, and per-ad live diagnostic inspector. |

---

## Environment Configuration

Configuration is managed via `.env` in the root of `adserve-frontend`:

```env
# URL where your Spring Boot backend is listening
VITE_API_BASE_URL=http://localhost:8080
```

> [!NOTE]
> The application uses `api.js` to read `import.meta.env.VITE_API_BASE_URL`. Do not hardcode localhost URLs in individual components.

---

## Installation & Running Locally

### Prerequisites
- Node.js version 18 or higher
- npm or yarn
- AdServe Spring Boot backend running on `http://localhost:8080`

### 1. Install Dependencies
```bash
cd adserve-frontend
npm install
```

### 2. Start Development Server
```bash
npm run dev
```

The application will be accessible at:
```text
http://localhost:5173
```

### 3. Build for Production
```bash
npm run build
```
Production assets are generated in `adserve-frontend/dist/`.

---

## API Integration Mapping

All communication is routed through `src/services/api.js`:

| React API Function | HTTP Method & Path | Backend Service |
| :--- | :--- | :--- |
| `getHealth()` | `GET /api/v1/health` | `HealthController` |
| `getAdvertisers()` | `GET /api/advertisers` | `AdvertiserService` |
| `createAdvertiser(data)` | `POST /api/advertisers` | `AdvertiserService` |
| `updateAdvertiser(id, data)`| `PUT /api/advertisers/{id}` | `AdvertiserService` |
| `deleteAdvertiser(id)` | `DELETE /api/advertisers/{id}` | `AdvertiserService` |
| `getCampaigns()` | `GET /api/campaigns` | `CampaignService` |
| `createCampaign(data)` | `POST /api/campaigns` | `CampaignService` |
| `updateCampaign(id, data)` | `PUT /api/campaigns/{id}` | `CampaignService` |
| `deleteCampaign(id)` | `DELETE /api/campaigns/{id}` | `CampaignService` |
| `getAds()` | `GET /api/ads` | `AdvertisementService` |
| `getAdById(id)` | `GET /api/ads/{id}` | `AdvertisementService` |
| `createAd(data)` | `POST /api/ads` | `AdvertisementService` |
| `updateAd(id, data)` | `PUT /api/ads/{id}` | `AdvertisementService` |
| `deleteAd(id)` | `DELETE /api/ads/{id}` | `AdvertisementService` |
| `serveAd(params)` | `GET /api/ad-server/serve` | `AdServingService` |
| `recordImpression(id)` | `POST /api/ad-server/{id}/impression` | `ImpressionService` |
| `recordClick(id)` | `POST /api/ad-server/{id}/click` | `ClickService` |
| `getAdAnalytics(id)` | `GET /api/ad-server/{id}/analytics` | `AnalyticsService` |
| `getDashboardOverview()` | `GET /api/ad-server/analytics/overview`| `AnalyticsService` |
| `getCampaignAnalytics()` | `GET /api/ad-server/analytics/campaigns`| `AnalyticsService` |
