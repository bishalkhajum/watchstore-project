import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';

/**
 * Builds the order, then auto-submits a hidden HTML form POSTing straight to
 * eSewa's sandbox payment URL with the signed fields the backend generated.
 * This has to be a real form POST (not an axios/fetch call) because eSewa's
 * flow expects the user's browser to land on its own hosted payment page.
 */
export default function Checkout({ refreshCart }) {
  const [address, setAddress] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [checkoutData, setCheckoutData] = useState(null);
  const formRef = useRef(null);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await client.post('/orders/checkout', {
        shippingAddress: address,
        shippingPhone: phone,
      });
      setCheckoutData(data);
      refreshCart();
    } catch (err) {
      setError(err.response?.data?.error || 'Checkout failed');
      setLoading(false);
    }
  };

  // Once we have the eSewa form fields, auto-submit the hidden form to
  // redirect the browser to eSewa's payment page.
  useEffect(() => {
    if (checkoutData && formRef.current) {
      formRef.current.submit();
    }
  }, [checkoutData]);

  if (checkoutData) {
    return (
      <div className="page container">
        <div className="center-box">
          <h3>Redirecting you to eSewa...</h3>
          <p style={{ color: '#6b7280', fontSize: 14 }}>
            Order <strong>{checkoutData.orderNumber}</strong> created. If you're not redirected automatically,
            click below.
          </p>
          <form ref={formRef} action={checkoutData.esewaPaymentUrl} method="POST">
            {Object.entries(checkoutData.esewaFormFields).map(([key, value]) => (
              <input key={key} type="hidden" name={key} value={value} />
            ))}
            <button className="btn btn-gold" type="submit">Continue to eSewa</button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="page container">
      <div className="page-title">Checkout</div>
      <div className="page-subtitle">Where should we ship your order?</div>

      <form className="form" onSubmit={handleSubmit}>
        <div className="field">
          <label>Shipping address</label>
          <textarea rows={3} value={address} onChange={(e) => setAddress(e.target.value)} required />
        </div>
        <div className="field">
          <label>Phone number</label>
          <input value={phone} onChange={(e) => setPhone(e.target.value)} required />
        </div>
        {error && <div className="error-text">{error}</div>}
        <button className="btn btn-primary btn-block" disabled={loading}>
          {loading ? 'Creating order...' : 'Pay with eSewa'}
        </button>
        <p style={{ fontSize: 12, color: '#6b7280', marginTop: 12 }}>
          eSewa sandbox test login — ID: <strong>9806800001</strong> (or any of 9806800002-9806800005),
          Password: <strong>Nepal@123</strong>, MPIN: <strong>1122</strong>, token: <strong>123456</strong>.
        </p>
      </form>
    </div>
  );
}
