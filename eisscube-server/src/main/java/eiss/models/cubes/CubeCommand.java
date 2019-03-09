package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

import java.time.Instant;

@Data
@Entity("cubecommands")
public class CubeCommand {

    @Id ObjectId id;

    @Property String status;

    @Indexed(options = @IndexOptions(name = "CubeCommandsCreatedIndex", unique = true))
    @Property Instant created;

    @Property Instant sent;
    @Property Instant received;

    @Property ObjectId cubeID; // reference to EISScube.id

    @Property String command;

    @Property Instant startTime;
    @Property Instant endTime;

    @Property Integer completeCycle;
    @Property Integer dutyCycle;

    @Property String transition;


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        if (command != null && !command.isEmpty()) {
            b.append("c=").append(command);
        }

        if (startTime != null) {
            long start = startTime.getEpochSecond();
            b.append("&st=").append(String.format("%d", start));
        }

        if (endTime != null) {
            long end = endTime.getEpochSecond();

            long start;
            if (startTime != null) {
                start = startTime.getEpochSecond();
            } else {
                start = Instant.now().getEpochSecond();
            }

            b.append("&dur=").append(String.format("%d", end - start));
        }

        if (completeCycle != null && completeCycle != 0) {
            b.append("&each=").append(String.format("%d", completeCycle));
        }

        if (dutyCycle != null && dutyCycle != 0) {
            b.append("&pct=").append(String.format("%d", dutyCycle));
        }

        if (transition != null && !transition.isEmpty()) {
            b.append("&edge=").append(transition);
        }

        if (id != null) {
            b.append("&id=").append(id.toString());
        }

        return b.toString();
    }

}
