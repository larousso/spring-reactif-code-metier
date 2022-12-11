# Spring réactif et code métier: le mariage improbable 

Voici le projet d'exemple de la conférence "Code métier et spring reactif : le mariage improbable ": 

```
La programmation reactive est à la mode, on trouve des clients réactifs pour faire du http, accéder aux bases nosql et même aux bases de données relationnelles. Depuis springboot 2, les reactives streams sont même introduits avec spring reactor et spring webflux.

Mais le constat est toujours le même: le code devient très compliqué et ça devient donc difficile de gérer du métier. Comment empiler, transformer, combiner les erreurs fonctionnelles proprement dans un monde ou tout n'est que throwable ?

Dans cette session je vous présenterai un pattern inspiré de l'api de "zio" (présente dans le monde scala), qu'on a mis en place dans une application très "métier" afin d'avoir du code expressif et de gérer les erreurs de manière fine.

A la fin on obtiendra du code typesafe, robuste, lisible. Le code objet procédurale n'a qu'a bien se tenir !!!
```

## La gestion des erreurs en java 

En java, on utilise les exceptions pour gérer les erreurs. On peut distinguer 2 types d'exceptions : 
 * les runtimes exceptions 
 * les checked exceptions

Même si les checked exceptions sont mal aimées, elles ont un avantage, on les retrouve dans la signature de la méthode. 
On peut donc en un coup d'œil comprendre le comportement d'une méthode. 

Un exemple simple mais très parlant c'est le cas de la division : 

Imaginons une méthode `diviser` : 

```java
public Float diviser(Integer nombre, Integer diviser); 
```

Ici, il faut aller regarder l'implémentation pour comprendre ce qu'il se passe. 

Pour avoir une signature plus parlante, il faudrait avoir cette signature : 

```java
public Float diviser(Integer nombre, Integer diviser) throws DivisionParZeroException; 
```

Ou bien celle ci 

```java
public Float diviser(Integer nombre, NonZeroInteger diviser); 
```

Les checked exceptions ont donc l'avantage de donner des informations précieuses à la simple lecture de la signature des méthodes. 

Le problème, c'est que la manipulation des exceptions peut être extrêmement pénible. 

Il existe une 3e solution qui est l'utilisation des `Either` de la lib `vavr`. 
Un `Either` peut être soit `Left` soit `Right` et par convention, le côté gauche représente les erreurs et le côté droit les succès.  

On aura alors cette signature : 

```java
public Either<DivisionParZero, Float> diviser(Integer nombre, Integer diviser); 
```

Avec les `Either`, il devient plus facile de manipuler les erreurs. 
Alors que c'est difficile à faire avec des `try` `catch`, on pourra facilement transformer, combiner, collecter ... les erreurs avec les `Either`. 

Le problème, c'est qu'il faudra aussi manipuler les succès. 
L'utilisation d'`Either` devient intéressante quand on a un nombre important de règles à gérer avec beaucoup de cas d'erreur à manipuler.  


## La gestion des erreurs dans du code reactif

Dans le monde reactif, on perd complétement la notion de checked exception. 

```java
public Mono<Person> creerUnePersonne(PersonDto person);
```

`reactor` offre la possibilité de catcher et de manipuler les erreurs, 
mais on doit aller lire l'implémentation pour comprendre une méthode. 


## Gérer plus finement les erreurs avec reactor 

En s'inspirant du monde fonctionnel, on peut créer une structure `IO<E, A>` qui permet de gérer à la fois côté non bloquant et à la fois les erreurs de façon typées.

En scala, on retrouve ce genre de concept avec les `EitherT` (qui vient du monde haskell) ou dans la lib `ZIO`.  

`IO<E, A>` sera juste un wrapper de `Mono<Either<E, A>>` mais qui va exposer tout un tas de méthodes utilitaires pour pouvoir combiner, composer, transformer etc. 

On va ainsi gérer 2 types d'erreurs : 
* les erreurs métier : `E` qui seront visible dans la signature des méthodes
* les erreurs techniques : l'équivalent des runtimes exception qu'on ne va pas gérer et juste laisser remonter. 

