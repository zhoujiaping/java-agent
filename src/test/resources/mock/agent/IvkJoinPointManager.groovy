package mock.agent

import groovy.transform.Synchronized

class IvkJoinPointManager {
    static def tl = new InheritableThreadLocal<List<IvkJoinPoint>>()

    static void saveJoinPoint(IvkJoinPoint joinPoint) {
        initTl()
        tl.get() << joinPoint
    }

    static IvkJoinPoint currentJoinPoint() {
        tl.get()[-1]
    }

    static IvkJoinPoint removeCurrentJoinPoint() {
        tl.get().removeLast()
    }

    @Synchronized
    static void initTl() {
        if (tl.get() == null) {
            tl.set([])
        }
    }

    static def withInvokerStackManage(IvkJoinPoint obj, Closure closure) {
        try {
            saveJoinPoint(obj)
            closure.call()
        } finally {
            removeCurrentJoinPoint()
        }
    }
}
