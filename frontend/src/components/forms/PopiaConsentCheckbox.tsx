import type { InputHTMLAttributes, ReactNode } from 'react';

type PopiaConsentCheckboxProps = {
  version: string;
  label: ReactNode;
  inputProps?: InputHTMLAttributes<HTMLInputElement>;
};

export const PopiaConsentCheckbox = ({ version, label, inputProps }: PopiaConsentCheckboxProps) => (
  <label className="sm:col-span-2 flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
    <input
      type="checkbox"
      className="mt-1 h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
      {...inputProps}
    />
    <span>
      {label} <span className="text-slate-500">(version {version})</span>
    </span>
  </label>
);
