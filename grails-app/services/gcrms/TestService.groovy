package gcrms

import grails.transaction.Transactional

import java.util.concurrent.locks.ReentrantLock

@Transactional
class TestService {

    def count = 0

    def lock = new ReentrantLock()

    def increase() {
        count ++
    }

    def go() {
        if (lock.tryLock()) {
            try {
                Thread.sleep(1000)
                def num = increase()
                println("execute job count " + num)
                Thread.sleep(1000l * 20)
                println("job " + num + "done")
            } catch (Exception e) {
                e.printStackTrace()
            } finally {
                lock.unlock()
            }
        } else {
            println("object is locked.")
        }
    }
}
