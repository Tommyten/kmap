package es.horm.kmap.sample

import com.example.generated.toBusinessModel
import es.horm.kmap.runtime.KmapTo

@KmapTo(BusinessModel::class)
@KmapTo(TestModel::class)
data class Dto(
    val a: Int,
    val b: Int,
    val c: Int,
)

data class BusinessModel(
    val a: Int,
    val b: Int
)
data class TestModel(
    val b: Int,
    val c: Int
)

fun main() {
    Dto(1,2,3).toBusinessModel()
    println("Ran!")
}
