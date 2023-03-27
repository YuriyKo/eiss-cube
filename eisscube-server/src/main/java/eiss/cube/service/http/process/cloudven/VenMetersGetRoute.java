package eiss.cube.service.http.process.cloudven;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.api.Api;
import eiss.models.cubes.CubeSetup;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

import static dev.morphia.query.filters.Filters.eq;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/cloudven/meters/{ven}")
public class VenMetersGetRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;

    @Inject
    public VenMetersGetRoute(Vertx vertx, @Cubes Datastore datastore) {
        this.vertx = vertx;
        this.datastore = datastore;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String ven = context.request().getParam("ven");

        vertx.executeBlocking(op -> {
            JsonArray meters = new JsonArray();

            Query<EISScube> q = datastore.find(EISScube.class);
            q.filter(eq("settings.VEN", ven));

            List<EISScube> cubes = q.iterator().toList();
            if (cubes != null) {
                cubes.forEach(cube -> {
                    CubeSetup setup = datastore.find(CubeSetup.class).filter(eq("cubeID", cube.getId())).first();
                    if (setup != null && setup.getInput() != null) {
                        if (setup.getInput().getConnected()) {
                            meters.add(cube.getName());
                        }
                    }
                });
            }

            op.complete(meters.encodePrettily());
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(String.valueOf(res.result()));
            }
        });
    }

}
