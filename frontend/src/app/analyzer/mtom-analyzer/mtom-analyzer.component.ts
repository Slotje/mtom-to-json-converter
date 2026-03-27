import { Component, CUSTOM_ELEMENTS_SCHEMA, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ConverterService, DetectedField } from '../../services/converter.service';

interface PayloadField {
  name: string;
  value: string;
  path: string;
}

interface PayloadSection {
  name: string;
  description: string;
  fields: PayloadField[];
  subsections: PayloadSection[];
}

@Component({
  selector: 'app-mtom-analyzer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mtom-analyzer.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class MtomAnalyzerComponent {
  @Output() fieldsDetected = new EventEmitter<DetectedField[]>();
  @Output() fileSelected = new EventEmitter<File>();

  loading = false;
  fields: DetectedField[] = [];
  error: string | null = null;
  suggestion: string | null = null;
  currentFile: File | null = null;
  payloadSections: PayloadSection[] = [];

  constructor(
    private converterService: ConverterService,
    private http: HttpClient
  ) {}

  reset() {
    this.fields = [];
    this.error = null;
    this.suggestion = null;
    this.currentFile = null;
    this.payloadSections = [];
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
        this.parsePayload(file);
        this.analyzeFile(file);
      },
      error: () => {
        this.loading = false;
        this.error = 'Kon sample MTOM bestand niet laden';
      }
    });
  }

  private parsePayload(file: File) {
    const reader = new FileReader();
    reader.onload = () => {
      const xmlText = reader.result as string;
      const parser = new DOMParser();
      const doc = parser.parseFromString(xmlText, 'application/xml');

      const parseError = doc.querySelector('parsererror');
      if (parseError) {
        return;
      }

      this.payloadSections = this.extractSections(doc);
    };
    reader.readAsText(file);
  }

  private extractSections(doc: Document): PayloadSection[] {
    const sections: PayloadSection[] = [];

    // Find the main body content (ECM_Bericht or similar root)
    const body = doc.getElementsByTagNameNS('*', 'Body')[0]
      || doc.getElementsByTagNameNS('*', 'Envelope')[0]
      || doc.documentElement;

    if (!body) return sections;

    // Walk the top-level sections inside the body
    const root = this.findFirstElement(body);
    if (!root) return sections;

    for (let i = 0; i < root.children.length; i++) {
      const child = root.children[i];
      const section = this.buildSection(child, '');
      if (section) {
        sections.push(section);
      }
    }

    return sections;
  }

  private findFirstElement(el: Element): Element {
    // Drill into wrapper elements (Envelope > Body > ECM_Bericht)
    for (let i = 0; i < el.children.length; i++) {
      const child = el.children[i];
      const localName = child.localName.toLowerCase();
      if (localName === 'header') continue;
      if (child.children.length > 0 && this.hasGrandchildren(child)) {
        return child;
      }
    }
    return el;
  }

  private hasGrandchildren(el: Element): boolean {
    for (let i = 0; i < el.children.length; i++) {
      if (el.children[i].children.length > 0) return true;
    }
    return false;
  }

  private buildSection(el: Element, parentPath: string): PayloadSection | null {
    const name = el.localName;
    const path = parentPath ? `${parentPath}/${name}` : name;
    const fields: PayloadField[] = [];
    const subsections: PayloadSection[] = [];

    for (let i = 0; i < el.children.length; i++) {
      const child = el.children[i];
      if (child.children.length > 0) {
        // Has children - recurse as subsection
        const sub = this.buildSection(child, path);
        if (sub) subsections.push(sub);
      } else {
        // Leaf node - extract as field
        const textContent = child.textContent?.trim() || '';
        fields.push({
          name: child.localName,
          value: this.truncateValue(textContent),
          path: `//${child.localName}`
        });
      }
    }

    // Only return if there's actual content
    if (fields.length === 0 && subsections.length === 0) return null;

    return {
      name,
      description: this.getSectionDescription(name),
      fields,
      subsections
    };
  }

  private truncateValue(value: string): string {
    if (value.length > 80) {
      return value.substring(0, 80) + '...';
    }
    return value;
  }

  private getSectionDescription(name: string): string {
    const descriptions: { [key: string]: string } = {
      'ECM_Metadata': 'Document metadata en classificatie',
      'MHS_Header': 'Berichtroutering en identificatie',
      'Content': 'Documentinhoud (bestand en binaire data)',
      'Eigenschappen': 'Documenteigenschappen',
      'Periode': 'Tijdsperiode en archiveringsdatums',
      'Classificatie': 'Berichtclassificatie en categorisering',
      'Organisatie': 'Organisatorische eigenaar-hierarchie',
      'Tijdstippen': 'Berichttijdstippen',
      'Berichttype': 'Type en routering van het bericht',
      'Kenmerken': 'Berichtkenmerken en correlatie'
    };
    return descriptions[name] || '';
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
