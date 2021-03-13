package org.opentcs.testvehicle;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketUtils {
    private String ip;
    private int port;

    public SocketUtils(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public SocketUtils(String ip, String port) {
        this.ip = ip;
        try {
            this.port = Integer.parseInt(port);
        }
        catch (Exception ex) {
            this.port = 4001;
        }
    }

    public String send(String msg) {
        Socket socket = null;
        String ret = "";
        try {
            socket = new Socket(ip, port);
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(msg.getBytes());
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[4096];
            int len = in.read(buf);
            ret = new String(buf, 0, len);
            out.close();
            in.close();
            socket.close();
            return ret;
        }
        catch (Exception ex) {
            try {
                socket.close();
            }
            catch (Exception e) {
            }
            return "";
        }
        finally {
            try {
                socket.close();
            }
            catch (Exception e) {
            }
        }

    }

    public byte[] send(byte[] bytes) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write(bytes);
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[8];
            int len = in.read(buf);
            out.close();
            in.close();
            socket.close();
            return buf;
        }
        catch (Exception ex) {
            try {
                socket.close();
            }
            catch (Exception e) {
            }
            return null;
        }
        finally {
            try {
                socket.close();
            }
            catch (Exception e) {
            }
        }
    }
}
