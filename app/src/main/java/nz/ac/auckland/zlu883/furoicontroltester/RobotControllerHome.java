package nz.ac.auckland.zlu883.furoicontroltester;

import android.util.Log;

/**
 * Created by zhlucc on 12/14/2015.
 */
public class RobotControllerHome implements BluetoothListener {


    private static RobotControllerHome INSTANCE;

    private static final String logTag = "Robot controller(home)";
    private static final byte robotControllerId = 0x00;

    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private BluetoothManager btManager;
    private byte[] btBuffer = new byte[1024];
    private int bufferCount = 0;

    // Robot status info
    public enum RobotMotion {OFF, SECURE, IDLE, STOP, ALARM, LOVE, DANCE, SURPRISE, HI, SAD}
    private RobotMotion currentMotion = RobotMotion.OFF;
    private boolean obstacleAvoidance = true;
    private int batteryPercent = 0;
    private int humidityPercent = 0;
    private int tempCelsius = 0;
    private int[] distanceSensors = new int[10]; // 10 in total
    private String robotMacAddress = "";
    private int[][] ledColor = new int[12][3]; // [r, g, b], 12 in total
    private long encoderCount = 0;
    private int angularVel = 0;
    private int linearVel= 0;
    private boolean[] robotStatus = new boolean[9]; // [flags: boot finished, sd card detected,
        // bluetooth fault check, bluetooth a2dp connected, bluetooth spp connected, left motor fault,
        // left motor operation check, right motor fault check, right motor operation check]
    private int serialNumber = 0;
    private int buildNumber = 0;
    private String buildDate = "";
    private String firmwareVersion = "";
    private String deviceId = "";

    private RobotControllerHome(){
        btManager = BluetoothManager.getInstance();
        btManager.registerListener(this);
    }

