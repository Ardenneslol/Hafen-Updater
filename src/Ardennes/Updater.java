package Ardennes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import Ardennes.UpdaterConfig.Item;

public class Updater {
    public UpdaterConfig cfg;
    private IUpdaterListener listener;


    public Updater(IUpdaterListener listener) {
        this.listener = listener;
        this.cfg = new UpdaterConfig();
    }

    public void update() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                List<Item> update = new ArrayList();
                Iterator var3 = Updater.this.cfg.items.iterator();

                Item item;
                while(var3.hasNext()) {
                    item = (Item)var3.next();
                    if (Updater.this.correct_platform(item)) {
                        Updater.this.set_date(item);
                        if (Updater.this.has_update(item)) {
                            Updater.this.listener.log(String.format("Updates found for '%s'", item.file.getName()));
                            update.add(item);
                        } else {
                            Updater.this.listener.log(String.format("No updates for '%s'", item.file.getName()));
                        }
                    }
                }

                var3 = update.iterator();

                while(var3.hasNext()) {
                    item = (Item)var3.next();
                    Updater.this.download(item);
                    if (item.extract != null) {
                        Updater.this.extract(item);
                    }
                }

                Updater.this.listener.fisnished();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private boolean correct_platform(Item item) {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        return os.indexOf(item.os) >= 0 && (arch.equals(item.arch) || item.arch.length() == 0);
    }

    private void set_date(Item item) {
        if (item.file.exists()) {
            item.date = item.file.lastModified();
        }

    }

    private boolean has_update(Item item) {
        try {
            URL url = new URL(item.link);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setIfModifiedSince(item.date);

            try {
                if (conn.getResponseCode() == 200) {
                    item.size = Long.parseLong(conn.getHeaderField("Content-Length"));
                    return true;
                }
            } catch (NumberFormatException var5) {
                ;
            }

            conn.disconnect();
        } catch (MalformedURLException var6) {
            var6.printStackTrace();
        } catch (IOException var7) {
            var7.printStackTrace();
        }

        return false;
    }

    private void download(Item item) {
        this.listener.log(String.format("Downloading '%s'", item.file.getName()));

        try {
            URL link = new URL(item.link);
            ReadableByteChannel rbc = Channels.newChannel(link.openStream());
            FileOutputStream fos = new FileOutputStream(item.file);
            long position = 0L;
            int step = 20480;
            this.listener.progress(position, item.size);

            while(position < item.size) {
                position += fos.getChannel().transferFrom(rbc, position, (long)step);
                this.listener.progress(position, item.size);
            }

            this.listener.progress(0L, item.size);
            fos.close();
        } catch (MalformedURLException var8) {
            var8.printStackTrace();
        } catch (IOException var9) {
            var9.printStackTrace();
        }

    }

    private void extract(Item item) {
        this.listener.log(String.format("Unpacking '%s'", item.file.getName()));
        try {
            new File("./data").mkdir();
            new File("./data/hud").mkdir();
            new File("./data/hud/sloth").mkdir();
            new File("./data/hud/sloth/buttons").mkdir();
            new File("./data/hud/sloth/chkbox").mkdir();
            new File("./data/hud/sloth/scroll").mkdir();
            new File("./data/hud/sloth/buttons/circular").mkdir();
            new File("./data/hud/sloth/buttons/circular/small").mkdir();
            new File("./data/hud/default").mkdir();
            new File("./data/hud/default/buttons").mkdir();
            new File("./data/hud/default/chkbox").mkdir();
            new File("./data/hud/default/scroll").mkdir();
            new File("./data/hud/default/buttons/circular").mkdir();
            new File("./data/hud/default/buttons/circular/small").mkdir();
            ZipFile zip = new ZipFile(item.file);

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                System.out.println(entry.getName());
                if(entry.isDirectory()){
               //     System.out.print("dir  : " + entry.getName());
                   // String destPath = File.separator + entry.getName();
                   // System.out.println(" => " + destPath);
                    File file = new File(entry.getName());
                    file.mkdirs();
                } else {
                    String destPath = entry.getName();

                    try(InputStream inputStream = zip.getInputStream(entry);
                        FileOutputStream outputStream = new FileOutputStream(destPath);
                    ){
                        int data = inputStream.read();
                        while(data != -1){
                            outputStream.write(data);
                            data = inputStream.read();
                        }
                    }
                    System.out.println("file : " + entry.getName() + " => " + destPath);
                }
            }
        } catch(IOException e){
            throw new RuntimeException("Error unzipping file " + item.file.getName(), e);
        }
    }
}
