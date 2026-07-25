import { validateFile, ALLOWED_MIME_TYPES, MAX_FILE_SIZE_BYTES, formatFileSize } from './file-validator';

function makeFile(name: string, type: string, content: Uint8Array | string): File {
  const bytes = typeof content === 'string' ? new TextEncoder().encode(content) : content;
  return new File([bytes as BlobPart], name, { type });
}

function magicPdf(): Uint8Array {
  return new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x35, 0x0a, 0x25, 0xe2, 0xe3]);
}

function magicPng(): Uint8Array {
  return new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d]);
}

function magicJpg(): Uint8Array {
  return new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01]);
}

function magicMp4(): Uint8Array {
  return new Uint8Array([0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d]);
}

function magicQuicktime(): Uint8Array {
  return new Uint8Array([0x00, 0x00, 0x00, 0x10, 0x71, 0x74, 0x6f, 0x70, 0x00, 0x00, 0x00, 0x00]);
}

function magicDoc(): Uint8Array {
  return new Uint8Array([0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1, 0x00, 0x00, 0x00, 0x00]);
}

function magicDocx(): Uint8Array {
  return new Uint8Array([0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00, 0x08, 0x00, 0x00, 0x00]);
}

describe('file-validator', () => {
  describe('ALLOWED_MIME_TYPES', () => {
    it('lists all backend whitelisted mime types', () => {
      expect(ALLOWED_MIME_TYPES).toContain('application/pdf');
      expect(ALLOWED_MIME_TYPES).toContain('image/png');
      expect(ALLOWED_MIME_TYPES).toContain('image/jpeg');
      expect(ALLOWED_MIME_TYPES).toContain('application/msword');
      expect(ALLOWED_MIME_TYPES).toContain('application/vnd.openxmlformats-officedocument.wordprocessingml.document');
      expect(ALLOWED_MIME_TYPES).toContain('video/mp4');
      expect(ALLOWED_MIME_TYPES).toContain('video/quicktime');
      expect(ALLOWED_MIME_TYPES.length).toBe(7);
    });
  });

  describe('MAX_FILE_SIZE_BYTES', () => {
    it('equals 15MB', () => {
      expect(MAX_FILE_SIZE_BYTES).toBe(15 * 1024 * 1024);
    });
  });

  describe('validateFile', () => {
    it('accepts a real PDF with matching magic bytes', async () => {
      const file = makeFile('cert.pdf', 'application/pdf', magicPdf());
      const result = await validateFile(file);
      expect(result.valid).toBeTrue();
      expect(result.mimeType).toBe('application/pdf');
      expect(result.errors).toEqual([]);
    });

    it('accepts a PNG with matching magic bytes', async () => {
      const file = makeFile('foto.png', 'image/png', magicPng());
      const result = await validateFile(file);
      expect(result.valid).toBeTrue();
      expect(result.mimeType).toBe('image/png');
    });

    it('accepts JPEG, MP4, QuickTime, DOC and DOCX magic bytes', async () => {
      const cases: Array<[string, string, Uint8Array]> = [
        ['a.jpg', 'image/jpeg', magicJpg()],
        ['a.mp4', 'video/mp4', magicMp4()],
        ['a.mov', 'video/quicktime', magicQuicktime()],
        ['a.doc', 'application/msword', magicDoc()],
        ['a.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', magicDocx()],
      ];
      for (const [name, type, bytes] of cases) {
        const result = await validateFile(makeFile(name, type, bytes));
        expect(result.valid).withContext(`${name} should be valid`).toBeTrue();
        expect(result.mimeType).withContext(`${name} detected mime`).toBe(type);
      }
    });

    it('rejects empty/missing file', async () => {
      const file = makeFile('empty.pdf', 'application/pdf', new Uint8Array(0));
      const result = await validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some(e => e.field === 'file')).toBeTrue();
    });

    it('rejects file exceeding max size', async () => {
      const big = new Uint8Array(MAX_FILE_SIZE_BYTES + 1);
      big.set(new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x35, 0x0a, 0x25, 0xe2, 0xe3]), 0);
      const file = new File([big as BlobPart], 'big.pdf', { type: 'application/pdf' });
      const result = await validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some(e => e.field === 'file_size')).toBeTrue();
    });

    it('rejects disallowed declared mime type', async () => {
      const file = makeFile('malware.exe', 'application/x-msdownload', new Uint8Array([0x4d, 0x5a, 0x90, 0x00]));
      const result = await validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some(e => e.field === 'mime_type')).toBeTrue();
    });

    it('rejects when declared mime mismatches magic bytes (spoofing)', async () => {
      const file = makeFile('pdf.exe', 'application/pdf', new Uint8Array([0x4d, 0x5a, 0x90, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]));
      const result = await validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some(e => e.field === 'mime_type' && e.message.includes('spoofing'))).toBeTrue();
    });

    it('rejects when content has no known magic bytes', async () => {
      const file = makeFile('weird.pdf', 'application/pdf', new Uint8Array([0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b]));
      const result = await validateFile(file);
      expect(result.valid).toBeFalse();
      expect(result.errors.some(e => e.field === 'mime_type' && e.message.includes('magic bytes'))).toBeTrue();
    });

    it('reports missing mime when browser does not provide it', async () => {
      const file = makeFile('noext', '', magicPdf());
      const result = await validateFile(file);
      expect(result.valid).toBeFalse();
      const mimeError = result.errors.find(e => e.field === 'mime_type');
      expect(mimeError?.message).toContain('tipo MIME');
    });
  });

  describe('formatFileSize', () => {
    it('formats bytes, KB and MB', () => {
      expect(formatFileSize(0)).toBe('0 B');
      expect(formatFileSize(512)).toBe('512 B');
      expect(formatFileSize(2048)).toBe('2.0 KB');
      expect(formatFileSize(1024 * 1024)).toBe('1.00 MB');
      expect(formatFileSize(5 * 1024 * 1024)).toBe('5.00 MB');
    });
  });
});
