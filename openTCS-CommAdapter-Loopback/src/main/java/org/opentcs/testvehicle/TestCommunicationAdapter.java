package org.opentcs.testvehicle;

import com.google.inject.assistedinject.Assisted;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.Route.Step;
import org.opentcs.drivers.vehicle.BasicVehicleCommAdapter;
import org.opentcs.drivers.vehicle.MovementCommand;
import org.opentcs.drivers.vehicle.VehicleProcessModel;
import org.opentcs.util.CyclicTask;
import org.opentcs.util.ExplainedBoolean;
import org.opentcs.virtualvehicle.LoopbackAdapterComponentsFactory;
import org.opentcs.virtualvehicle.VirtualVehicleConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import static java.util.Objects.requireNonNull;

public class TestCommunicationAdapter extends BasicVehicleCommAdapter {

    private TestAdapterComponentsFactory componentsFactory;
    private Vehicle vehicle;
    private boolean initialized;
    private CyclicTask testTask;

    @Inject
    public TestCommunicationAdapter(TestAdapterComponentsFactory componentsFactory,@Assisted Vehicle vehicle) {
        super(new TestVehicleModel(vehicle), 2, 1, "CHARGE");
        this.componentsFactory = componentsFactory;
        this.vehicle = vehicle;
    }




    @Override
    public void initialize() {
        initialized = true;
        //网络通信,获取当前位置，电量，等信息
        //getProcessModel().setVehicleState(Vehicle.State.IDLE);
        //getProcessModel().setVehiclePosition("Point-0001");
    }

    @Override
    public synchronized void enable() {
        if (isEnabled()) {
            return;
        }
        //开启线程(略)
        //testTask = new TestTask();
        //Thread simThread = new Thread(testTask, getName() + "-Task");
        //simThread.start();
        super.enable();
    }

    @Override
    public synchronized void disable() {
        if (!isEnabled()) {
            return;
        }
        //线程停止
        //testTask.terminate();
        //testTask = null;
        super.disable();
    }

    @Override
    public void sendCommand(MovementCommand cmd)
            throws IllegalArgumentException {
        requireNonNull(cmd, "cmd");
    }

    @Override
    public ExplainedBoolean canProcess(List<String> operations) {
        requireNonNull(operations, "operations");

        final boolean canProcess = isEnabled();
        final String reason = canProcess ? "" : "adapter not enabled";
        return new ExplainedBoolean(canProcess, reason);
    }

    @Override
    public void processMessage(Object message) {
    }

    @Override
    protected void connectVehicle() {

    }

    @Override
    protected void disconnectVehicle() {

    }

    @Override
    protected boolean isVehicleConnected() {
        return true;
    }

    /**
     * 内部类，用于处理运行步骤
     */
    private class TestTask
            extends CyclicTask {

        private TestTask() {
            super(0);
        }

        //线程执行
        @Override
        protected void runActualTask() {
            try {
                //获取状态  位置  速度  反向
                final MovementCommand curCommand;
                synchronized (TestCommunicationAdapter.this) {
                    curCommand = getSentQueue().peek();
                }
                final Step curStep = curCommand.getStep();
                //运行Step，略
                if (!curCommand.isWithoutOperation()) {
                    //运行操作（上料或者下料，略）
                }
                if (getSentQueue().size() <= 1 && getCommandQueue().isEmpty()) {
                    getProcessModel().setVehicleState(Vehicle.State.IDLE);
                }
                //更新UI
                synchronized (TestCommunicationAdapter.this) {
                    MovementCommand sentCmd = getSentQueue().poll();
                    if (sentCmd != null && sentCmd.equals(curCommand)) {
                        getProcessModel().commandExecuted(curCommand);
                        TestCommunicationAdapter.this.notify();
                    }
                }
            }
            catch (Exception ex) {

            }
        }
    }
}
