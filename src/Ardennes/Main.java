package Ardennes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import static Ardennes.UpdaterConfig.dir;



public class Main extends JFrame implements IUpdaterListener {
    private static final int PROGRESS_MAX = 1024;
    private static final long serialVersionUID = 1L;
    private static Updater updater;
    private FileOutputStream log;
    private static Main gui;

    public static void main(String[] args) {
        updateLauncher();
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {}
        gui = new Main();
        gui.setVisible(true);
        gui.setSize(350, 450);
        gui.log(String.format("OS: '%s', arch: '%s'", System.getProperty("os.name"), System.getProperty("os.arch")));
        gui.log("Checking for updates...");
        updater = new Updater(gui);
        updater.update();
    }

    private JTextArea logbox;
    private JProgressBar progress;

    public Main(){
        super("HnH updater");
        try {
            if(!dir.exists()){
                dir.mkdirs();
            }
            log = new FileOutputStream(new File(dir, "updater.log"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel p;
        add(p = new JPanel());
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

        p.add(logbox = new JTextArea());
        logbox.setEditable(false);
        logbox.setFont(logbox.getFont().deriveFont(10.0f));

        p.add(progress = new JProgressBar());
        progress.setMinimum(0);
        progress.setMaximum(PROGRESS_MAX);
        pack();
    }

    @Override
    public void log(String message) {
        message = message.concat("\n");
        logbox.append(message);
        try {
            if(log != null){log.write(message.getBytes());}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fisnished() {
        this.log("Starting client...");
        String libs = String.format("-Djava.library.path=\"%%PATH%%\"%s.", File.pathSeparator);
        UpdaterConfig cfg = updater.cfg;
        ProcessBuilder pb = new ProcessBuilder(new String[]{"java", "-Xss" + cfg.mem, "-Xms" + cfg.mem2, "-Xmx" + cfg.mem3, libs, "-jar", cfg.jar, "-U", cfg.res, cfg.server});
        pb.directory(UpdaterConfig.dir.getAbsoluteFile());

        try {
            pb.start();
        } catch (IOException var6) {
            var6.printStackTrace();
        }

        try {
            if (this.log != null) {
                this.log.flush();
                this.log.close();
            }
        } catch (IOException var5) {
            var5.printStackTrace();
        }

        System.exit(0);
    }

    public static void updateLauncher() {
        System.out.println("Checking for updates for launcher...");
       // gui.log("Checking for updates to launcher...");
        try {
            String api =
                    IOUtils.toString(new URL("https://api.github.com/repos/Ardenneslol/Hafen-Updater/releases/latest"));
            String size = api.substring(api.indexOf("\"size\":") + 7, api.indexOf(",\"download"));
            File launcherJar = new File("./lib/LauncherUpdate.zip");
            if (Integer.parseInt(size) != launcherJar.length()) {
                System.out.println("Update found! Downloading...");
            //    gui.log("Updates found for launcher...");
                URL downloadUrl = new URL(
                        api.substring(api.lastIndexOf("download_url\":\"") + 15, api.indexOf("\"}],\"tarball")));
                FileUtils.copyURLToFile(downloadUrl, launcherJar);
                System.out.println("Unzipping");
               // gui.log("Unzipping...");
                ZipFile zipFile = new ZipFile(launcherJar);
                System.out.println("Unzipping2");
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                System.out.println("Unzipping3");
                while (entries.hasMoreElements()) {
                    System.out.println("Unzipping4");
                    ZipEntry entry = entries.nextElement();
                    File entryDestination = new File(".", entry.getName());
                    if (entry.isDirectory()) {
                        entryDestination.mkdirs();
                    } else {
                        System.out.println("Unpacking");
                        entryDestination.getParentFile().mkdirs();
                        InputStream in = zipFile.getInputStream(entry);
                        OutputStream out = new FileOutputStream(entryDestination);
                        IOUtils.copy(in, out);
                        IOUtils.closeQuietly(in);
                        out.close();
                    }
                }
                zipFile.close();
             //   gui.log("Launcher update done");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void progress(long position, long size) {
        this.progress.setValue((int)(1024.0F * ((float)position / (float)size)));
    }
}
