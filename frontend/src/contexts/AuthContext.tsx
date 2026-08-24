'use client';
import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { auth as authApi, setAccessToken } from '@/services/api';
import { User, Cart } from '@/types';
import { cartApi } from '@/services/cartService';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  cart: Cart | null;
  refreshCart: () => Promise<void>;
  cartCount: number;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [cart, setCart] = useState<Cart | null>(null);

  const refreshCart = useCallback(async () => {
    try {
      const res = await cartApi.get();
      if (res.data) setCart(res.data);
    } catch { setCart(null); }
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const res = await authApi.getProfile();
      if (res.data) setUser(res.data as unknown as User);
    } catch { setUser(null); setAccessToken(null); }
  }, []);

  useEffect(() => {
    const init = async () => {
      try {
        const tokenRes = await authApi.refresh();
        if (tokenRes.data?.accessToken) {
          setAccessToken(tokenRes.data.accessToken);
          setUser(tokenRes.data.user);
          await refreshCart();
        }
      } catch { /* not logged in */ }
      setLoading(false);
    };
    init();
  }, [refreshCart]);

  const login = async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    if (res.data?.accessToken) {
      setAccessToken(res.data.accessToken);
      setUser(res.data.user);
      await refreshCart();
    }
  };

  const register = async (email: string, password: string, name: string) => {
    await authApi.register(email, password, name, 'dev-token');
  };

  const logout = async () => {
    try { await authApi.logout(); } catch { /* ok */ }
    setAccessToken(null);
    setUser(null);
    setCart(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser, cart, refreshCart, cartCount: cart?.totalItems || 0 }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
