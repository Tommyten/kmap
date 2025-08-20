package es.horm.kmap.runtime.annotation

import es.horm.kmap.runtime.KmapTransformer
import es.horm.kmap.runtime.NOOP
import kotlin.reflect.KClass

public annotation class Mapping(
    val source: String,
    val target: String,
    val transformer: KClass<out KmapTransformer<*, *>> = NOOP::class,
)
