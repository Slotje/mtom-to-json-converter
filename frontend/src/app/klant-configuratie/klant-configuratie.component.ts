import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Klant, ContactPersoon } from '../model/klant.model';
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

  // Accordion state
  expandedKlantId: string | null = null;

  // Add/edit state
  showAddWizard = false;
  addStep = 1;
  editingKlant: Klant | null = null;
  isEditing = false;

  // New klant form
  newKlant: Klant = this.emptyKlant();

  // Dropdown menu
  showDropdown = false;

  // File references
  configFileInput: HTMLInputElement | null = null;
  conversieFileInput: HTMLInputElement | null = null;

  objectStoreOptions = ['OS_BTE_PROD', 'OS_BTE_TEST', 'OS_BTE_ACC', 'OS_BTE_ONT'];

  constructor(private klantService: KlantService) {}

  ngOnInit() {
    this.klanten = this.klantService.getKlanten();
    this.filteredKlanten = [...this.klanten];
  }

  // --- Search ---
  onSearch() {
    const term = this.searchTerm.toLowerCase();
    this.filteredKlanten = this.klanten.filter(k =>
      k.klantnaam.toLowerCase().includes(term) ||
      k.domeinKeten.toLowerCase().includes(term) ||
      k.oplosgroep.toLowerCase().includes(term) ||
      k.id.toLowerCase().includes(term)
    );
  }

  // --- Accordion ---
  toggleKlant(klant: Klant) {
    if (this.expandedKlantId === klant.id) {
      this.expandedKlantId = null;
    } else {
      this.expandedKlantId = klant.id;
      this.isEditing = false;
    }
  }

  isExpanded(klant: Klant): boolean {
    return this.expandedKlantId === klant.id;
  }

  // --- Add klant wizard ---
  startAddKlant() {
    this.showAddWizard = true;
    this.addStep = 1;
    this.newKlant = this.emptyKlant();
    this.newKlant.id = this.klantService.generateId();
    this.showDropdown = false;
    this.expandedKlantId = null;
  }

  cancelAdd() {
    this.showAddWizard = false;
    this.newKlant = this.emptyKlant();
  }

  goToStep2() {
    this.addStep = 2;
  }

  finishAdd() {
    this.klantService.addKlant(this.newKlant);
    this.klanten = this.klantService.getKlanten();
    this.onSearch();
    this.showAddWizard = false;
    this.expandedKlantId = this.newKlant.id;
    this.newKlant = this.emptyKlant();
  }

  addContact() {
    this.newKlant.contacten.push({ naam: '', email: '', telefoon: '' });
  }

  removeContact(idx: number) {
    if (this.newKlant.contacten.length > 1) {
      this.newKlant.contacten.splice(idx, 1);
    }
  }

  // --- Edit klant ---
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

  // --- Import / Export ---
  toggleDropdown() {
    this.showDropdown = !this.showDropdown;
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
    this.showDropdown = false;
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
    this.showDropdown = false;
  }

  // --- Config upload ---
  onConfigUpload(event: Event, target: 'new' | 'edit') {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const reader = new FileReader();
    reader.onload = () => {
      const content = reader.result as string;
      if (target === 'new') {
        this.newKlant.configuratieYaml = content;
      } else if (this.editingKlant) {
        this.editingKlant.configuratieYaml = content;
      }
    };
    reader.readAsText(input.files[0]);
  }

  onConversieUpload(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const reader = new FileReader();
    reader.onload = () => {
      this.newKlant.conversieBestand = reader.result as string;
    };
    reader.readAsText(input.files[0]);
  }

  // --- Helpers ---
  private emptyKlant(): Klant {
    return {
      id: '',
      klantnaam: '',
      domeinKeten: '',
      oplosgroep: '',
      contacten: [{ naam: '', email: '', telefoon: '' }],
      objectStore: '',
      klasse: ''
    };
  }
}
