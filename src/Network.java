import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.Thread.sleep;

class Device extends Thread {
    public String name, type;
    public int connectionID;
    public Router router;
    public String status;

    public Device(String name, String type, Router router) {
        this.name = name;
        this.type = type;
        this.router = router;
        connectionID = 1;
    }

    @Override
    public void run() {
        try {
            router.semaphore.wait(this);
            connectionID = router.connect(this);
            status = "Connection " + connectionID + ": " + name + " Occupied";
            LogIn();
            OnlineActivity();
            router.disconnect(this);
            router.semaphore.signal();
            status = "Connection " + connectionID + ": " + name + " Disconnected";

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void OnlineActivity() throws InterruptedException {
        System.out.println("Connection " + connectionID + ": " + name + " Performs online activity");
        Thread.sleep((long) (Math.random() * 2000));
    }

    public void LogIn() throws InterruptedException {
        System.out.println("Connection " + connectionID + ": " + name + " Logged in");
    }
}

class Router {
    public boolean[] connected;
    public int maxDevices, currentConnectedDevices;
    public Semaphore semaphore;

    Router(int maxDevices) {
        this.maxDevices = maxDevices;
        semaphore = new Semaphore(maxDevices);
        connected = new boolean[maxDevices];
    }

    public synchronized int connect(Device device) throws InterruptedException {
        for (int i = 0; i < maxDevices; i++) {
            if (!connected[i]) {
                currentConnectedDevices++;
                device.connectionID = i + 1;
                connected[i] = true;
                sleep(200);
                break;
            }
        }
        return device.connectionID;
    }

    public synchronized void disconnect(Device device) {
        currentConnectedDevices--;
        connected[device.connectionID - 1] = false;
        notify();
        System.out.println("Connection " + device.connectionID + ": " + device.name + " Logged out");
    }
}

class Semaphore {
    int value;

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void wait(Device device) throws InterruptedException {
        value--;
        if (value < 0) {
            System.out.println(device.name + " (" + device.type + ")" + " arrived and waiting");
            wait();
        } else {
            System.out.println(device.name + " (" + device.type + ")" + " arrived");
        }

        device.router.connect(device);
    }

    public synchronized void signal() {
        value++;
        if (value <= 0) {
            notify();
        }
    }
}

public class Network {
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        int numberOfConnections, numberOfDevices;
        ArrayList<Device> devices = new ArrayList<>();

        Scanner input = new Scanner(System.in);

        System.out.println("What is the number of WI-FI Connections?");
        numberOfConnections = input.nextInt();
        Router router = new Router(numberOfConnections);

        System.out.println("What is the number of devices Clients want to connect?");
        numberOfDevices = input.nextInt();

        System.out.println("Enter details for device name (i.e., C1) and type (i.e., mobile, pc, tablet...): ");
        for (int i = 0; i < numberOfDevices; i++) {
            Device newDevice = new Device(input.next(), input.next(), router);
            devices.add(newDevice);
        }

        PrintStream originalOut = System.out;
        PrintStream fileOut = new PrintStream(new File("output.txt"));
        System.setOut(fileOut);

        for (int i = 0; i < numberOfDevices; i++) {
            sleep(200);
            devices.get(i).start();
        }

        for (Device device : devices) {
            device.join();
        }

        System.setOut(originalOut);
    }
}
