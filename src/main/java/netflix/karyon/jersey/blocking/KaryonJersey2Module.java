package netflix.karyon.jersey.blocking;

import io.netty.buffer.ByteBuf;
import netflix.karyon.transport.http.KaryonHttpModule;

public abstract class KaryonJersey2Module extends KaryonHttpModule<ByteBuf, ByteBuf> { // OK

    public KaryonJersey2Module() {
        super("karyonJerseyModule", ByteBuf.class, ByteBuf.class);
    }

    protected KaryonJersey2Module(String moduleName) {
        super(moduleName, ByteBuf.class, ByteBuf.class);
    }

    @Override
    protected void configure() {
        bindRouter().to(Jersey2BasedRouter.class);
        super.configure();
    }
}
