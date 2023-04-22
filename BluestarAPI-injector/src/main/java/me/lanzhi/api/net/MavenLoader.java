package me.lanzhi.api.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class MavenLoader
{
    /**
     * 阿里云镜像仓库
     */
    public final static String DEFAULT_MAVEN_REPO="https://maven.aliyun.com/nexus/content/groups/public/";
    /**
     * Maven官方仓库
     */
    public final static String MAVEN_REPO="https://mvnrepository.com/artifact/";

    private MavenLoader()
    {
    }

    public static void loadLibrary(String group,String artifact,File folder) throws IOException,
        NoSuchAlgorithmException
    {
        loadLibrary(group,artifact,"latest",folder);
    }

    /**
     * 从Maven仓库加载一个依赖,按照路径保存进文件夹
     *
     * @param group    组
     * @param artifact 项目
     * @param version  版本
     * @param folder   保存的文件夹
     */
    public static void loadLibrary(String group,String artifact,String version,File folder) throws IOException,
        NoSuchAlgorithmException
    {
        loadLibrary(group,artifact,version,DEFAULT_MAVEN_REPO,folder,true);
    }

    public static void loadLibrary(String group,String artifact,String version,String repo,File folder,
                                   boolean checkMD5) throws IOException, NoSuchAlgorithmException
    {
        File file=new File(folder,group.replace('.','/')+'/'+artifact+'/'+version+'/'+artifact+'-'+version+".jar");
        loadMavenLibrary(group,artifact,version,repo,file,checkMD5);
    }

    /**
     * 从Maven仓库加载一个依赖
     *
     * @param group    组
     * @param artifact 项目
     * @param version  版本,如果为null则获取最新版本
     * @param repo     仓库
     * @param file     文件
     * @param checkMD5 是否在下载完成后通过MD5校验下载正确性
     */
    public static void loadMavenLibrary(String group,String artifact,String version,String repo,File file,
                                        boolean checkMD5) throws IOException, NoSuchAlgorithmException
    {
        Objects.requireNonNull(group);
        Objects.requireNonNull(artifact);
        Objects.requireNonNull(file);
        if (version==null)
        {
            version=getLatestVersion(group,artifact,repo);
        }
        if (version==null)
        {
            throw new RuntimeException("无法获取最新版本");
        }
        String url=repo+group.replace('.','/')+'/'+artifact+'/'+version+'/'+artifact+'-'+version+".jar";
        //先下载文件,下载结束后再获取MD5
        if (file.exists())
        {
            if (!checkMD5||Objects.equals(getMD5(url),getMD5(file)))
            {
                return;
            }
            else
                file.delete();
        }
        file.getParentFile().mkdirs();
        file.createNewFile();
        download(url,file,checkMD5);
    }

    public static String getLatestVersion(String group,String artifact,String repo) throws IOException
    {
        String url=repo+group.replace('.','/')+'/'+artifact+"/maven-metadata.xml";
        try (var in=new URL(url).openStream())
        {
            //从中解析出最新版本
            //不使用JDOM或者DOM,因为这两个库太大了,手动解析
            String xml=new String(in.readAllBytes());
            int index=xml.indexOf("<latest>");
            if (index==-1)
            {
                return null;
            }
            int index2=xml.indexOf("</latest>",index);
            if (index2==-1)
            {
                return null;
            }
            return xml.substring(index+8,index2);
        }
    }

    public static String getMD5(String url) throws IOException
    {
        try (var in=new URL(url+".md5").openStream())
        {
            return new String(in.readAllBytes());
        }
    }

    public static String getMD5(File file) throws IOException, NoSuchAlgorithmException
    {
        MessageDigest md=MessageDigest.getInstance("MD5");
        FileInputStream fis=new FileInputStream(file);
        byte[] buffer=new byte[1024];
        int length;
        while ((length=fis.read(buffer))!=-1)
        {
            md.update(buffer,0,length);
        }
        fis.close();
        byte[] md5Bytes=md.digest();
        StringBuilder sb=new StringBuilder();
        for (byte b: md5Bytes)
        {
            sb.append(String.format("%02x",b));
        }
        return sb.toString();
    }

    public static void download(String url,File file,boolean checkMD5) throws IOException, NoSuchAlgorithmException
    {
        file.getParentFile().mkdirs();
        file.delete();
        file.createNewFile();
        if (!file.isFile())
            throw new IOException("Cannot create file");
        try (var in=new URL(url).openStream())
        {
            Files.copy(in,file.toPath());
        }
        if (checkMD5)
        {
            if (!Objects.equals(getMD5(url),getMD5(file)))
            {
                file.delete();
                throw new IOException("MD5校验失败");
            }
        }
    }

    public static void loadLibrary(String group,String artifact,String version,String repo,File folder) throws IOException, NoSuchAlgorithmException
    {
        loadLibrary(group,artifact,version,DEFAULT_MAVEN_REPO,folder,true);
    }

    public static void loadLibrary(MavenLibrary library,File folder) throws IOException, NoSuchAlgorithmException
    {
        loadLibrary(library.group,library.artifact,library.version,library.repo,folder,true);
    }

    public static MavenLibrary of(String group,String artifact)
    {
        return new MavenLibrary(group,artifact);
    }

    public static MavenLibrary of(String group,String artifact,String version)
    {
        return new MavenLibrary(group,artifact,version);
    }

    public static MavenLibrary of(String group,String artifact,String version,String repo)
    {
        return new MavenLibrary(group,artifact,version,repo);
    }

    public static final class MavenLibrary
    {
        public final String group;
        public final String artifact;
        public final String version;
        public final String repo;

        public MavenLibrary(String group,String artifact)
        {
            this(group,artifact,"latest");
        }

        public MavenLibrary(String group,String artifact,String version)
        {
            this(group,artifact,version,MavenLoader.DEFAULT_MAVEN_REPO);
        }

        public MavenLibrary(String group,String artifact,String version,String repo)
        {
            this.group=group;
            this.artifact=artifact;
            this.version=version;
            this.repo=repo;
        }
    }
}