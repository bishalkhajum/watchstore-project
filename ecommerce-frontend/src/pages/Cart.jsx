import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import client from '../api/client';

export default function Cart({ refreshCart }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const load = () => {
    setLoading(true);
    client.get('/cart').then((res) => setItems(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const updateQty = async (productId, quantity) => {
    if (quantity < 1) return;
    await client.post('/cart/items', { productId, quantity });
    load();
    refreshCart();
  };

  const removeItem = async (cartItemId) => {
    await client.delete(`/cart/items/${cartItemId}`);
    load();
    refreshCart();
  };

  const total = items.reduce((sum, i) => sum + Number(i.lineTotal), 0);

  if (loading) return <div className="page container">Loading...</div>;

  return (
    <div className="page container">
      <div className="page-title">Your Cart</div>

      {items.length === 0 ? (
        <div className="empty-state">
          Your cart is empty. <Link to="/">Browse watches</Link>
        </div>
      ) : (
        <div className="cart-layout" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 32 }}>
          <div>
            {items.map((item) => (
              <div className="row-item" key={item.cartItemId}>
                <img src={item.imageUrl} alt={item.productName} />
                <div style={{ flex: 1 }}>
                  <div className="name">{item.productName}</div>
                  <div className="meta">NPR {Number(item.unitPrice).toLocaleString()} each</div>
                </div>
                <input
                  type="number"
                  min={1}
                  max={item.availableStock}
                  value={item.quantity}
                  className="qty-input"
                  onChange={(e) => updateQty(item.productId, Number(e.target.value))}
                />
                <div style={{ width: 100, textAlign: 'right', fontWeight: 600 }}>
                  NPR {Number(item.lineTotal).toLocaleString()}
                </div>
                <button className="btn btn-danger" onClick={() => removeItem(item.cartItemId)}>Remove</button>
              </div>
            ))}
          </div>

          <div className="summary-box">
            <div className="summary-row">
              <span>Subtotal</span>
              <span>NPR {total.toLocaleString()}</span>
            </div>
            <div className="summary-row summary-total">
              <span>Total</span>
              <span>NPR {total.toLocaleString()}</span>
            </div>
            <button className="btn btn-primary btn-block" style={{ marginTop: 16 }}
              onClick={() => navigate('/checkout')}>
              Proceed to checkout
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
