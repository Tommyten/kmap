package es.horm.kmap.sample

import com.example.generated.toTestModel
import es.horm.kmap.runtime.KmapTo
import es.horm.kmap.runtime.KmapTransformer
import es.horm.kmap.runtime.Mapping

class ConverterForC : KmapTransformer<Int, Int> {
    override fun transform(value: Int): Int = value * 4
}

class DtoSumAggregator : KmapTransformer<Dto, Int> {
    override fun transform(value: Dto): Int = with(value) { a + b + c}
}

@KmapTo(
    target = BusinessModel::class,
    mappings = [Mapping("c", "test", ConverterForC::class)],
    //aggregators = [Aggregator("sum", DtoSumAggregator::class)]
)
@KmapTo(TestModel::class)
data class Dto(
    val a: Int,
    val b: Int,
    val c: Int,
)

data class BusinessModel(
    val a: Int,
    val b: Int,
    val test: Int,
   // val sum: Int
)
data class TestModel(
    val b: Int,
    val c: Int
)

fun main() {
    Dto(1,2,3).toTestModel()
    println("Ran!")
}
