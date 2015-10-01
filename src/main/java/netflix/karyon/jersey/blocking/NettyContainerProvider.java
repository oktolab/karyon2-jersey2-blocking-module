package netflix.karyon.jersey.blocking;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.spi.ContainerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


public class NettyContainerProvider implements ContainerProvider { // OK

    private static final Logger logger = LoggerFactory.getLogger(NettyContainerProvider.class);

	@SuppressWarnings("unchecked")
	@Override
	public <T> T createContainer(Class<T> type, Application application) throws ProcessingException {
		Preconditions.checkNotNull(type);
      Preconditions.checkNotNull(application);
      if (!type.equals(NettyContainer.class)) {
          logger.error(
                  "Netty container provider can only create container of type {}. Invoked to create container of type {}",
                  NettyContainer.class.getName(), type.getName());
      }
      return (T) new NettyContainer(application);
	}

    
}
