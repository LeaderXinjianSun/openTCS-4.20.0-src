package org.opentcs.testvehicle;

import com.google.gson.Gson;
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
    private final SocketUtils socketUtils;

    @Inject
    public TestCommunicationAdapter(TestAdapterComponentsFactory componentsFactory,
                                    TestVehicleConfiguration configuration,
                                    @Assisted Vehicle vehicle) {
        super(new TestVehicleModel(vehicle),
                configuration.commandQueueCapacity(),
                1,
                configuration.rechargeOperation());
        this.componentsFactory = componentsFactory;
        this.vehicle = vehicle;
        String ip = vehicle.getProperty("IP");
        String port = vehicle.getProperty("Port");
        this.socketUtils = new SocketUtils(ip, port);
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
        testTask = new TestTask();
        Thread simThread = new Thread(testTask, getName() + "-Task");
        simThread.start();
        super.enable();
    }

    @Override
    public synchronized void disable() {
        if (!isEnabled()) {
            return;
        }
        //线程停止
        testTask.terminate();
        testTask = null;
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
                //获取状态  位置  速度  方向等
                TestCommCMD tcmd = new TestCommCMD();
                tcmd.cmd = "Read";
                tcmd.pathFrom = "";
                tcmd.pathTo = "";
                Gson gson = new Gson();
                String userJson = gson.toJson(tcmd);
                //String str = socketUtils.send("{\"cmd\":\"Read\",\"pathFrom\":\"\",\"pathTo\":\"\"}");
                String str = socketUtils.send(userJson);
                Thread.sleep(1000);
                if (str == null) {
                    //Thread.sleep(1000);
                    return;
                }
                TestCommREV trev = gson.fromJson(str, TestCommREV.class);

                String currentPoint = trev.message1;
                String currentStatus = trev.message2;
                getProcessModel().setVehiclePosition(currentPoint);
                if (currentStatus.equals("free")) {
                    getProcessModel().setVehicleState(Vehicle.State.IDLE);
                } else if (currentStatus.equals("executing")) {
                    getProcessModel().setVehicleState(Vehicle.State.EXECUTING);
                }


                final MovementCommand curCommand;
                synchronized (TestCommunicationAdapter.this) {
                    curCommand = getSentQueue().peek();
                }
                if (curCommand == null) {
                    Thread.sleep(1000);
                    return;
                }
                final Step curStep = curCommand.getStep();
                simulateMovement(curStep);
                if (!curCommand.isWithoutOperation()) {
                    simulateOperation(curCommand.getOperation());
                }
                if (isTerminated()) {
                    Thread.sleep(1000);
                    return;
                }
                if (getSentQueue().size() <= 1 && getCommandQueue().isEmpty()) {
                    getProcessModel().setVehicleState(Vehicle.State.IDLE);
                }
                synchronized (TestCommunicationAdapter.this) {
                    MovementCommand sentCmd = getSentQueue().poll();
                    if (sentCmd != null && sentCmd.equals(curCommand)) {
                        getProcessModel().commandExecuted(curCommand);
                        TestCommunicationAdapter.this.notify();
                    }
                }
                Thread.sleep(1000);
            } catch (Exception ex) {

            }
        }

        private void simulateMovement(Step step) throws Exception {
            if (step.getPath() == null) {
                return;
            }
            Vehicle.Orientation orientation = step.getVehicleOrientation();
            long pathLength = step.getPath().getLength();
            int maxVelocity;
            switch (orientation) {
                case BACKWARD:
                    maxVelocity = step.getPath().getMaxReverseVelocity();
                    break;
                default:
                    maxVelocity = step.getPath().getMaxVelocity();
                    break;
            }
            String pointName = step.getDestinationPoint().getName();
            getProcessModel().setVehicleState(Vehicle.State.EXECUTING);
            String currentPoint = "";
            String currentStatus = "";
            boolean flag = false;
            while (!flag) {
                TestCommCMD tcmd = new TestCommCMD();
                tcmd.cmd = "Path";
                tcmd.pathFrom = currentPoint;
                tcmd.pathTo = pointName;
                Gson gson = new Gson();
                String userJson = gson.toJson(tcmd);
                //String str = socketUtils.send("{\"cmd\":\"Path\",\"pathFrom\":\"" + currentPoint + "\",\"pathTo\":\"" + pointName + "\"}");
                String str = socketUtils.send(userJson);
                TestCommREV trev = gson.fromJson(str, TestCommREV.class);
                if (trev.message1.equals("OK") && trev.cmd.equals("Path")) {
                    flag = true;
                }
                Thread.sleep(1000);
            }

            while (!currentPoint.equals(pointName) && !isTerminated()) {
                TestCommCMD tcmd = new TestCommCMD();
                tcmd.cmd = "Read";
                tcmd.pathFrom = "";
                tcmd.pathTo = "";
                Gson gson = new Gson();
                String userJson = gson.toJson(tcmd);
                //String str = socketUtils.send("{\"cmd\":\"Read\",\"pathFrom\":\"\",\"pathTo\":\"\"}");
                String str = socketUtils.send(userJson);
                Thread.sleep(1000);
                if (str == null) {
                    //Thread.sleep(1000);
                    return;
                }

                TestCommREV trev = gson.fromJson(str, TestCommREV.class);
                currentPoint = trev.message1;
                currentStatus = trev.message2;
                getProcessModel().setVehiclePosition(currentPoint);
                if (currentStatus.equals("free")) {
                    getProcessModel().setVehicleState(Vehicle.State.IDLE);
                } else if (currentStatus.equals("executing")) {
                    getProcessModel().setVehicleState(Vehicle.State.EXECUTING);
                }
            }
        }

        private void simulateOperation(String operation) throws Exception {
            requireNonNull(operation, "operation");
            if (isTerminated()) {
                return;
            }
//      getProcessModel().setVehicleState(Vehicle.State.EXECUTING);
//      if (operation.equals(getProcessModel().getLoadOperation())) {
//        //getProcessModel().setVehicleLoadHandlingDevices(Arrays.asList(new LoadHandlingDevice(LHD_NAME, true)));
//      }
//      else if (operation.equals(getProcessModel().getUnloadOperation())) {
//        //getProcessModel().setVehicleLoadHandlingDevices(Arrays.asList(new LoadHandlingDevice(LHD_NAME, false)));
//      }
        }
    }

}
