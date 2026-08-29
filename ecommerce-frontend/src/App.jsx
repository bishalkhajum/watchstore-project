import { useCallback, useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import PrivateRoute from './components/PrivateRoute';
import Home from './pages/Home';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import PaymentResult from './pages/PaymentResult';
import Orders from './pages/Orders';
import Login from './pages/Login';
import Register from './pages/Register';
import Admin from './pages/Admin';
import client from './api/client';
import { useAuth } from './context/AuthContext';

export default function App() {
  const { user } = useAuth();
  const [cartCount, setCartCount] = useState(0);

  const refreshCart = useCallback(() => {
    if (!user) {
      setCartCount(0);
      return;
    }
    client.get('/cart')
      .then((res) => setCartCount(res.data.reduce((sum, i) => sum + i.quantity, 0)))
      .catch(() => setCartCount(0));
  }, [user]);

  useEffect(() => { refreshCart(); }, [refreshCart]);

  return (
    <>
      <Navbar cartCount={cartCount} />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/products/:id" element={<ProductDetail refreshCart={refreshCart} />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/cart" element={<PrivateRoute><Cart refreshCart={refreshCart} /></PrivateRoute>} />
        <Route path="/checkout" element={<PrivateRoute><Checkout refreshCart={refreshCart} /></PrivateRoute>} />
        <Route path="/payment/result" element={<PaymentResult refreshCart={refreshCart} />} />
        <Route path="/orders" element={<PrivateRoute><Orders /></PrivateRoute>} />
        <Route path="/admin" element={<PrivateRoute adminOnly><Admin /></PrivateRoute>} />
      </Routes>
    </>
  );
}
