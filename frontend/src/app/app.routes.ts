import { Routes } from '@angular/router';
import { ConfigUploadComponent } from './config/config-upload/config-upload.component';
import { MtomAnalyzerComponent } from './analyzer/mtom-analyzer/mtom-analyzer.component';
import { VisualMappingComponent } from './mapping/visual-mapping/visual-mapping.component';
import { ValidationPanelComponent } from './results/validation-panel/validation-panel.component';
import { ResultDisplayComponent } from './results/result-display/result-display.component';

export const routes: Routes = [
  { path: '', redirectTo: 'config', pathMatch: 'full' },
  { path: 'config', component: ConfigUploadComponent },
  { path: 'analyze', component: MtomAnalyzerComponent },
  { path: 'mapping', component: VisualMappingComponent },
  { path: 'validation', component: ValidationPanelComponent },
  { path: 'result', component: ResultDisplayComponent }
];
