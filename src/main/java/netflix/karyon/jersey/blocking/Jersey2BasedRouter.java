package netflix.karyon.jersey.blocking;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpRequestHeaders;
import io.reactivex.netty.protocol.http.server.HttpResponseHeaders;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.SecurityContext;

import netflix.karyon.transport.util.HttpContentInputStream;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import br.com.oktolab.jersey.server.PropertiesBasedResourceConfig;

import com.google.inject.Injector;

public class Jersey2BasedRouter implements RequestHandler<ByteBuf, ByteBuf> {

    private static final Logger LOG = LoggerFactory.getLogger(Jersey2BasedRouter.class);

    private final PropertiesBasedResourceConfig resourceConfig;
    private final Injector injector;
    private ApplicationHandler application;

    public Jersey2BasedRouter() {
        this(null);
    }

    @Inject
    public Jersey2BasedRouter(Injector injector) {
        this.injector = injector;
        ServiceIteratorProviderImpl.registerWithJersey();
        resourceConfig = new PropertiesBasedResourceConfig();
    }
    
    @PostConstruct
    public void start() {
        NettyContainer container = ContainerFactory.createContainer(NettyContainer.class, resourceConfig);
//        if (null != injector) {
//            container = ContainerFactory.createContainer(NettyContainer.class, application);/* resourceConfig,
//                                                         new GuiceComponentProviderFactory(resourceConfig, injector));*/
//        } else {
//            container = ContainerFactory.createContainer(NettyContainer.class, resourceConfig);
//        }
        Application app = container.getApplication();
        application =  new ApplicationHandler(app);
        LOG.info("Started Jersey based request router.");
    }

    @PreDestroy
    public void stop() {
        LOG.info("Stopped Jersey based request router.");
        // application.destroy(); ? TODO
    }
    
    @Override
    public Observable<Void> handle(final HttpServerRequest<ByteBuf> request, final HttpServerResponse<ByteBuf> response) {
    	try {
    		final InputStream requestData = new HttpContentInputStream( response.getAllocator(), request.getContent() );
    		
    		URI baseUri = new URI("/");
    		URI uri = new URI(request.getUri());
    		PropertiesDelegate delegate = resourceConfig.getPropertiesDelegate();
    		
    		if ("/favicon.ico".equals(uri.getPath())) {
    			// TODO GET BEHAVIOR FROM MODULE CLASS
    			request.ignoreContent();
    			return response.close();
    		}
    		
    		ContainerRequest containerRequest = new ContainerRequest(baseUri, uri, request.getHttpMethod().name(), 
    				getSecurityContext(), delegate);
    		
    		final HttpRequestHeaders headers = request.getHeaders();
    		if (headers != null && !headers.isEmpty()) {
    			Set<String> names = headers.names();
    			for (String key : names) {
    				containerRequest.getHeaders().add(key, headers.get(key));
				}
    		}
    		containerRequest.setEntityStream(requestData);
    		final ContainerResponseWriter containerResponse = bridgeResponse(response);
    		containerRequest.setWriter(containerResponse);
    		return Observable.create(new Observable.OnSubscribe<Void>() {
    			@Override
    			public void call(Subscriber<? super Void> subscriber) {
    				try {
    					application.handle(containerRequest); // TODO
    					subscriber.onCompleted();
    				} catch (Exception e) {
    					LOG.error("Failed to handle request.", e);
    					subscriber.onError(e);
    				}
    				finally {
    					try {
    						requestData.close();
    					}
    					catch( IOException e ) {
    						// NOOP
    					}
    				}
    			}
    		}).doOnTerminate(new Action0() {
    			@Override
    			public void call() {
    				response.close(true); // Since this runs in a different thread, it needs an explicit flush,
    				// else the LastHttpContent will never be flushed and the client will not finish.
    			}
    		}).subscribeOn(Schedulers.io());
    	} catch (Exception e) {
    		LOG.error("Ferrou!", e);
    		return null;
    	}

    }
    
    public SecurityContext getSecurityContext () {
		return  new SecurityContext() {
			
			@Override
			public boolean isUserInRole(String role) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public boolean isSecure() {
				// TODO Auto-generated method stub
				return true;
			}
			
			@Override
			public Principal getUserPrincipal() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public String getAuthenticationScheme() {
				// TODO Auto-generated method stub
				return null;
			}
		};
    }
    
    ContainerResponseWriter bridgeResponse(final HttpServerResponse<ByteBuf> serverResponse) {
        return new ContainerResponseWriter() {

            private final ByteBuf contentBuffer = serverResponse.getChannel().alloc().buffer();

            @Override
            public OutputStream writeResponseStatusAndHeaders(
					long contentLength, ContainerResponse response)
					throws ContainerException {
                int responseStatus = response.getStatus();
                serverResponse.setStatus(HttpResponseStatus.valueOf(responseStatus));
                HttpResponseHeaders responseHeaders = serverResponse.getHeaders();
                for(Map.Entry<String, List<Object>> header : response.getHeaders().entrySet()){
                    responseHeaders.setHeader(header.getKey(), header.getValue());
                }
                return new ByteBufOutputStream(contentBuffer);
            }

			@Override
			public boolean suspend(long timeOut, TimeUnit timeUnit,
					TimeoutHandler timeoutHandler) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void setSuspendTimeout(long timeOut, TimeUnit timeUnit)
					throws IllegalStateException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void commit() {
				serverResponse.writeAndFlush(contentBuffer); // TODO CHECK
			}

			@Override
			public void failure(Throwable error) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean enableResponseBuffering() {
				// TODO Auto-generated method stub
				return false;
			}
        };
    }

}
