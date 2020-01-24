package nanshakov.app

import nanshakov.ioc.annotation.PostConstruct
import nanshakov.ioc.annotation.Service

interface TestService {
    fun smth()
}

@Service()
class TestServiceImpl(private var service2: TestService2) : TestService {

    @PostConstruct
    fun post() {
        println("post() triggered")
    }

    override fun smth() {
        service2.smth2()
        println("smth()")
    }
}