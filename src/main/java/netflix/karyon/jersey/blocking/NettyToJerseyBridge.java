package netflix.karyon.jersey.blocking;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NettyToJerseyBridge {

    private static final Logger logger = LoggerFactory.getLogger(NettyToJerseyBridge.class);

    private final ApplicationHandler application;

    NettyToJerseyBridge(ApplicationHandler application) {
        this.application = application;
    }
    

//    ContainerRequest bridgeRequest(final HttpServerRequest<ByteBuf> nettyRequest, InputStream requestData ) {
//        try {
//            URI baseUri = new URI("/"); // Since the netty server does not have a context path element as such, so base uri is always /
//            URI uri = new URI(nettyRequest.getUri());
//            return new ContainerRequest(baseUri, uri, nettyRequest.getHttpMethod().name(), null, null); // TODO
////            		SecurityContext, PropertiesDelegate);
////          return new ContainerRequest(application, nettyRequest.getHttpMethod().name(),
////           	 								baseUri, uri, new JerseyRequestHeadersAdapter(nettyRequest.getHeaders()),
////            									requestData );
//        } catch (URISyntaxException e) {
//            logger.error(String.format("Invalid request uri: %s", nettyRequest.getUri()), e);
//            throw new IllegalArgumentException(e);
//        }
//    }

}
