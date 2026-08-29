import { useEffect, useState } from 'react';
import client from '../api/client';
import StatusBadge from '../components/StatusBadge';

const emptyForm = { name: '', brand: '', description: '', price: '', stockQuantity: '', imageUrl: '', categoryId: '' };
const money = (value) => `NPR ${Number(value || 0).toLocaleString()}`;

export default function Admin() {
  const [tab, setTab] = useState('overview');
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [orders, setOrders] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadProducts = () => client.get('/products').then((res) => setProducts(res.data));
  const loadCategories = () => client.get('/categories').then((res) => setCategories(res.data));
  const loadOrders = () => client.get('/admin/orders').then((res) => setOrders(res.data));

  useEffect(() => { loadProducts(); loadCategories(); loadOrders(); }, []);

  const update = (key) => (e) => setForm({ ...form, [key]: e.target.value });
  const resetForm = () => { setForm(emptyForm); setEditingId(null); };
  const editProduct = (product) => {
    setForm({ name: product.name, brand: product.brand || '', description: product.description || '', price: product.price, stockQuantity: product.stockQuantity, imageUrl: product.imageUrl || '', categoryId: product.categoryId || '' });
    setEditingId(product.id); setTab('add-product');
  };
  const submitProduct = async (e) => {
    e.preventDefault(); setError(''); setSaving(true);
    const payload = { ...form, price: Number(form.price), stockQuantity: Number(form.stockQuantity), categoryId: form.categoryId ? Number(form.categoryId) : null };
    try {
      if (editingId) await client.put(`/admin/products/${editingId}`, payload);
      else await client.post('/admin/products', payload);
      resetForm(); await loadProducts(); setTab('products');
    } catch (err) { setError(err.response?.data?.error || 'Could not save product'); }
    finally { setSaving(false); }
  };
  const deleteProduct = async (id) => {
    if (!confirm('Remove this product from the catalog?')) return;
    await client.delete(`/admin/products/${id}`); loadProducts();
  };

  const paidOrders = orders.filter((order) => order.status === 'PAID');
  const revenue = paidOrders.reduce((sum, order) => sum + Number(order.totalAmount), 0);
  const unitsSold = paidOrders.reduce((sum, order) => sum + order.items.reduce((itemSum, item) => itemSum + item.quantity, 0), 0);
  const customers = new Set(orders.map((order) => order.customerEmail)).size;
  const lowStock = products.filter((product) => product.stockQuantity <= 3);
  const bestSellers = Object.values(paidOrders.flatMap((order) => order.items).reduce((map, item) => {
    map[item.productName] = map[item.productName] || { name: item.productName, units: 0, revenue: 0 };
    map[item.productName].units += item.quantity;
    map[item.productName].revenue += Number(item.priceAtPurchase) * item.quantity;
    return map;
  }, {})).sort((a, b) => b.units - a.units).slice(0, 5);
  const recentMonths = Array.from({ length: 6 }, (_, index) => {
    const date = new Date(); date.setMonth(date.getMonth() - (5 - index));
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    return { label: date.toLocaleDateString('en-US', { month: 'short' }), value: paidOrders.filter((order) => { const orderDate = new Date(order.createdAt); return `${orderDate.getFullYear()}-${orderDate.getMonth()}` === key; }).reduce((sum, order) => sum + Number(order.totalAmount), 0) };
  });
  const maxRevenue = Math.max(...recentMonths.map((month) => month.value), 1);

  return (
    <main className="admin-shell">
      <header className="admin-header">
        <div><div className="eyebrow">Operations / {new Date().toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}</div><h1>Good morning, admin.</h1></div>
        <div className="page-subtitle">A clear view of your watch business.</div>
      </header>
      <nav className="admin-tabs" aria-label="Admin sections">
        {['overview', 'products', 'orders', 'add-product'].map((name) => <button key={name} className={tab === name ? 'btn btn-primary' : 'btn btn-outline'} onClick={() => { if (name === 'add-product') resetForm(); setTab(name); }}>{name === 'add-product' ? 'Add product' : name[0].toUpperCase() + name.slice(1)}</button>)}
      </nav>

      {tab === 'overview' && <>
        <section className="kpi-grid">
          <div className="kpi"><div className="kpi-label">Sales overview</div><div className="kpi-value">{money(revenue)}</div><div className="kpi-note">{paidOrders.length} paid orders</div></div>
          <div className="kpi"><div className="kpi-label">Profit & loss</div><div className="kpi-value">{money(revenue)}</div><div className="kpi-note">Gross revenue proxy</div></div>
          <div className="kpi"><div className="kpi-label">Orders</div><div className="kpi-value">{orders.length}</div><div className="kpi-note">{unitsSold} units sold</div></div>
          <div className="kpi"><div className="kpi-label">Customers</div><div className="kpi-value">{customers}</div><div className="kpi-note">Unique purchasers</div></div>
        </section>
        <section className="admin-grid">
          <div className="admin-panel"><h2>Revenue performance</h2><div className="bar-chart">{recentMonths.map((month) => <div className="bar-column" key={month.label}><div className="bar" style={{ height: `${Math.max((month.value / maxRevenue) * 100, 4)}%` }} title={money(month.value)} /><span>{month.label}</span></div>)}</div></div>
          <div className="admin-panel"><h2>Inventory pulse</h2><div className="kpi-value">{products.reduce((sum, product) => sum + product.stockQuantity, 0)}</div><p className="page-subtitle">Total units currently listed</p><div className="summary-row"><span>Low stock</span><strong>{lowStock.length} products</strong></div><div className="summary-row"><span>Catalog size</span><strong>{products.length} products</strong></div></div>
        </section>
        <section className="admin-grid">
          <div className="admin-panel"><h2>Best-selling products</h2>{bestSellers.length ? bestSellers.map((item, index) => <div className="product-rank" key={item.name}><span className="rank-number">0{index + 1}</span><span>{item.name}</span><strong>{item.units} sold</strong></div>) : <p className="page-subtitle">Sales data will appear here after the first paid order.</p>}</div>
          <div className="admin-panel"><h2>Low-stock products</h2>{lowStock.length ? lowStock.slice(0, 5).map((product) => <div className="product-rank" key={product.id}><span>{product.name}</span><strong className="stock-low">{product.stockQuantity} left</strong></div>) : <p className="page-subtitle">All products have healthy stock.</p>}</div>
        </section>
        <section className="admin-panel"><h2>Recent activity</h2><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Order</th><th>Customer</th><th>Value</th><th>Status</th><th>Date</th></tr></thead><tbody>{orders.slice(0, 6).map((order) => <tr key={order.id}><td>{order.orderNumber}</td><td>{order.customerName || order.customerEmail}</td><td>{money(order.totalAmount)}</td><td><StatusBadge status={order.status} /></td><td>{new Date(order.createdAt).toLocaleDateString()}</td></tr>)}</tbody></table></div></section>
      </>}

      {tab === 'products' && <section className="admin-panel"><h2>Product catalog</h2><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Name</th><th>Brand</th><th>Category</th><th>Price</th><th>Stock</th><th /></tr></thead><tbody>{products.map((product) => <tr key={product.id}><td>{product.name}</td><td>{product.brand}</td><td>{product.categoryName || 'Uncategorized'}</td><td>{money(product.price)}</td><td><div>{product.stockQuantity}</div><div className="stock-meter"><span style={{ width: `${Math.min(product.stockQuantity * 10, 100)}%` }} /></div></td><td><button className="btn btn-outline" onClick={() => editProduct(product)}>Edit</button> <button className="btn btn-danger" onClick={() => deleteProduct(product.id)}>Remove</button></td></tr>)}</tbody></table></div></section>}

      {tab === 'orders' && <section className="admin-panel"><h2>All orders</h2><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Order #</th><th>Customer</th><th>Total</th><th>Status</th><th>Date</th></tr></thead><tbody>{orders.map((order) => <tr key={order.id}><td>{order.orderNumber}</td><td>{order.customerEmail}</td><td>{money(order.totalAmount)}</td><td><StatusBadge status={order.status} /></td><td>{new Date(order.createdAt).toLocaleDateString()}</td></tr>)}</tbody></table></div></section>}

      {tab === 'add-product' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1.35fr) minmax(240px, .65fr)', gap: 18, alignItems: 'start' }}>
          <form className="admin-panel" onSubmit={submitProduct}>
            <div style={{ borderBottom: '1px solid var(--line)', paddingBottom: 16, marginBottom: 22 }}>
              <div className="eyebrow">Catalog editor</div>
              <h2 style={{ margin: '5px 0 4px' }}>{editingId ? 'Refine this product' : 'Add a new timepiece'}</h2>
              <div className="page-subtitle" style={{ margin: 0 }}>Give your next product a clear, considered presence in the collection.</div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
              {[['name', 'Product name'], ['brand', 'Brand']].map(([key, label]) => (
                <div className="field" key={key}><label>{label}</label><input placeholder={key === 'name' ? 'e.g. Heritage Automatic' : 'e.g. Seiko'} value={form[key]} onChange={update(key)} required={key === 'name'} /></div>
              ))}
            </div>
            <div className="field"><label>Description</label><textarea rows={5} placeholder="Describe the materials, movement and character of this watch..." value={form.description} onChange={update('description')} /></div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
              <div className="field"><label>Price (NPR)</label><input type="number" step="0.01" min="0" placeholder="0.00" value={form.price} onChange={update('price')} required /></div>
              <div className="field"><label>Stock quantity</label><input type="number" min="0" placeholder="0" value={form.stockQuantity} onChange={update('stockQuantity')} required /></div>
            </div>
            <div className="field"><label>Image URL</label><input type="url" placeholder="https://..." value={form.imageUrl} onChange={update('imageUrl')} /></div>
            <div className="field"><label>Category</label><select value={form.categoryId} onChange={update('categoryId')}><option value="">Uncategorized</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div>
            {error && <div className="error-text">{error}</div>}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 24 }}>
              <button type="button" className="btn btn-outline" onClick={resetForm}>Clear</button>
              <button className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : editingId ? 'Update product' : 'Create product'}</button>
            </div>
          </form>
          <aside className="admin-panel" style={{ position: 'sticky', top: 90 }}>
            <div className="eyebrow">Live preview</div>
            <div style={{ margin: '14px 0 18px', aspectRatio: '1 / 1', background: '#e7e2d8', borderRadius: 3, overflow: 'hidden', display: 'grid', placeItems: 'center' }}>
              {form.imageUrl ? <img src={form.imageUrl} alt="Product preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} /> : <span style={{ color: 'var(--muted)', fontSize: 13 }}>Product image preview</span>}
            </div>
            <div className="card-brand">{form.brand || 'Brand name'}</div>
            <div style={{ fontFamily: 'Georgia, serif', fontSize: 23, margin: '5px 0 9px' }}>{form.name || 'Your watch name'}</div>
            <div style={{ fontWeight: 700 }}>{form.price ? money(form.price) : 'NPR 0'}</div>
            <div className="page-subtitle" style={{ margin: '8px 0 0', fontSize: 13 }}>Preview updates as you fill in the details.</div>
          </aside>
        </div>
      )}
    </main>
  );
}
