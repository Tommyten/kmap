package es.horm.kmap.runtime

public fun interface KmapTransformer<in T, out O> {
    public fun transform(value: T): O
}

public object NOOP : KmapTransformer<Any, Any> {
    override fun transform(value: Any): Any = value
}
