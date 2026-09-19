import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  Megaphone,
  Image as ImageIcon,
  BarChart3,
  X,
  Shield,
  Briefcase,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const NAV_ITEMS = [
  { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { name: 'Advertisers', path: '/advertisers', icon: Building2, adminOnly: true },
  { name: 'Campaigns', path: '/campaigns', icon: Megaphone },
  { name: 'Advertisements', path: '/ads', icon: ImageIcon },
  { name: 'Analytics', path: '/analytics', icon: BarChart3 },
];

export default function Sidebar({ isOpen, onClose }) {
  const { isAdmin, role, user } = useAuth();

  const filteredNavItems = NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin);

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-xs lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar Panel */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-40 w-64 bg-slate-900 text-slate-300 border-r border-slate-800 flex flex-col transition-transform duration-200 ease-in-out lg:translate-x-0 lg:static ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Header / Brand for Mobile */}
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-800 lg:hidden">
          <span className="text-lg font-bold text-white tracking-tight">
            Ad<span className="text-blue-500">Serve</span>
          </span>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-md"
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Section */}
        <div className="flex-1 py-6 px-4 space-y-1.5 overflow-y-auto">
          <div className="px-3 mb-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">
            Platform Navigation
          </div>

          {filteredNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white shadow-sm shadow-blue-500/30'
                      : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/70'
                  }`
                }
              >
                <Icon className="w-4 h-4 shrink-0" />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </div>

        {/* System & Role Info Footer */}
        <div className="p-4 border-t border-slate-800/80 bg-slate-950/40 text-xs text-slate-400 space-y-2">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-slate-400">
              {isAdmin ? <Shield className="w-3.5 h-3.5 text-purple-400" /> : <Briefcase className="w-3.5 h-3.5 text-emerald-400" />}
              Role Scope
            </span>
            <span className={`font-semibold ${isAdmin ? 'text-purple-400' : 'text-emerald-400'}`}>
              {role || 'Guest'}
            </span>
          </div>
          <div className="flex items-center justify-between text-[11px] text-slate-500">
            <span>Spring Boot + JWT</span>
            <span className="text-emerald-400">Stateless</span>
          </div>
        </div>
      </aside>
    </>
  );
}
