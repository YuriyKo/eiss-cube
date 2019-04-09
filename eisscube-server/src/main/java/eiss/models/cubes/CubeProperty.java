package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;

@Data
@Entity("cubeproperties")
public class CubeProperty {

    @Id ObjectId id;

    @Property String name;
    @Property String label;
    @Property String description;

}
