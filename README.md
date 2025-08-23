# KMap - Mapping functions for Kotlin, made effortless
[![Static Badge](https://img.shields.io/badge/Maven_Central-v0.1.0-orange)](https://central.sonatype.com/search?q=es.horm.kmap)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue)](https://github.com/Tommyten/kmap/blob/master/LICENSE)

## What is kmap?
kmap is a KSP plugin that aims to minimize the amount of boilerplate code in a project
by generating mapping functions to the accordingly annotated types.

## Setup
First you need to add the KSP Gradle Plugin
```kotlin
plugins {
    id("com.google.devtools.ksp") version "CURRENT_KSP_VERSION"
}
```

You then need to add the kmap processor so that ksp can actually use it:

```kotlin
dependencies {
    ksp("es.horm.kmap:kmap-processor:0.1.0")
}
```

Then you need to add the kmap runtime library so that you can use the required annotations:

**Multiplatform Projects:**
```kotlin
sourceSets {
    val commonMain by getting {
        dependencies {
            implementation("es.horm.kmap:kmap-runtime:0.1.0")
        }
    }
}
```
**Single Platform Projects:**
```kotlin
dependencies {
    implementation("es.horm.kmap:kmap-runtime:0.1.0")
}
```

## Usage
Currently, the main annotations in kmap are the `@KmapTo` and `@KmapFrom` annotations.
To generate a mapper from class `Source` to class `Destination` you either annotate class `Source` with the `@KmapTo` annotation like so:
```kotlin
@KmapTo(Target::class)
data class Source(...)
```
which will generate an extension function on `Source` like so: `fun Source.toTarget()`.

Or you annotate your `Target` class with the `@KmapFrom` annotation like so:
```kotlin
@KmapFrom(Source::class)
data class Target(...)
```
which will also generate an extension function on `Source` called `toTarget()`.

During the build phase the kmap-processor will now generate mapping functions for all classes annotated with either `@KmapFrom` or `@KmapTo`.
You can then freely use these mapping functions in your code.


### Map between different parameter names
For cases, where the parameters in the source and target classes are different you can
use the `Mapping` Annotation. Both `@KmapTo` and `@KmapFrom` take an array of `Mapping` annotations
as parameter like so:

```kotlin
@KmapTo(
    target = PersonBusinessModel::class,
    mappings = [Mapping(source = "yearsOld", target = "age")]
)
data class PersonDto(
    val yearsOld: Int,
    //...
)
```

### Map parameters with a transformation step
For the more complicated cases, where you need to do some kind of transformation between your source
and target *kmap* offers an extension to the `Mapping` mechanism called `Transformer`:
```kotlin
class AgeTransformer : KmapTransformer<Int, Int> {
    override fun transform(value: Int): Int = (value * 365.25).roundToInt()
}

@KmapTo(
    target = PersonBusinessModel::class,
    mappings = [Mapping(source = "yearsOld", target = "daysOld", transformer = AgeTransformer::class)]
)
data class PersonDto(
    val yearsOld: Int,
    //...
)
```

Like this you can not only transform from, say `Int` to `Int` but also between different types, like
for example `String` to kotlinx datetime's `LocalDate`:
```kotlin
class DateTransformer : KmapTransformer<String, LocalDate> {
    override fun transform(value: String): LocalDate = LocalDate.parse(value)
}

@KmapTo(
    target = PersonBusinessModel::class,
    mappings = [Mapping(source = "birthDate", target = "birthDate", transformer = DateTransformer::class)]
)
data class PersonDto(
    val birthDate: Int,
    //...
)
```

### Map from multiple parameters to one
To map from multiple parameters to a single parameter you can use an `Aggegator`. Both `@KmapTo` and `@KmapFrom`
take an array of aggregators as parameter.
Using aggregators you can easily map from multiple parameters to a single one like so:
```kotlin
class NameAggregator : KmapTransformer<PersonDto, String> {
    override fun transform(value: PersonDto): String = with(value) { "$lastName, $firstName" }
}

@KmapTo(
    target = PersonBusinessModel::class,
    aggregators = [Aggregator(target = "name", NameAggregator::class)]
)
data class PersonDto(
    val firstName: String,
    val lastName: String,
)
```
Both Aggregators and Transformers use the same abstraction to achieve the same result: `KmapTransformer`.
However, when using `KmapTransformer` to implement an Aggregator the first type parameter is the source class as
this is the value where you can freely decide which parameters you will choose to map to your defined target parameter.

## Limitations
While kmap already offers quite a few ways to map between your classes nested mapping is currently not supported (see Roadmap).

## Roadmap / TODOs

### Nested Mapping
There currently exist no properly supported way to do nested mappings.
You probably could use an aggregator for each property. However, that would be way too verbose since you'd need one aggregator
for each single property of your nested object (in the example below that'd already be two aggregators although the Address class
doesn't even represent a complete Address yet).
Mappings wouldn't work, because checks are being made to see whether the source exists verbatim. 
```kotlin
@KmapTo(
    target = PersonBusinessModel::class,
    mappings = [Mapping(source = "yearsOld", target = "age")],
    aggregators = [Aggregator(target = "name", NameAggregator::class)]
)
data class PersonDto(
    val firstName: String,
    val lastName: String,
    val yearsOld: Int,
    val address: Address,
)

data class Address(
    val street: String,
    val houseNumber: Int,
    // ...
)


data class PersonBusinessModel(
    val name: String, // aggregate from firstName and lastName -> Handled by aggregator
    val age: Int, // called yearsOld in Dto -> handled by mapper
    val street: String, // currently no way of mapping PersonDto.address.street to this, could be handled by an aggregator but should also be doable with a mapping in my mind
    val houseNumber: Int, // see above
)
```

### Check Mapping Types
Currently, I do not check, whether a Transformer actually returns the type it is supposed to.
How should type conflicts be handled? -> Probably error out, causing the Processor to stop

### Proper Error Messages
Error Messages are currently not helpful at all, they should be improved in order to point users to where something is
going wrong.

### Configurability
- Package where the extension functions are generated to
- Naming Scheme of the generated extension functions
- Choose which file the functions will be generated in
- Choose mapping function visibility

### Two Way mapping
- Currently, the KmapTo and KmapFrom annotations allow generating a `Source.toTarget()` function. Maybe there would
be some utility in having a way to generate a `Target.Companion.fromSource()` function? This may be a viable thing to
implement if there is a need in the Community.

## Credits

Thank you to Daniel Pelsmaeker for teaching me [How to debug KSP processors](https://pelsmaeker.net/articles/debugging-ksp-gradle-plugin/)