import { useEffect, useId, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
export function ExplorePanel({ title, icon, children, action, id }: { title: string; icon: ReactNode; children: ReactNode; action?: ReactNode; id?: string }) {
  return <section id={id} className="ed-panel"><header>{icon}<h2>{title}</h2>{action}</header>{children}</section>;
}
export function ExploreDialog({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  const titleId = useId();
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => { ref.current?.showModal(); const previous = document.body.style.overflow; document.body.style.overflow = 'hidden'; return () => { document.body.style.overflow = previous; }; }, []);
  return <dialog aria-labelledby={titleId} ref={ref} className="ex-dialog ec-explorer" onCancel={onClose} onClick={e => { if (e.target === e.currentTarget) onClose(); }}><header><h2 id={titleId}>{title}</h2><button autoFocus aria-label="Close dialog" onClick={onClose}><X /></button></header>{children}</dialog>;
}
