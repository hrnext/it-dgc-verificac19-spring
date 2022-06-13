<h1 align="center">Java Spring Digital Green Certificate SDK</h1>        

[![Java CI with Maven](https://github.com/hrnext/it-dgc-verificac19-spring/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/hrnext/it-dgc-verificac19-spring/actions/workflows/maven.yml)

# Indice
- [Contesto](#contesto)
- [Installazione](#installazione)
- [Aggiornamenti](#aggiornamenti)
- [Uso](#uso)
- [Licenza](#licenza)
  - [Dettaglio licenza](#dettaglio-licenza)

# Contesto
**Attenzione, questo repository è derivato dalle specifiche presenti in <a href="https://github.com/ministero-salute/it-dgc-verificac19-sdk-android/">ministero-salute/it-dgc-verificac19-sdk-android</a>!**

**L'elenco le librerie utilizzabili è presente in questa <a href="https://github.com/ministero-salute/it-dgc-verificac19-sdk-onboarding#lista-librerie">lista</a>. La pagina contiene anche informazioni sulle policy di accettazione e rimozione dalla lista stessa. Fate riferimento ad essa prima di utilizzo in ambienti di produzione.**

Questo repository contiene un Software Development Kit (SDK), che consente di integrare nei sistemi
le funzionalit&agrave; di verifica della Certificazione verde COVID-19, mediante
la lettura del QR code.

# Trattamento dati personali
Il trattamento dei dati personali svolto dalle soluzioni applicative sviluppate
a partire dalla presente SDK deve essere effettuato limitatamente alle
informazioni pertinenti e alle operazioni strettamente necessarie alla verifica
della validit&agrave; delle Certificazioni verdi COVID-19. Inoltre &egrave; fatto esplicito
divieto di conservare il codice a barre bidimensionale (QR code) delle
Certificazioni verdi COVID-19 sottoposte a verifica, nonché di estrarre,
consultare, registrare o comunque trattare per finalit&agrave; ulteriori rispetto
a quelle previste per la verifica della Certificazione verde COVID-19 o le
informazioni rilevate dalla lettura dei QR code e le informazioni fornite in
esito ai controlli, come indicato nel DPCM 12 ottobre 2021    
 
# Installazione

Il codice sorgente è suddiviso nei due seguenti moduli maven.
1. **it-dgc-verificac19-spring-core** è una libreria JAR che fornisce un supporto alle web application Spring che hanno la necessità di integrare nei sistemi le funzionalità di verifica della Certificazione verde COVID-19
2. **it-dgc-verificac19-spring-rest-api** è una applicazione Spring Boot che funge da POC dell'estensione Java Spring.

###   

# Aggiornamenti
L'aggiornamento dei dati: regole di validazione, certificati di firma e DGC revocati avviene mediante un cron schedulato quotidianamente

```
  @Retryable(maxAttempts = Integer.MAX_VALUE)
  @Scheduled(cron = "@daily")
  public void doWork() {
    LOG.info("key fetching start");
    boolean res = verifierRepository.syncData();
    LOG.info("key fetching result: {}", res);
    if (!res) {
      throw new RuntimeException("Error on sync data, retry");
    }
  }
  
  @Override
  public boolean syncData() {
    try {
      if (fetchValidationRules() == false || fetchCertificates() == false) {
        return false;
      }
    } catch (IOException | RuntimeException e) {
      LOG.error("Fetch validation rules / certificates error", e);
      return false;
    }

    if (preferences.isDrlSyncActive()) {
      getCRLStatus();
    }

    preferences.setDateLastFetch(LocalDateTime.now());
    return true;
  }
  
  In sintesi:
  //aggiorna le regole di validazione
  fetchValidationRules()
  
  //aggiorna lo status e la lista dei certificati
  fetchCertificates()
  
  //aggiorna le liste di revoca
  getCRLStatus()
  
  ```
La verifica della DRL viene implementata come uno step opzionale del processo ed è guidato da un apposito configuration item propagato tramite la Piattaforma Nazionale.
Per ottimizzare il traffico di rete, le DRL non vengono trasferite sottoforma di SNAPSHOT complete (ad eccezione del primo avvio), tipicamente sono rappresentate da DIFF partizionate in CHUNK.
Per garantire la corretta persistenza della DRL Revocation List sono stati introdotti controlli di congruenza per assicurare che l'esito della costruzione dell nuova DRL abbia avuto l'esito sperato.

# Uso

Esempio:  
 
```
	//Spring Injection
	@Autowired
	VerifierService verifierService;

	public void test(){
	  String qrCodeTxt = 'HC1:6BF.......';
	  ValidationScanMode validationScanMode = ValidationScanMode.CLASSIC_DGP;
	  CertificateSimple certificateSimple = verifierService.verify(qrCodeTxt, validationScanMode);
	}
```

Osservando la risposta del metodo &egrave; restituito un oggetto 
`it.dgc.verificac19.model.CertificateSimple` che contiene
il risultato della verifica.
Il data model contiene i dati relativi alla
persona, la data di nascita, il timestamp di verifica e lo stato della
verifica. 

Tipologia Base (NORMAL_DGP): l'sdk considera valide le certificazioni verdi generate da vaccinazione, da guarigione, da tampone.

Tipologia Rafforzata (SUPER_DGP): l'sdk considera valide solo le certificazioni verdi generate da vaccinazione o da guarigione.

Tipologia Booster (BOOSTER_DGP): l'sdk considera valide solo le certificazioni verdi rilasciate a seguito della somministrazione di una dose di richiamo (booster) e quelle rilasciate al completamento del ciclo vaccinale o guarigione, richiedendo per queste ultime una ulteriore validazione di un tampone (esito negativo di un test al SARS-CoV-2 eseguito nelle 48 ore precedenti).

Basandosi su questi dati &egrave; possibile disegnare la UI e fornire all'operatore lo
stato della verifica del DGC.
 
## Java 8 support

Selezionado il profilo Maven apposito, si abilita il supporto a Java 8

```
>mvn clean package -Pjava8
```

Per il supporto a Java 8 per le librerie:
<a href="https://github.com/DIGGSweden/dgc-java/blob/main/README.md#for-java-8-users">dgc-java</a>

# Licenza

## Dettaglio Licenza
La licenza per questo repository &egrave; una `Apache License 2.0`.
All'interno del file [LICENSE](./LICENSE) sono presenti le informazioni
specifiche.
