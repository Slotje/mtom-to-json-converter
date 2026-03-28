import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Klant } from '../model/klant.model';
import { KlantService } from '../services/klant.service';

@Component({
  selector: 'app-klant-configuratie',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './klant-configuratie.component.html',
  styleUrls: ['./klant-configuratie.component.css'],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class KlantConfiguratieComponent implements OnInit {
  klanten: Klant[] = [];
  filteredKlanten: Klant[] = [];
  searchTerm = '';
  expandedKlantId: string | null = null;
  isEditing = false;
  editingKlant: Klant | null = null;
  objectStoreOptions = ['OS_BTE_PROD', 'OS_BTE_TEST', 'OS_BTE_ACC', 'OS_BTE_ONT'];

  constructor(private klantService: KlantService, private router: Router) {}

  ngOnInit() {
    this.klanten = this.klantService.getKlanten();
    this.filteredKlanten = [...this.klanten];
  }

  onSearch() {
    const term = this.searchTerm.toLowerCase();
    this.filteredKlanten = this.klanten.filter(k =>
      k.klantnaam.toLowerCase().includes(term) ||
      k.domeinKeten.toLowerCase().includes(term) ||
      k.oplosgroep.toLowerCase().includes(term) ||
      k.id.toLowerCase().includes(term)
    );
  }

  toggleKlant(klant: Klant) {
    this.expandedKlantId = this.expandedKlantId === klant.id ? null : klant.id;
    this.isEditing = false;
    this.editingKlant = null;
  }

  isExpanded(klant: Klant): boolean {
    return this.expandedKlantId === klant.id;
  }

  startAddKlant() {
    this.router.navigateByUrl('/klantconfiguratie/toevoegen');
  }

  startEdit(klant: Klant) {
    this.editingKlant = JSON.parse(JSON.stringify(klant));
    this.isEditing = true;
  }

  cancelEdit() {
    this.editingKlant = null;
    this.isEditing = false;
  }

  saveEdit() {
    if (this.editingKlant) {
      this.klantService.updateKlant(this.editingKlant);
      this.klanten = this.klantService.getKlanten();
      this.onSearch();
      this.isEditing = false;
      this.editingKlant = null;
    }
  }

  addEditContact() {
    this.editingKlant?.contacten.push({ naam: '', email: '', telefoon: '' });
  }

  removeEditContact(idx: number) {
    if (this.editingKlant && this.editingKlant.contacten.length > 1) {
      this.editingKlant.contacten.splice(idx, 1);
    }
  }

  deleteKlant(klant: Klant) {
    this.klantService.deleteKlant(klant.id);
    this.klanten = this.klantService.getKlanten();
    this.onSearch();
    this.expandedKlantId = null;
  }

  importKlant(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const klant = this.klantService.importKlant(reader.result as string);
        if (!klant.id) klant.id = this.klantService.generateId();
        this.klantService.addKlant(klant);
        this.klanten = this.klantService.getKlanten();
        this.onSearch();
      } catch (e) {
        console.error('Import failed:', e);
      }
    };
    reader.readAsText(input.files[0]);
  }

  exportKlant() {
    const klant = this.klanten.find(k => k.id === this.expandedKlantId);
    if (!klant) return;
    const json = this.klantService.exportKlant(klant);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `klant-${klant.klantnaam.replace(/\s+/g, '-')}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
