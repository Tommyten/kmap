package es.horm.kmap.runtime.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
public annotation class KmapTo(
    val target: KClass<*>,
    val mappings: Array<Mapping> = [],
    val aggregators: Array<Aggregator> = []
)
