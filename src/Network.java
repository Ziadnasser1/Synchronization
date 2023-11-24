import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import static java.lang.Thread.sleep;

class Device extends Thread {
    public String name, type;
    public int connectionID;
    public Router router;

    public Device(String name, String type, Router router) {
        this.name = name;
        this.type = type;
        this.router = router;
        connectionID = 1;
    }

    @Override
    public void run() {
        try {
            router.sem.P(this);
            connectionID = router.connect(this);

            System.out.println("Connection " + connectionID + ": " + name + " Occupied");
            logIn();
            onlineActivity();
            router.disconnect(this);

            router.sem.V(this, connectionID);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onlineActivity() throws InterruptedException {
        System.out.println("Connection " + connectionID + ": " + name + " Performs online activity");
        sleep((long) (Math.random() * 2000));
    }

    public void logIn() throws InterruptedException {
        System.out.println("Connection " + connectionID + ": " + name + " Logged in");
    }
}

class Router {
    public ArrayList<Device> connectedDevices;
    public Semaphore sem;

    Router(int maxDevices) {
        connectedDevices = new ArrayList<>();
        sem = new Semaphore(maxDevices);
    }

    public synchronized int connect(Device device) throws InterruptedException {
        int connectionID = connectedDevices.size() + 1;
        connectedDevices.add(device);
        sleep(100);
        return connectionID;
    }

    public synchronized void disconnect(Device device) {
        connectedDevices.remove(device);
        System.out.println("Connection " + device.connectionID + ": " + device.name + " Logged out");
    }
}

class Semaphore {
    int value;

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void P(Device device) throws InterruptedException {
        value--;
        if (value < 0) {
            System.out.println(device.name + " (" + device.type + ")" + " arrived and waiting");
            wait();
        } else {
            System.out.println(device.name + " (" + device.type + ")" + " arrived");
        }
    }

    public synchronized void V(Device device, int connectionID) {
        System.out.println("Connection " + connectionID + ": (" + device.name + ")" + " logged out");
        value++;
        if (value <= 0) notify();
    }
}

public class Network {
    public static void main(String[] args) throws InterruptedException {
        int numberOfConnections, numberOfDevices;
        ArrayList<Device> devices = new ArrayList<>();

        Scanner input = new Scanner(System.in);

        System.out.println("What is the number of WI-FI Connections?");
        numberOfConnections = input.nextInt();
        Router router = new Router(numberOfConnections);

        System.out.println("What is the number of devices Clients want to connect?");
        numberOfDevices = input.nextInt();

        System.out.println("Enter details for device name (i.e. C1) and type (i.e. mobile, pc, tablet...): ");
        for (int i = 0; i < numberOfDevices; i++) {
            Device newDevice = new Device(input.next(), input.next(), router);
            devices.add(newDevice);
        }

        try (PrintStream fileStream = new PrintStream(new FileOutputStream("output.txt"))) {
            System.setOut(fileStream);

            for (Device device : devices) {
                device.start();
            }

            try {
                for (Device device : devices) {
                    device.join();
                }
            } catch (InterruptedException e) {
                System.out.println("Join interrupted");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        }
    }
}
