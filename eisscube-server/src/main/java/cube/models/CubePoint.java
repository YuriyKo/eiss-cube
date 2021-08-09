package cube.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import lombok.Data;

@Data
@Entity(useDiscriminator = false)
public class CubePoint {

    @Property Double lat;
    @Property Double lng;

}
