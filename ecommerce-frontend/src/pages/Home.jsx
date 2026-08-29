import { useEffect, useState } from 'react';
import client from '../api/client';
import ProductCard from '../components/ProductCard';

export default function Home() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [categoryId, setCategoryId] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    client.get('/categories').then((res) => setCategories(res.data));
  }, []);

  useEffect(() => {
    setLoading(true);
    const params = {};
    if (categoryId) params.categoryId = categoryId;
    if (search) params.search = search;
    client.get('/products', { params })
      .then((res) => setProducts(res.data))
      .finally(() => setLoading(false));
  }, [categoryId, search]);

  return (
    <div className="page container">
      <section className="storefront-hero">
        <div className="hero-copy">
          <div className="eyebrow">The horological edit</div>
          <h1>Time, made personal.</h1>
          <p>Considered watches for the moments you keep. Explore a refined collection built around craft, clarity and everyday ritual.</p>
          <a href="#collection" className="btn btn-primary">Explore the collection</a>
        </div>
        <div className="hero-image">
          <img src="https://images.unsplash.com/photo-1523170335258-f5ed11844a49?auto=format&fit=crop&w=1100&q=85" alt="Classic watch on a warm stone surface" />
        </div>
      </section>

      <div id="collection" className="section-heading">
        <div><div className="eyebrow">Curated selection</div><h2>Find your signature</h2></div>
        <span className="page-subtitle">{products.length} pieces</span>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 24, flexWrap: 'wrap' }}>
        <input
          placeholder="Search watches..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="field-input"
        />
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}
          className="field-input">
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : products.length === 0 ? (
        <div className="empty-state">No watches match that search.</div>
      ) : (
        <div className="grid">
          {products.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      )}
    </div>
  );
}
