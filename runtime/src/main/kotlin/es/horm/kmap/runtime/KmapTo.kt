package es.horm.kmap.runtime

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class KmapTo(
    val target: KClass<*>,
    val mappings: Array<Mapping> = [],
    val aggregators: Array<Aggregator> = []
)

annotation class Mapping(
    val source: String,
    val target: String,
    val transformer: KClass<out KmapTransformer<*, *>> = NOOP::class,
)

fun interface KmapTransformer<in T, out O> {
    fun transform(value: T): O
}

object NOOP : KmapTransformer<Any, Any> {
    override fun transform(value: Any) = value
}

annotation class Aggregator(
    val target: String,
    val transformer: KClass<out KmapTransformer<*, *>> = KmapTransformer::class,
)
