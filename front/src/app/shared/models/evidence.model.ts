export interface Evidence {
  id: string;
  incidencia_id: string;
  file_name: string;
  file_url: string;
  file_key: string;
  mime_type: string;
  file_size: number;
  created_at: string;
  updated_at: string;
}

export interface CreateEvidenceRequest {
  file_name: string;
  file_key: string;
  mime_type: string;
  file_size: number;
}

export interface PresignedUrlResponse {
  presigned_url: string;
  object_key: string;
}
