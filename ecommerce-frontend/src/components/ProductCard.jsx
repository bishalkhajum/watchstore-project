import { Link } from 'react-router-dom';

export default function ProductCard({ product }) {
  return (
    <Link to={`/products/${product.id}`} className="card">
      <img src={product.imageUrl} alt={product.name} />
      <div className="card-body">
        <div className="card-brand">{product.brand}</div>
        <div className="card-name">{product.name}</div>
        <div className="card-price">NPR {Number(product.price).toLocaleString()}</div>
        {product.stockQuantity <= 3 && product.stockQuantity > 0 && (
          <div className="stock-low">Only {product.stockQuantity} left</div>
        )}
        {product.stockQuantity === 0 && <div className="stock-low">Out of stock</div>}
      </div>
    </Link>
  );
}
