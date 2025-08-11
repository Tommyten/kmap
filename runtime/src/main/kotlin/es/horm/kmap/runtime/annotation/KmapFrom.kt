package es.horm.kmap.runtime.annotation

import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class KmapFrom(
    val source: KClass<*>,
    val mappings: Array<Mapping> = [],
    val aggregators: Array<Aggregator> = [],
)
