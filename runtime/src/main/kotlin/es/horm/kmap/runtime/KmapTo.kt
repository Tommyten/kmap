package es.horm.kmap.runtime

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class KmapTo(
    val target: KClass<*>,
    val mappings: Array<Mapping> = [],
)

annotation class Mapping(
    val source: String,
    val target: String,
    val transformer: KClass<out KmapTransformer<*, *>> = KmapTransformer::class,
)

annotation class Transformation(
    val transformer: KClass<KmapTransformer<*, *>>
)

fun interface KmapTransformer<in T, out O> {
    fun transform(value: T): O
}
