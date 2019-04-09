package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static eiss.utils.reactadmin.ParamName.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/commands")
public class ListRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public ListRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);

        // search
        String search = request.getParam(FILTER);
        if (search != null && !search.isEmpty()) {
            search = search.toLowerCase()
                    .replaceAll(" ", "")
                    .replaceAll("relay", "r")
                    .replaceAll("cointing", "i")
                    .replaceAll("cycle", "cyc")
                    .replaceAll("pulse", "cp")
                    .replaceAll("stop", "off");

            q.field("command").containsIgnoreCase(search);
        }
        // ~search

        // filters
        String cubeID = request.getParam("cubeID");
        if (cubeID != null && !cubeID.isEmpty()) {
            if (ObjectId.isValid(cubeID)) {
                q.field("cubeID").equal(new ObjectId(cubeID));
            }
        }
        String sinceTime = request.getParam("timestamp_gte");
        if (sinceTime != null && !sinceTime.isEmpty()) {
            q.field("created").greaterThanOrEq(Instant.parse(sinceTime));
        }
        String beforeTime = request.getParam("timestamp_lte");
        if (beforeTime != null && !beforeTime.isEmpty()) {
            q.field("created").lessThanOrEq(Instant.parse(beforeTime));
        }
        // ~filters

        // sorts
        String sort = request.getParam(SORT);
        String order = request.getParam(ORDER);
        if (sort != null && order != null && !sort.isEmpty() && !order.isEmpty()) {
            q.order(order.equalsIgnoreCase(ASC) ? sort : "-" + sort);
        } else {
            q.order("created");
        }

        // skip/limit
        FindOptions o = new FindOptions();
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(Integer.valueOf(s)).limit(Integer.valueOf(e));
        }

        vertx.executeBlocking(op -> {
            List<CubeCommand> result = q.asList(o);
            op.complete(result);
        }, res -> {
            if (res.succeeded()) {
                vertx.executeBlocking(c -> {
                    Long result = q.count();
                    c.complete(result);
                }, c -> {
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .putHeader("X-Total-Count", String.valueOf(c.result()))
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
                });
            } else {
                response
                    .setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Cannot get list of Commands")
                    .end();
            }
        });
    }

}
