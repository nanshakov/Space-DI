package nanshakov.ioc

import mu.KotlinLogging
import nanshakov.Context
import nanshakov.ioc.annotation.PostConstruct
import nanshakov.ioc.annotation.Service
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.io.ComponentNameProvider
import org.jgrapht.io.DOTExporter
import org.jgrapht.traverse.TopologicalOrderIterator
import org.reflections.ReflectionUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.Parameter


class Application {
    private val logger = KotlinLogging.logger {}
    private val interfaceToClass = HashMap<Class<*>, List<Class<*>>>()
    private val context = Context()

    fun run(pkg: Package) {
        val config = ConfigurationBuilder()
            .setScanners(ResourcesScanner(), SubTypesScanner(false), TypeAnnotationsScanner())
            .setUrls(ClasspathHelper.forPackage(pkg.name))
            .filterInputsBy(FilterBuilder().includePackage(pkg.name))

        val reflections = Reflections(config)
        val services = reflections.getTypesAnnotatedWith(Service::class.java)
        logger.debug { "Found ${services.size} classes @Service" }

        interfaceToClass.putAll(readStructure(services))
        val tree: Graph<Class<*>, DefaultEdge> = DirectedAcyclicGraph(DefaultEdge::class.java)

        services.forEach { c: Class<*>? ->
            val constructors = ReflectionUtils.getAllConstructors(c)
            check(constructors.size <= 1)
            val argsTypes = constructors.elementAt(0).parameterTypes
            tree.addVertex(c)
            argsTypes.forEach {
                //резолвим класс имплементирующий интерфей
                val impl = findClassByInterface(it)
                tree.addVertex(impl)
                tree.addEdge(impl, c)
            }
        }
        this.print(tree)

        val orderIterator: TopologicalOrderIterator<Class<*>, DefaultEdge> = TopologicalOrderIterator(tree)
        while (orderIterator.hasNext()) {
            val cl = orderIterator.next();
            logger.debug { cl }
            val constructor = cl.constructors[0]
            //создаем инстанс
            val instance = if (constructor.parameters.isEmpty()) {
                constructor.newInstance()
            } else {
                val resolveParams: Array<Any> = resolveParams(constructor.parameters, context)
                constructor.newInstance(*resolveParams)
            }
            context.push(instance)
            tryInvokePostAction(instance)
        }
        runRunnable()

        logger.debug { context }
    }

    private fun tryInvokePostAction(instance: Any) {
        val methods = instance.javaClass.declaredMethods
        if (methods.any { it.isAnnotationPresent(PostConstruct::class.java) }) {
            val postConstructMethod = methods.first() {
                it.isAnnotationPresent(PostConstruct::class.java)
            }
            logger.debug { "Found method $postConstructMethod with annotation @PostConstruct" }
            logger.debug { "invoke..." }
            postConstructMethod.invoke(instance)
        }
    }

    private fun runRunnable() {
        logger.info { "Running..." }
        val runnableClasses = interfaceToClass[Runnable::class.java]
        val instances = context.get(runnableClasses!!)
        instances.forEach {
            val run = it.javaClass.getMethod("run")
            logger.debug { "Found runnable method $run in $it" }
            logger.debug { "invoke..." }
            run.invoke(it)
        }
    }

    //возвращает список подходящих обьектов
    private fun resolveParams(params: Array<Parameter>, context: Context): Array<Any> {
        return params.map {
            context.get(findClassByInterface(it.type))
        }.toTypedArray()
    }

    //ищет классы по интерфейсу
    private fun findClassByInterface(_interface: Class<*>): Class<*> {
        return interfaceToClass[_interface]!!.first()
    }

    //создает мапу interface -> all classes impl
    private fun readStructure(classes: Set<Class<*>>): HashMap<Class<*>, List<Class<*>>> {
        val interfaceToClass = HashMap<Class<*>, List<Class<*>>>()

        classes.forEach { cl ->
            val interfaces = cl.interfaces
            if (interfaces.isEmpty())
                logger.warn { "Interfaces for class @Service $cl is empty" }
            interfaces.forEach {
                if (interfaceToClass.containsKey(it)) {
                    val list = interfaceToClass[it]
                    list!!.plus(cl)
                    interfaceToClass[it] = list
                }
                interfaceToClass.putIfAbsent(it, listOf(cl))
            }
        }
        return interfaceToClass
    }

    //https://dreampuf.github.io/GraphvizOnline/#
    private fun print(graph: Graph<Class<*>, DefaultEdge>) {
        val vertexIdProvider: ComponentNameProvider<Class<*>> =
            ComponentNameProvider { cl -> cl.name.replace('.', '_') }
        val vertexLabelProvider: ComponentNameProvider<Class<*>> =
            ComponentNameProvider { cl -> cl.name }
        val exporter = DOTExporter<Class<*>, DefaultEdge>(vertexIdProvider, vertexLabelProvider, null)
        val writer: Writer = StringWriter()
        exporter.exportGraph(graph, writer)
        logger.debug { writer.toString() }
    }

    private fun printUseTopologicalOrder(graph: Graph<Class<*>, DefaultEdge>) {
        val orderIterator: TopologicalOrderIterator<Class<*>, DefaultEdge> = TopologicalOrderIterator(graph)
        println("\nTopological Ordering:")
        while (orderIterator.hasNext()) {
            logger.debug { orderIterator.next() }
        }
    }
}