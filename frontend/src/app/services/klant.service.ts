import { Injectable } from '@angular/core';
import { Klant } from '../model/klant.model';

@Injectable({ providedIn: 'root' })
export class KlantService {
  private storageKey = 'bte-klanten';
  private versionKey = 'bte-klanten-version';
  private currentVersion = '2';

  // Shared state for add wizard (between step 1 and step 2)
  pendingKlant: Klant | null = null;

  getKlanten(): Klant[] {
    const storedVersion = localStorage.getItem(this.versionKey);
    if (storedVersion !== this.currentVersion) {
      localStorage.removeItem(this.storageKey);
      localStorage.setItem(this.versionKey, this.currentVersion);
    }
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
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440001',
        klantnaam: 'Perron - Inkomensbelasting',
        domeinKeten: 'Inkomensbelasting',
        oplosgroep: 'BB ECM Perron',
        contacten: [
          { naam: 'Postbus Perron', email: 'perron-ecm@belastingdienst.nl', telefoon: '0800-0544' },
          { naam: 'Jan de Vries', email: 'j.devries@belastingdienst.nl', telefoon: '06-12345678' }
        ],
        objectStore: 'OS_BTE_PROD',
        klasse: 'BTE_Bericht'
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440002',
        klantnaam: 'MHS - Douane Export',
        domeinKeten: 'Douane',
        oplosgroep: 'BB ECM Douane',
        contacten: [
          { naam: 'Douane Beheer', email: 'douane-ecm@belastingdienst.nl', telefoon: '0800-0545' }
        ],
        objectStore: 'OS_BTE_ACC',
        klasse: 'BTE_Export'
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440003',
        klantnaam: 'ECM - Toeslagen',
        domeinKeten: 'Toeslagen',
        oplosgroep: 'BB ECM Toeslagen',
        contacten: [
          { naam: 'Postbus Toeslagen', email: 'toeslagen-ecm@belastingdienst.nl', telefoon: '0800-0546' },
          { naam: 'Petra Jansen', email: 'p.jansen@belastingdienst.nl', telefoon: '06-98765432' }
        ],
        objectStore: 'OS_BTE_PROD',
        klasse: 'BTE_Toeslag'
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440004',
        klantnaam: 'Perron - Vennootschapsbelasting',
        domeinKeten: 'Vennootschapsbelasting',
        oplosgroep: 'BB ECM Perron',
        contacten: [
          { naam: 'VPB Beheer', email: 'vpb-ecm@belastingdienst.nl', telefoon: '0800-0547' }
        ],
        objectStore: 'OS_BTE_TEST',
        klasse: 'BTE_VPB'
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440005',
        klantnaam: 'MHS - Accijns',
        domeinKeten: 'Accijns',
        oplosgroep: 'BB ECM Douane',
        contacten: [
          { naam: 'Accijns Postbus', email: 'accijns-ecm@belastingdienst.nl', telefoon: '0800-0548' },
          { naam: 'Karel Smit', email: 'k.smit@belastingdienst.nl', telefoon: '06-11223344' },
          { naam: 'Lisa van Dijk', email: 'l.vandijk@belastingdienst.nl', telefoon: '06-55667788' }
        ],
        objectStore: 'OS_BTE_ONT',
        klasse: 'BTE_Accijns'
      },
      {
        id: '550e8400-e29b-41d4-a716-446655440006',
        klantnaam: 'COS - Motorrijtuigenbelasting',
        domeinKeten: 'Motorrijtuigenbelasting',
        oplosgroep: 'BB ECM Support',
        contacten: [
          { naam: 'MRB Beheer', email: 'mrb-ecm@belastingdienst.nl', telefoon: '0800-0549' }
        ],
        objectStore: 'OS_BTE_PROD',
        klasse: 'BTE_MRB'
      }
    ];
  }
}
