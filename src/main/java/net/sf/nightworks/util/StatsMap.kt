package net.sf.nightworks.util

// TODO: check that provided map contains complete enum keys
class StatsMap<K : Enum<K>>(val map: MutableMap<K, Int>) : Map<K, Int> {
    override val entries: Set<Map.Entry<K, Int>> get() = map.entries
    override val keys: Set<K> get() = map.keys
    override val size: Int get() = map.size
    override val values: Collection<Int> get() = map.values
    override fun containsKey(key: K) = map.containsKey(key)
    override fun containsValue(value: Int) = map.containsValue(value)
    override fun isEmpty() = map.isEmpty()

    override fun get(key: K) = map[key]!!
    operator fun set(key: K, value: Int) {
        map[key] = get(key) + value
    }

    fun fill(v: Int) {
        map.keys.forEach { map[it] = v }
    }

    fun fill(f: (s: K) -> Int) {
        map.keys.forEach { map[it] = f(it) }
    }

    fun copyFrom(src: StatsMap<K>) {
        src.map.keys.forEach { map[it] = src[it] }
    }
}