package ui;

import java.awt.BorderLayout;
//import java.awt.GraphicsDevice;
//import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Method;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

//import com.apple.eawt.Application;
//import com.apple.eawt.FullScreenUtilities;

public class ImageTest {

    private static void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("ImageTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // true fullscreen
//        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
//                .getDefaultScreenDevice();
//
//        if (gd.isFullScreenSupported()) {
//            gd.setFullScreenWindow(frame);
//        }

        // pseudo fullscreen
        // frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        // frame.setUndecorated(true);

        ImageIcon img = createImageIcon("img.png", null);

        JLabel label = new JLabel(img);
        frame.getContentPane().add(label, BorderLayout.CENTER);

        // Display the window.
        frame.setVisible(true);

        // osx fullscreen
//         FullScreenUtilities.setWindowCanFullScreen(frame, true);
//         Application.getApplication().requestToggleFullScreen(frame);
         //setWindowCanFullScreenOsx(frame, true);
         requestToggleFullScreenOsx(frame);
    }

    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path,
            String description) {
        java.net.URL imgURL = ImageTest.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    protected static void setWindowCanFullScreenOsx(JFrame frame,
            boolean enable) {
        // FullScreenUtilities.setWindowCanFullScreen(frame, true);
        try {
            Class<?> fullScreenUtilitiesClass = Class
                    .forName("com.apple.eawt.FullScreenUtilities");
            Method setWindowCanFullScreenMethod = fullScreenUtilitiesClass
                    .getMethod("setWindowCanFullScreen",
                            new Class<?>[] { Window.class, Boolean.class });
            setWindowCanFullScreenMethod.invoke(fullScreenUtilitiesClass, frame,
                    enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void requestToggleFullScreenOsx(JFrame frame) {
        // Application.getApplication().requestToggleFullScreen(frame);
        try {
            Class<?> applicationClass = Class
                    .forName("com.apple.eawt.Application");
            Method getApplicationMethod = applicationClass
                    .getMethod("getApplication", new Class<?>[] {});
            getApplicationMethod.setAccessible(true);
            Object application = getApplicationMethod.invoke(applicationClass);
            Method requestToggleFullScreenMethod = application.getClass()
                    .getMethod("requestToggleFullScreen",
                            new Class<?>[] { Window.class });
            requestToggleFullScreenMethod.invoke(application, frame);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
