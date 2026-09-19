import React from 'react';

export default function StatCard({
  title,
  value,
  icon: Icon,
  subtitle,
  change,
  isPositive = true,
  color = 'blue',
  loading = false,
}) {
  const colorMap = {
    blue: 'bg-blue-50 text-blue-600 border-blue-100',
    green: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    indigo: 'bg-indigo-50 text-indigo-600 border-indigo-100',
    purple: 'bg-purple-50 text-purple-600 border-purple-100',
    amber: 'bg-amber-50 text-amber-600 border-amber-100',
    cyan: 'bg-cyan-50 text-cyan-600 border-cyan-100',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200/80 p-5 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
          {title}
        </span>
        {Icon && (
          <div className={`p-2.5 rounded-lg border ${colorMap[color] || colorMap.blue}`}>
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      <div className="mt-4">
        {loading ? (
          <div className="h-8 w-24 bg-slate-200 animate-pulse rounded"></div>
        ) : (
          <div className="text-2xl lg:text-3xl font-bold tracking-tight text-slate-900">
            {value}
          </div>
        )}

        {(change || subtitle) && (
          <div className="mt-2 flex items-center gap-2 text-xs">
            {change && (
              <span
                className={`font-semibold px-1.5 py-0.5 rounded ${
                  isPositive
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-rose-50 text-rose-700'
                }`}
              >
                {change}
              </span>
            )}
            {subtitle && <span className="text-slate-500">{subtitle}</span>}
          </div>
        )}
      </div>
    </div>
  );
}
