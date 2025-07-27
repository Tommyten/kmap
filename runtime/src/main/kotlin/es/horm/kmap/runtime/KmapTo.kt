package es.horm.kmap.runtime

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class KmapTo(
    val target: KClass<*>,
    val mappings: Array<Mapping> = [],
)

annotation class Mapping(val source: String, val target: String)
