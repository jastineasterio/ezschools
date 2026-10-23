import { useId, useMemo, useState, type ChangeEventHandler } from 'react';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';

export interface SearchableOption {
  value: string;
  label: string;
}

interface SearchableSelectProps {
  label: string;
  value: string;
  options: SearchableOption[];
  placeholder: string;
  onChange: ChangeEventHandler<HTMLSelectElement>;
  loading?: boolean;
  disabled?: boolean;
  emptyMessage?: string;
}

export function SearchableSelect({
  label,
  value,
  options,
  placeholder,
  onChange,
  loading = false,
  disabled = false,
  emptyMessage = 'No options available',
}: SearchableSelectProps) {
  const selectId = useId();
  const [search, setSearch] = useState('');
  const filteredOptions = useMemo(() => {
    const normalizedSearch = search.trim().toLocaleLowerCase();
    return normalizedSearch
      ? options.filter((option) => option.label.toLocaleLowerCase().includes(normalizedSearch))
      : options;
  }, [options, search]);

  return (
    <div className="space-y-2">
      <label htmlFor={selectId} className="text-sm font-medium">{label}</label>
      <Input
        type="search"
        aria-label={`Search ${label.toLocaleLowerCase()}`}
        placeholder={`Search ${label.toLocaleLowerCase()}...`}
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        disabled={disabled || loading}
      />
      <Select
        id={selectId}
        aria-label={label}
        value={value}
        onChange={onChange}
        disabled={disabled || loading}
        options={[
          { value: '', label: loading ? 'Loading...' : placeholder },
          ...filteredOptions,
        ]}
      />
      {!loading && filteredOptions.length === 0 && (
        <p className="text-sm text-muted-foreground">{emptyMessage}</p>
      )}
    </div>
  );
}
