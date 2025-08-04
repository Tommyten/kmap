# kmap

## TODOs

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
Currently, I do not check, whether a Transformer actually returns the type it is supposed to 
How to handle type conflicts?

### Proper Error Messages

### Configurability
- Package where the extension functions are generated to
- Naming Scheme of the generated extension functions

### Two Way mapping
- [ ] implement a KmapFrom Annotation that does basically the same as @KmapTo just source and
  target are the other way round.
- 

## Credits

Thank you to Daniel Pelsmaeker for teaching me [How to debug KSP processors](https://pelsmaeker.net/articles/debugging-ksp-gradle-plugin/)