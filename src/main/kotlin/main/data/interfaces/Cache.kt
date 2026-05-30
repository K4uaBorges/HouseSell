package main.data.interfaces

interface Cache<K, V> {
    fun getById(id: K): V?

    fun put(
        id: K,
        value: V,
    )

    fun clear()
}
