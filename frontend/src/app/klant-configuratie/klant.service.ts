import { Injectable } from '@angular/core';
import { Klant } from './klant.model';

@Injectable({ providedIn: 'root' })
export class KlantService {
  private storageKey = 'bte-klanten';

  getKlanten(): Klant[] {
    const data = localStorage.getItem(this.storageKey);
    return data ? JSON.parse(data) : this.getDefaultKlanten();
  }

  saveKlanten(klanten: Klant[]): void {
    localStorage.setItem(this.storageKey, JSON.stringify(klanten));
  }

  addKlant(klant: Klant): void {
    const klanten = this.getKlanten();
    klanten.push(klant);
    this.saveKlanten(klanten);
  }

  updateKlant(klant: Klant): void {
    const klanten = this.getKlanten();
    const idx = klanten.findIndex(k => k.id === klant.id);
    if (idx >= 0) {
      klanten[idx] = klant;
      this.saveKlanten(klanten);
    }
  }

  deleteKlant(id: string): void {
    const klanten = this.getKlanten().filter(k => k.id !== id);
    this.saveKlanten(klanten);
  }

  generateId(): string {
    return crypto.randomUUID();
  }

  exportKlant(klant: Klant): string {
    return JSON.stringify(klant, null, 2);
  }

  importKlant(json: string): Klant {
    return JSON.parse(json);
  }

  private getDefaultKlanten(): Klant[] {
    return [
      {
        id: '550e8400-e29b-41d4-a716-446655440000',
        klantnaam: 'COS - VAT Refund',
        domeinKeten: 'Omzetbelasting',
        oplosgroep: 'BB ECM Support',
        contacten: [
          { naam: 'Functioneel Beheer', email: 'bte-support@belastingdienst.nl', telefoon: '0800-0543' }
        ],
        objectStore: 'OS_BTE_PROD',
        klasse: 'BTE_Aangifte'
      }
    ];
  }
}
