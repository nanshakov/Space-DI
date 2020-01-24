package nanshakov

class Context {
    private val context = HashMap<Class<*>, List<Any>>()

    fun push(instance: Any) {
        if (context.containsKey(instance.javaClass)) {
            val list = context[instance.javaClass]!!
            list.plus(instance)
            context[instance.javaClass] = list
        } else {
            context[instance.javaClass] = listOf(instance)
        }
    }

    fun get(instance: Collection<Class<*>>): List<Any> {
        val result = listOf<Any>()
        instance.forEach { result.plus(get(it)) }
        return result
    }

    fun get(instance: Class<*>) = context[instance.javaClass]!!.first()

}