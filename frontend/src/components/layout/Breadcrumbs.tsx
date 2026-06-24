import { Link } from 'react-router-dom';

export const Breadcrumbs = ({ items }: { items: Array<{ label: string; to?: string }> }) => (
  <nav aria-label="Breadcrumb" className="mb-4 text-sm text-slate-500">
    {items.map((item, idx) => (
      <span key={item.label}>
        {item.to ? <Link to={item.to}>{item.label}</Link> : item.label}
        {idx < items.length - 1 ? ' / ' : ''}
      </span>
    ))}
  </nav>
);
