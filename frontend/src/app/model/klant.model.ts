export interface ContactPersoon {
  naam: string;
  email: string;
  telefoon: string;
}

export interface Klant {
  id: string;
  klantnaam: string;
  domeinKeten: string;
  oplosgroep: string;
  contacten: ContactPersoon[];
  objectStore: string;
  klasse: string;
  configuratieYaml?: string;
  conversieBestand?: string;
}
