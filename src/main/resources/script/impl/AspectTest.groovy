import org.aspectj.lang.ProceedingJoinPoint

/**
 * if you just want to proxy spring beans,
 * you can config a aspect,then proxy the aspect.
 * some times AssistProxy does not works
 * (because there is limitation for javassist),
 * this is the way to resolve the problem.
 * @param joinPoint
 * @return
 */
def around(ProceedingJoinPoint joinPoint){
    logger.info("###############"+joinPoint)
    return joinPoint.proceed()
}
return this