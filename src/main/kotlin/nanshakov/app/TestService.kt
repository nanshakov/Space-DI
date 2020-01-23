package nanshakov.app

import nanshakov.ioc.annotation.PostInit
import nanshakov.ioc.annotation.Service
import nanshakov.ioc.annotation.Type

interface TestService {
    fun smth()
}

@Service(type = Type.SINGLETON)
class TestServiceImpl(private var service2: TestService2) : TestService {

    @PostInit
    private fun post() {
        println(TestService::class.java.toString() + " post()")
    }

    override fun smth() {
        service2.smth2()
        println(TestService::class.java.toString() + " smth()")
    }
}