package eiss.cube.service.http.process.eiss_api.properties;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyRequest;
import eiss.cube.json.messages.properties.PropertyResponse;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/properties/new")
public class NewRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public NewRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.body().asString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    PropertyRequest req = gson.fromJson(jsonBody, PropertyRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        PropertyResponse res = newProperty(req);
                        op.complete(res);
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end(gson.toJson(res.result()));
                } else {
                    response.setStatusCode(SC_BAD_REQUEST)
                            .setStatusMessage(res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
        }
    }

    private PropertyResponse newProperty(PropertyRequest req) {
        PropertyResponse rc = new PropertyResponse();

        CubeProperty property = new CubeProperty();
        property.setName(req.getProperty().getName());
        property.setLabel(req.getProperty().getLabel());
        property.setDescription(req.getProperty().getDescription());

        CubeProperty p = datastore.save(property);
        if (p != null) {
            rc.setProperty(
                Property.builder()
                    .id(p.getId().toString())
                    .name(p.getName())
                    .label(p.getLabel())
                    .description(p.getDescription())
                .build()
            );
        }

        return rc;
    }


}
