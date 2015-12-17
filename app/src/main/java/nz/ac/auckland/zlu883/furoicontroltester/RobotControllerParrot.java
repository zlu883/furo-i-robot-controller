package nz.ac.auckland.zlu883.furoicontroltester;

import android.text.method.Touch;
import android.util.Log;

/**
 * Controller class used to construct/decipher bluetooth messages for communicating
 * with the Furo-i Parrot. Also keeps tack of the robot's status.
 *
 * Created by Jonny Lu on 12/14/2015.
 */
public class RobotControllerParrot implements BluetoothListener {

    private static RobotControllerParrot INSTANCE;

    public static int LEFT = 1;
    public static int RIGHT = 2;

    private String logTag = "Robot controller(parrot)";

    private BluetoothManager btManager;
    private byte[] btBuffer = new byte[1024];
    private int bufferCount = 0;

    // Robot status info
    public enum PsdRange {CLOSE, MID, FAR}
    public enum TouchPattern {NONE, HIT, PAT, RUB, PUSH}
    private PsdRange[] psdSensors = {PsdRange.FAR, PsdRange.FAR, PsdRange.FAR, PsdRange.FAR, PsdRange.FAR}; // [front left, front right, back right, back center, back left]
    private boolean[] cliffSensors = {false, false, false, false}; // [front left, front right, back right, back left]
    private boolean[] touchSensors = {false, false, false, false, false, false, false, false, false};
    private TouchPattern touchPattern = TouchPattern.NONE;
    private int batteryLevel = 0;
    private int batteryVoltage = 0;
    private boolean[] ledPatterns = {false, false, false, false, false}; // [head red, head green, head blue, wing red, wing green, wing blue]
    private long encoderCountLeft = 0;
    private long encoderCountRight = 0;

