import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ConverterService, DetectedField } from '../../services/converter.service';

@Component({
  selector: 'app-mtom-analyzer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="mtom-analyzer">
      <h2>Stap 2: MTOM Bronbestand Uploaden</h2>

      <div class="sample-mtom">
        <button class="sample-btn" (click)="loadSampleMtom()" [disabled]="loading">
          &#128196; Gebruik voorbeeld MTOM bericht
        </button>
        <span class="sample-hint">Laadt een voorbeeldbericht met alle standaard Belastingdienst velden</span>
      </div>

      <div class="divider"><span>of upload een eigen bestand</span></div>

      <div class="upload-zone"
           [class.dragover]="isDragOver"
           (dragover)="onDragOver($event)"
           (dragleave)="onDragLeave($event)"
           (drop)="onDrop($event)">
        <p>Sleep een MTOM/XML bestand hierheen of</p>
        <label class="upload-btn">
          Kies bestand
          <input type="file" accept=".xml,.msg,.mtom,.mime" (change)="onFileSelect($event)" hidden>
        </label>
      </div>

      <div *ngIf="loading" class="loading">Bestand analyseren...</div>

      <div *ngIf="error" class="error-message">
        <strong>Fout:</strong> {{ error }}
        <p *ngIf="suggestion" class="suggestion">Suggestie: {{ suggestion }}</p>
      </div>

      <div *ngIf="fields.length > 0" class="detected-fields">
        <h3>Automatisch Gedetecteerde Velden ({{ fields.length }})</h3>
        <p class="info-text">
          Onderstaande velden zijn automatisch gedetecteerd in het MTOM bericht.
          U hoeft geen XPath expressies te kennen - de tool detecteert dit automatisch.
        </p>
        <table class="fields-table">
          <thead>
            <tr>
              <th>Veldnaam</th>
              <th>XPath Pad</th>
              <th>Voorbeeldwaarde</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let field of fields">
              <td><strong>{{ field.fieldName }}</strong></td>
              <td><code class="xpath">{{ field.xpathExpression }}</code></td>
              <td class="sample-value">{{ field.sampleValue }}</td>
              <td><span class="type-badge">{{ field.detectedType }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .mtom-analyzer { margin-bottom: 1rem; }

    .sample-mtom { display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem; }
    .sample-btn {
      padding: 0.6rem 1.25rem; background: #388e3c; color: white;
      border: none; border-radius: 6px; cursor: pointer; font-size: 0.9rem;
      font-weight: 600; transition: all 0.2s;
    }
    .sample-btn:hover:not(:disabled) { background: #2e7d32; }
    .sample-btn:disabled { background: #bbb; cursor: not-allowed; }
    .sample-hint { font-size: 0.8rem; color: #666; }

    .divider {
      display: flex; align-items: center; gap: 1rem;
      margin: 1rem 0; color: #999; font-size: 0.85rem;
    }
    .divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: #e0e0e0; }

    .upload-zone {
      border: 2px dashed #ccc; border-radius: 8px;
      padding: 1.5rem; text-align: center; transition: all 0.3s; background: #fafafa;
    }
    .upload-zone.dragover { border-color: #388e3c; background: #e8f5e9; }
    .upload-btn {
      display: inline-block; padding: 0.5rem 1.5rem;
      background: #388e3c; color: white; border-radius: 4px;
      cursor: pointer; margin-top: 0.5rem;
    }
    .upload-btn:hover { background: #2e7d32; }
    .loading { text-align: center; padding: 1rem; color: #1976d2; }
    .error-message { background: #ffebee; padding: 1rem; border-radius: 4px; margin-top: 1rem; color: #c62828; }
    .suggestion { color: #555; font-style: italic; }
    .info-text { color: #666; font-style: italic; margin-bottom: 1rem; }
    .detected-fields { margin-top: 1.5rem; }
    .fields-table { width: 100%; border-collapse: collapse; }
    .fields-table th, .fields-table td { padding: 0.5rem; border: 1px solid #e0e0e0; }
    .fields-table th { background: #f5f5f5; text-align: left; }
    .xpath { font-size: 0.85rem; color: #5d4037; background: #efebe9; padding: 2px 4px; border-radius: 3px; }
    .sample-value { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .type-badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 0.8rem; background: #e8f5e9; color: #2e7d32; }
  `]
})
export class MtomAnalyzerComponent {
  @Output() fieldsDetected = new EventEmitter<DetectedField[]>();
  @Output() fileSelected = new EventEmitter<File>();

  isDragOver = false;
  loading = false;
  fields: DetectedField[] = [];
  error: string | null = null;
  suggestion: string | null = null;
  currentFile: File | null = null;

  constructor(
    private converterService: ConverterService,
    private http: HttpClient
  ) {}

  loadSampleMtom() {
    this.loading = true;
    this.error = null;

    this.http.get('assets/samples/sample-mtom.xml', { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const file = new File([blob], 'sample-mtom.xml', { type: 'application/xml' });
        this.currentFile = file;
        this.fileSelected.emit(file);
        this.analyzeFile(file);
      },
      error: () => {
        this.loading = false;
        this.error = 'Kon sample MTOM bestand niet laden';
      }
    });
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.processFile(file);
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.processFile(file);
  }

  private processFile(file: File) {
    this.currentFile = file;
    this.fileSelected.emit(file);
    this.analyzeFile(file);
  }

  private analyzeFile(file: File) {
    this.loading = true;
    this.error = null;

    this.converterService.analyzeMtom(file).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.fields = response.fields;
          this.fieldsDetected.emit(response.fields);
        } else {
          this.error = response.error || 'Analyse mislukt';
          this.suggestion = response.suggestion || null;
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || 'Fout bij het verbinden met de server';
        this.suggestion = err.error?.suggestion || 'Controleer of de backend draait op http://localhost:8080';
      }
    });
  }
}
