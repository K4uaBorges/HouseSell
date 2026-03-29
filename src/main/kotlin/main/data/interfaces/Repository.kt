package main.data.interfaces

interface Repository<K, V> {
    fun create(value: V): V

    fun save(value: V): V

    fun getById(key: K): V

    fun getAll(): List<V>

    fun deleteById(key: K)

    fun clear()

    fun update(updated: V): V
}