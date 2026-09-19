import React, { createContext, useContext, useState, useEffect } from 'react';
import { loginUser, registerUser, getCurrentUser } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(true);

  // Validate session on mount if token exists
  useEffect(() => {
    const verifyToken = async () => {
      const storedToken = localStorage.getItem('token');
      if (!storedToken) {
        setLoading(false);
        return;
      }

      try {
        const response = await getCurrentUser();
        const userData = response.data || response;
        setUser(userData);
        localStorage.setItem('user', JSON.stringify(userData));
      } catch (err) {
        console.warn('Session expired or invalid, clearing credentials:', err.message);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    verifyToken();
  }, []);

  const login = async (email, password) => {
    const response = await loginUser({ email, password });
    const payload = response.data || response;
    const { token: jwtToken, user: userInfo } = payload;

    localStorage.setItem('token', jwtToken);
    localStorage.setItem('user', JSON.stringify(userInfo));

    setToken(jwtToken);
    setUser(userInfo);
    return userInfo;
  };

  const register = async (name, email, password) => {
    const response = await registerUser({ name, email, password });
    const payload = response.data || response;
    const { token: jwtToken, user: userInfo } = payload;

    localStorage.setItem('token', jwtToken);
    localStorage.setItem('user', JSON.stringify(userInfo));

    setToken(jwtToken);
    setUser(userInfo);
    return userInfo;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  const value = {
    user,
    token,
    role: user?.role || null,
    isAdmin: user?.role === 'ADMIN',
    isAdvertiser: user?.role === 'ADVERTISER',
    advertiserId: user?.advertiserId || null,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
