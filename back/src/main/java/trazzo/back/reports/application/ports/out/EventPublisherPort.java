package trazzo.back.reports.application.ports.out;

public interface EventPublisherPort {
    void publishEvent(Object event);
}
