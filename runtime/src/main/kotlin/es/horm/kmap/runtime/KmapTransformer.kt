package es.horm.kmap.runtime

fun interface KmapTransformer<in T, out O> {
    fun transform(value: T): O
}

object NOOP : KmapTransformer<Any, Any> {
    override fun transform(value: Any) = value
}
