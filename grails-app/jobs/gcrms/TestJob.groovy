package gcrms

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class TestJob {
    def testService
    /*def concurrent = true
    Lock lock = new ReentrantLock()*/

    static triggers = {
      //simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute() {
        // execute job
        testService.go()

    }
}
