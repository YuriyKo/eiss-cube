package eiss.cube.service.http.process.eiss_api.properties;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.json.messages.properties.Property;
import eiss.cube.json.messages.properties.PropertyRequest;
import eiss.cube.json.messages.properties.PropertyResponse;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperator;
import dev.morphia.query.experimental.updates.UpdateOperators;
import eiss.api.Api;
import eiss.models.cubes.CubeProperty;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/eiss-api/properties/edit")
public class EditRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public EditRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
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
                        PropertyResponse res = editProperty(req);
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

    private PropertyResponse editProperty(PropertyRequest req) {
        PropertyResponse rc = new PropertyResponse();

        String id = req.getProperty().getId();
        if (ObjectId.isValid(id)) {
            Query<CubeProperty> property = datastore.find(CubeProperty.class);
            property.filter(Filters.eq("_id", new ObjectId(id)));

            List<UpdateOperator> updates = new ArrayList<>();
            updates.add(UpdateOperators.set("name", req.getProperty().getName()));
            updates.add(UpdateOperators.set("label", req.getProperty().getLabel()));
            updates.add(UpdateOperators.set("description", req.getProperty().getDescription()));

            property.update(updates.get(0), updates.stream().skip(1).toArray(UpdateOperator[]::new)).execute();

            // get an updated version
            CubeProperty p = property.first();
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
        }

        return rc;
    }


}
