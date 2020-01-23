package nanshakov.app

import nanshakov.ioc.Application
import nanshakov.ioc.annotation.Service
import nanshakov.ioc.annotation.Type

@Service(type = Type.SINGLETON)
class Main(private var service: TestService) : Runnable {

    override fun run() {
        service.smth()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application().run(Main::class.java.getPackage())
        }
    }
}