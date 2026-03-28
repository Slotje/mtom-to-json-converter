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

  constructor(private klantService: KlantService, private router: Router) {}

  ngOnInit() {
    // Resume pending klant if returning from step 2 back
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

  goToStep2() {
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
      this.klant.configuratieYaml = reader.result as string;
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
