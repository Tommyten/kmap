package es.horm.kmap.moduleA

import es.horm.kmap.moduleB.SomeBusinessModel
import es.horm.kmap.runtime.annotation.Aggregator
import es.horm.kmap.runtime.KmapTransformer
import es.horm.kmap.runtime.annotation.KmapTo
import es.horm.kmap.runtime.annotation.Mapping

class NameAggregator : KmapTransformer<SomeDto, Int> {
    override fun transform(value: SomeDto): Int = value.a + value.b
}

@KmapTo(
    target = SomeBusinessModel::class,
    mappings = [Mapping(source = "c", target = "name")],
    aggregators = [Aggregator(target = "sum", NameAggregator::class),]
)
data class SomeDto(
    val a: Int,
    val b: Int,
    val c: String,
)
