import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '', address: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(form.fullName, form.email, form.password, form.phone, form.address);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page container">
      <div className="center-box" style={{ textAlign: 'left' }}>
        <h2 style={{ marginTop: 0 }}>Create an account</h2>
        <form className="form" onSubmit={handleSubmit}>
          <div className="field">
            <label>Full name</label>
            <input value={form.fullName} onChange={update('fullName')} required />
          </div>
          <div className="field">
            <label>Email</label>
            <input type="email" value={form.email} onChange={update('email')} required />
          </div>
          <div className="field">
            <label>Password (min 6 characters)</label>
            <input type="password" value={form.password} onChange={update('password')} minLength={6} required />
          </div>
          <div className="field">
            <label>Phone</label>
            <input value={form.phone} onChange={update('phone')} />
          </div>
          <div className="field">
            <label>Address</label>
            <input value={form.address} onChange={update('address')} />
          </div>
          {error && <div className="error-text">{error}</div>}
          <button className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Creating account...' : 'Sign up'}
          </button>
        </form>
        <p style={{ marginTop: 16, fontSize: 14 }}>
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
