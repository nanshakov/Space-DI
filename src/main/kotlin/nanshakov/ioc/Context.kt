package nanshakov.ioc

import mu.KotlinLogging

class Context {
    private val logger = KotlinLogging.logger {}
    private val context = HashMap<Class<*>, List<Any>>()

    fun push(instance: Any) {
        logger.trace { "push to context $instance" }
        if (context.containsKey(instance.javaClass)) {
            val list = context[instance.javaClass]!!
            list.plus(instance)
            context[instance.javaClass] = list
        } else {
            context[instance.javaClass] = listOf(instance)
        }
    }

    fun get(instance: Collection<Class<*>>) = instance.map { get(it) }.toTypedArray()

    fun get(instance: Class<*>) = context[instance]!!.first()

    override fun toString(): String {
        return context.toString()
    }
}