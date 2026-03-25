import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValidationResult } from '../../services/converter.service';

@Component({
  selector: 'app-validation-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="validation-panel" *ngIf="validationResults.length > 0">
      <div class="section-header">
        <h2>Drielaagse Validatie</h2>
        <p class="subtitle">Elke configuratie wordt gevalideerd op drie niveaus voordat deze in productie mag komen.</p>
      </div>

      <!-- Overall status -->
      <div class="overall-status" [class.all-valid]="allValid" [class.has-errors]="!allValid">
        <div class="overall-icon">{{ allValid ? '&#10004;' : '&#9888;' }}</div>
        <div class="overall-text">
          <strong>{{ allValid ? 'Alle validatielagen geslaagd' : 'Validatie niet volledig geslaagd' }}</strong>
          <span>{{ totalErrors }} fout(en), {{ totalWarnings }} waarschuwing(en) over {{ validationResults.length }} lagen</span>
        </div>
      </div>

      <!-- Three layer cards -->
      <div class="layers-grid">
        <div *ngFor="let result of validationResults; let i = index"
             class="layer-card"
             [class.valid]="result.valid && (result.warnings || []).length === 0"
             [class.warning]="result.valid && (result.warnings || []).length > 0"
             [class.invalid]="!result.valid">

          <!-- Layer header -->
          <div class="layer-top">
            <div class="layer-number">{{ i + 1 }}</div>
            <div class="layer-info">
              <div class="layer-name">{{ getLayerName(result.layer) }}</div>
              <div class="layer-desc">{{ getLayerDescription(result.layer) }}</div>
            </div>
            <div class="layer-badge-container">
              <div class="layer-badge" *ngIf="result.valid && (result.warnings || []).length === 0">
                <span class="badge-icon-check">&#10004;</span> GESLAAGD
              </div>
              <div class="layer-badge warn-badge" *ngIf="result.valid && (result.warnings || []).length > 0">
                <span class="badge-icon-warn">&#9888;</span> WAARSCHUWINGEN
              </div>
              <div class="layer-badge fail-badge" *ngIf="!result.valid">
                <span class="badge-icon-fail">&#10006;</span> MISLUKT
              </div>
            </div>
          </div>

          <!-- Expandable details -->
          <div class="layer-toggle" (click)="toggleLayer(result.layer)">
            <span>{{ expandedLayers[result.layer] ? 'Details verbergen' : 'Details bekijken' }}
              ({{ result.errors.length + (result.warnings || []).length }} items)</span>
            <span class="toggle-arrow">{{ expandedLayers[result.layer] ? '&#9660;' : '&#9654;' }}</span>
          </div>

          <div class="layer-details" *ngIf="expandedLayers[result.layer]">
            <!-- Errors -->
            <div *ngFor="let error of result.errors" class="detail-item error-item">
              <div class="detail-top">
                <span class="detail-code">{{ error.code }}</span>
                <span class="detail-severity severity-error">FOUT</span>
              </div>
              <div class="detail-message">{{ error.message }}</div>
              <div class="detail-suggestion">
                <span class="suggestion-icon">&#128161;</span>
                <span>{{ error.suggestion }}</span>
              </div>
            </div>

            <!-- Warnings -->
            <div *ngFor="let warning of (result.warnings || [])" class="detail-item warning-item">
              <div class="detail-top">
                <span class="detail-code">{{ warning.code }}</span>
                <span class="detail-severity severity-warning">WAARSCHUWING</span>
              </div>
              <div class="detail-message">{{ warning.message }}</div>
              <div class="detail-suggestion">
                <span class="suggestion-icon">&#128161;</span>
                <span>{{ warning.suggestion }}</span>
              </div>
            </div>

            <!-- No issues -->
            <div *ngIf="result.errors.length === 0 && (result.warnings || []).length === 0" class="no-issues">
              <span class="no-issues-icon">&#10004;</span>
              Geen problemen gevonden in deze validatielaag.
            </div>
          </div>
        </div>
      </div>

      <!-- Validation flow diagram -->
      <div class="validation-flow">
        <div class="flow-title">Validatie Proces</div>
        <div class="flow-steps">
          <div *ngFor="let result of validationResults; let i = index; let last = last" class="flow-step-group">
            <div class="flow-step" [class.flow-valid]="result.valid" [class.flow-invalid]="!result.valid">
              <div class="flow-step-num">{{ i + 1 }}</div>
              <div class="flow-step-name">{{ getLayerShortName(result.layer) }}</div>
              <div class="flow-step-icon">{{ result.valid ? '&#10004;' : '&#10006;' }}</div>
            </div>
            <div *ngIf="!last" class="flow-connector" [class.flow-valid]="result.valid" [class.flow-invalid]="!result.valid">
              &#10142;
            </div>
          </div>
          <div class="flow-step flow-result" [class.flow-valid]="allValid" [class.flow-invalid]="!allValid">
            <div class="flow-step-name">{{ allValid ? 'KLAAR' : 'GEBLOKKEERD' }}</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .validation-panel { margin-bottom: 2rem; }
    .section-header { margin-bottom: 1.5rem; }
    .section-header h2 { color: #1565c0; margin-bottom: 0.25rem; }
    .subtitle { color: #666; margin: 0; }

    /* Overall status */
    .overall-status {
      display: flex; align-items: center; gap: 1rem;
      padding: 1rem 1.5rem; border-radius: 10px; margin-bottom: 1.5rem;
    }
    .overall-status.all-valid { background: linear-gradient(135deg, #e8f5e9, #c8e6c9); }
    .overall-status.has-errors { background: linear-gradient(135deg, #ffebee, #ffcdd2); }
    .overall-icon { font-size: 2rem; }
    .all-valid .overall-icon { color: #2e7d32; }
    .has-errors .overall-icon { color: #c62828; }
    .overall-text { display: flex; flex-direction: column; }
    .overall-text strong { font-size: 1.1rem; }
    .overall-text span { font-size: 0.85rem; color: #666; }

    /* Layer cards grid */
    .layers-grid { display: flex; flex-direction: column; gap: 1rem; margin-bottom: 1.5rem; }

    .layer-card {
      border-radius: 10px; overflow: hidden;
      border: 2px solid #e0e0e0; transition: all 0.3s;
    }
    .layer-card.valid { border-color: #4caf50; }
    .layer-card.warning { border-color: #ff9800; }
    .layer-card.invalid { border-color: #f44336; }

    .layer-top {
      display: flex; align-items: center; gap: 1rem;
      padding: 1rem 1.25rem;
    }
    .layer-card.valid .layer-top { background: linear-gradient(135deg, #e8f5e9, #f1f8e9); }
    .layer-card.warning .layer-top { background: linear-gradient(135deg, #fff8e1, #fff3e0); }
    .layer-card.invalid .layer-top { background: linear-gradient(135deg, #ffebee, #fce4ec); }

    .layer-number {
      width: 40px; height: 40px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 1.2rem; font-weight: bold; color: white; flex-shrink: 0;
    }
    .layer-card.valid .layer-number { background: #4caf50; }
    .layer-card.warning .layer-number { background: #ff9800; }
    .layer-card.invalid .layer-number { background: #f44336; }

    .layer-info { flex: 1; }
    .layer-name { font-weight: 700; font-size: 1rem; color: #333; }
    .layer-desc { font-size: 0.8rem; color: #666; }

    .layer-badge-container { flex-shrink: 0; }
    .layer-badge {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 4px 12px; border-radius: 20px;
      font-size: 0.75rem; font-weight: 700; letter-spacing: 0.5px;
      background: #4caf50; color: white;
    }
    .warn-badge { background: #ff9800; }
    .fail-badge { background: #f44336; }

    /* Toggle */
    .layer-toggle {
      display: flex; justify-content: space-between; align-items: center;
      padding: 0.5rem 1.25rem; background: #f9f9f9;
      cursor: pointer; font-size: 0.85rem; color: #666;
      border-top: 1px solid #eee; user-select: none;
    }
    .layer-toggle:hover { background: #f0f0f0; }
    .toggle-arrow { font-size: 0.8rem; }

    /* Details */
    .layer-details { padding: 0.75rem 1.25rem 1rem; }
    .detail-item { padding: 0.75rem; border-radius: 6px; margin-bottom: 0.5rem; }
    .error-item { background: #fff3e0; border-left: 4px solid #f44336; }
    .warning-item { background: #fffde7; border-left: 4px solid #ff9800; }
    .detail-top { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
    .detail-code { font-family: 'Consolas', monospace; font-weight: bold; font-size: 0.85rem; }
    .detail-severity {
      font-size: 0.65rem; padding: 2px 6px; border-radius: 3px;
      font-weight: 700; letter-spacing: 0.5px;
    }
    .severity-error { background: #f44336; color: white; }
    .severity-warning { background: #ff9800; color: white; }
    .detail-message { font-size: 0.9rem; color: #333; margin-bottom: 0.25rem; }
    .detail-suggestion {
      display: flex; align-items: flex-start; gap: 0.4rem;
      font-size: 0.82rem; color: #1565c0; background: #e3f2fd;
      padding: 0.4rem 0.6rem; border-radius: 4px;
    }
    .suggestion-icon { flex-shrink: 0; }
    .no-issues { display: flex; align-items: center; gap: 0.5rem; color: #2e7d32; font-style: italic; padding: 0.5rem; }
    .no-issues-icon { font-size: 1.2rem; }

    /* Validation flow diagram */
    .validation-flow {
      background: #263238; border-radius: 10px; padding: 1.25rem 1.5rem;
      color: white;
    }
    .flow-title { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1px; color: #90a4ae; margin-bottom: 1rem; }
    .flow-steps { display: flex; align-items: center; justify-content: center; gap: 0; flex-wrap: wrap; }
    .flow-step-group { display: flex; align-items: center; }
    .flow-step {
      display: flex; flex-direction: column; align-items: center; gap: 4px;
      padding: 0.75rem 1.25rem; border-radius: 8px;
      min-width: 100px; text-align: center;
    }
    .flow-step.flow-valid { background: rgba(76, 175, 80, 0.2); border: 1px solid #4caf50; }
    .flow-step.flow-invalid { background: rgba(244, 67, 54, 0.2); border: 1px solid #f44336; }
    .flow-step-num { font-size: 0.7rem; color: #90a4ae; }
    .flow-step-name { font-size: 0.8rem; font-weight: 600; }
    .flow-step-icon { font-size: 1.2rem; }
    .flow-valid .flow-step-icon { color: #4caf50; }
    .flow-invalid .flow-step-icon { color: #f44336; }
    .flow-connector { font-size: 1.5rem; margin: 0 0.5rem; }
    .flow-connector.flow-valid { color: #4caf50; }
    .flow-connector.flow-invalid { color: #f44336; }
    .flow-result { font-size: 0.9rem; font-weight: 700; margin-left: 0.5rem; }
  `]
})
export class ValidationPanelComponent implements OnChanges {
  @Input() validationResults: ValidationResult[] = [];

  expandedLayers: { [key: string]: boolean } = {};
  allValid = true;
  totalErrors = 0;
  totalWarnings = 0;

  ngOnChanges(changes: SimpleChanges) {
    this.totalErrors = this.validationResults.reduce((sum, r) => sum + r.errors.length, 0);
    this.totalWarnings = this.validationResults.reduce((sum, r) => sum + (r.warnings || []).length, 0);
    this.allValid = this.validationResults.every(r => r.valid);

    // Auto-expand layers with issues
    for (const result of this.validationResults) {
      if (!result.valid || (result.warnings || []).length > 0) {
        this.expandedLayers[result.layer] = true;
      }
    }
  }

  toggleLayer(layer: string) {
    this.expandedLayers[layer] = !this.expandedLayers[layer];
  }

  getLayerName(layer: string): string {
    switch (layer) {
      case 'SCHEMA': return 'JSON Schema Validatie';
      case 'BUSINESS': return 'Business Rule Validatie';
      case 'REFERENCE': return 'Referentie-integriteit Validatie';
      default: return layer;
    }
  }

  getLayerShortName(layer: string): string {
    switch (layer) {
      case 'SCHEMA': return 'Schema';
      case 'BUSINESS': return 'Business';
      case 'REFERENCE': return 'Referentie';
      default: return layer;
    }
  }

  getLayerDescription(layer: string): string {
    switch (layer) {
      case 'SCHEMA': return 'Controleert structuur, verplichte velden en datatypes van de configuratie';
      case 'BUSINESS': return 'Valideert business regels: dubbele mappings, formaten en veldaantallen';
      case 'REFERENCE': return 'Controleert of XPath expressies geldig zijn en datatypes overeenkomen';
      default: return '';
    }
  }
}
