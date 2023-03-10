package org.jboss.eap.qe.microprofile.fault.tolerance.deployments.v20.priority;

import jakarta.enterprise.context.RequestScoped;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@RequestScoped
public class InterceptorsContext {
    private final Queue<String> orderQueue = new ConcurrentLinkedDeque<>();

    public Queue<String> getOrderQueue() {
        return orderQueue;
    }
}
