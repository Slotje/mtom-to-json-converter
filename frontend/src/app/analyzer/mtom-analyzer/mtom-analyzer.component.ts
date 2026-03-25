import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ConverterService, DetectedField } from '../../services/converter.service';

@Component({
  selector: 'app-mtom-analyzer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mtom-analyzer.component.html'
})
export class MtomAnalyzerComponent {
  @Output() fieldsDetected = new EventEmitter<DetectedField[]>();
  @Output() fileSelected = new EventEmitter<File>();

  loading = false;
  fields: DetectedField[] = [];
  error: string | null = null;
  suggestion: string | null = null;
  currentFile: File | null = null;
  payloadContent: string | null = null;

  constructor(
    private converterService: ConverterService,
    private http: HttpClient
  ) {}

  reset() {
    this.fields = [];
    this.error = null;
    this.suggestion = null;
    this.currentFile = null;
    this.payloadContent = null;
    this.loading = false;
  }

  loadSampleMtom() {
    this.loading = true;
    this.error = null;

    this.http.get('assets/samples/sample-mtom.xml', { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const file = new File([blob], 'sample-mtom.xml', { type: 'application/xml' });
        this.currentFile = file;
        this.fileSelected.emit(file);
        this.readPayloadContent(file);
        this.analyzeFile(file);
      },
      error: () => {
        this.loading = false;
        this.error = 'Kon sample MTOM bestand niet laden';
      }
    });
  }

  private readPayloadContent(file: File) {
    const reader = new FileReader();
    reader.onload = () => {
      this.payloadContent = reader.result as string;
    };
    reader.readAsText(file);
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
