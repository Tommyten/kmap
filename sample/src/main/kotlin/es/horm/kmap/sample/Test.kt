package es.horm.kmap.sample

import com.example.generated.toPersonBusinessModel
import es.horm.kmap.runtime.annotation.Aggregator
import es.horm.kmap.runtime.KmapTransformer
import es.horm.kmap.runtime.annotation.KmapTo
import es.horm.kmap.runtime.annotation.Mapping

internal class NameAggregator : KmapTransformer<PersonDto, String> {
    override fun transform(value: PersonDto): String = with(value) { "$lastName, $firstName" }
}

@KmapTo(
    target = PersonBusinessModel::class,
    mappings = [Mapping(source = "yearsOld", target = "age")],
    aggregators = [Aggregator(target = "name", NameAggregator::class)]
)
data class PersonDto(
    val firstName: String,
    val lastName: String,
    val yearsOld: Int,
)

data class PersonBusinessModel(
    val name: String, // aggregate from firstName and lastName -> Handled by aggregator
    val age: Int, // called yearsOld in Dto -> handled by mapper
)

@KmapTo(B::class)
data class A(
    val a: Int
)

data class B(val a: Int)

fun main() {
    val personFromApi = PersonDto("Max", "Mustermann", 42)
    val person = personFromApi.toPersonBusinessModel()
    println("Dto is: $personFromApi")
    println("Model is: $person")
}
