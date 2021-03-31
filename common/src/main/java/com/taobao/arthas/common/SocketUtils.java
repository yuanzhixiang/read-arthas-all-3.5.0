package com.taobao.arthas.common;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Random;

import javax.net.ServerSocketFactory;

/**
 *
 * @author hengyunabc 2018-11-07
 *
 */
public class SocketUtils {

    /**
     * The default minimum value for port ranges used when finding an available
     * socket port.
     */
    public static final int PORT_RANGE_MIN = 1024;

    /**
     * The default maximum value for port ranges used when finding an available
     * socket port.
     */
    public static final int PORT_RANGE_MAX = 65535;

    private static final Random random = new Random(System.currentTimeMillis());

    private SocketUtils() {
    }

    public static long findTcpListenProcess(int port) {
        try {

            // 如果为 windows 系统则走下面的判断方式
            if (OSUtils.isWindows()) {
                // 通过命令找出所有监听的端口，然后遍历看看我们的端口是否在其中
                String[] command = { "netstat", "-ano", "-p", "TCP" };
                List<String> lines = ExecutingCommand.runNative(command);
                for (String line : lines) {
                    if (line.contains("LISTENING")) {
                        // TCP 0.0.0.0:49168 0.0.0.0:0 LISTENING 476
                        String[] strings = line.trim().split("\\s+");
                        if (strings.length == 5) {
                            if (strings[1].endsWith(":" + port)) {
                                return Long.parseLong(strings[4]);
                            }
                        }
                    }
                }
            }

            // 如果 Linux or Mac 系统则走下面的判断方式
            if (OSUtils.isLinux() || OSUtils.isMac()) {
                // 通过命令直接找出该端口是否被使用
                String pid = ExecutingCommand.getFirstAnswer("lsof -t -s TCP:LISTEN -i TCP:" + port);
                if (!pid.trim().isEmpty()) {
                    return Long.parseLong(pid);
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return -1;
    }

    public static boolean isTcpPortAvailable(int port) {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(port, 1,
                    InetAddress.getByName("localhost"));
            serverSocket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Find an available TCP port randomly selected from the range
     * [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}].
     * 
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    public static int findAvailableTcpPort() {
        return findAvailableTcpPort(PORT_RANGE_MIN);
    }

    /**
     * Find an available TCP port randomly selected from the range [{@code minPort},
     * {@value #PORT_RANGE_MAX}].
     * 
     * @param minPort the minimum port number
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    public static int findAvailableTcpPort(int minPort) {
        return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
    }

    /**
     * Find an available TCP port randomly selected from the range [{@code minPort},
     * {@code maxPort}].
     * 
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return an available TCP port number
     * @throws IllegalStateException if no available port could be found
     */
    public static int findAvailableTcpPort(int minPort, int maxPort) {
        return findAvailablePort(minPort, maxPort);
    }

    /**
     * Find an available port for this {@code SocketType}, randomly selected from
     * the range [{@code minPort}, {@code maxPort}].
     * 
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return an available port number for this socket type
     * @throws IllegalStateException if no available port could be found
     */
    private static int findAvailablePort(int minPort, int maxPort) {

        int portRange = maxPort - minPort;
        int candidatePort;
        int searchCounter = 0;
        do {
            if (searchCounter > portRange) {
                throw new IllegalStateException(
                        String.format("Could not find an available tcp port in the range [%d, %d] after %d attempts",
                                minPort, maxPort, searchCounter));
            }
            candidatePort = findRandomPort(minPort, maxPort);
            searchCounter++;
        } while (!isTcpPortAvailable(candidatePort));

        return candidatePort;
    }

    /**
     * Find a pseudo-random port number within the range [{@code minPort},
     * {@code maxPort}].
     * 
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return a random port number within the specified range
     */
    private static int findRandomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        return minPort + random.nextInt(portRange + 1);
    }
}
