package org.opentcs.testvehicle;

import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.VehicleProcessModel;

import javax.annotation.Nonnull;

public class TestVehicleModel extends VehicleProcessModel {
    /**
     * Creates a new instance.
     *
     * @param attachedVehicle The vehicle attached to the new instance.
     */
    public TestVehicleModel(@Nonnull Vehicle attachedVehicle) {
        super(attachedVehicle);
    }
}