    public static RobotControllerHome getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RobotControllerHome();
        }
        return INSTANCE;
    }

    public void processRobotMessage(byte[] message) {
        if (checkCRC(message) && message.length == 8) {
            switch (message[1]) {
                case (0x00):
                    processId(message);
                    break;
                case (0x01):
                    processFirmwareVersion(message);
                    break;
                case (0x02):
                    processBuildDate(message);
                    break;
                case (0x03):
                    processBuildNumber(message);
                    break;
                case (0x04):
                    processSerialNumber(message);
                    break;
                case (0x05):
                    processRobotStatus(message);
                    break;
                case (0x23):
                    processMotorVelocity(message);
                    break;
                case (0x26):
                    processEncoderCount(message);
                    break;
                case (0x59):
                    processRobotMacAddress(message);
                    break;
                case (0x60):
                    processDistanceSensors(message);
                    break;
                case (0x64):
                    processTemperatureSensor(message);
                    break;
                case (0x65):
                    processHumiditySensor(message);
                    break;
                case (0x66):
                    processBatteryInfo(message);
                    break;
                default:
                    Log.i(logTag, "Bad message type");
                    break;
            }
        }
    }

    private boolean checkCRC(byte[] message) {
        byte crc = (byte) 0x00;
        for(int i = 0; i < message.length - 1; i++) {
            crc += message[i];
        }
        if(message[message.length-1] == ~crc) {
            return true;
        } else {
            return false;
        }
    }

    public void dataReceived(byte[] data, int byteCount) {
        int i = 0;
        while (i < byteCount) {
            btBuffer[bufferCount] = data[i];
            if (bufferCount > 2 && (btBuffer[bufferCount] == (byte) 0x54) && (btBuffer[bufferCount-1] == (byte) 0x55)) {
                byte[] message = new byte[bufferCount-1];
                for (int j = 0; j < bufferCount - 1; j++) {
                    message[j] = btBuffer[j];
                }
                processRobotMessage(message);
                bufferCount = 0;
            } else {
                bufferCount++;
            }
            i++;
        }
    }

    public void btConnect() {
        btManager.btConnect();
    }

    public void btDisconnect() {
        btManager.btDisconnect();
    }

    private void processId(byte[] message) {

    }

    private void processFirmwareVersion(byte[] message) {

    }

    private void processBuildDate(byte[] message) {

    }

    private void processBuildNumber(byte[] message) {

    }

    private void processSerialNumber(byte[] message) {

    }

    private void processRobotStatus(byte[] message) {

    }

    private void processBatteryInfo(byte[] message) {

    }

    private void processRobotMacAddress(byte[] message) {

    }

    private void processMotorVelocity(byte[] message) {

    }

    private void processEncoderCount(byte[] message) {

    }

    private void processDistanceSensors(byte[] message) {

    }

    private void processTemperatureSensor(byte[] message) {

    }

    private void processHumiditySensor(byte[] message) {

    }

    private byte[] constructCommand(byte[] data, boolean writeRequest) {
        byte[] command = new byte[7];
        command[0] = robotControllerId;
        if (writeRequest)
            command[1] = (byte) (data[0] + 0x80);
        else
            command[1] = data[0];
        for (int i = 2; i < 7; i++) {
            command[i] = data[i-1];
        }
        return command;
    }

    public void sendCmdMessage (byte[] command) {
        byte[] cmdMessage = new byte[command.length + 3];
        cmdMessage[0] = (byte) 0x55;
        cmdMessage[1] = (byte) 0x54;
        for (int i = 0; i < command.length; i++) {
            cmdMessage[i+2] = command[i];
        }
        cmdMessage[cmdMessage.length-1] = makeCRC(command);
        btManager.btSend(cmdMessage);
    }

    private byte makeCRC(byte[] command) {
        byte val = (byte) 0x00;
        for (int i = 0; i < command.length; i++) {
            val += command[i];
        }
        return (byte) ~val;
    }



    // ---------------------------------------------------------------------
    // The functions below serve as the API for the robot controller
    // ---------------------------------------------------------------------

    public void setRobotMotion(RobotMotion motion) {
        byte[] data = new byte[6];
        data[0] = 0x21;
        data[1] = 0x00;
        if (motion == RobotMotion.OFF) {
            data[2] = 0x00;
        } else if (motion == RobotMotion.SECURE) {
            data[2] = 0x0A;
        } else if (motion == RobotMotion.IDLE) {
            data[2] = 0x11;
        } else if (motion == RobotMotion.STOP) {
            data[2] = 0x12;
        } else if (motion == RobotMotion.ALARM) {
            data[2] = 0x13;
        } else if (motion == RobotMotion.LOVE) {
            data[2] = 0x15;
        } else if (motion == RobotMotion.DANCE) {
            data[2] = 0x17;
        } else if (motion == RobotMotion.SURPRISE) {
            data[2] = 0x20;
        } else if (motion == RobotMotion.HI) {
            data[2] = 0x21;
        } else if (motion == RobotMotion.SAD) {
            data[2] = 0x22;
        }
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;
        sendCmdMessage(constructCommand(data, true));
    }

    public void setMotorVelocity(int linearVel, int angularVel, int duration) {
        byte[] data = new byte[6];
        data[0] = 0x22;
        data[1] = 0x00;
        data[2] = (byte) linearVel;
        data[3] = (byte) angularVel;
        data[4] = (byte) duration;
        data[5] = (byte) (duration >> 8);
        sendCmdMessage(constructCommand(data, true));
    }

    public void setLedColor(int red, int green, int blue, int position) {
        byte[] data = new byte[6];
        data[0] = 0x56;
        data[1] = (byte) position;
        data[2] = (byte) red;
        data[3] = (byte) green;
        data[4] = (byte) blue;
        data[5] = 0x00;
        sendCmdMessage(constructCommand(data, true));
    }

    public void setObstacleAvoidance(boolean enable) {
        byte[] data = new byte[6];
        data[0] = 0x1F;
        data[1] = 0x00;
        if (enable) {
            data[2] = 0x01;
        } else {
            data[2] = 0x00;
        }
        data[3] = 0x00;
        data[4] = 0x00;
        data[5] = 0x00;
        sendCmdMessage(constructCommand(data, true));
    }

    public void setBuildDate(int year, int month) {

    }

    public void setBuildNumber(int buildNumber) {

    }

    public void setSerialNumber(int serialNumber) {

    }

    public void setRobotMacAddress(String macAddress) {

    }

    public String[] requestEncoderCount() {
        return null;
    }

    public String[] requestCurrentVelocity() {
        return null;
    }

    public String[] requestDistanceSensor() {
        return null;
    }

    public String[] requestTemperatureSensor() {
        return null;
    }

    public String[] requestHumiditySensor() {
        return null;
    }

    public String[] requestBatteryInfo() {
        return null;
    }

}
