package eiss.cube.service.http.process.lora;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
import eiss.cube.db.Cube;
import eiss.cube.db.Eiss;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.LORAcube;
import eiss.models.eiss.Group;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.toMap;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/lora/{id}")
public class PutRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore eissDatastore;
    private final Datastore cubeDatastore;
    private final Gson gson;

    @Inject
    public PutRoute(Vertx vertx, @Eiss Datastore eissDatastore, @Cube Datastore cubeDatastore, Gson gson) {
        this.vertx = vertx;
        this.eissDatastore = eissDatastore;
        this.cubeDatastore = cubeDatastore;
        this.gson = gson;
    }

    @PUT
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String id = request.getParam("id");
        if (!ObjectId.isValid(id)) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("id '%s' is not valid", id))
                    .end();
            return;
        }

        String json = context.getBodyAsString();
        log.info("Update existing LORAcube: {} by: {}", id, json);
        LORAcube cube = gson.fromJson(json, LORAcube.class);
        if (cube == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage(String.format("Unable to update LORAcube with id: %s", id))
                    .end();
            return;
        }

        vertx.executeBlocking(cube_op -> {
            Query<LORAcube> q = cubeDatastore.createQuery(LORAcube.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<LORAcube> ops = cubeDatastore.createUpdateOperations(LORAcube.class);

            if (cube.getName() == null) {
                ops.unset("name");
            } else {
                ops.set("name", cube.getName());
            }

            if (cube.getAddress() == null) {
                ops.unset("address");
            } else {
                ops.set("address", cube.getAddress());
            }

            if (cube.getCity() == null) {
                ops.unset("city");
            } else {
                ops.set("city", cube.getCity());
            }

            if (cube.getZipCode() == null) {
                ops.unset("zipCode");
            } else {
                ops.set("zipCode", cube.getZipCode());
            }

            if (cube.getCustomerID() == null) {
                ops.unset("customerID");
            } else {
                ops.set("customerID", cube.getCustomerID());
            }

            if (cube.getZone() == null) {
                ops.unset("zone");
            } else {
                ops.set("zone", cube.getZone());
            }

            if (cube.getSubZone() == null) {
                ops.unset("subZone");
            } else {
                ops.set("subZone", cube.getSubZone());
            }

            if (cube.getSettings() == null) {
                ops.unset("settings");
            } else {
                // do not keep an empty string (convert it to null
                Map<String, Object> settings = cube.getSettings()
                    .entrySet()
                    .stream()
                    .filter(entry -> ((entry.getValue() instanceof String) && !(((String) entry.getValue()).isEmpty()))
                    ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                cube.setSettings(new BasicDBObject(settings));

                ops.set("settings", cube.getSettings());
            }

            // put cube under group
            Query<Group> groupQuery = eissDatastore.createQuery(Group.class);
            if (cube.getGroup_id() != null && !cube.getGroup_id().isEmpty()) {
                ops.set("group_id", cube.getGroup_id());
                groupQuery.criteria("_id").equal(new ObjectId(cube.getGroup_id()));
                Group group = groupQuery.first();
                if (group != null) {
                    ops.set("group", group.getName());
                } else {
                    ops.unset("group");
                }
            } else if (cube.getGroup() != null && !cube.getGroup().isEmpty()) {
                ops.set("group", cube.getGroup());
                groupQuery.criteria("name").equal(cube.getGroup());
                Group group = groupQuery.first();
                if (group != null) {
                    ops.set("group_id", group.getId().toString());
                } else {
                    ops.unset("group_id");
                }
            }
            // ~put cube under group

            UpdateResults result = cubeDatastore.update(q, ops);
            if (result.getUpdatedCount() == 1) {
                cube_op.complete(gson.toJson(cube));
            } else {
                cube_op.fail(String.format("Unable to update LORAcube with id: %s", id));
            }
        }, cube_res -> {
            if (cube_res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(SC_OK)
                        .end(String.valueOf(cube_res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(cube_res.cause().getMessage())
                        .end();
            }
        });
    }

}
