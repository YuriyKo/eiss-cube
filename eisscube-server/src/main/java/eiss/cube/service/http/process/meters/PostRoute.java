package eiss.cube.service.http.process.meters;

import com.google.gson.Gson;
import eiss.cube.input.Conversion;
import eiss.api.Api;
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
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/meters")
public class PostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Gson gson;
    private final Conversion conversion;

    @Inject
    public PostRoute(Vertx vertx, Gson gson, Conversion conversion) {
        this.vertx = vertx;
        this.gson = gson;
        this.conversion = conversion;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.body().asString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    MeterRequest meterRequest = gson.fromJson(jsonBody, MeterRequest.class);
                    if (meterRequest == null) {
                        op.fail("Bad request");
                    } else {
                        MeterResponse meterResponse = new MeterResponse();

                        conversion.process(meterRequest, meterResponse);

                        op.complete(meterResponse);
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

}
