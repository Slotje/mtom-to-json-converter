import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConversionResult } from '../../services/converter.service';

@Component({
  selector: 'app-result-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './result-display.component.html'
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
