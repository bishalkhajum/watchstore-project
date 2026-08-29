import { useEffect, useState } from 'react';
import client from '../api/client';
import StatusBadge from '../components/StatusBadge';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [retryingId, setRetryingId] = useState(null);
  const [retryData, setRetryData] = useState(null);

  const load = () => {
    setLoading(true);
    client.get('/orders').then((res) => setOrders(res.data)).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const retryPayment = async (orderNumber) => {
    setRetryingId(orderNumber);
    try {
      const { data } = await client.post(`/orders/${orderNumber}/retry-payment`);
      setRetryData(data);
    } catch (err) {
      alert(err.response?.data?.error || 'Could not retry payment');
      setRetryingId(null);
    }
  };

  // Auto-submit the retry payment form once we have it, same pattern as Checkout.
  useEffect(() => {
    if (retryData) {
      const form = document.getElementById('retry-form');
      if (form) form.submit();
    }
  }, [retryData]);

  if (loading) return <div className="page container">Loading...</div>;

  if (retryData) {
    return (
      <div className="page container">
        <div className="center-box">
          <h3>Redirecting you to eSewa...</h3>
          <form id="retry-form" action={retryData.esewaPaymentUrl} method="POST">
            {Object.entries(retryData.esewaFormFields).map(([key, value]) => (
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
      <div className="page-title">My Orders</div>

      {orders.length === 0 ? (
        <div className="empty-state">You haven't placed any orders yet.</div>
      ) : (
        orders.map((order) => (
          <div className="order-card" key={order.id}>
            <div className="order-card-header">
              <div>
                <strong>{order.orderNumber}</strong>
                <div style={{ fontSize: 12, color: '#6b7280' }}>
                  {new Date(order.createdAt).toLocaleString()}
                </div>
              </div>
              <StatusBadge status={order.status} />
            </div>

            <table>
              <thead>
                <tr><th>Item</th><th>Qty</th><th>Price</th></tr>
              </thead>
              <tbody>
                {order.items.map((item, idx) => (
                  <tr key={idx}>
                    <td>{item.productName}</td>
                    <td>{item.quantity}</td>
                    <td>NPR {Number(item.priceAtPurchase).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
              <div style={{ fontWeight: 700 }}>Total: NPR {Number(order.totalAmount).toLocaleString()}</div>
              {(order.status === 'PENDING_PAYMENT' || order.status === 'FAILED') && (
                <button className="btn btn-gold" disabled={retryingId === order.orderNumber}
                  onClick={() => retryPayment(order.orderNumber)}>
                  {retryingId === order.orderNumber ? 'Preparing...' : 'Retry payment'}
                </button>
              )}
            </div>
          </div>
        ))
      )}
    </div>
  );
}
