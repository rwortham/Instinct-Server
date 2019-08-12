package instinct;

import java.io.File;
import java.io.IOException;

public class Logger {

    private String logfileName = null;

    public Logger(String fileName) {
        this.logfileName = fileName;
        File tmpFile = new File(logfileName);
        boolean exists = tmpFile.exists();

        if(!exists){
            File file = new File(logfileName);
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public String getFullFileName() {
        return logfileName;
    }
}
