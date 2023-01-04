package me.lanzhi.api.net.dowloader;

import java.io.*;
import java.net.URL;
import java.util.function.Consumer;

public class DefaultDownload
{
    public static DefaultDownloader download(String serverPath,String localPath) throws IOException
    {
        return download(serverPath,localPath,-1);
    }

    public static DefaultDownloader download(String serverPath,String localPath,int timeOut) throws IOException
    {
        File file=new File(localPath);
        if (!file.exists())
        {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        URL url=new URL(serverPath);
        var conn=url.openConnection();
        if (timeOut>0)
        {
            conn.setConnectTimeout(timeOut);
            conn.setReadTimeout(timeOut);
        }
        var downloader=new DefaultDownloader(conn.getContentLengthLong(),localPath);
        new Thread()
        {
            @Override
            public void run()
            {
                try (InputStream inputStream=conn.getInputStream();
                     FileOutputStream outputStream=new FileOutputStream(file))
                {
                    copy(inputStream,outputStream,downloader::add,downloader);
                    if (downloader.status()==Downloader.Status.Cancel||downloader.status()==Downloader.Status.Error)
                    {
                        outputStream.close();
                        file.delete();
                    }
                }
                catch (Throwable e)
                {
                    downloader.errorCause(e);
                }
            }
        }.start();
        return downloader;
    }

    protected static void copy(InputStream in,OutputStream out,Consumer<Long> add,Downloader manager) throws IOException
    {
        byte[] buffer=new byte[1024];
        int len;
        while ((len=in.read(buffer))!=-1)
        {
            if (manager.pause())
            {
                synchronized (manager)
                {
                    try
                    {
                        manager.wait();
                    }
                    catch (Throwable ignored)
                    {
                    }
                }
            }
            if (!manager.running())
            {
                break;
            }
            out.write(buffer,0,len);
            add.accept((long) len);
        }
    }

    private static class DefaultDownloader extends Downloader
    {
        public DefaultDownloader(long totalSize,String file)
        {
            super(totalSize,file);
        }
    }
}
