package trazzo.back.corehr.domain.specification;

import trazzo.back.corehr.domain.model.attendance.Device;

public class DeviceActiveSpec {

    public boolean isSatisfiedBy(Device device) {
        return device != null && device.isState();
    }
}
