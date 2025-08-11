package es.horm.kmap.runtime.annotation

import es.horm.kmap.runtime.KmapTransformer
import kotlin.reflect.KClass

annotation class Aggregator(
    val target: String,
    val transformer: KClass<out KmapTransformer<*, *>> = KmapTransformer::class,
)
