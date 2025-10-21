package de.Huskthedev.HusksStuff.MacOS;

public class DetectOS {
    public static void main(String[] args) {
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            System.out.println("Windows OS");
        } else if (os.contains("Linux")) {
            System.out.println("Linux OS");
        } else if (os.contains("Mac OS")) {
            System.out.println("Mac OS");
            System.out.println("Discord RPC will be disabled");
        }
    }
}
