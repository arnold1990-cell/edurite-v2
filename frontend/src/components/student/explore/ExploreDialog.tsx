import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';

export function ExploreDialog({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(()=>{ ref.current?.showModal(); const previous=document.body.style.overflow; document.body.style.overflow='hidden'; return()=>{document.body.style.overflow=previous;}; },[]);
  return <dialog ref={ref} className="ex-dialog ec-explorer" onCancel={event=>{event.preventDefault();onClose();}}><header><h2>{title}</h2><button aria-label="Close panel" onClick={onClose}><X size={20}/></button></header><div className="ex-dialog-body">{children}</div></dialog>;
}
