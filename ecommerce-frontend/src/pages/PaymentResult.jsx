import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import client from '../api/client';

/**
 * Landing page after eSewa redirects the browser back (whether eSewa says
 * success or failure). We NEVER trust the redirect by itself - we take the
 * transaction_uuid off the URL and ask our own backend to independently
 * re-verify with eSewa's status API before showing anything as confirmed.
 */
export default function PaymentResult({ refreshCart }) {
  const [searchParams] = useSearchParams();
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [checking, setChecking] = useState(true);

  const transactionUuid = searchParams.get('transaction_uuid');

  useEffect(() => {
    if (!transactionUuid) {
      setError('Missing payment reference.');
      setChecking(false);
      return;
    }
    client.get(`/payment/verify/${transactionUuid}`)
      .then((res) => {
        setResult(res.data);
        refreshCart();
      })
      .catch((err) => setError(err.response?.data?.error || 'Could not verify payment'))
      .finally(() => setChecking(false));
  }, [transactionUuid]);

  return (
    <div className="page container">
      <div className="center-box">
        {checking && (
          <>
            <h3>Confirming your payment...</h3>
            <p style={{ color: '#6b7280', fontSize: 14 }}>
              We're checking directly with eSewa. This only takes a moment.
            </p>
          </>
        )}

        {!checking && error && (
          <>
            <h3 style={{ color: '#b5433a' }}>Something went wrong</h3>
            <p style={{ color: '#6b7280', fontSize: 14 }}>{error}</p>
            <Link to="/orders" className="btn btn-outline" style={{ marginTop: 16 }}>View my orders</Link>
          </>
        )}

        {!checking && result && result.paymentStatus === 'SUCCESS' && (
          <>
            <h3 style={{ color: '#2e7d4f' }}>Payment confirmed!</h3>
            <p style={{ color: '#6b7280', fontSize: 14 }}>
              Order <strong>{result.orderNumber}</strong> is now {result.orderStatus.toLowerCase()}.
            </p>
            <Link to="/orders" className="btn btn-primary" style={{ marginTop: 16 }}>View my orders</Link>
          </>
        )}

        {!checking && result && result.paymentStatus === 'PENDING' && (
          <>
            <h3 style={{ color: '#a06a11' }}>Still confirming...</h3>
            <p style={{ color: '#6b7280', fontSize: 14 }}>{result.message}</p>
            <Link to="/orders" className="btn btn-outline" style={{ marginTop: 16 }}>Check order status</Link>
          </>
        )}

        {!checking && result && result.paymentStatus === 'FAILED' && (
          <>
            <h3 style={{ color: '#b5433a' }}>Payment failed</h3>
            <p style={{ color: '#6b7280', fontSize: 14 }}>{result.message}</p>
            <Link to="/orders" className="btn btn-outline" style={{ marginTop: 16 }}>Go to order &amp; retry</Link>
          </>
        )}
      </div>
    </div>
  );
}
