export const ALLOWED_MIME_TYPES: readonly string[] = [
  'application/pdf',
  'image/png',
  'image/jpeg',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'video/mp4',
  'video/quicktime',
] as const;

export const MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024;

export interface FileValidationError {
  field: 'file' | 'mime_type' | 'file_size';
  message: string;
}

interface MagicByteRule {
  mimeType: string;
  offset: number;
  bytes: number[];
}

const MAGIC_BYTE_RULES: readonly MagicByteRule[] = [
  { mimeType: 'application/pdf', offset: 0, bytes: [0x25, 0x50, 0x44, 0x46] },
  { mimeType: 'image/png', offset: 0, bytes: [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a] },
  { mimeType: 'image/jpeg', offset: 0, bytes: [0xff, 0xd8, 0xff] },
  { mimeType: 'application/msword', offset: 0, bytes: [0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1] },
  {
    mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    offset: 0,
    bytes: [0x50, 0x4b, 0x03, 0x04],
  },
  { mimeType: 'video/mp4', offset: 4, bytes: [0x66, 0x74, 0x79, 0x70] },
  { mimeType: 'video/quicktime', offset: 4, bytes: [0x71, 0x74, 0x6f, 0x70] },
  { mimeType: 'application/x-msdownload', offset: 0, bytes: [0x4d, 0x5a, 0x90, 0x00] },
];

function matchesMagicBytes(header: Uint8Array, rule: MagicByteRule): boolean {
  if (header.length < rule.offset + rule.bytes.length) return false;
  for (let i = 0; i < rule.bytes.length; i++) {
    if (header[rule.offset + i] !== rule.bytes[i]) return false;
  }
  return true;
}

function detectMimeTypeFromHeader(header: Uint8Array): string | null {
  for (const rule of MAGIC_BYTE_RULES) {
    if (matchesMagicBytes(header, rule)) return rule.mimeType;
  }
  return null;
}

export interface FileValidationResult {
  valid: boolean;
  mimeType: string | null;
  errors: FileValidationError[];
}

export async function validateFile(file: File): Promise<FileValidationResult> {
  const errors: FileValidationError[] = [];

  if (!file || file.size === 0) {
    errors.push({ field: 'file', message: 'No se seleccionó ningún archivo o el archivo está vacío.' });
    return { valid: false, mimeType: null, errors };
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    errors.push({
      field: 'file_size',
      message: `El archivo supera el máximo permitido de 15MB (tamaño actual: ${(file.size / (1024 * 1024)).toFixed(2)}MB).`,
    });
  }

  const declaredMime = file.type?.trim().toLowerCase() ?? '';
  if (!declaredMime) {
    errors.push({ field: 'mime_type', message: 'El navegador no reportó el tipo MIME del archivo.' });
  } else if (!ALLOWED_MIME_TYPES.includes(declaredMime)) {
    errors.push({
      field: 'mime_type',
      message: `El tipo MIME "${declaredMime}" no está permitido. Formatos válidos: PDF, PNG, JPEG, DOC, DOCX, MP4, QuickTime.`,
    });
  }

  const header = await readHeader(file, 12);
  const detectedMime = detectMimeTypeFromHeader(header);

  if (detectedMime && declaredMime && detectedMime !== declaredMime) {
    errors.push({
      field: 'mime_type',
      message: `El tipo MIME declarado "${declaredMime}" no coincide con el contenido real del archivo (detectado: "${detectedMime}"). Posible spoofing.`,
    });
  }

  if (detectedMime === null && declaredMime) {
    errors.push({
      field: 'mime_type',
      message: 'No se pudo verificar el contenido del archivo contra firmas conocidas (magic bytes).',
    });
  }

  return {
    valid: errors.length === 0,
    mimeType: detectedMime ?? declaredMime ?? null,
    errors,
  };
}

function readHeader(file: File, bytes: number): Promise<Uint8Array> {
  return new Promise((resolve, reject) => {
    const slice = file.slice(0, bytes);
    const reader = new FileReader();
    reader.onload = () => resolve(new Uint8Array(reader.result as ArrayBuffer));
    reader.onerror = () => reject(reader.error ?? new Error('No se pudo leer el archivo.'));
    reader.readAsArrayBuffer(slice);
  });
}

export function formatFileSize(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}
