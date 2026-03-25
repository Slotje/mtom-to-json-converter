import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DetectedField, FieldMapping } from '../../services/converter.service';

interface MappingConnection {
  source: DetectedField;
  target: FieldMapping;
  matched: boolean;
}

@Component({
  selector: 'app-visual-mapping',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="visual-mapping" *ngIf="detectedFields.length > 0 && configFields.length > 0">
      <div class="section-header">
        <h2>Visuele Mapping: MTOM naar JSON</h2>
        <p class="subtitle">Hieronder ziet u hoe bronvelden uit het MTOM bericht worden gemapped naar de JSON doelvelden via de YAML configuratie.</p>
      </div>

      <!-- Mapping stats banner -->
      <div class="stats-banner">
        <div class="stat">
          <div class="stat-number">{{ detectedFields.length }}</div>
          <div class="stat-label">Bronvelden (MTOM)</div>
        </div>
        <div class="stat-arrow">&#10142;</div>
        <div class="stat">
          <div class="stat-number">{{ connections.length }}</div>
          <div class="stat-label">Gemapte Velden</div>
        </div>
        <div class="stat-arrow">&#10142;</div>
        <div class="stat">
          <div class="stat-number">{{ configFields.length }}</div>
          <div class="stat-label">Doelvelden (JSON)</div>
        </div>
        <div class="stat-separator"></div>
        <div class="stat" [class.stat-ok]="unmappedRequired.length === 0" [class.stat-warn]="unmappedRequired.length > 0">
          <div class="stat-number">{{ unmappedRequired.length }}</div>
          <div class="stat-label">Ontbrekend (verplicht)</div>
        </div>
      </div>

      <!-- Main mapping visualization -->
      <div class="mapping-table">
        <div class="mapping-header">
          <div class="col-source">MTOM Bronveld</div>
          <div class="col-arrow"></div>
          <div class="col-target">JSON Doelveld</div>
          <div class="col-value">Waarde uit MTOM</div>
          <div class="col-type">Type</div>
          <div class="col-status">Status</div>
        </div>

        <!-- Matched connections -->
        <div *ngFor="let conn of connections; let i = index"
             class="mapping-row"
             [class.animate-in]="true"
             [style.animation-delay]="(i * 80) + 'ms'">
          <div class="col-source">
            <div class="field-badge source-badge">
              <span class="badge-icon">&#128196;</span>
              <span class="badge-name">{{ conn.source.fieldName }}</span>
            </div>
            <code class="xpath-mini">{{ conn.source.xpathExpression }}</code>
          </div>
          <div class="col-arrow">
            <div class="arrow-line">
              <div class="arrow-dot start"></div>
              <div class="arrow-body"></div>
              <div class="arrow-head">&#9654;</div>
            </div>
          </div>
          <div class="col-target">
            <div class="field-badge target-badge">
              <span class="badge-icon">&#128204;</span>
              <span class="badge-name">{{ conn.target.targetProperty }}</span>
            </div>
            <span *ngIf="conn.target.required" class="required-tag">VERPLICHT</span>
          </div>
          <div class="col-value">
            <span class="value-text">{{ conn.source.sampleValue }}</span>
          </div>
          <div class="col-type">
            <span class="type-pill" [attr.data-type]="conn.target.dataType">{{ conn.target.dataType }}</span>
          </div>
          <div class="col-status">
            <span class="status-ok">&#10004; Gemapped</span>
          </div>
        </div>

        <!-- Unmapped required fields -->
        <div *ngFor="let field of unmappedRequired; let i = index"
             class="mapping-row unmapped"
             [style.animation-delay]="((connections.length + i) * 80) + 'ms'">
          <div class="col-source">
            <div class="field-badge missing-badge">
              <span class="badge-icon">&#10067;</span>
              <span class="badge-name">Niet gevonden</span>
            </div>
            <code class="xpath-mini">{{ field.sourceField }}</code>
          </div>
          <div class="col-arrow">
            <div class="arrow-line broken">
              <div class="arrow-dot start broken"></div>
              <div class="arrow-body broken"></div>
              <div class="arrow-head broken">&#10006;</div>
            </div>
          </div>
          <div class="col-target">
            <div class="field-badge target-badge-warn">
              <span class="badge-icon">&#9888;</span>
              <span class="badge-name">{{ field.targetProperty }}</span>
            </div>
            <span class="required-tag">VERPLICHT</span>
          </div>
          <div class="col-value">
            <span class="value-missing">{{ field.defaultValue ? '(default: ' + field.defaultValue + ')' : '- geen waarde -' }}</span>
          </div>
          <div class="col-type">
            <span class="type-pill" [attr.data-type]="field.dataType">{{ field.dataType }}</span>
          </div>
          <div class="col-status">
            <span class="status-missing">&#10006; Ontbreekt</span>
          </div>
        </div>

        <!-- Unmapped optional fields -->
        <div *ngFor="let field of unmappedOptional; let i = index"
             class="mapping-row optional-unmapped"
             [style.animation-delay]="((connections.length + unmappedRequired.length + i) * 80) + 'ms'">
          <div class="col-source">
            <div class="field-badge missing-badge-light">
              <span class="badge-icon">&#8212;</span>
              <span class="badge-name">Niet gevonden</span>
            </div>
            <code class="xpath-mini">{{ field.sourceField }}</code>
          </div>
          <div class="col-arrow">
            <div class="arrow-line dimmed">
              <div class="arrow-dot start dimmed"></div>
              <div class="arrow-body dimmed"></div>
              <div class="arrow-head dimmed">&#8594;</div>
            </div>
          </div>
          <div class="col-target">
            <div class="field-badge target-badge-dim">
              <span class="badge-name">{{ field.targetProperty }}</span>
            </div>
            <span class="optional-tag">OPTIONEEL</span>
          </div>
          <div class="col-value">
            <span class="value-missing">{{ field.defaultValue ? '(default: ' + field.defaultValue + ')' : '-' }}</span>
          </div>
          <div class="col-type">
            <span class="type-pill" [attr.data-type]="field.dataType">{{ field.dataType }}</span>
          </div>
          <div class="col-status">
            <span class="status-optional">&#8212; Optioneel</span>
          </div>
        </div>
      </div>

      <!-- Extra detected fields not in config -->
      <div *ngIf="unmappedSourceFields.length > 0" class="extra-fields">
        <h3>Extra velden in MTOM (niet in configuratie)</h3>
        <div class="extra-fields-list">
          <span *ngFor="let field of unmappedSourceFields" class="extra-field-chip">
            {{ field.fieldName }}: <em>{{ field.sampleValue }}</em>
          </span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .visual-mapping { margin-bottom: 2rem; }
    .section-header { margin-bottom: 1.5rem; }
    .section-header h2 { color: #1565c0; margin-bottom: 0.25rem; }
    .subtitle { color: #666; margin: 0; }

    /* Stats banner */
    .stats-banner {
      display: flex; align-items: center; gap: 1.5rem;
      background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
      padding: 1rem 1.5rem; border-radius: 10px; margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }
    .stat { text-align: center; }
    .stat-number { font-size: 1.8rem; font-weight: bold; color: #1565c0; }
    .stat-label { font-size: 0.75rem; color: #666; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-arrow { font-size: 1.5rem; color: #1976d2; }
    .stat-separator { width: 1px; height: 40px; background: #ccc; margin: 0 0.5rem; }
    .stat-ok .stat-number { color: #2e7d32; }
    .stat-warn .stat-number { color: #c62828; }

    /* Mapping table */
    .mapping-table { border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; }
    .mapping-header {
      display: grid; grid-template-columns: 2fr 80px 2fr 1.5fr 80px 110px;
      background: #263238; color: white; padding: 0.75rem 1rem;
      font-weight: 600; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.5px;
    }
    .mapping-row {
      display: grid; grid-template-columns: 2fr 80px 2fr 1.5fr 80px 110px;
      padding: 0.6rem 1rem; border-bottom: 1px solid #f0f0f0;
      align-items: center; transition: background 0.2s;
      animation: slideIn 0.4s ease-out both;
    }
    .mapping-row:hover { background: #fafafa; }
    .mapping-row.unmapped { background: #fff3e0; }
    .mapping-row.unmapped:hover { background: #ffe0b2; }
    .mapping-row.optional-unmapped { background: #fafafa; opacity: 0.7; }

    @keyframes slideIn {
      from { opacity: 0; transform: translateX(-10px); }
      to { opacity: 1; transform: translateX(0); }
    }

    /* Field badges */
    .field-badge {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 8px; border-radius: 6px; font-size: 0.85rem; font-weight: 600;
    }
    .source-badge { background: #bbdefb; color: #0d47a1; }
    .target-badge { background: #c8e6c9; color: #1b5e20; }
    .target-badge-warn { background: #ffcdd2; color: #b71c1c; }
    .target-badge-dim { background: #e0e0e0; color: #666; padding: 3px 8px; border-radius: 6px; font-size: 0.85rem; }
    .missing-badge { background: #ffecb3; color: #e65100; }
    .missing-badge-light { background: #f5f5f5; color: #999; }
    .badge-icon { font-size: 0.9rem; }
    .badge-name { white-space: nowrap; }

    .xpath-mini {
      display: block; font-size: 0.7rem; color: #78909c; margin-top: 2px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 220px;
    }

    .required-tag {
      display: inline-block; font-size: 0.6rem; padding: 1px 5px; border-radius: 3px;
      background: #f44336; color: white; font-weight: bold; margin-left: 6px; vertical-align: middle;
    }
    .optional-tag {
      display: inline-block; font-size: 0.6rem; padding: 1px 5px; border-radius: 3px;
      background: #bdbdbd; color: white; font-weight: bold; margin-left: 6px; vertical-align: middle;
    }

    /* Arrow visualization */
    .arrow-line { display: flex; align-items: center; justify-content: center; gap: 2px; }
    .arrow-dot { width: 8px; height: 8px; border-radius: 50%; background: #1976d2; }
    .arrow-body { width: 30px; height: 2px; background: #1976d2; }
    .arrow-head { color: #1976d2; font-size: 1rem; }
    .arrow-dot.broken { background: #f44336; }
    .arrow-body.broken { background: #f44336; border-style: dashed; height: 1px; border-top: 2px dashed #f44336; background: transparent; }
    .arrow-head.broken { color: #f44336; }
    .arrow-dot.dimmed { background: #ccc; }
    .arrow-body.dimmed { background: #ccc; }
    .arrow-head.dimmed { color: #ccc; }

    /* Values */
    .value-text { font-size: 0.85rem; color: #333; font-family: 'Consolas', monospace; }
    .value-missing { font-size: 0.8rem; color: #999; font-style: italic; }

    /* Type pills */
    .type-pill {
      display: inline-block; padding: 2px 8px; border-radius: 10px;
      font-size: 0.7rem; font-weight: 600; text-transform: uppercase;
    }
    .type-pill[data-type="string"] { background: #e3f2fd; color: #1565c0; }
    .type-pill[data-type="date"] { background: #f3e5f5; color: #7b1fa2; }
    .type-pill[data-type="integer"] { background: #e8f5e9; color: #2e7d32; }
    .type-pill[data-type="boolean"] { background: #fff3e0; color: #e65100; }

    /* Status indicators */
    .status-ok { color: #2e7d32; font-weight: 600; font-size: 0.8rem; }
    .status-missing { color: #c62828; font-weight: 600; font-size: 0.8rem; }
    .status-optional { color: #999; font-size: 0.8rem; }

    /* Extra fields */
    .extra-fields { margin-top: 1rem; padding: 1rem; background: #f5f5f5; border-radius: 8px; }
    .extra-fields h3 { font-size: 0.9rem; color: #666; margin-bottom: 0.5rem; }
    .extra-fields-list { display: flex; flex-wrap: wrap; gap: 6px; }
    .extra-field-chip {
      display: inline-block; padding: 4px 10px; background: white;
      border: 1px solid #e0e0e0; border-radius: 16px; font-size: 0.8rem; color: #555;
    }
    .extra-field-chip em { color: #1976d2; }
  `]
})
export class VisualMappingComponent implements OnChanges {
  @Input() detectedFields: DetectedField[] = [];
  @Input() configFields: FieldMapping[] = [];

  connections: MappingConnection[] = [];
  unmappedRequired: FieldMapping[] = [];
  unmappedOptional: FieldMapping[] = [];
  unmappedSourceFields: DetectedField[] = [];

  ngOnChanges(changes: SimpleChanges) {
    this.autoMap();
  }

  private autoMap() {
    this.connections = [];
    const mappedSources = new Set<DetectedField>();

    for (const configField of this.configFields) {
      const match = this.detectedFields.find(df => {
        const normalizedSource = configField.sourceField.toLowerCase();
        const normalizedDetected = df.xpathExpression.toLowerCase();
        const fieldName = df.fieldName.toLowerCase();

        // Match XPath directly
        if (normalizedSource === normalizedDetected) return true;

        // Match by key attribute pattern: //value[@key='X'] matches fieldName X
        const keyMatch = normalizedSource.match(/@key='([^']+)'/);
        if (keyMatch && keyMatch[1].toLowerCase() === fieldName) return true;

        // Match by element name: //Bestandsnaam matches fieldName Bestandsnaam
        const elemName = normalizedSource.replace('//', '').toLowerCase();
        if (elemName === fieldName) return true;

        return false;
      });

      if (match) {
        this.connections.push({ source: match, target: configField, matched: true });
        mappedSources.add(match);
      }
    }

    this.unmappedRequired = this.configFields.filter(
      f => f.required && !this.connections.some(c => c.target === f)
    );

    this.unmappedOptional = this.configFields.filter(
      f => !f.required && !this.connections.some(c => c.target === f)
    );

    this.unmappedSourceFields = this.detectedFields.filter(f => !mappedSources.has(f));
  }
}
