package netflix.karyon.jersey.blocking;

import javax.ws.rs.core.Application;

public class NettyContainer { // OK

    private final Application application; // com.sun.jersey.spi.container.WebApplication;
//    private final NettyToJerseyBridge nettyToJerseyBridge;

    public NettyContainer(Application application) {
        this.application = application;
//        nettyToJerseyBridge = new NettyToJerseyBridge(application);
    }

//    NettyToJerseyBridge getNettyToJerseyBridge() {
//        return nettyToJerseyBridge;
//    }

    Application getApplication() {
        return application;
    }
}
