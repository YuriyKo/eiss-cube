package eiss.cube.service.tcp.process;

import eiss.cube.randname.Randname;
import eiss.models.cubes.CubeCommand;
import eiss.models.cubes.CubeMeter;
import eiss.models.cubes.CubeTest;
import eiss.models.cubes.EISScube;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import xyz.morphia.Datastore;
import xyz.morphia.query.Query;
import xyz.morphia.query.UpdateOperations;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class CubeHandler implements Handler<NetSocket> {

    private static Map<String, String> clientMap = new HashMap<>();

    private Vertx vertx;
    private EventBus eventBus;
    private Datastore datastore;
    private Randname randname;

    @Inject
    public CubeHandler(Vertx vertx, Datastore datastore, Randname randname) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.randname = randname;

        eventBus = vertx.eventBus();
        eventBus.<JsonObject>consumer("eisscube", message -> {
            JsonObject json = message.body();

            String id = json.getString("id");
            String deviceId = json.getString("to");
            String command = json.getString("cmd");

            send(id, deviceId, command);
        });
        eventBus.<JsonObject>consumer("eisscubetest", message -> {
            JsonObject json = message.body();

            String deviceId = json.getString("to");
            String command = json.getString("cmd");

            sendNoStore(deviceId, command);
        });

    }

    // send command to EISScube (save record for history)
    private void send(final String id, final String deviceId, final String command) {
        vertx.executeBlocking(future -> {
            Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
            q.criteria("_id").equal(new ObjectId(id));

            UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);

            String writeHandlerID = clientMap.get(deviceId);
            if (writeHandlerID != null) {
                Buffer outBuffer = Buffer.buffer();
                outBuffer.appendString(command).appendString("\0");

                eventBus.send(writeHandlerID, outBuffer);

                ops.set("sent", Instant.now());
                ops.set("status", "Sending");
                datastore.update(q, ops);

                future.complete(String.format("DeviceID: %s is online. Sending message: %s", deviceId, command));
            } else {
                ops.set("status", "Pending");
                datastore.update(q, ops);

                future.fail(String.format("DeviceID: %s is offline. Pending message: %s", deviceId, command));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // send command to EISScube (no save record)
    private void sendNoStore(final String deviceId, final String command) {
        String writeHandlerID = clientMap.get(deviceId);
        if (writeHandlerID != null) {
            Buffer outBuffer = Buffer.buffer();
            outBuffer.appendString(command).appendString("\0");

            eventBus.send(writeHandlerID, outBuffer);

            log.info("DeviceID: %s is ONLINE. Sending message: %s", deviceId, command);
       } else {
            log.info("DeviceID: %s is OFFLINE. Dropped message: %s", deviceId, command);
        }
    }

    @Override
    public void handle(NetSocket socket) {
        StringBuilder received = new StringBuilder();
        socket.handler(buffer -> {
            String packet = buffer.getString(0, buffer.length());
            received.append(packet);
            if (received.indexOf("\0") != -1) {
                Arrays.stream(received.toString().split("\0")).forEach(message -> {
                    if (!message.isEmpty()) {
                        if (message.contains("auth")) { // auth
                            doAuth(socket, message);
                        } else if (message.equalsIgnoreCase("I")) { // ping-pong
                            doPing(socket);
                        } else { // other messages
                            parseMessage(socket, message);
                        }
                    }
                    received.delete(0, received.length());
                });
            }
        })
        .closeHandler(h -> {
            log.error("Socket closed");
            goOffline(socket);
        })
        .exceptionHandler(h -> {
            log.error("Socket problem: {}", h.getMessage());
            socket.close();
        });

        // let EISSCube 30 sec to establish connection and send "auth" request
        vertx.setTimer(30000, id -> {
            socket.write("auth\0");
            log.info("Client is connected!!! Waiting for auth...");
        });
    }

    private void doAuth(NetSocket socket, String message) {
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            String deviceID = parts[1]; // SIM card number
            if (deviceID != null) {
                clientMap.put(deviceID, socket.writeHandlerID());

                // Send greeting for a new connection.
                DateTimeFormatter df = DateTimeFormatter.ofPattern("z MM/dd/yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));
                String welcome = "Welcome to EISSCube Server!\n" + "Server time: " + df.format(Instant.now()) + "\n\n\0";
                socket.write(welcome );

                updateCube(deviceID);

                getPendingCommands(deviceID);
            }
        }
    }

    private void updateCube(String deviceID) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube == null) {
                cube = new EISScube();
                cube.setDeviceID(deviceID);
                cube.setName(randname.next()); // random name from dictionary
            }

            Instant timestamp = Instant.now();

            cube.setOnline(Boolean.TRUE);
            cube.setTimeStarted(timestamp);
            cube.setLastPing(timestamp);

            datastore.save(cube);

            future.complete();
        }, res -> log.info("DeviceID: {} is ONLINE", deviceID));
    }

    private void getPendingCommands(String deviceID) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube != null) {
                Query<CubeCommand> qc = datastore.createQuery(CubeCommand.class);
                qc.and(
                    qc.criteria("cubeID").equal(cube.getId()),
                    qc.criteria("status").equal("Pending")
                );
                qc.order("-created");

                List<CubeCommand> list = qc.asList();

                list.forEach(cmd ->
                    vertx.eventBus().send("eisscube", new JsonObject()
                        .put("id", cmd.getId().toString())
                        .put("to", deviceID)
                        .put("cmd", cmd.toString()))
                );
            }

            future.complete();
        }, res_future -> log.debug("Served pending commands for DeviceID: {} ", deviceID));
    }

    private void doPing(final NetSocket socket) {
        String deviceID = getDeviceID(socket.writeHandlerID());

        socket.write("O\0");

        Instant timestamp = Instant.now();
        vertx.executeBlocking(future -> {

            Query<EISScube> q = datastore.createQuery(EISScube.class);
            q.criteria("deviceID").equal(deviceID);

            UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                .set("online", Boolean.TRUE)
                .set("lastPing", timestamp);

            datastore.update(q, ops);
            future.complete();
        }, res -> log.info("Ping from DeviceID: {}", deviceID));
    }

    private void goOffline(final NetSocket socket) {
        String deviceID = getDeviceID(socket.writeHandlerID());

        if (!deviceID.equalsIgnoreCase("Unknown")) {
            vertx.executeBlocking(future -> {
                Query<EISScube> q = datastore.createQuery(EISScube.class);
                q.criteria("deviceID").equal(deviceID);

                UpdateOperations<EISScube> ops = datastore.createUpdateOperations(EISScube.class)
                    .set("online", Boolean.FALSE);

                datastore.update(q, ops);
                future.complete();
            }, res -> {
                log.debug("DeviceID: {} is OFFLINE", deviceID);
                clientMap.remove(deviceID);
            });
        } else {
            log.info("Socket is closed (Unknown DeviceID)");
        }
    }

    private String getDeviceID(final String writeHandlerID) {
        Optional<String> deviceID = clientMap.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equalsIgnoreCase(writeHandlerID))
            .map(Map.Entry::getKey).findFirst();

        return deviceID.orElse("Unknown");
    }

    private void parseMessage(final NetSocket socket, String message) {
        String deviceID = getDeviceID(socket.writeHandlerID());

        log.info("Message: {} from DeviceID: {}", message, deviceID);

        // all command acknowledgment contains 'id' - CubeCommand id
        if (message.contains("id")) {
            acknowledgeCommand(deviceID, message);
        }

        // all reports record contains 'ts' - timestamp
        if (message.contains("ts")) {
            saveReport(deviceID, message);
        }

        // status contains 'r', 'i', 'ss'
        if (message.contains("r") &&  message.contains("i") && message.contains("ss")) {
            saveStatus(deviceID, message);
        }

    }

    private void acknowledgeCommand(String deviceID, String message) {
        vertx.executeBlocking(future -> {
            String id = message.replace("id=", "");

            if (ObjectId.isValid(id)) {
                Query<CubeCommand> q = datastore.createQuery(CubeCommand.class);
                q.criteria("_id").equal(new ObjectId(id));

                UpdateOperations<CubeCommand> ops = datastore.createUpdateOperations(CubeCommand.class);
                ops.set("received", Instant.now());
                ops.set("status", "Received");
                datastore.update(q, ops);

                future.complete(String.format("DeviceID: %s acknowledge command id: %s", deviceID, id));
            } else {
                future.fail(String.format("DeviceID: %s failed to acknowledge command id: {}", deviceID, id));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    private void saveReport(String deviceID, String message) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube != null) {
                Instant ts = null;
                String v = null;
                String type = "pulse"; // default

                for (String part : message.split("&")) {
                    if (part.startsWith("ts=")) {
                        String timestamp = part.replace("ts=", "");
                        ts = Instant.ofEpochSecond(Long.valueOf(timestamp));
                    }
                    if (part.startsWith("v=")) {
                        v = part.replace("v=", "");
                    }
                    if (part.startsWith("dur=")) {
                        type = "cycle";
                        v = part.replace("dur=", "");
                    }
                }

                if (ts != null && v != null) {
                    CubeMeter cubeMeter = new CubeMeter();
                    cubeMeter.setCubeID(cube.getId());
                    cubeMeter.setType(type);
                    cubeMeter.setTimestamp(ts);
                    cubeMeter.setValue(Double.valueOf(v));

                    datastore.save(cubeMeter);
                    future.complete(String.format("DeviceID: %s - report saved into DB", deviceID));
                } else {
                    future.fail(String.format("DeviceID: %s - report failed to save into DB", deviceID));
                }
            } else {
                future.fail(String.format("DeviceID: %s not found", deviceID));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

    // r=on&i=high&ss=3
    private void saveStatus(String deviceID, String message) {
        Query<EISScube> q = datastore.createQuery(EISScube.class);
        q.criteria("deviceID").equal(deviceID);

        vertx.executeBlocking(future -> {
            EISScube cube = q.get();
            if (cube != null) {
                String r = null;
                String i = null;
                String ss = null;

                for (String part : message.split("&")) {
                    if (part.startsWith("r=")) {
                        r = part.replace("r=", "");
                    }
                    if (part.startsWith("i=")) {
                        i = part.replace("i=", "");
                    }
                    if (part.startsWith("ss=")) {
                        ss = part.replace("ss=", "");
                    }
                }

                if (r != null && i != null && ss != null) {
                    Query<CubeTest> qs = datastore.createQuery(CubeTest.class);
                    qs.criteria("cubeID").equal(cube.getId().toString());

                    UpdateOperations<CubeTest> ops = datastore.createUpdateOperations(CubeTest.class)
                            .set("r", Integer.valueOf(r))
                            .set("i", Integer.valueOf(i))
                            .set("ss", Integer.valueOf(ss));

                    datastore.update(qs, ops, true);
                    future.complete(String.format("DeviceID: %s - status saved into DB", deviceID));
                } else {
                    future.fail(String.format("DeviceID: %s - status failed to save into DB", deviceID));
                }
            } else {
                future.fail(String.format("DeviceID: %s not found", deviceID));
            }
        }, res -> {
            if (res.succeeded()) {
                log.info(res.result().toString());
            } else {
                log.info(res.cause().getMessage());
            }
        });
    }

}
