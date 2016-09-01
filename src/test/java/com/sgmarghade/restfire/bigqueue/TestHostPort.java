package com.sgmarghade.restfire.bigqueue;

/**
 * Created by swapnil on 23/12/15.
 */
public class TestHostPort {
    private String hostName;
    private int port;

    public TestHostPort() {
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "TestHostPort{" +
                "hostName='" + hostName + '\'' +
                ", port=" + port +
                '}';
    }
}