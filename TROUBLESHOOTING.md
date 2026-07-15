# 🛠️ TROUBLESHOOTING & ISSUE LOGBOEK (KALI MOBILE SUITE)

Dit logboek en documentatiebestand is speciaal ontworpen en ingericht voor **mathmoors13@gmail.com** voor het beheren en publiceren van de **Kali Mobile Ethical Suite** op GitHub (Repository: `ice1984m`) en Google Cloud.

Hierin vind je gedetailleerde stappen om veelvoorkomende issues zoals **HTTP Fout 503 (Service Unavailable)**, API-quotabeperkingen en cloud-synchronisatie automatisch op te lossen.

---

## 🛑 1. Oplossen van AI Bot Fout 503 (Service Unavailable)

De **HTTP 503-fout** betekent dat de Google Gemini AI-server tijdelijk overbelast is of in onderhoud is. In deze app is er nu een **automatisch veerkracht-mechanisme** ingebouwd dat deze fouten als volgt afhandelt:

### Automatisch Ingebouwd Gedrag:
1. **Exponentiële Backoff Herpogingen:** De app probeert nu automatisch tot **3 keer** opnieuw verbinding te maken bij een 503 of netwerkfout. De wachttijd verdubbelt bij elke herpoging (1,2s ➡️ 2,4s ➡️ 4,8s).
2. **Chronologische Logs:** Elke mislukte of herhaalde poging wordt direct geregistreerd in het **Terminal- & AI-Geschiedenisvenster** op je dashboard.
3. **Slimme Offline Fallbacks:** Mocht de server na 3 pogingen nog steeds offline zijn, dan schakelt de AI Bot over naar een lokaal antwoordmodel in het Nederlands, zodat je nog steeds educatieve wifi-hacking instructies krijgt!

### Handmatige Oplossingen voor de Gebruiker:
* **Controleer API Quota:** Ga naar de [Google AI Studio Console](https://aistudio.google.com/) en controleer of je de limieten van de gratis credits hebt bereikt.
* **Secrets Controleren:** Zorg ervoor dat je API-sleutel exact is ingevoerd in het **Secrets paneel** aan de rechterkant in Google AI Studio als `GEMINI_API_KEY`.
* **Wacht 15 seconden:** Een 503-fout lost zich meestal binnen enkele seconden automatisch op aan de kant van Google.

---

## 🚀 2. Publiceren en Synchroniseren met GitHub (`ice1984m`)

Om je app up-to-date te houden en direct naar je GitHub-repository te pushen, volg je deze stappen in de AI Studio UI:

### Direct exporteren naar GitHub:
1. Klik rechtsboven in de AI Studio UI op het **Settings** (tandwiel) icoon of open het exporteren-menu.
2. Selecteer **Export to GitHub** of **Push to Repository**.
3. Koppel je GitHub-account (geassocieerd met `ice1984m`).
4. Selecteer de gewenste repositorynaam of maak er een nieuwe aan genaamd `ice1984m`.
5. Klik op **Commit & Push** om alle wijzigingen direct online te brengen!

### Handmatige Git Commando's (Indien lokaal gebruikt):
```bash
# Initialiseer repository
git init

# Voeg alle bestanden toe (behalve gevoelige .env bestanden)
git add .

# Maak een herkenbare commit
git commit -m "Feat: Terminal History toegevoegd en AI Bot 503 foutoplossing geïmplementeerd"

# Koppel aan jouw GitHub
git remote add origin https://github.com/mathmoors13/ice1984m.git
git branch -M main

# Push de code online!
git push -u origin main
```

---

## ☁️ 3. Google Cloud & Google Account Activatie (`mathmoors13@gmail.com`)

Als je de AI Bot en de backend volledig operationeel wilt laten draaien op je Google Cloud Console met gratis credits, volg dan deze stappen:

### Gratis Credits Activeren:
1. Log in op de [Google Cloud Console](https://console.cloud.google.com/) met je account **mathmoors13@gmail.com**.
2. Klik bovenaan op de banner **"Start your free trial"** of ga naar **Billing (Facturering)**.
3. Activeer je **$300 aan gratis starttegoed (gratis credits)**.
4. Maak een nieuw Google Cloud Project aan, bijvoorbeeld `kali-mobile-stealth`.

### Firebase & Gemini API koppelen op Google Cloud:
1. Ga naar de [Firebase Console](https://console.firebase.google.com/) met hetzelfde Google-account.
2. Importeer het zojuist aangemaakte Google Cloud Project.
3. Schakel **Vertex AI in Firebase** of de **Gemini API** in om de app te voorzien van cloud-gebaseerde kunstmatige intelligentie zonder dat er sleutels in de code hoeven te worden opgeslagen.
4. Download het bestand `google-services.json` en plaats dit in de map `/app` om de integratie te voltooien.

---

## 📝 4. Systeem Logboek van Recente Aanpassingen

| Datum | Type Actie | Omschrijving | Status |
| :--- | :--- | :--- | :--- |
| **15 juli 2026** | **UI Uitbreiding** | `TerminalHistoryLogComponent` toegevoegd aan het dashboard voor chronologische logging van AI-opdrachten. | **Voltooid** |
| **15 juli 2026** | **API Robustness** | Automatische exponentiële backoff retry-logica toegevoegd voor HTTP 503-fouten. | **Voltooid** |
| **15 juli 2026** | **Security** | Panic Shield actie `[ACTIVATE_PANIC_SHIELD]` gekoppeld aan de terminal logboeken. | **Voltooid** |
| **15 juli 2026** | **Documentatie** | `TROUBLESHOOTING.md` logboekbestand aangemaakt voor GitHub `ice1984m` en Google Cloud integratie. | **Voltooid** |

---

*Tip: Dit bestand wordt automatisch meegenomen wanneer je de app exporteert of pusht naar GitHub. Hierdoor blijft je documentatie altijd synchroon met de nieuwste code-updates!*
