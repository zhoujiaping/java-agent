package mock.agent

import java.lang.reflect.Method

class IvkJoinPoint {
    Object self
    Method thisMethod
    Method proceed
    Object[] args

    def proceed() {
        proceed.invoke(self, args)
    }
}
