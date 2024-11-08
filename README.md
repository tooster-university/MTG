# TL;DR

This was supposed to be an MVP backend and CLI frontend for extenible MTG game engine. It didn't work out. It became a chat server and client with command line argument parser. It can handle local and server commands though and has some interesting annotation-based command declaration system.

# Projekt
### by Maksymilian Polarczyk

## Ogólnie:

Projekt to głównie aplikacja klient-serwer będąca frameworkiem na przyszłe projekty silników do gier, wraz z przykładowym silnikiem gier który można postawić na takim serwerze.

### opis struktury projektu:

  * pakiet `client` zawiera definicja klas odpowiedzialnych za część klienta. 
  * pakiet `server` to kod serwera, zarząjący połączeniami, przychodzącymi klientami, czatem i hubami.
  * pakiet `common` to most łączący pakiety `client` i `server` - zawiera klasy wykorzystywane przez obie strony m. in. Formatter, maszynę stanów i parser komend. W podkatalogu `proto` znajdują się wygenerowane klasy wiadomości przesyłanych między klientem i serwerem
  * pakier `MTG` zawiera definicje klas odpowiedzialne za silnik do gry Magic The Gathering. Jest on zaimplementowany aktualnie tylko częściowo.

### Wzorce i poglądowe opisy klas:

  * Client-Server pattern: klasy `Server`, `User`, `Client`. `Client` łączy się do `Server`a, serwer oczekuje na nadchodzace połączenia i tworzy osobne wątki `User` nasłuchujące na połączeniu klient-serwer. Klient i Serwer mają zaimplementowany protokół 4-way-handshake bez uwierzytelniania, gdzie wysyłają sobie na początku dane konfiguracyjne. Aplikacja potrafi wykryć zerwanie połączenia, zawieszonego klienta, nieodpowiadający serwer i inne problemy łącza. Dokładniejszy opis protokołu komunikacyjnego jest wewnątrz pliku `Messages.proto`
  * Factory pattern: wewnątrz `Deck.java` oraz `Card.java`, Tworzone są i inicjalizowane obiekty talii i kart, na podstawie odpowiednio klas `DeckModel` oraz `CardModel`.
  * Strategy pattern: wszystkie pliki `*.yml *.yaml` są wykorzystywane do parametryzacji serwera. w Pliku `config.yml` znajdują się ogólne ustawienia silnika MTG, w plikach z katalagu `resources/decks/*.yml` znajdują się modele talii kart a w `resources/cards/*.yml` definicje kart. Aktualnie została zaimplementowana tylko częściowa obsługa parametryzacji kart 
  * Mixins: Dzięki domyślnych metodach w interfejsach w Javie 8 udało się zaimplementować wzorzec Mixin w klasach dziedziczących po `Command` oraz `State`. 
  * Finite State Machine: Klasa `FIniteStateMachine` jest abstrakcyjną klasą reprezentującą automat skończony. Wewnętrzny interfejs `State` reprezentuje pojedynczy stan maszyny stanów. Jest to interfejs, a nie klasa abstrakcyjna, aby można było wykorzystać ją jako mixin dla stanów implementowanych na enumach. Dlaczego tak? A ponieważ enumy javove są nierozszerzalne, tzn. nie można dziedziczyć po enumie. Mixiny pozwalają na dodawanie funkcjonalności do zmiennych enumów. Dodatkowo wykorzystuję kilka hacków języka aby tworzyć proste systemy kontroli przepływu dla maszyny stanów - klasa `AbortTransition` jest lekkim obiektem dziedziczącym po `Throwable` i pozwala przerwać przejście między stanami w dowolnym momencie.
  * ResourceManager: Klasa `ResourceManager` jest lekko powiązana z klasami `DeckModel` i `CardModel`. Zarządza ona ładowaniem i doładowywaniem modeli w trakcie działania programu.
  * Parser: klasa `CommandController` przyjmuje definicję klasy implementującej `Command` i parsuje linię wejścia użytkownika do formatu interpretowalnego przez serwer.
  
Dużo klas zostało zrobionych jako klasy wewnętrzne. Taka decyzja została podięta po to, aby łatwiej było rozróżnić które elementy należą do jakiej mechaniki.

### Co i jak:

Odpalamy server (można podać port na którym będzie słuchał) oraz klientów (można podać IP i port, domyślnie localhost i port 62442). Następnie mamy pełną kontrolę nad serwerem za pomocy udostępnionego interfejsu do poleceń - `ServerCommand` definiuje wszystkie komendy. W dowolnym momencie mamy możliwość wywołania `/help` aby otrzymać pomoc odn. dostępnych komend.

