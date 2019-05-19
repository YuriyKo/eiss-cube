package eiss.cube.service.tcp;

import com.google.inject.AbstractModule;
import eiss.cube.service.tcp.process.CubeClientHandler;

public class TcpModule extends AbstractModule {

    protected void configure() {
        bind(Tcp.class).asEagerSingleton();
        bind(CubeClientHandler.class).asEagerSingleton();
    }

}
