package me.nullaqua.api.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.function.BiConsumer;

/**
 * 代表一个YAML类型的文件
 */
public class YamlFile extends YamlConfiguration
{
    private final File file;
    private long time=0;
    private boolean exists=true;
    private final boolean autoSave;
    private int isLoading=0;

    public YamlFile(@NotNull File file,Plugin plugin,boolean autoReload,BiConsumer<YamlFile,Event> biConsumer,
                    boolean autoSave)
    {
        this.file=file;
        try
        {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        catch (IOException e)
        {
            System.out.println("§4[BluestarAPI]创建文件时出错:"+file.getName());
        }
        if (autoReload&&biConsumer!=null)
        {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,() ->
            {
                if (file.exists()&&!exists)
                {
                    exists=file.exists();
                    time=file.lastModified();
                    biConsumer.accept(YamlFile.this,Event.CREATE);
                }
                else if (!file.exists()&&exists)
                {
                    exists=file.exists();
                    time=file.lastModified();
                    biConsumer.accept(YamlFile.this,Event.DELETE);
                }
                else if (exists&&file.lastModified()!=time)
                {
                    time=file.lastModified();
                    biConsumer.accept(YamlFile.this,Event.UPDATE);
                }
            },0,20);
        }
        this.autoSave=autoSave;
    }

    public static Plugin getPlugin()
    {
        return JavaPlugin.getProvidingPlugin(YamlFile.class);
    }

    @Override
    public void loadFromString(@NotNull String contents) throws InvalidConfigurationException
    {
        synchronized (this)
        {
            isLoading++;
            try
            {
                super.loadFromString(contents);
            }
            finally
            {
                isLoading--;
            }
        }
    }

    @Override
    public void load(@NotNull File file) throws IOException, InvalidConfigurationException
    {
        synchronized (this)
        {
            isLoading++;
            try
            {
                super.load(file);
            }
            finally
            {
                isLoading--;
            }
        }
    }

    /**
     * 创建一个YAML文件
     *
     * @param file 文件
     */
    public YamlFile(@NotNull File file,@NotNull Plugin plugin)
    {
        this(file,plugin,true);
    }

    public YamlFile(@NotNull File file,@NotNull Plugin plugin,boolean autoReload)
    {
        this(file,plugin,autoReload,(yamlFile,event) ->
        {
            if (event==Event.UPDATE)
            {
                yamlFile.reload();
            }
        });
    }

    public YamlFile(@NotNull File file,@NotNull Plugin plugin,boolean autoReload,boolean autoSave)
    {
        this(file,plugin,autoReload,(yamlFile,event) ->
        {
            if (event==Event.UPDATE)
            {
                yamlFile.reload();
            }
        },autoSave);
    }

    public YamlFile(@NotNull File file,Plugin plugin,boolean autoReload,String message)
    {
        this(file,plugin,autoReload,message,true);
    }

    public YamlFile(@NotNull File file,Plugin plugin,boolean autoReload,BiConsumer<YamlFile,Event> biConsumer)
    {
        this(file,plugin,autoReload,biConsumer,true);
    }

    public YamlFile(@NotNull File file,Plugin plugin,boolean autoReload,String message,boolean autoSave)
    {
        this(file,plugin,autoReload,(yamlFile,event) ->
        {
            if (event==Event.UPDATE)
            {
                yamlFile.reload();
                Bukkit.getLogger().info(message);
            }
        },autoSave);
    }

    @Override
    public void load(@NotNull Reader reader) throws IOException, InvalidConfigurationException
    {
        synchronized (this)
        {
            isLoading++;
            try
            {
                super.load(reader);
            }
            finally
            {
                isLoading--;
            }
        }
    }

    @NotNull
    public YamlFile reload()
    {
        if (!file.exists())
        {
            try
            {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            catch (IOException e)
            {
                System.out.println("§4[BluestarAPI]创建文件时出错:"+file.getName());
                e.printStackTrace();
            }
        }
        try
        {
            this.loadFromString(new String(Files.readAllBytes(file.toPath())));
        }
        catch (IOException|InvalidConfigurationException e)
        {
            System.out.println("§4[BluestarAPI]加载文件时出错:"+file.getName());
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void load(@NotNull String file) throws IOException, InvalidConfigurationException
    {
        synchronized (this)
        {
            isLoading++;
            try
            {
                super.load(file);
            }
            finally
            {
                isLoading--;
            }
        }
    }

    @Override
    public void set(@NotNull String path,@Nullable Object value)
    {
        super.set(path,value);
        //如果自动保存,且调用栈中没有此类,则保存
        if (autoSave)
        {
            synchronized (this)
            {
                if (isLoading==0)
                {
                    this.save();
                }
            }
        }
    }

    public enum Event
    {
        UPDATE,
        DELETE,
        CREATE
    }

    @NotNull
    public YamlFile save()
    {
        synchronized (this)
        {
            if (isLoading!=0)
            {
                getPlugin().getLogger().severe("§4[BluestarAPI]正在加载文件,无法保存文件:"+file.getName());
                return this;
            }
        }
        try
        {
            file.createNewFile();
            this.save(file);
        }
        catch (IOException e)
        {
            System.out.println("§4[BluestarAPI]无法保存文件:"+file.getName());
        }
        return this;
    }
}