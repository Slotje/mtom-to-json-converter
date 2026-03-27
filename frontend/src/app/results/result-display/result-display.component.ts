import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConversionResult } from '../../services/converter.service';
import { WizardStateService } from '../../services/wizard-state.service';

@Component({
  selector: 'app-result-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './result-display.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ResultDisplayComponent implements OnInit {
  result: ConversionResult | null = null;

  constructor(private wizard: WizardStateService) {}

  ngOnInit() {
    this.result = this.wizard.conversionResult;
  }

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
