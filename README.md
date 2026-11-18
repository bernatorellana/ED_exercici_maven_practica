# Pràctica de Maven: el projecte `meteo`

En aquesta pràctica aplicareu tot el que heu vist al document *Introducció a Maven* a un projecte nou, més gran i amb més llibreries. Haureu de repetir els mateixos passos que vam fer amb `generador-pdf`, però aquesta vegada sense la recepta escrita: haureu de trobar vosaltres mateixos quines dependències calen i com configurar-ho tot.

Aquest projecte està fet perquè **passin coses**: algunes llibreries no es comporten com esperaríeu. Quan alguna cosa falli, no busqueu directament la solució: llegiu el missatge d'error, feu servir `mvn dependency:tree` i investigueu. Saber diagnosticar aquests problemes és tan important com saber escriure el POM.

> Recordeu: a PowerShell, poseu sempre els paràmetres `-D` entre cometes: `"-Dnom=valor"`.

---

## Índex

1. [El programa](#1-el-programa)
2. [Crear el projecte](#2-crear-el-projecte)
3. [Afegir les dependències](#3-afegir-les-dependències)
4. [Tests i arbre de dependències](#4-tests-i-arbre-de-dependències)
5. [Executar el programa](#5-executar-el-programa)
6. [Empaquetar amb una carpeta `lib`](#6-empaquetar-amb-una-carpeta-lib)
7. [Empaquetar en un *fat jar*](#7-empaquetar-en-un-fat-jar)
8. [Partir el projecte en dos mòduls](#8-partir-el-projecte-en-dos-mòduls)
9. [Què cal lliurar](#9-què-cal-lliurar)

---

## 1. El programa

`meteo` descarrega les temperatures dels últims dies d'una població qualsevol, en calcula estadístiques i en mostra un gràfic:

```
java -jar meteo.jar [població] [dies]        (per defecte: Barcelona, 90)
```

1. Busca les coordenades de la població amb l'API de geocodificació d'[Open-Meteo](https://open-meteo.com/).
2. Descarrega les temperatures màximes i mínimes i la precipitació de cada dia, amb l'API d'històric d'Open-Meteo. Les dues APIs són públiques i gratuïtes, i no cal registrar-se.
3. Calcula estadístiques: mitjanes, mediana, desviació, extrems, dies de pluja i la **tendència** (una regressió lineal sobre la temperatura mitjana).
4. Dibuixa un gràfic amb les màximes, les mínimes i la recta de tendència, el desa en un fitxer PNG i el mostra en una finestra. A sota del gràfic, una capsa de text mostra els mateixos missatges que surten per la consola.

### Les fonts

Teniu les fonts a la carpeta `src`, ja organitzades amb l'estructura de Maven:

```
src/
├── main/
│   ├── java/edu/meteo/
│   │   ├── dades/
│   │   │   ├── Ubicacio.java              ← record: població i coordenades
│   │   │   ├── MesuraDiaria.java          ← record: dades d'un dia
│   │   │   ├── ParserOpenMeteo.java       ← JSON → objectes Java
│   │   │   └── ClientOpenMeteo.java       ← peticions HTTP a l'API
│   │   ├── estadistica/
│   │   │   ├── ResumEstadistic.java       ← record: resultats
│   │   │   └── AnalitzadorTemperatures.java
│   │   └── app/
│   │       ├── Main.java
│   │       ├── GraficTemperatures.java    ← construeix el gràfic
│   │       ├── FinestraMeteo.java         ← finestra Swing
│   │       └── AppenderAreaText.java      ← envia el log a la finestra
│   └── resources/
│       └── logback.xml                    ← configuració del log
└── test/
    ├── java/edu/meteo/
    │   ├── dades/ParserOpenMeteoTest.java
    │   ├── dades/ClientOpenMeteoTest.java
    │   ├── estadistica/AnalitzadorTemperaturesTest.java
    │   └── app/GraficTemperaturesTest.java
    └── resources/
        ├── historic-barcelona.json        ← resposta real de l'API (90 dies)
        └── geocodificacio-girona.json     ← resposta real de l'API
```

Llegiu el codi abans de començar. Fixeu-vos especialment en els `import` de cada classe: us diran quines llibreries calen.

### Les llibreries

| Funció | Llibreria | Quines classes la fan servir |
|---|---|---|
| Peticions HTTP | **OkHttp** 5.x (okhttp-jvm)| `ClientOpenMeteo` |
| JSON | **Jackson** 2.x (`jackson-databind`) | `ParserOpenMeteo` |
| Dates de Java a Jackson | **Jackson** — mòdul `jsr310` 2.x | `ParserOpenMeteo` |
| Estadística | **Apache Commons Math** 3.x | `AnalitzadorTemperatures` |
| Gràfics | **JFreeChart** 1.5.x | `GraficTemperatures`, `FinestraMeteo` |
| Log (API) | **SLF4J** 2.x (`slf4j-api`) | gairebé totes |
| Log (implementació) | **Logback** (`logback-classic`) 1.6.x | `AppenderAreaText`, `logback.xml` |
| Tests | **JUnit** 5 | tots els tests |
| Servidor HTTP fals per als tests | **MockWebServer** 5.x (`mockwebserver3`) | `ClientOpenMeteoTest` |

> ⚠️ **Compte amb les versions majors.** El codi està escrit per a **Jackson 2.x**. A mvnrepository també hi trobareu Jackson 3.x, que ha canviat de `groupId` (`tools.jackson...` en lloc de `com.fasterxml.jackson...`) i de paquets: no és compatible amb el nostre codi. El mateix passa amb Commons Math: el codi fa servir el paquet `org.apache.commons.math3`.

### SLF4J i Logback: API i implementació

El codi escriu els missatges de log amb **SLF4J** (`LoggerFactory.getLogger(...)`, `log.info(...)`). SLF4J és només una **API**, una "façana". Qui realment escriu els missatges (a la consola, a un fitxer, a la finestra...) és una **implementació**, en el nostre cas **Logback**, configurada al fitxer `logback.xml`.

Aquesta separació permet que una llibreria escrigui missatges de log sense imposar a qui la fa servir quina implementació ha de fer servir. Ho aprofitarem a l'apartat 8.

---

## 2. Crear el projecte

Creeu un projecte Maven amb l'arquetip `maven-archetype-quickstart`, com a la secció 2 del document *Introducció a Maven*:

- `groupId`: `edu.meteo`
- `artifactId`: `meteo`

Esborreu les classes d'exemple de l'arquetip (`App.java` i `AppTest.java`) i copieu-hi el contingut de la carpeta `src` que us donem, respectant on va cada cosa.

**Abans de continuar**, responeu:

1. On ha d'anar `logback.xml`? I els fitxers `.json`? Per què uns van a `main` i els altres a `test`?
2. Quina versió de Java fa servir el projecte? Hi ha alguna classe que no compilaria amb Java 11? (Pista: fixeu-vos en com estan declarats `Ubicacio` i `MesuraDiaria`, i en el JSON de `ParserOpenMeteoTest`.)

---

## 3. Afegir les dependències

▶
```
mvn compile
```

Fallarà, és clar. Aneu afegint les dependències al POM, buscant-les a <https://mvnrepository.com/>, fins que compili. Recomanació: **afegiu-les d'una en una** i torneu a compilar cada vegada, així veureu com van desapareixent els errors.

Recordeu triar bé el **scope** de cada dependència (vegeu la secció 7.5 del document *Introducció a Maven*).

> 🔍 **Si alguna cosa no quadra.** Pot passar que afegiu una dependència, Maven la baixi sense cap error, i el compilador continuï dient que el paquet no existeix. Si us passa:
> - Mireu el jar que ha baixat Maven al repositori local (`C:\Users\<usuari>\.m2\repository\...`). Quant ocupa? Obriu-lo amb un programa de zip: què conté?
> - Executeu `mvn dependency:tree`. Hi ha alguna dependència amb un nom semblant?
> - Mireu la pàgina de la llibreria a mvnrepository: quins altres artefactes hi ha amb noms semblants?
>
> Expliqueu a la memòria què ha passat i per què.

Quan compili, responeu:

3. Quantes dependències heu declarat al POM? Quines tenen scope `test`?
4. `ParserOpenMeteo` fa servir la classe `JavaTimeModule`. Què passaria si traguéssiu la línia `.registerModule(new JavaTimeModule())`? Proveu-ho i executeu els tests. Copieu el missatge d'error i expliqueu-lo. Després deixeu-ho com estava.

---

## 4. Tests i arbre de dependències

▶
```
mvn clean test
```

Han de passar **13 tests**.

5. Els tests de `ClientOpenMeteoTest` comproven les peticions HTTP. Necessiten connexió a Internet? Com ho fan? (Llegiu el codi del test.)
6. Per què creieu que els tests fan servir respostes JSON desades en fitxers, en lloc de connectar-se a l'API real? Doneu almenys dos motius.

▶
```
mvn dependency:tree
```

7. Quants jars calen per **executar** el programa (sense comptar els de test)? Quantes dependències heu declarat vosaltres?
8. Hi ha alguna dependència que us sorprengui? Hi ha una llibreria d'un **altre llenguatge de programació**: quina és, qui la porta i per què?
9. `jackson-databind` porta dues dependències transitives. Quines són?

---

## 5. Executar el programa

Primer, comproveu que no podeu executar el jar que genera `mvn package` directament (`java -jar ...`), i expliqueu per què (ja ho sabeu del primer exercici).

Executeu-lo amb el plugin Exec. Configureu el POM perquè n'hi hagi prou amb `mvn compile exec:java`, i passeu-li arguments:

▶
```
mvn compile exec:java "-Dexec.args=Girona 60"
```

S'ha d'obrir una finestra amb el gràfic i els missatges, i s'ha de crear un fitxer `temperatures-girona.png` a la carpeta des d'on l'executeu.

10. Proveu el programa amb una població que no existeixi. Què passa? On es tracta aquest error al codi?
11. Mireu `logback.xml`. Quants *appenders* hi ha configurats i on escriu cadascun? Quin és el nostre?
12. Canvieu el nivell del log a `WARN` i executeu el programa: què es veu ara a la consola i a la finestra? Després canvieu-lo a `DEBUG`: quin missatge nou apareix, i de quina classe? Surt algun missatge d'OkHttp, de Jackson o de JFreeChart? Per què creieu que no? (Pista: no totes les llibreries fan servir SLF4J per escriure el seu log.) Deixeu-lo a `INFO`.

> **Possibles problemes:**
> - **Error 429**. L'API d'Open-Meteo és gratuïta, però limita el nombre de peticions per dia des d'una mateixa adreça IP. Amb un ús normal no hi arribareu, però no feu bucles que la cridin moltes vegades seguides.
> - **Accents estranys a la consola de Windows**. Si a la finestra del programa els accents es veuen bé però a la consola no, el problema és la codificació de la consola de Windows, no el programa.

---

## 6. Empaquetar amb una carpeta `lib`

Configureu el projecte perquè `mvn package` generi un jar amb `Main-Class` i `Class-Path` al manifest, i copiï les dependències a `target\lib`, com a la secció 10 del document *Introducció a Maven*.

▶
```
mvn clean package
java -jar target\meteo-1.0-SNAPSHOT.jar Tarragona 30
```

13. Quants jars hi ha a `target\lib`? Coincideix amb la resposta de la pregunta 7? Hi ha cap jar de test?
14. Quant ocupa la carpeta `lib` en total? Quina és la llibreria que ocupa més? I la segona?

---

## 7. Empaquetar en un *fat jar*

Afegiu el `maven-shade-plugin` perquè generi també un fat jar amb el sufix `-all`, com a la secció 11 del document *Introducció a Maven*.

▶
```
mvn clean package
java -jar target\meteo-1.0-SNAPSHOT-all.jar Lleida
```

15. Quant ocupa el fat jar? I el jar normal? Per què tanta diferència?
16. El plugin mostra diversos avisos. Classifiqueu-los: quins fitxers es repeteixen entre llibreries? Algun d'aquests avisos afecta el funcionament del programa?
17. Els avisos parlen de `module-info.class`. Què és aquest fitxer? Per què un fat jar "trenca" el que representa?
18. **Experiment amb el `ServicesResourceTransformer`.** Llisteu els fitxers de `META-INF/services` del fat jar (`jar tf target\meteo-1.0-SNAPSHOT-all.jar | findstr services`). De quines llibreries venen? Ara traieu el `ServicesResourceTransformer` del POM, torneu a empaquetar i executeu el fat jar. Funciona? Per què, en aquest projecte concret, no passa res? En quin cas sí que fallaria? Torneu a posar el transformer.

---

## 8. Partir el projecte en dos mòduls

Ara convertirem `meteo` en un projecte **multimòdul** amb dues parts, com a la secció 12.2 del document *Introducció a Maven*:

- **`meteo-dades`**: tot el que és **descarregar i processar** dades. És una llibreria: no té `main`, ni finestres, ni gràfics.
- **`meteo-app`**: la **interfície**: el gràfic, la finestra i el `Main`. Fa servir `meteo-dades`.

Partir un projecte no és només moure carpetes: cal decidir **on va cada classe, cada dependència, cada test i cada recurs**. Feu aquesta anàlisi **abans** de tocar res.

### 8.1 On va cada classe

El criteri principal és la **direcció de les dependències**: el mòdul de dades no pot dependre de la interfície, perquè llavors no seria reutilitzable (per exemple, des d'una aplicació web o des d'una línia de comandes sense gràfics). En canvi, la interfície sí que pot dependre de les dades.

Mireu els `import` de cada classe i dibuixeu un graf de dependències **entre els nostres paquets** (`dades`, `estadistica`, `app`). Hauríeu d'obtenir això:

```
          app
         /   \
        ▼     ▼
 estadistica ──► dades
```

- `app` fa servir `dades` (`Ubicacio`, `MesuraDiaria`, `ClientOpenMeteo`) i `estadistica` (`ResumEstadistic`...).
- `estadistica` fa servir `dades` (`MesuraDiaria`).
- `dades` no fa servir cap altre paquet nostre.

Per tant, el repartiment és:

| Mòdul | Paquets | Classes |
|---|---|---|
| `meteo-dades` | `edu.meteo.dades`, `edu.meteo.estadistica` | `Ubicacio`, `MesuraDiaria`, `ParserOpenMeteo`, `ClientOpenMeteo`, `ResumEstadistic`, `AnalitzadorTemperatures` |
| `meteo-app` | `edu.meteo.app` | `Main`, `GraficTemperatures`, `FinestraMeteo`, `AppenderAreaText` |

Fixeu-vos que:

- **Els *records* del model** (`Ubicacio`, `MesuraDiaria`, `ResumEstadistic`) els fan servir tots dos mòduls. Per això han d'anar al mòdul **de baix** (`meteo-dades`). Si anessin a `meteo-app`, `meteo-dades` necessitaria `meteo-app`, i `meteo-app` necessitaria `meteo-dades`: una **dependència circular**, que Maven no permet.
- **`GraficTemperatures`** no fa cap descàrrega ni cap càlcul, però podríem dubtar si és "processar dades". Va a `meteo-app` perquè depèn de JFreeChart: si fos a `meteo-dades`, qualsevol projecte que volgués només les dades s'emportaria una llibreria de gràfics de 1,6 MB.

19. Es podria haver fet un tercer mòdul `meteo-estadistica`? Com quedaria el graf de dependències? Quins avantatges i inconvenients tindria?

### 8.2 On va cada dependència

Cada dependència va al mòdul **on hi ha les classes que la fan servir**:

| Dependència | Mòdul | Scope | Per què |
|---|---|---|---|
| OkHttp | `meteo-dades` | compile | `ClientOpenMeteo` |
| Jackson (`databind` i `jsr310`) | `meteo-dades` | compile | `ParserOpenMeteo` |
| Commons Math | `meteo-dades` | compile | `AnalitzadorTemperatures` |
| SLF4J (`slf4j-api`) | `meteo-dades` | compile | Les classes de dades escriuen missatges de log |
| MockWebServer | `meteo-dades` | test | `ClientOpenMeteoTest` |
| `meteo-dades` | `meteo-app` | compile | Les classes de la interfície fan servir les de dades |
| JFreeChart | `meteo-app` | compile | `GraficTemperatures`, `FinestraMeteo` |
| Logback (`logback-classic`) | `meteo-app` | compile | `AppenderAreaText` i `logback.xml` |
| JUnit | tots dos | test | Al POM pare, a `<dependencies>`, perquè l'heretin tots dos |

La fila més interessant és la del log. **Una llibreria (`meteo-dades`) només ha de dependre de l'API de log (`slf4j-api`), mai d'una implementació.** És l'aplicació final (`meteo-app`) qui decideix com i on s'escriuen els missatges. Si `meteo-dades` portés Logback, qualsevol aplicació que la fes servir s'hauria d'empassar Logback encara que en volgués fer servir una altra.

`meteo-app` **no declara** OkHttp, Jackson ni Commons Math: li arriben de manera **transitiva** a través de `meteo-dades`.

Totes les **versions** van al `dependencyManagement` del POM pare, i els fills declaren les dependències **sense versió**.

### 8.3 On va cada test i cada recurs

Els tests i els recursos segueixen les classes que proven o que els fan servir:

| Fitxer | Mòdul |
|---|---|
| `ParserOpenMeteoTest`, `ClientOpenMeteoTest`, `AnalitzadorTemperaturesTest` | `meteo-dades` |
| `historic-barcelona.json`, `geocodificacio-girona.json` (`src/test/resources`) | `meteo-dades` |
| `GraficTemperaturesTest` | `meteo-app` |
| `logback.xml` (`src/main/resources`) | `meteo-app` |

`logback.xml` va a `meteo-app` per la mateixa raó que Logback: la configuració del log és una decisió de l'aplicació, no de la llibreria.

### 8.4 L'estructura final

```
meteo/
├── pom.xml                          ← POM pare: <modules>, versions, JUnit, pluginManagement
├── meteo-dades/
│   ├── pom.xml
│   └── src/
│       ├── main/java/edu/meteo/dades/...
│       ├── main/java/edu/meteo/estadistica/...
│       ├── test/java/edu/meteo/dades/...
│       ├── test/java/edu/meteo/estadistica/...
│       └── test/resources/*.json
└── meteo-app/
    ├── pom.xml                      ← plugins: exec, jar, dependency, shade
    └── src/
        ├── main/java/edu/meteo/app/...
        ├── main/resources/logback.xml
        └── test/java/edu/meteo/app/...
```

Els plugins d'empaquetat (`lib` i fat jar) i la propietat `exec.mainClass` van només a **`meteo-app`**: és l'únic mòdul que s'executa. `meteo-dades` genera un jar normal, com qualsevol llibreria.

### 8.5 Fer-ho

1. Creeu la carpeta `meteo` amb el POM pare, i les carpetes dels dos mòduls amb els seus POM. Podeu partir dels POM del projecte d'un sol mòdul.
2. Moveu cada fitxer al seu mòdul, segons les taules anteriors.
3. Construïu-ho tot des de la carpeta `meteo`:

▶
```
mvn clean package
```

Comproveu l'ordre en què el *reactor* construeix els mòduls, i que passen **12 tests** a `meteo-dades` i **1** a `meteo-app`.

4. Executeu el programa:

▶
```
java -jar meteo-app\target\meteo-app-1.0-SNAPSHOT-all.jar Girona
```

> 🔍 **Una altra cosa que passa.** En passar els tests de `meteo-dades` apareixerà aquest avís:
> ```
> SLF4J(W): No SLF4J providers were found.
> SLF4J(W): Defaulting to no-operation (NOP) logger implementation
> ```
> No és cap error, i els tests passen. Però en el projecte d'un sol mòdul no sortia.

20. Per què surt aquest avís ara? Què vol dir "NOP logger"? Es veuen els missatges de log de `ClientOpenMeteo` mentre passen els tests?
21. Arregleu-ho **sense** afegir Logback com a dependència normal de `meteo-dades` (recordeu l'apartat 8.2). Quin scope heu fet servir? Aquesta dependència arriba a `meteo-app` de manera transitiva? Comproveu-ho amb `mvn dependency:tree`.

### 8.6 Comprovacions i preguntes finals

22. Executeu `mvn dependency:tree` des de l'arrel. Compareu l'arbre de `meteo-app` amb el del projecte d'un sol mòdul. Quines dependències declara directament `meteo-app` i quines li arriben a través de `meteo-dades`?
23. Afegiu temporalment a `Main.java` una línia que faci servir directament Jackson (per exemple, `new com.fasterxml.jackson.databind.ObjectMapper();`). Compila? Per què, si `meteo-app` no declara Jackson al seu POM? És una bona pràctica? Després traieu la línia.
24. *(Avançat)* Executeu `mvn dependency:analyze` des de l'arrel. Aquest goal compara les classes que fa servir realment el codi amb les dependències declarades. Interpreteu els avisos `Used undeclared dependencies` i `Unused declared dependencies` de cada mòdul. Té raó en tot? (Pista: Logback a `meteo-dades` es fa servir, però no des del codi Java.)
25. Executeu el programa amb `exec:java`. Proveu-ho des de l'arrel i des de la carpeta `meteo-app`. Què passa en cada cas, i per què? (Vegeu la secció 12.2 del document *Introducció a Maven*.)

---

## 9. Què cal lliurar

- El projecte d'un sol mòdul `meteo` (apartats 2 a 7), **sense** la carpeta `target`.
- El projecte multimòdul (apartat 8), **sense** les carpetes `target`.
- Un document amb les respostes a les preguntes, incloent-hi:
  - el problema que heu trobat a l'apartat 3 amb alguna dependència i com l'heu diagnosticat;
  - les captures de pantalla de la finestra del programa i de l'arbre de dependències del projecte multimòdul.
