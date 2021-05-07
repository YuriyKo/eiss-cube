package cube.service.http.process.reports;

import com.google.gson.Gson;
import cube.db.Cube;
import dev.morphia.query.experimental.filters.Filters;
import eiss.api.Api;
import cube.models.CubeReport;
import cube.models.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/reports/{id}")
public class GetRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public GetRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!ObjectId.isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id: %s is not valid", id))
                    .end();
            return;
        }

        Query<CubeReport> q = datastore.find(CubeReport.class);
        q.filter(Filters.eq("_id", new ObjectId(id)));

        vertx.executeBlocking(op -> {
            CubeReport report = q.first();
            if (report != null) {
                op.complete(gson.toJson(report));
            } else {
                op.fail(String.format("CubeReport: %s not found", id));
            }
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(String.valueOf(res.result()));
            } else {
                response.setStatusCode(SC_NOT_FOUND)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
