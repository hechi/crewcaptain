'use client';

import { useState, useRef } from 'react';
import { BulkImportResponse } from '@/types/bulk-import';
import { importPersonsCsv } from '@/lib/api-client';

interface CsvImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportComplete: (result: BulkImportResponse) => void;
  token: string;
}

export default function CsvImportModal({ isOpen, onClose, onImportComplete, token }: CsvImportModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string[][]>([]);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<BulkImportResponse | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    setError(null);
    setResult(null);

    if (!selectedFile) {
      setFile(null);
      setPreview([]);
      return;
    }

    if (!selectedFile.name.endsWith('.csv') && selectedFile.type !== 'text/csv') {
      setError('Please select a CSV file');
      setFile(null);
      setPreview([]);
      return;
    }

    setFile(selectedFile);

    // Parse preview (first 6 rows)
    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target?.result as string;
      const lines = text.split('\n').filter(line => line.trim().length > 0);
      const previewLines = lines.slice(0, 6).map(line => parseCsvLine(line));
      setPreview(previewLines);
    };
    reader.readAsText(selectedFile);
  };

  const parseCsvLine = (line: string): string[] => {
    const fields: string[] = [];
    let current = '';
    let inQuotes = false;

    for (let i = 0; i < line.length; i++) {
      const c = line[i];
      if (c === '"' && !inQuotes) {
        inQuotes = true;
      } else if (c === '"' && inQuotes) {
        if (i + 1 < line.length && line[i + 1] === '"') {
          current += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else if (c === ',' && !inQuotes) {
        fields.push(current);
        current = '';
      } else {
        current += c;
      }
    }
    fields.push(current);
    return fields;
  };

  const handleImport = async () => {
    if (!file) return;

    setImporting(true);
    setError(null);

    try {
      const importResult = await importPersonsCsv(token, file);
      setResult(importResult);
      onImportComplete(importResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed');
    } finally {
      setImporting(false);
    }
  };

  const handleClose = () => {
    setFile(null);
    setPreview([]);
    setError(null);
    setResult(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    onClose();
  };

  return (
    <div
      data-testid="csv-import-modal"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={(e) => { if (e.target === e.currentTarget) handleClose(); }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="csv-import-title"
    >
      <div
        style={{
          backgroundColor: 'var(--color-bg-card)',
          borderRadius: 'var(--radius-large)',
          border: '1px solid var(--color-border)',
          padding: 'var(--space-6)',
          maxWidth: '600px',
          width: '90%',
          maxHeight: '80vh',
          overflow: 'auto',
        }}
      >
        <h2
          id="csv-import-title"
          style={{
            margin: '0 0 var(--space-4) 0',
            fontSize: 'var(--text-h3)',
            fontFamily: 'var(--font-heading)',
            color: 'var(--color-text-primary)',
          }}
        >
          Import People from CSV
        </h2>

        <p style={{ color: 'var(--color-text-secondary)', marginBottom: 'var(--space-3)', fontSize: 'var(--text-small)' }}>
          Upload a CSV file with columns: <code>name</code> (required), <code>preferred_name</code>, <code>role_title</code>, <code>timezone</code>, <code>start_date</code> (YYYY-MM-DD), <code>email</code>, <code>tags</code> (pipe-separated).
        </p>

        <pre
          data-testid="csv-example"
          style={{
            backgroundColor: 'var(--color-bg-elevated)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-small)',
            padding: 'var(--space-3)',
            marginBottom: 'var(--space-4)',
            fontSize: 'var(--text-small)',
            fontFamily: 'var(--font-mono)',
            color: 'var(--color-text-secondary)',
            overflowX: 'auto',
            whiteSpace: 'pre',
            margin: '0 0 var(--space-4) 0',
          }}
        >{`name,preferred_name,role_title,timezone,start_date,email,tags
Alice Smith,Ali,Senior Engineer,America/New_York,2023-01-15,alice@example.com,engineering|senior
Bob Jones,,Designer,Europe/London,2022-06-01,bob@example.com,design`}</pre>

        {!result && (
          <>
            <div style={{ marginBottom: 'var(--space-4)' }}>
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv,text/csv"
                onChange={handleFileChange}
                data-testid="csv-file-input"
                style={{
                  color: 'var(--color-text-primary)',
                  fontSize: 'var(--text-body)',
                }}
              />
            </div>

            {preview.length > 0 && (
              <div style={{ marginBottom: 'var(--space-4)', overflowX: 'auto' }}>
                <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--text-small)', marginBottom: 'var(--space-2)' }}>
                  Preview (first {Math.min(preview.length - 1, 5)} rows):
                </p>
                <table
                  data-testid="csv-preview-table"
                  style={{
                    width: '100%',
                    borderCollapse: 'collapse',
                    fontSize: 'var(--text-small)',
                  }}
                >
                  <thead>
                    <tr>
                      {preview[0]?.map((header, i) => (
                        <th
                          key={i}
                          style={{
                            padding: 'var(--space-2)',
                            borderBottom: '1px solid var(--color-border)',
                            textAlign: 'left',
                            color: 'var(--color-primary)',
                            fontFamily: 'var(--font-mono)',
                          }}
                        >
                          {header}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {preview.slice(1).map((row, rowIdx) => (
                      <tr key={rowIdx}>
                        {row.map((cell, cellIdx) => (
                          <td
                            key={cellIdx}
                            style={{
                              padding: 'var(--space-2)',
                              borderBottom: '1px solid var(--color-border)',
                              color: 'var(--color-text-primary)',
                            }}
                          >
                            {cell || <span style={{ color: 'var(--color-text-muted)' }}>—</span>}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}

        {error && (
          <div
            data-testid="import-error"
            style={{
              color: 'var(--color-alert)',
              padding: 'var(--space-3)',
              marginBottom: 'var(--space-4)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: 'rgba(255, 59, 48, 0.1)',
              border: '1px solid var(--color-alert)',
            }}
          >
            {error}
          </div>
        )}

        {result && (
          <div data-testid="import-result" style={{ marginBottom: 'var(--space-4)' }}>
            <div style={{
              padding: 'var(--space-3)',
              borderRadius: 'var(--radius-small)',
              backgroundColor: result.errorCount === 0 ? 'rgba(0, 255, 136, 0.1)' : 'rgba(255, 200, 0, 0.1)',
              border: `1px solid ${result.errorCount === 0 ? 'var(--color-success)' : 'var(--color-warning)'}`,
              marginBottom: 'var(--space-3)',
            }}>
              <p style={{ color: 'var(--color-text-primary)', margin: 0, fontWeight: 'var(--weight-semibold)' }}>
                {result.successCount} {result.successCount === 1 ? 'person' : 'people'} imported successfully
                {result.errorCount > 0 && `, ${result.errorCount} ${result.errorCount === 1 ? 'error' : 'errors'}`}
              </p>
            </div>

            {result.errors.length > 0 && (
              <div style={{ maxHeight: '200px', overflow: 'auto' }}>
                <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--text-small)', marginBottom: 'var(--space-2)' }}>
                  Errors:
                </p>
                <ul style={{ margin: 0, paddingLeft: 'var(--space-4)', color: 'var(--color-alert)', fontSize: 'var(--text-small)' }}>
                  {result.errors.map((err, i) => (
                    <li key={i} style={{ marginBottom: 'var(--space-1)' }}>{err}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--space-3)' }}>
          <button
            type="button"
            onClick={handleClose}
            data-testid="csv-import-close"
            style={{
              padding: '8px 16px',
              backgroundColor: 'transparent',
              color: 'var(--color-text-secondary)',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-medium)',
              fontSize: 'var(--text-body)',
              cursor: 'pointer',
            }}
          >
            {result ? 'Done' : 'Cancel'}
          </button>

          {!result && (
            <button
              type="button"
              onClick={handleImport}
              disabled={!file || importing}
              data-testid="csv-import-submit"
              style={{
                padding: '8px 16px',
                backgroundColor: file && !importing ? 'var(--color-primary)' : 'var(--color-bg-elevated)',
                color: file && !importing ? 'var(--color-bg-base)' : 'var(--color-text-muted)',
                border: 'none',
                borderRadius: 'var(--radius-medium)',
                fontSize: 'var(--text-body)',
                fontWeight: 'var(--weight-semibold)',
                cursor: file && !importing ? 'pointer' : 'not-allowed',
              }}
            >
              {importing ? 'Importing...' : 'Import'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
