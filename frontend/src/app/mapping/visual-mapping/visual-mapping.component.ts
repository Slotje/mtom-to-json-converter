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
  templateUrl: './visual-mapping.component.html'
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
