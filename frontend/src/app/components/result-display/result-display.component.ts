import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConversionResult } from '../../services/converter.service';

@Component({
  selector: 'app-result-display',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="result-display" *ngIf="result">
      <div class="section-header">
        <h2>Conversie Resultaat: MTOM &#10142; JSON</h2>
      </div>

      <div class="result-banner" [class.success]="result.success" [class.failure]="!result.success">
        <div class="banner-icon">{{ result.success ? '&#10004;' : '&#10006;' }}</div>
        <div class="banner-text">
          <strong>{{ result.success ? 'MTOM bericht succesvol geconverteerd naar JSON' : 'Conversie voltooid met validatiefouten' }}</strong>
          <span>{{ extractedFieldEntries.length }} velden geextraheerd uit het MTOM bericht</span>
        </div>
        <div class="banner-actions">
          <button class="action-btn download" (click)="downloadJson()">&#11015; Download JSON</button>
          <button class="action-btn copy" (click)="copyJson()">&#128203; Kopieer</button>
        </div>
      </div>

      <!-- Side by side: MTOM fields -> JSON output -->
      <div class="comparison">
        <!-- Left: Extracted from MTOM -->
        <div class="comparison-panel source-panel">
          <div class="panel-header source-header">
            <span class="panel-icon">&#128196;</span>
            <span>Geextraheerd uit MTOM</span>
            <span class="panel-count">{{ extractedFieldEntries.length }} velden</span>
          </div>
          <div class="panel-body">
            <div *ngFor="let entry of extractedFieldEntries; let i = index"
                 class="field-row"
                 [class.not-found]="entry[1] === '(niet gevonden)'"
                 [class.has-default]="entry[1].startsWith('(default)')"
                 [style.animation-delay]="(i * 50) + 'ms'">
              <div class="field-row-left">
                <code class="field-xpath">{{ entry[0] }}</code>
              </div>
              <div class="field-row-right">
                <span *ngIf="entry[1] === '(niet gevonden)'" class="value-badge missing-value">&#10006; Niet gevonden</span>
                <span *ngIf="entry[1].startsWith('(default)')" class="value-badge default-value">&#8594; {{ entry[1] }}</span>
                <span *ngIf="entry[1] !== '(niet gevonden)' && !entry[1].startsWith('(default)')" class="value-badge found-value">{{ entry[1] }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Center: Arrow -->
        <div class="comparison-arrow">
          <div class="arrow-vertical">
            <div class="arrow-label">YAML<br>Mapping</div>
            <div class="arrow-big">&#10142;</div>
          </div>
        </div>

        <!-- Right: JSON Output -->
        <div class="comparison-panel output-panel">
          <div class="panel-header output-header">
            <span class="panel-icon">&#128204;</span>
            <span>JSON Output</span>
            <span class="panel-count">{{ jsonLineCount }} regels</span>
          </div>
          <div class="panel-body json-body">
            <pre class="json-output"><code>{{ jsonFormatted }}</code></pre>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .result-display { margin-bottom: 2rem; }
    .section-header h2 { color: #1565c0; margin-bottom: 0.25rem; }

    /* Result banner */
    .result-banner {
      display: flex; align-items: center; gap: 1rem;
      padding: 1rem 1.5rem; border-radius: 10px; margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }
    .result-banner.success { background: linear-gradient(135deg, #e8f5e9, #c8e6c9); }
    .result-banner.failure { background: linear-gradient(135deg, #ffebee, #ffcdd2); }
    .banner-icon { font-size: 2rem; }
    .success .banner-icon { color: #2e7d32; }
    .failure .banner-icon { color: #c62828; }
    .banner-text { flex: 1; display: flex; flex-direction: column; }
    .banner-text strong { font-size: 1.05rem; }
    .banner-text span { font-size: 0.82rem; color: #666; }
    .banner-actions { display: flex; gap: 0.5rem; }
    .action-btn {
      padding: 0.5rem 1rem; border: none; border-radius: 6px;
      cursor: pointer; font-size: 0.85rem; font-weight: 600;
      transition: all 0.2s;
    }
    .action-btn.download { background: #1976d2; color: white; }
    .action-btn.download:hover { background: #1565c0; }
    .action-btn.copy { background: white; color: #333; border: 1px solid #ccc; }
    .action-btn.copy:hover { background: #f5f5f5; }

    /* Comparison layout */
    .comparison { display: grid; grid-template-columns: 1fr 80px 1fr; gap: 0; margin-top: 1rem; }
    .comparison-panel { border: 2px solid #e0e0e0; border-radius: 10px; overflow: hidden; }
    .source-panel { border-color: #90caf9; }
    .output-panel { border-color: #a5d6a7; }

    .panel-header {
      display: flex; align-items: center; gap: 0.5rem;
      padding: 0.75rem 1rem; font-weight: 700; font-size: 0.9rem;
    }
    .source-header { background: #1565c0; color: white; }
    .output-header { background: #2e7d32; color: white; }
    .panel-icon { font-size: 1.1rem; }
    .panel-count { margin-left: auto; font-size: 0.75rem; opacity: 0.8; font-weight: normal; }
    .panel-body { max-height: 600px; overflow-y: auto; }

    /* Field rows */
    .field-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 0.5rem 1rem; border-bottom: 1px solid #f0f0f0;
      animation: fadeIn 0.3s ease-out both;
    }
    .field-row:hover { background: #fafafa; }
    .field-row.not-found { background: #fff3e0; }
    .field-row.has-default { background: #f3e5f5; }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

    .field-xpath {
      font-size: 0.78rem; color: #37474f; background: #eceff1;
      padding: 2px 6px; border-radius: 3px; word-break: break-all;
    }
    .field-row-left { flex: 1; min-width: 0; }
    .field-row-right { flex-shrink: 0; margin-left: 0.5rem; }

    .value-badge { font-size: 0.82rem; padding: 2px 8px; border-radius: 4px; white-space: nowrap; }
    .found-value { background: #e8f5e9; color: #1b5e20; font-family: 'Consolas', monospace; }
    .missing-value { background: #ffcdd2; color: #b71c1c; }
    .default-value { background: #e1bee7; color: #6a1b9a; font-style: italic; }

    /* Center arrow */
    .comparison-arrow { display: flex; align-items: center; justify-content: center; }
    .arrow-vertical { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
    .arrow-label {
      font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px;
      color: #999; text-align: center; line-height: 1.3;
    }
    .arrow-big { font-size: 2rem; color: #1976d2; }

    /* JSON output */
    .json-body { padding: 0; }
    .json-output {
      margin: 0; padding: 1rem; background: #1a2327; color: #eeffff;
      font-size: 0.82rem; line-height: 1.5; overflow-x: auto;
      min-height: 200px;
    }

    @media (max-width: 900px) {
      .comparison { grid-template-columns: 1fr; gap: 1rem; }
      .comparison-arrow { transform: rotate(90deg); padding: 1rem 0; }
    }
  `]
})
export class ResultDisplayComponent {
  @Input() result: ConversionResult | null = null;

  get jsonFormatted(): string {
    return this.result?.jsonOutput ? JSON.stringify(this.result.jsonOutput, null, 2) : '';
  }

  get jsonLineCount(): number {
    return this.jsonFormatted.split('\n').length;
  }

  get extractedFieldEntries(): [string, string][] {
    if (!this.result?.extractedFields) return [];
    return Object.entries(this.result.extractedFields);
  }

  downloadJson() {
    if (!this.result?.jsonOutput) return;
    const blob = new Blob([JSON.stringify(this.result.jsonOutput, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'conversion-result.json';
    a.click();
    URL.revokeObjectURL(url);
  }

  copyJson() {
    navigator.clipboard.writeText(JSON.stringify(this.result?.jsonOutput, null, 2));
  }
}
