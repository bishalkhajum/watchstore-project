export default function StatusBadge({ status }) {
  const map = {
    PAID: 'badge-paid',
    PENDING_PAYMENT: 'badge-pending',
    FAILED: 'badge-failed',
    CANCELLED: 'badge-cancelled',
  };
  const label = {
    PAID: 'Paid',
    PENDING_PAYMENT: 'Pending Payment',
    FAILED: 'Payment Failed',
    CANCELLED: 'Cancelled',
  };
  return <span className={`badge ${map[status] || ''}`}>{label[status] || status}</span>;
}