Quand on expose une API, et qu'on utilise un `IO<E, A>` alors
* `A` sera une 200 
* `E` sera une 400 bad request
* les erreurs techniques seront des 500 


L'implémentation de notre IO se trouve ici [IO.java](src/main/java/io/IO.java). 

### Les IO dans la vraie vie 

On peut voir les IO comme une sorte de builder qui permet de décrire les étapes, et pour chaque étape comment gérer les erreurs ou les succès. 

Dans l'application de ce repo, on gère un service d'appel à l'aide avec des superheroes. 
En fonction d'un problème, on va essayer de trouver le superhero disponible qui est le plus à même de gérer le problème. Tout ça, en éliminant les superheroes pour qui la mission serait risquée.  

Il y a ici 3 domaines :
* abilities : permet de gérer les capacités des super heroes 
* superheroes : permet de gérer la liste des super heroes et leur disponibilité 
* weakness : permet de vérifier les faiblesses potentielles des super heroes 

#### Rentrons dans le code

Le service `SuperHeroes` expose cette api : 

```java
public IO<SuperheroError, Superhero> lookForSuperhero(String name);
```

Ou `SuperheroError` peut être de 2 types `SuperheroUnknown` ou `SuperheroUnavailable` : 

```java 
public sealed interface SuperheroError extends AppError {

    record SuperheroUnknown(String message, String name) implements SuperheroError {
        public SuperheroUnknown(String name) {
            this(MessageFormat.format("{0} is unknown", name), name);
        }
    }

    record SuperheroUnavailable(String message, String name) implements SuperheroError {
        public SuperheroUnavailable(String name) {
            this(MessageFormat.format("{0} is not available at the moment", name), name);
        }
    }
}
```

Ceci veut dire qu'on aura un `SuperHero` en résultat si tout se passe bien ou alors une erreur pour indiquer que
 * soit le superhero recherché n'existe pas 
 * soit le superhero recherché n'est pas disponible

Voici l'implémentation : 

```java
public IO<SuperheroError, Superhero> lookForSuperhero(String name) {
    return IO
        .<SuperheroError, Option<Superhero>>fromMono(superheroRepository.findByName(name)) // <1>
        .flatMap(mayBeSuperHero ->
            IO.fromOption(mayBeSuperHero, () -> new SuperheroUnknown(name)) // <2>
        )
        .filter(hero -> hero.isAvailable, () -> new SuperheroUnavailable(name)); // <3>
}
```

1. on part de `superheroRepository.findByName` qui retourne un `Mono<Option<Superhero>>`, va juste "caster" pour définir que les erreurs seront de type `SuperheroError`.
   * ici, si la base de données n'est pas dispo, on finit en "failure" avec une exception
2. `flatMap` permet de composer avec un fonction qui retourne un IO, dans notre cas une function `Option<Superhero> -> IO<SuperheroError, B>`. 
   * on crée un IO depuis un option
   * si l'option est vide on retourne une erreur de type `SuperheroUnknown`
3. on utilise filter pour enlever les superhero non disponible
   * un superhero non dispo se caractérise par une erreur de type `SuperheroUnavailable`


On va retrouver la même logique pour les 2 autres domaines. 


Pour les faiblesses, on veut juste checker si le superhero est vulnérable au problème à résoudre, dans ce cas seule l'erreur nous importe, on aura `Tuple0` en cas succès (un peu comme void mais instantiable) : 

```java
public IO<WeaknessesError, Tuple0> checkWeaknesses(Superhero superhero, Problem problem);
```

Pour les capacités, on va lister les capacités qui peuvent répondre au problème à résoudre 

```java
public  IO<AbilityUnmatch, List<Ability>> checkAbilities(Superhero superhero, Problem problem);
```

