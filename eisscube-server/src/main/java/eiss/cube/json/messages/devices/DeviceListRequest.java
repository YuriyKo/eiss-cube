package eiss.cube.json.messages.devices;

import lombok.Data;

@Data
public class DeviceListRequest {

	private Integer start = 0;
	private Integer limit = 10;

}
