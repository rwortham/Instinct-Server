package instinct;

import java.util.HashMap;

public class RobotStreamData {

    protected static volatile String ROBOT_INCOMING_STRING = null;

    public static String getRobotIncomingString() {
        return ROBOT_INCOMING_STRING;
    }

    public static void setRobotIncomingString(String robotIncomingString) {
        ROBOT_INCOMING_STRING = robotIncomingString;
    }
}