Passons maintenant au service final. Dans ce service, on veut
1. recherche un superhero. Si non trouvé, on s'arrête la, pas besoin d'aller plus loin. 
2. si on a trouvé le superhero, on veut vérifier : ses capacités et ses faiblesses 
   * à la fin, on veut retourner tous les problèmes en même temps en cas d'erreur 

On a donc 2 sortes de comportements à gérer : 
* le mode "coupe circuit" : en cas d'erreur, on s'arrête 
* le mode "collecte" : en cas d'erreur, on continue pour collecter les erreurs suivantes 

Ceci va se matérialiser par 
* `flatMap` : la composition qui va s'arrêter à la première erreur trouvée
* `parZip` (parallel zip) : qui permet de combiner des résultats 
  * `IO<List<E>, C> parZip(IO<E, A>, IO<E, B>, (A, B) -> C)` 
  * En cas d'erreur : le côté gauche est une `List<E>` 
  * En cas de succès : une fonction permet de combiner les résultats obtenu en un résultat final 

On pourrait aussi appeler `parZip` `combine` ou `product`. En fonctionnel, c'est le comportement qu'on retrouve sur les applicatives.   

Dans le code, ça donnera donc : 

```java
public IO<HelpErrors, HelpResult> findHelp(AskForHelp askForHelp) {
  return this.superHeroes.lookForSuperhero(askForHelp.name) // <1>
      .mapError(HelpErrors::fromSuperheroError) // <2>
      .flatMap(superhero ->
              IO.parZip( // <3> 
                      abilities.checkAbilities(superhero, askForHelp.problem).<AppError>downcast(),
                      weaknesses.checkWeaknesses(superhero, askForHelp.problem).<AppError>downcast(),
                      (abilities, __) -> HelpResult.builder() // <4> 
                              .hero(superhero)
                              .matchingAbilities(abilities)
                              .build()
              )
              .mapError(HelpErrors::new) // <5> 
      );
}
```

1. on cherche un superhero, si non trouvé on a une erreur de type `SuperheroError`
2. Comme tous les domaines ne partagent pas le même type d'erreur, il faut convertir en une erreur commune. 
3. Si c'est ok, on a notre superhero, on va pouvoir récupérer ses capacités et vérifier ses faiblesses en parallèle
4. on combine les résultats obtenus 
5. on aligne les erreurs 

Il ne reste plus qu'a gérer la couche http :
* les succès seront un 200 ok 
* les erreurs seront une 400 bad request 
  * on aura une erreur détaillée avec un maximum d'informations 

```java
 public Mono<ServerResponse> findHelp(ServerRequest request) {
     return request
             .bodyToMono(AskForHelp.class)
             .flatMap(command ->
                 findHelpService.findHelp(command)
                     .foldMono(
                          helpErrors -> ServerResponse.badRequest().bodyValue(helpErrors.dtoErrors()),
                          ok -> ServerResponse.ok().bodyValue(ok)
                     )
             );
 }
```

#### Conclusion 

J'utilise ça depuis quelques années maintenant et je trouve ça très agréable. 
Ca demande quand même une prise en main pour les nouveaux arrivants mais ça amène un plus pour le code métier comparé à du reactor vanilla. 

Si je suis courageux, j'en ferai une lib !


## Démarrer l'application 

```
./gradlew bootRun
```

## Appeler l'api

Une erreur retounée 
```bash
curl -XPOST http://localhost:8080/api/helps/_command -H 'Content-Type: application/json' -d '{"name":"superman","problem":"FellIntoWater"}' --include
```
Une erreur retounée 
```bash
curl -XPOST http://localhost:8080/api/helps/_command -H 'Content-Type: application/json' -d '{"name":"luffy","problem":"CarAccident"}' --include
```

Plusieurs erreurs retounées 
```bash
curl -XPOST http://localhost:8080/api/helps/_command -H 'Content-Type: application/json' -d '{"name":"luffy","problem":"FellIntoWater"}' --include
```
Cas ok 
```bash
curl -XPOST http://localhost:8080/api/helps/_command -H 'Content-Type: application/json' -d '{"name":"luffy","problem":"SuperVilain"}' --include
```
