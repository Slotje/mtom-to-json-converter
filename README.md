# MTOM-to-JSON Converter - POC

Proof of Concept voor het BTE (Bulk Toevoer ECM) project: een applicatie die MTOM berichten omzet naar JSON met behulp van YAML mapping configuraties.

## Architectuur

- **Backend**: Quarkus 3.x (Java 17) met Jakarta Mail voor MTOM parsing
- **Frontend**: Angular 17 met visuele mapping interface
- **Validatie**: Drielaags (JSON Schema, Business Rules, Referentie-integriteit)

## Starten

### Backend
```bash
cd backend
./mvnw quarkus:dev
```
Backend draait op http://localhost:8080

### Frontend
```bash
cd frontend
npm install
ng serve
```
Frontend draait op http://localhost:4200

## Demo

De applicatie bevat 4 voorbeeldconfiguraties:
1. **Correcte Mapping** - Toont succesvolle MTOM-naar-JSON conversie
2. **Schema Validatie Fout** - Laat zien hoe Laag 1 ontbrekende velden en ongeldige types detecteert
3. **Business Rule Fout** - Laat zien hoe Laag 2 dubbele mappings en ongeldige formaten vindt
4. **Referentie-integriteit Fout** - Laat zien hoe Laag 3 type-incompatibiliteiten en ontbrekende XPath velden detecteert

## Features

- YAML configuratie upload met validatie
- Automatische velddetectie uit MTOM berichten (geen XPath kennis nodig)
- Visuele mapping van MTOM bronvelden naar JSON doelvelden
- Drielaagse validatie met foutmeldingen en suggesties
- JSON output met download en kopieer functionaliteit
