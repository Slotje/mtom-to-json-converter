import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FieldMapping {
  sourceField: string;
  targetProperty: string;
  dataType: string;
  required: boolean;
  format?: string;
  defaultValue?: string;
  multiValue?: boolean;
  transformation?: string;
}

export interface ClientConfig {
  clientId: string;
  clientName: string;
  metadataMapping: {
    objectStore: string;
    documentClass: string;
    fields: FieldMapping[];
  };
  businessInfo: {
    contactEmail: string;
    supportGroup: string;
    maxMessageSize?: number;
    maxMessagesPerDay?: number;
  };
  processingRules?: {
    retentionDays: number;
    autoRetryEnabled: boolean;
    processingEnabled: boolean;
  };
}

export interface DetectedField {
  fieldName: string;
  xpathExpression: string;
  sampleValue: string;
  detectedType: string;
}

export interface ValidationError {
  code: string;
  message: string;
  suggestion: string;
  severity: string;
}

export interface ValidationResult {
  layer: string;
  valid: boolean;
  errors: ValidationError[];
  warnings: ValidationError[];
}

export interface ConversionResult {
  success: boolean;
  jsonOutput: any;
  validationResults: ValidationResult[];
  extractedFields: { [key: string]: string };
}

export interface ConfigUploadResponse {
  config: ClientConfig;
  validation: ValidationResult;
  success: boolean;
  error?: string;
  suggestion?: string;
}

export interface AnalyzeResponse {
  success: boolean;
  fields: DetectedField[];
  totalFields: number;
  error?: string;
  suggestion?: string;
}

@Injectable({ providedIn: 'root' })
export class ConverterService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  uploadConfig(yamlContent: string): Observable<ConfigUploadResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'text/plain' });
    return this.http.post<ConfigUploadResponse>(`${this.baseUrl}/config/upload-text`, yamlContent, { headers });
  }

  getConfig(): Observable<ClientConfig> {
    return this.http.get<ClientConfig>(`${this.baseUrl}/config`);
  }

  analyzeMtom(file: File): Observable<AnalyzeResponse> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/octet-stream' });
    return this.http.post<AnalyzeResponse>(`${this.baseUrl}/mtom/analyze`, file, { headers });
  }

  convert(file: File): Observable<ConversionResult> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/octet-stream' });
    return this.http.post<ConversionResult>(`${this.baseUrl}/convert`, file, { headers });
  }

  validate(): Observable<any> {
    return this.http.post(`${this.baseUrl}/validate`, {});
  }
}
