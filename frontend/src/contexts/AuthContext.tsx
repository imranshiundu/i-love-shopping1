'use client';
import { createContext, useContext, useState, useEffect, useCallback, useRef, ReactNode } from 'react';
import { auth as authApi, cart as cartRest, setAccessToken } from '@/services/api';
import { User, Cart } from '@/types';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  cart: Cart | null;
  cartLoading: boolean;
  refreshCart: () => Promise<void>;
  addToCart: (productId: string, quantity: number) => Promise<void>;
  cartCount: number;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [cart, setCart] = useState<Cart | null>(null);
  const [cartLoading, setCartLoading] = useState(true);
  const hydrated = useRef(false);

  const refreshCart = useCallback(async () => {
    try {
      const res = await cartRest.get();
      setCart(res.data ?? null);
    } catch { setCart(null); }
    setCartLoading(false);
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const res = await authApi.getProfile();
      if (res.data) setUser(res.data as unknown as User);
    } catch { setUser(null); setAccessToken(null); }
  }, []);

  useEffect(() => {
    if (hydrated.current) return;
    hydrated.current = true;

    // 1. Set loading false immediately — don't block UI
    setLoading(false);

    // 2. Hydrate session + cart in background (non-blocking)
    (async () => {
      try {
        const tokenRes = await authApi.refresh();
        if (tokenRes.data?.accessToken) {
          setAccessToken(tokenRes.data.accessToken);
          setUser(tokenRes.data.user);
        }
      } catch { /* not logged in — that's fine */ }
      // Load cart regardless of auth state
      refreshCart();
    })();
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
    const res = await authApi.register(email, password, name, 'dev-bypass-token');
    if (res.data?.accessToken) {
      setAccessToken(res.data.accessToken);
      setUser(res.data.user);
      await refreshCart();
    }
  };

  const logout = async () => {
    try { await authApi.logout(); } catch { /* ok */ }
    setAccessToken(null);
    setUser(null);
    setCart(null);
  };

  const addToCart = useCallback(async (productId: string, quantity: number) => {
    await cartRest.addItem(productId, quantity);
    await refreshCart();
  }, [refreshCart]);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser, cart, cartLoading, refreshCart, addToCart, cartCount: cart?.totalItems || 0 }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

