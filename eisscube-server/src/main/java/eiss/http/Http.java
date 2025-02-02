package eiss.http;

import eiss.config.ApiUserConfig;
import eiss.config.AppConfig;
import eiss.home.Config;
import eiss.config.ServerConfig;
import eiss.jwt.ExpiredTokenException;
import eiss.jwt.Jwt;
import eiss.api.ApiBuilder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import java.util.Base64;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Slf4j
public class Http extends AbstractVerticle {

    private final ServerConfig cfg;
    private final ApiUserConfig apiUser;
    private final Router router;
    private final ApiBuilder builder;
    private final Jwt jwt;

    private final Vertx vertx;
    private HttpServer server;

    @Inject
    public Http(AppConfig cfg, ApiBuilder builder, Jwt jwt, Vertx vertx) {
        this.cfg = cfg.getEissCubeConfig();
        this.apiUser = cfg.getApiUserConfig();
        this.router = Router.router(vertx);
        this.builder = builder;
        this.jwt = jwt;
        this.vertx = vertx;

        // build Routes based on annotations
        setupRoutes();
    }

    @Override
    public void start() throws Exception {

        int port = Integer.parseInt(cfg.getHttpPort());

        HttpServerOptions options = new HttpServerOptions()
            .setPort(port)
            .setLogActivity(FALSE)
            .setCompressionSupported(TRUE)
            .setTcpFastOpen(TRUE)
            .setTcpCork(TRUE)
            .setTcpQuickAck(TRUE)
            .setReusePort(TRUE);

        server = vertx.createHttpServer(options);

        server
            .requestHandler(router)
            .listen(h -> {
                if (h.succeeded()) {
                    log.info("Start HTTP server to listen on port: {}", port);
                } else {
                    log.error("Failed to start HTTP server on port: {}", port);
                }
            });
    }

    @Override
    public void stop() throws Exception {
        log.info("Stop HTTP server");
        server.close();
    }

    private void setupRoutes() {
        router.route()
            .handler(CorsHandler.create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.OPTIONS)

                .allowedHeader(HttpHeaderNames.ORIGIN.toString())
                .allowedHeader(HttpHeaderNames.CONTENT_TYPE.toString())
                .allowedHeader(HttpHeaderNames.AUTHORIZATION.toString())
                .allowedHeader(HttpHeaderNames.ACCEPT.toString())

                .exposedHeader("X-Total-Count")
            );

        router.route().handler(BodyHandler.create());

        router.route("/*").handler(context -> {
            HttpServerResponse response = context.response();
            String auth = context.request().getHeader("Authorization");
            if (auth != null) {
                if (auth.startsWith("Basic ")) {
                    try {
                        String decoded = new String(Base64.getDecoder().decode(auth.replaceAll("Basic ", "")));

                        String user, pass;
                        int colonIdx = decoded.indexOf(":");
                        if (colonIdx != -1) {
                            user = decoded.substring(0, colonIdx);
                            pass = decoded.substring(colonIdx + 1);
                        } else {
                            user = decoded;
                            pass = null;
                        }

                        if (!user.isEmpty() && user.equals(apiUser.getUsername())) {
                            if (pass != null && !pass.isEmpty() && pass.equals(apiUser.getPassword())) {
                                // store user and role in context
                                context.put("user", user);

                                // Call the next handler
                                context.next();
                            }
                        }
                    } catch (RuntimeException e) {
                        response.setStatusCode(SC_UNAUTHORIZED)
                                .setStatusMessage("Bad username or password")
                                .end();
                    }
               } else {
                    try {
                        jwt.decodeAuthHeader(Config.INSTANCE.getKey(), auth);

                    /* allow access only for roles:
                            export const SYSADMIN = "securityadmin";
                            export const ADMIN = "admin";
                            export const MANAGER = "manager";
                            export const OPERATOR = "operator";
                            export const VIEWER = "viewer";
                    */
                        String role = jwt.getRole();
                        if (role.equalsIgnoreCase("securityadmin") ||
                                role.equalsIgnoreCase("admin") ||
                                role.equalsIgnoreCase("manager") ||
                                role.equalsIgnoreCase("operator") ||
                                role.equalsIgnoreCase("viewer"))
                        {
                            // store user and role in context
                            context.put("user_id", jwt.getUser_id());
                            context.put("group_id", jwt.getGroup_id());
                            context.put("role", role);

                            // Call the next handler
                            context.next();
                        } else {
                            response.setStatusCode(SC_UNAUTHORIZED)
                                    .setStatusMessage("Invalid role of user")
                                    .end();
                        }

                    } catch (ExpiredTokenException ex) {
                        response.setStatusCode(SC_UNAUTHORIZED)
                                .setStatusMessage("Token expired")
                                .end();
                    } catch (IllegalArgumentException ex) {
                        response.setStatusCode(SC_UNAUTHORIZED)
                                .setStatusMessage("Invalid token")
                                .end();
                    }
                }
            } else {
                response.setStatusCode(SC_UNAUTHORIZED)
                        .setStatusMessage("Unauthorized")
                        .end();
            }
        });

        builder.build(router);
    }

}
