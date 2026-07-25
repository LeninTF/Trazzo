const TOKEN_KEY = 'trazzo_token';

export interface DownloadOptions {
  fileName?: string;
}

export async function downloadWithAuth(url: string, opts: DownloadOptions = {}): Promise<void> {
  const token = localStorage.getItem(TOKEN_KEY);
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(url, { credentials: 'include', headers });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);

  const blob = await response.blob();
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = opts.fileName ?? 'descarga';
  link.click();
  URL.revokeObjectURL(link.href);
}
