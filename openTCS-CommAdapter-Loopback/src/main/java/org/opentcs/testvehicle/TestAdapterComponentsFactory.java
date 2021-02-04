package org.opentcs.testvehicle;

import org.opentcs.data.model.Vehicle;

public interface TestAdapterComponentsFactory {
    TestCommunicationAdapter createTestCommAdapter(Vehicle vehicle);
}
