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
