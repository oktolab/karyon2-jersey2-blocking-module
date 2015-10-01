package netflix.karyon.jersey.blocking;

import java.util.Iterator;

import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.server.spi.ContainerProvider;

import com.google.common.collect.Iterators;

@SuppressWarnings("rawtypes")
class ServiceIteratorProviderImpl<T> extends ServiceFinder.ServiceIteratorProvider {

    static {
        /**
         * This iterator provider override makes it possible to not mandate the presence of a jar with a META-INF/ based
         * Service provider discovery which is the default for jersey.
         */
        ServiceFinder.setIteratorProvider(new ServiceIteratorProviderImpl());
    }

    private static final Iterator<? extends ContainerProvider> nettyContainerProviderIter =
            Iterators.singletonIterator(new NettyContainerProvider());

    private final ServiceFinder.DefaultServiceIteratorProvider defaultProvider;

    ServiceIteratorProviderImpl() {
        defaultProvider = new ServiceFinder.DefaultServiceIteratorProvider();
    }

    public static void registerWithJersey() {
        // Static block does the register.
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding" })
    public <T> Iterator<T> createIterator(Class<T> service, String serviceName, ClassLoader loader,
                                      boolean ignoreOnClassNotFound) {
        Iterator<T> defaultIterator = defaultProvider.createIterator(service, serviceName, loader, ignoreOnClassNotFound);
        if (service.isAssignableFrom(NettyContainerProvider.class)) {
            return (Iterator<T>) Iterators.concat(defaultIterator, nettyContainerProviderIter);
        }
        return defaultIterator;
    }

    @SuppressWarnings("hiding")
	@Override
    public <T> Iterator<Class<T>> createClassIterator(Class<T> service, String serviceName, ClassLoader loader,
                                                  boolean ignoreOnClassNotFound) {
        return defaultProvider.createClassIterator(service, serviceName, loader, ignoreOnClassNotFound);
    }

}
