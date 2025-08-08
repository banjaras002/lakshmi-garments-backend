package com.lakshmigarments.utility;

import java.io.IOException;

public class StreamlitRunner {
    public static void startStreamlit() {
        try {
            // Set the command to run your Streamlit app from the 'streamlit' folder
            String command = "streamlit run streamlit/app.py --no-browser";  // Relative path

            // Use ProcessBuilder to run the command
            Process process = new ProcessBuilder(command.split(" ")).start();

            // Optional: You can wait for the process to complete (if needed)
            process.waitFor();
            System.out.println("started streamlit");
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
