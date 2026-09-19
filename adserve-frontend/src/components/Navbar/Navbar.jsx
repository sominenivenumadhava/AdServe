import React, { useEffect, useState } from 'react';
import { Layers, Menu, LogOut, ShieldCheck, UserCheck } from 'lucide-react';
import { getHealth } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function Navbar({ onToggleSidebar }) {
  const [backendHealthy, setBackendHealthy] = useState(null);
  const { user, role, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const checkStatus = async () => {
      try {
        await getHealth();
        setBackendHealthy(true);
      } catch (err) {
        setBackendHealthy(false);
      }
    };

    checkStatus();
    const interval = setInterval(checkStatus, 30000); // poll every 30s
    return () => clearInterval(interval);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  return (
    <header className="sticky top-0 z-30 h-16 bg-white border-b border-slate-200/80 px-4 sm:px-6 flex items-center justify-between shadow-xs">
      {/* Left: Mobile Toggle & Brand */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="lg:hidden p-2 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-slate-700 transition"
          aria-label="Toggle navigation"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-700 to-blue-500 flex items-center justify-center text-white shadow-sm shadow-blue-500/20">
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <span className="text-lg font-bold tracking-tight text-slate-900">
              Ad<span className="text-blue-600">Serve</span>
            </span>
            <span className="hidden sm:inline-block ml-2 text-[10px] font-semibold uppercase tracking-wider px-1.5 py-0.5 rounded bg-blue-50 text-blue-700 border border-blue-200/60">
              Console
            </span>
          </div>
        </div>
      </div>

      {/* Right: Backend Health & Profile */}
      <div className="flex items-center gap-3">
        {/* Backend Status Indicator */}
        <div className="hidden sm:flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium border bg-slate-50 border-slate-200 text-slate-600">
          <span className="relative flex h-2 w-2">
            {backendHealthy === true && (
              <>
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </>
            )}
            {backendHealthy === false && (
              <span className="relative inline-flex rounded-full h-2 w-2 bg-rose-500"></span>
            )}
            {backendHealthy === null && (
              <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-400"></span>
            )}
          </span>
          <span className="text-slate-600">
            {backendHealthy === true
              ? 'Backend Online'
              : backendHealthy === false
              ? 'Backend Disconnected'
              : 'Connecting...'}
          </span>
        </div>

        {/* User Profile Pill & Role Badge */}
        {user && (
          <div className="flex items-center gap-2.5 pl-2 sm:border-l sm:border-slate-200">
            <div className="w-8 h-8 rounded-full bg-slate-800 text-white flex items-center justify-center text-xs font-semibold shadow-xs">
              {getInitials(user.name)}
            </div>
            <div className="hidden md:block text-left text-xs">
              <div className="flex items-center gap-1.5">
                <span className="font-semibold text-slate-800 leading-tight truncate max-w-[120px]">
                  {user.name}
                </span>
                <span
                  className={`text-[9px] font-bold px-1.5 py-0.2 rounded uppercase border ${
                    isAdmin
                      ? 'bg-purple-50 text-purple-700 border-purple-200'
                      : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                  }`}
                >
                  {role}
                </span>
              </div>
              <div className="text-slate-400 text-[10px] truncate max-w-[130px]">
                {user.email}
              </div>
            </div>

            {/* Logout Button */}
            <button
              onClick={handleLogout}
              title="Sign Out"
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition cursor-pointer ml-1"
              aria-label="Log out"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
