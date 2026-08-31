'use client';
import { FiSmartphone } from 'react-icons/fi';

interface PhoneInputProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  id?: string;
  label?: string;
  hint?: string;
}

export default function PhoneInput({
  value,
  onChange,
  placeholder = '0712345678',
  className = '',
  id,
  label,
  hint,
}: PhoneInputProps) {
  // Digits only; allow leading + (e.g. +254...)
  const sanitize = (raw: string) => {
    let out = raw.replace(/[^\d+]/g, '');
    if (out.startsWith('+')) out = '+' + out.replace(/\+/g, '');
    else out = out.replace(/\+/g, '');
    return out.slice(0, 15);
  };

  return (
    <div>
      {label && <label htmlFor={id} className="mb-1 block text-sm font-medium text-stone-700">{label}</label>}
      <div className="relative">
        <FiSmartphone className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" />
        <input
          id={id}
          type="tel"
          inputMode="numeric"
          autoComplete="tel"
          value={value}
          onChange={e => onChange(sanitize(e.target.value))}
          onKeyDown={e => {
            // Block letter/non-numeric keys entirely
            if (e.key.length === 1 && !/[0-9+]/.test(e.key)) e.preventDefault();
          }}
          placeholder={placeholder}
          className={`w-full rounded-xl border border-stone-300 bg-stone-50/50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-stone-400 focus:border-primary-500 focus:bg-white focus:outline-none focus:ring-4 focus:ring-primary-100 ${className}`}
        />
      </div>
      {hint && <p className="mt-1 text-xs text-stone-400">{hint}</p>}
    </div>
  );
}
