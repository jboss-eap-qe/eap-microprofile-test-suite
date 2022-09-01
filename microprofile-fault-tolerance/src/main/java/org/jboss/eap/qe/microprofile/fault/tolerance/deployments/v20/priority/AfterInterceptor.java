package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.priority;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Priority(6000)
@AfterFT
public class AfterInterceptor {
    @Inject
    private InterceptorsContext context;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        context.getOrderQueue().add(this.getClass().getSimpleName());
        return ctx.proceed();
    }
}