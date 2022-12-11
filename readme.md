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
On peut donc en un coup d'oeil comprendre le comportement d'une méthode. 

Un exemple simple mais très parlant c'est le cas de la division : 

Immaginons une méthode `diviser` : 

```java
public Float diviser(Integer nombre, Integer diviser); 
```

Ici il faut aller regarder l'implémentation pour comprendre ce qui se passe. 

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

Il existe une 3 ème solution qui est l'utilisation des `Either` de la lib `vavr`. 
Un `Either` peut être soit `Left` soit `Right` et par convention, le côté gauche représente les erreurs et le côté droit les succès.  

On aura alors cette signature : 

```java
public Either<DivisionParZero, Float> diviser(Integer nombre, Integer diviser); 
```

Avec les `Either`, il devient plus facile de manipuler les erreurs. 
Alors que c'est difficile à faire avec des `try` `catch`, on pourra facilement transformer, combiner, collecter ... les erreur avec les `Either`. 

Le problème c'est qu'il faudra aussi manipuler les succès. 
L'utilisation d'`Either` devient intéressante quand on a un nombre important de règles à gérer avec beaucoup de cas d'erreur à manipuler.  


## La gestion des erreurs dans du code reactif

Dans le monde reactif, on perd complétement la notion de checked exception. 

```java
Mono<Person> creerUnePersonne(PersonDto person);
```

`reactor` offre la possibilité de catcher et de manipuler les erreurs 
mais on doit aller lire l'implémentation pour comprenndre une méthode. 


## Gérer plus finement les erreurs avec reactor 

En s'inspirant du monde fonctionnel, on peut créer une structure `IO<E, A>` qui permet de gérer à la fois côté non bloquant et à la fois les erreurs de façon typé.

En scala, on retrouve ce genre de concept avec les `EitherT` (qui vient du monde haskell) ou dans la lib `ZIO`.  

`IO<E, A>` sera juste un wrapper de `Mono<Either<E, A>>` mais qui va exposer tout un tas de méthodes utilitaires pour pouvoir combiner, composer, transformer etc. 

On va gérer 2 types d'erreurs : 
* les erreurs métier : `E` qui seront visible dans la signature des méthodes
* les erreurs techniques : l'équivalent des runtimes exception qu'on ne va pas gérer et juste laisser remonter. 

Quand on expose une API, et qu'on utilise un `IO<E, A>` alors
* `A` sera une 200 
* `E` sera une 400 bad request
* les erreurs techniques seront des 500 


L'implémentation de IO se trouve ici [IO.java](src/main/java/io/IO.java). 

### Les IO dans la vari vie 




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
