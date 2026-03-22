package com.oms.domain.port.outbound;

import com.oms.domain.event.DomainEvent;

import java.util.List;

public interface EventPublisher {

    void publish(List<DomainEvent> events);
}
