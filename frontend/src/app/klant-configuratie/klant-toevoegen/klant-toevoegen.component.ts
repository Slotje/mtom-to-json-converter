import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Klant } from '../../model/klant.model';
import { KlantService } from '../../services/klant.service';

@Component({
  selector: 'app-klant-toevoegen',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './klant-toevoegen.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class KlantToevoegenComponent implements OnInit {
  klant: Klant = this.emptyKlant();
  step = 1;
  objectStoreOptions = ['OS_BTE_PROD', 'OS_BTE_TEST', 'OS_BTE_ACC', 'OS_BTE_ONT'];
  validationErrors: string[] = [];

  constructor(private klantService: KlantService, private router: Router) {}

  ngOnInit() {
    if (this.klantService.pendingKlant) {
      this.klant = this.klantService.pendingKlant;
    } else {
      this.klant.id = this.klantService.generateId();
    }
  }

  addContact() {
    this.klant.contacten.push({ naam: '', email: '', telefoon: '' });
  }

  removeContact(idx: number) {
    if (this.klant.contacten.length > 1) {
      this.klant.contacten.splice(idx, 1);
    }
  }

  validate(): boolean {
    this.validationErrors = [];
    if (!this.klant.klantnaam.trim()) this.validationErrors.push('Klantnaam is verplicht');
    if (!this.klant.domeinKeten.trim()) this.validationErrors.push('Domein/keten is verplicht');
    if (!this.klant.oplosgroep.trim()) this.validationErrors.push('Oplosgroep is verplicht');
    if (!this.klant.objectStore) this.validationErrors.push('ObjectStore is verplicht');
    if (!this.klant.klasse.trim()) this.validationErrors.push('Klasse is verplicht');
    const firstContact = this.klant.contacten[0];
    if (!firstContact || !firstContact.naam.trim()) this.validationErrors.push('Contactpersoon naam is verplicht');
    if (!firstContact || !firstContact.email.trim()) this.validationErrors.push('Contactpersoon e-mailadres is verplicht');
    return this.validationErrors.length === 0;
  }

  goToStep2() {
    if (!this.validate()) return;
    this.klantService.pendingKlant = this.klant;
    this.step = 2;
  }

  goToStep1() {
    this.step = 1;
  }

  onConfigUpload(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const reader = new FileReader();
    reader.onload = () => {
      const content = reader.result as string;
      this.klant.configuratieYaml = content;
      try {
        const config = JSON.parse(content);
        if (config.klantnaam) this.klant.klantnaam = config.klantnaam;
        if (config.domeinKeten) this.klant.domeinKeten = config.domeinKeten;
        if (config.oplosgroep) this.klant.oplosgroep = config.oplosgroep;
        if (config.objectStore) this.klant.objectStore = config.objectStore;
        if (config.klasse) this.klant.klasse = config.klasse;
        if (config.contacten && Array.isArray(config.contacten) && config.contacten.length > 0) {
          this.klant.contacten = config.contacten;
        }
        this.validationErrors = [];
      } catch {
        // Not JSON - store as raw config (YAML)
      }
    };
    reader.readAsText(input.files[0]);
  }

  onConversieUpload(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const reader = new FileReader();
    reader.onload = () => {
      this.klant.conversieBestand = reader.result as string;
    };
    reader.readAsText(input.files[0]);
  }

  finishAdd() {
    this.klantService.addKlant(this.klant);
    this.klantService.pendingKlant = null;
    this.router.navigateByUrl('/klantconfiguratie');
  }

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
