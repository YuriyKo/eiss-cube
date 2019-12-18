package eiss.cube.service.http.process.eiss_api.commands;

import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import eiss.cube.json.messages.commands.Command;
import eiss.cube.json.messages.commands.CommandListForDeviceRequest;
import eiss.cube.json.messages.commands.CommandListRequest;
import eiss.cube.json.messages.commands.CommandListResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.CubeCommand;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/eiss-api/commands/listfordevice")
public class ListForDeviceRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public ListForDeviceRoute(Vertx vertx, Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.getBodyAsString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    CommandListForDeviceRequest req = gson.fromJson(jsonBody, CommandListForDeviceRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        CommandListResponse res = getListOfCommandsForDevice(req);
                        op.complete(gson.toJson(res));
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end((String)res.result());
                } else {
                    response.setStatusCode(SC_INTERNAL_SERVER_ERROR)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                .end();
        }
    }

    private CommandListResponse getListOfCommandsForDevice(CommandListForDeviceRequest req) {
        CommandListResponse rc = new CommandListResponse();

        Query<CubeCommand> commands = datastore.createQuery(CubeCommand.class);

        // filter
        commands.criteria("cubeID").equal(new ObjectId(req.getDeviceID()));

        // projections

        // skip/limit
        FindOptions options = new FindOptions();
        Integer s = req.getStart();
        Integer l = req.getLimit();
        if (s != null && l != null) {
            options.skip(s).limit(l);
        }

        // get & convert
        commands.find(options).toList().forEach(c -> {
            rc.getCommands().add(
                Command.builder()
                        .id(c.getId().toString())
                        .deviceID(c.getCubeID().toString())
                        .command(c.getCommand())
                        .completeCycle(c.getCompleteCycle())
                        .dutyCycle(c.getDutyCycle())
                        .transition(c.getTransition())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .sent(c.getSent())
                        .created(c.getCreated())
                        .received(c.getReceived())
                        .status(c.getStatus())
                    .build()
                );
            }
        );

        // total number of records
        rc.setTotal(commands.count());

        return rc;
    }

}
