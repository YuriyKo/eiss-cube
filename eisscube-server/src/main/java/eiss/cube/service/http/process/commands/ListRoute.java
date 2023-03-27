package eiss.cube.service.http.process.commands;

import com.google.gson.Gson;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeCommand;
import eiss.db.Cubes;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.time.Instant;
import java.util.List;

import static dev.morphia.query.Sort.ascending;
import static dev.morphia.query.Sort.descending;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.filters.Filters.regex;
import static eiss.utils.reactadmin.ParamName.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.lang.Integer.parseInt;
import static jakarta.servlet.http.HttpServletResponse.*;
import static org.bson.types.ObjectId.isValid;

@Slf4j
@Api
@Path("/commands")
public class ListRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;

    @Inject
    public ListRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @GET
    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        Query<CubeCommand> q = datastore.find(CubeCommand.class);
        FindOptions o = new FindOptions();

        if (context.get("role").equals("securityadmin")) {
            // filters
            String group_id = request.getParam("group_id");
            if (group_id != null) {
                q.filter(eq("group_id", group_id));
            }
            // ~filters
        } else {
            q.filter(eq("group_id", context.get("group_id")));
        }

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

            q.filter(regex("command").pattern("^" + search).caseInsensitive());;
        }
        // ~search

        // filters
        String cubeID = request.getParam("cubeID");
        if (cubeID != null && !cubeID.isEmpty()) {
            if (isValid(cubeID)) {
                q.filter(eq("cubeID", new ObjectId(cubeID)));
            }
        }
        String sinceTime = request.getParam("timestamp_gte");
        if (sinceTime != null && !sinceTime.isEmpty()) {
            q.filter(gte("created", Instant.parse(sinceTime)));
        }
        String beforeTime = request.getParam("timestamp_lte");
        if (beforeTime != null && !beforeTime.isEmpty()) {
            q.filter(lte("created", Instant.parse(beforeTime)));
        }
        // ~filters

        // sorts
        String byField = request.getParam(SORT);
        String order = request.getParam(ORDER);
        if (byField != null && order != null && !byField.isEmpty() && !order.isEmpty()) {
            o.sort(order.equalsIgnoreCase(ASC) ? ascending(byField) : descending(byField));
        } else {
            o.sort(ascending("created"));
        }
        // ~sorts

        // projections
        o.projection().include("cubeID", "cubeName", "command", "status", "created", "group", "group_id");
        // ~projections

        // skip/limit
        String s = request.getParam(START);
        String e = request.getParam(END);
        if (s != null && e != null && !s.isEmpty() && !e.isEmpty()) {
            o.skip(parseInt(s)).limit(parseInt(e) - parseInt(s));
        }
        // ~skip/limit

        vertx.executeBlocking(op -> {
            List<CubeCommand> list = q.iterator(o).toList();
            op.complete(list);
        }, res -> {
            if (res.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .putHeader("X-Total-Count", String.valueOf(q.count()))
                        .setStatusCode(SC_OK)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });
    }

}
