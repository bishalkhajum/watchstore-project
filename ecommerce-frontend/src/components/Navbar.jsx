import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ cartCount }) {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="navbar">
      <div className="container">
        <Link to={isAdmin ? '/admin' : '/'} className="brand">Watch<span>Store</span></Link>
        <div className="nav-links">
          {!isAdmin && <Link to="/">Shop</Link>}
          {user && !isAdmin && (
            <Link to="/cart">
              Cart {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
            </Link>
          )}
          {isAdmin && <Link to="/admin">Admin</Link>}
          {user && !isAdmin && <Link to="/orders">My Orders</Link>}
          {user ? (
            <>
              <span style={{ opacity: 0.8 }}>Hi, {user.fullName.split(' ')[0]}</span>
              <button onClick={handleLogout}>Log out</button>
            </>
          ) : (
            <>
              <Link to="/login">Log in</Link>
              <Link to="/register">Sign up</Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
