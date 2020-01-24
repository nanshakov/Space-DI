package nanshakov.app

import nanshakov.ioc.annotation.Service

interface TestService2 {
    fun smth2()
}

@Service()
class TestService2Impl : TestService2 {

    override fun smth2() {
        println(TestService2::class.java.toString() + " smth()")
    }
}