    public static RobotControllerParrot getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RobotControllerParrot();
        }
        return INSTANCE;
    }

    private RobotControllerParrot(){
        btManager = BluetoothManager.getInstance();
        btManager.registerListener(this);
    }

    public void processRobotMessage(byte[] message) {
        if (checkCRC(message)) {
            switch (message[0]) {
                case (0x16):
                    processTouchSensor(message);
                    break;
                case (0x17):
                    processPsdSensor(message);
                    break;
                case (0x18):
                    processBatteryInfo(message);
                    break;
                case (0x2e):
                    processEncoderCount(message);
                    break;
                default:
                    Log.i(logTag, "Bad message type");
                    break;
            }
        } else {
            Log.i(logTag, "Bad message CRC");
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

    private void processTouchSensor(byte[] message) {
        switch (message[4]) {
            case (0x00): touchPattern = TouchPattern.NONE; break;
            case (0x01): touchPattern = TouchPattern.HIT; break;
            case (0x02): touchPattern = TouchPattern.PAT; break;
            case (0x03): touchPattern = TouchPattern.RUB; break;
            case (0x04): touchPattern = TouchPattern.PUSH; break;
            default: Log.i(logTag, "Wrong touch pattern"); break;
        }
        for (int i = 0; i < 8; i++) {
            touchSensors[i] = ((message[5] >> i) & 1) == 1;
        }
        touchSensors[8] = (message[6] & 1) == 1;
    }

    private void processPsdSensor(byte[] message) {
        for (int i = 4; i < 9; i++) {
            switch (message[i]) {
                case (0x00): psdSensors[i-4] = PsdRange.CLOSE; break;
                case (0x01): psdSensors[i-4] = PsdRange.MID; break;
                case (0x02): psdSensors[i-4] = PsdRange.FAR; break;
                default: Log.i(logTag, "Wrong psd sensor value"); break;
            }
        }
        for (int i = 0; i < 4; i++) {
            cliffSensors[i] = ((message[9] >> i) & 1) == 1;
        }
    }

    private void processBatteryInfo(byte[] message) {
        batteryLevel = message[4];
        batteryVoltage = (int)(((message[6] << 8) + message[5]) & 0xFF);
    }

    private void processEncoderCount(byte[] message) {
        encoderCountLeft = message[4] + (message[5] << 8) + (message[6] << 16) + (message[7] << 24);
        encoderCountRight = message[8] + (message[9] << 8) + (message[10] << 16) + (message[11] << 24);
    }

    private void sendCmdMessage (byte[] command) {
        byte[] cmdMessage = new byte[command.length + 3];
        cmdMessage[0] = (byte) 0xff;
        cmdMessage[1] = (byte) 0xff;
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

    public byte[] setEncoderTransmission(boolean enable) {
        byte[] command = new byte[5];
        command[0] = (byte) 0x2e;
        command[1] = (byte) 0x04;
        command[2] = (byte) 0x03;
        command[3] = (byte) 0xa0;
        if (enable)
            command[5] = (byte) 0x05;
        else
            command[5] = (byte) 0x00;
        return command;
    }

    public byte[] setMotorVelocity(int leftVel, int leftDuration, int rightVel, int rightDuration) {
        byte[] command = new byte[15];
        command[0] = (byte) 0xfe;
        command[1] = (byte) 0x0e;
        command[2] = (byte) 0x06;
        command[3] = (byte) 0x20;
        command[4] = (byte) 0x04;
        command[5] = (byte) 0x2e;
        command[6] = (byte) leftVel;
        command[7] = (byte) (leftVel >> 8);
        command[8] = (byte) leftDuration;
        command[9] = (byte) (leftDuration >> 8);
        command[10] = (byte) 0x2f;
        command[11] = (byte) rightVel;
        command[12] = (byte) (rightVel >> 8);
        command[13] = (byte) rightDuration;
        command[14] = (byte) (rightDuration >> 8);
        return command;
    }

    public byte[] setWingMovement(int wing, int targetPos, int vel) {
        byte[] command = new byte[10];
        command[0] = (byte) 0xfe;
        command[1] = (byte) 0x09;
        command[2] = (byte) 0x83;
        command[3] = (byte) 0x1e;
        command[4] = (byte) 0x04;
        if (wing == LEFT)
            command[5] = (byte) 0x2d;
        else if (wing == RIGHT)
            command[5] = (byte) 0x2b;
        else
            Log.i(logTag, "Bad wing id");
        command[6] = (byte) targetPos;
        command[7] = (byte) (targetPos >> 8);
        command[8] = (byte) vel;
        command[9] = (byte) (vel >> 8);
        return command;
    }

    public byte[] setLedColor(boolean wingBlue, boolean wingGreen, boolean wingRed, boolean headBlue,
                              boolean headGreen, boolean headRed) {
        byte[] command = new byte[5];
        command[0] = (byte) 0x25;
        command[1] = (byte) 0x04;
        command[2] = (byte) 0x03;
        command[3] = (byte) 0xc0;
        int b = 0;
        if (headRed) {
            ledPatterns[0] = true;
            b = b + 32;
        } else {
            ledPatterns[0] = false;
        }
        if (headGreen) {
            ledPatterns[1] = true;
            b = b + 16;
        } else {
            ledPatterns[1] = false;
        }
        if (headBlue) {
            ledPatterns[2] = true;
            b = b + 8;
        } else {
            ledPatterns[2] = false;
        }
        if (wingRed) {
            ledPatterns[3] = true;
            b = b + 4;
        } else {
            ledPatterns[3] = false;
        }
        if (wingGreen) {
            ledPatterns[4] = true;
            b = b + 2;
        } else {
            ledPatterns[4] = false;
        }
        if (wingBlue) {
            ledPatterns[5] = true;
            b = b + 1;
        } else {
            ledPatterns[5] = false;
        }
        command[4] = (byte) b;
        return command;
    }

    public void dataReceived(byte[] data, int byteCount) {
        int i = 0;
        while (i < byteCount) {
            if (data[i] == 0xff && data[i+1] == 0xff) {
                processRobotMessage(btBuffer);
                bufferCount = 0;
                i = i + 2;
            } else {
                btBuffer[bufferCount] = data[i];
                bufferCount++;
                i++;
            }
        }
    }

    public void btConnect() {
        btManager.btConnect();
    }

    public void btDisconnect() {
        btManager.btDisconnect();
    }

}
