package nanshakov.ioc

import mu.KotlinLogging
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


class Application {
    private val logger = KotlinLogging.logger {}

    fun run(pkg: Package) {
        val config = ConfigurationBuilder()
            .setScanners(ResourcesScanner(), SubTypesScanner(false), TypeAnnotationsScanner())
            .setUrls(ClasspathHelper.forPackage(pkg.name))
            .filterInputsBy(FilterBuilder().includePackage(pkg.name))

        val reflections = Reflections(config)
        val services = reflections.getTypesAnnotatedWith(Service::class.java)
        logger.info { "Found ${services.size} classes @Service" }

        val context = constructContext(services)

        val tree: Graph<Class<*>, DefaultEdge> = DirectedAcyclicGraph(DefaultEdge::class.java)

        services.forEach { c: Class<*>? ->
            val constructors = ReflectionUtils.getAllConstructors(c)
            check(constructors.size <= 1)
            val argsTypes = constructors.elementAt(0).parameterTypes
            tree.addVertex(c)
            argsTypes.forEach {
                //резолвим класс имплементирующий интерфей
                val impl = context[it]!!.first()
                tree.addVertex(impl)
                tree.addEdge(impl, c)
            }
        }
        this.print(tree)
        printUseTopologicalOrder(tree)

    }

    private fun constructContext(classes: Set<Class<*>>): HashMap<Class<*>, List<Class<*>>> {
        val interfaceToClass = HashMap<Class<*>, List<Class<*>>>()

        classes.forEach { cl ->
            val interfaces = cl!!.interfaces
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


    private fun print(graph: Graph<Class<*>, DefaultEdge>) {
        val vertexIdProvider: ComponentNameProvider<Class<*>> =
            ComponentNameProvider { cl -> cl.name.replace('.', '_') }
        val vertexLabelProvider: ComponentNameProvider<Class<*>> =
            ComponentNameProvider { cl -> cl.name }
        val exporter = DOTExporter<Class<*>, DefaultEdge>(vertexIdProvider, vertexLabelProvider, null)
        val writer: Writer = StringWriter()
        exporter.exportGraph(graph, writer)
        logger.info { writer.toString() }
    }

    private fun printUseTopologicalOrder(graph: Graph<Class<*>, DefaultEdge>) {
        val orderIterator: TopologicalOrderIterator<Class<*>, DefaultEdge> = TopologicalOrderIterator(graph)
        println("\nTopological Ordering:")
        while (orderIterator.hasNext()) {
            logger.info { orderIterator.next() }
        }
    }
}