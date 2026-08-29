import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function ProductDetail({ refreshCart }) {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [qty, setQty] = useState(1);
  const [message, setMessage] = useState('');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    client.get(`/products/${id}`).then((res) => setProduct(res.data));
  }, [id]);

  const addToCart = async () => {
    if (!user) {
      navigate('/login');
      return;
    }
    setAdding(true);
    setMessage('');
    try {
      await client.post('/cart/items', { productId: product.id, quantity: qty });
      setMessage('Added to cart.');
      refreshCart();
    } catch (err) {
      setMessage(err.response?.data?.error || 'Could not add to cart');
    } finally {
      setAdding(false);
    }
  };

  if (!product) return <div className="page container">Loading...</div>;

  return (
    <div className="page container">
      <div className="product-layout" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 40 }}>
        <img src={product.imageUrl} alt={product.name}
          style={{ width: '100%', borderRadius: 10, objectFit: 'cover', maxHeight: 480 }} />
        <div>
          <div className="card-brand">{product.brand}</div>
          <h1 style={{ margin: '4px 0' }}>{product.name}</h1>
          <div style={{ fontSize: 22, fontWeight: 700, margin: '12px 0' }}>
            NPR {Number(product.price).toLocaleString()}
          </div>
          <p style={{ color: '#4b5563', lineHeight: 1.6 }}>{product.description}</p>
          <p style={{ fontSize: 13, color: '#6b7280' }}>
            {product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : 'Out of stock'}
          </p>

          {product.stockQuantity > 0 && (
            <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 20 }}>
              <input
                type="number"
                min={1}
                max={product.stockQuantity}
                value={qty}
                onChange={(e) => setQty(Math.max(1, Math.min(product.stockQuantity, Number(e.target.value))))}
                className="qty-input"
              />
              <button className="btn btn-primary" disabled={adding} onClick={addToCart}>
                {adding ? 'Adding...' : 'Add to cart'}
              </button>
            </div>
          )}
          {message && <p style={{ marginTop: 12 }}>{message}</p>}
        </div>
      </div>
    </div>
  );
}
