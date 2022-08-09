package me.lanzhi.bluestarapi.api.player.input;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;


public final class PlayerChatInput<T> implements Listener
{

    private static List<UUID> players=new ArrayList<UUID>();
    private EnumMap<EndReason, PlayerChatInput<?>> chainAfter;
    private BiFunction<Player, String, Boolean> onInvalidInput;
    private BiFunction<Player, String, Boolean> isValidInput;
    private BiFunction<Player, String, T> setValue;
    private BiConsumer<Player, T> onFinish;
    private Consumer<Player> onCancel;
    private Consumer<Player> onExpire;
    private Runnable onDisconnect;
    private Player player;
    private String invalidInputMessgae;
    private String sendValueMessage;
    private String onExpireMessage;
    private String cancel;
    private Plugin main;
    private int expiresAfter;
    private boolean started;
    private boolean repeat;
    private T value;
    private BukkitTask task;
    private EndReason end;


    public PlayerChatInput(@NotNull Plugin plugin,@NotNull Player player,@Nullable T startOn,@Nullable String invalidInputMessgae,@Nullable String sendValueMessage,@NotNull BiFunction<Player, String, Boolean> isValidInput,@NotNull BiFunction<Player, String, T> setValue,@NotNull BiConsumer<Player, T> onFinish,@NotNull Consumer<Player> onCancel,@NotNull String cancel,@NotNull BiFunction<Player, String, Boolean> onInvalidInput,boolean repeat,@Nullable EnumMap<EndReason, PlayerChatInput<?>> chainAfter,int expiresAfter,@NotNull Consumer<Player> onExpire,@Nullable String whenExpireMessage,@NotNull Runnable onDisconnect)
    {
        Objects.requireNonNull(plugin,"main can't be null");
        Objects.requireNonNull(player,"player can't be null");
        Objects.requireNonNull(invalidInputMessgae,"isValidInput can't be null");
        Objects.requireNonNull(sendValueMessage,"isValidInput can't be null");
        Objects.requireNonNull(isValidInput,"isValidInput can't be null");
        Objects.requireNonNull(setValue,"setValue can't be null");
        Objects.requireNonNull(onFinish,"onFinish can't be null");
        Objects.requireNonNull(onFinish,"onCancel can't be null");
        Objects.requireNonNull(onInvalidInput,"onInvalidInput can't be null");
        Objects.requireNonNull(cancel,"cancel can't be null");
        Objects.requireNonNull(onExpire,"onExpire can't be null");
        Objects.requireNonNull(onDisconnect,"onDisconnect can't be null");
        this.main=plugin;
        this.player=player;
        this.invalidInputMessgae=invalidInputMessgae;
        this.sendValueMessage=sendValueMessage;
        this.isValidInput=isValidInput;
        this.setValue=setValue;
        this.onFinish=onFinish;
        this.onCancel=onCancel;
        this.cancel=cancel;
        this.onInvalidInput=onInvalidInput;
        this.value=startOn;
        this.repeat=repeat;
        this.chainAfter=chainAfter;
        this.expiresAfter=expiresAfter;
        this.onExpire=onExpire;
        this.onExpireMessage=whenExpireMessage;
        this.onDisconnect=onDisconnect;
    }

    private static void addPlayer(UUID player)
    {
        players.add(player);
    }

    private static void removePlayer(UUID player)
    {
        players.remove(player);
    }


    public static boolean isInputing(UUID player)
    {
        return players.contains(player);
    }

    @EventHandler
    public void onPlayerChatEvent(AsyncPlayerChatEvent e)
    {
        if (!player.getUniqueId().equals(e.getPlayer().getUniqueId()))
        {
            return;
        }
        if (!isStarted())
        {
            return;
        }
        e.setCancelled(true);
        Bukkit.getScheduler().runTask(main,()->runEventOnMainThread(e.getMessage()));
    }

    private void runEventOnMainThread(String message)
    {
        if (message.equalsIgnoreCase(cancel))
        {
            onCancel.accept(player);
            end(EndReason.PLAYER_CANCELLS);
            return;
        }
        if (isValidInput.apply(player,message))
        {
            value=setValue.apply(player,message);
            onFinish.accept(player,value);
            end(EndReason.FINISH);
        }
        else
        {
            if (onInvalidInput.apply(player,message))
            {
                if (invalidInputMessgae!=null)
                {
                    player.sendMessage(invalidInputMessgae);
                }
                if (sendValueMessage!=null&&repeat)
                {
                    player.sendMessage(sendValueMessage);
                }
            }
            if (!repeat)
            {
                onExpire.accept(player);
                end(EndReason.INVALID_INPUT);
            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e)
    {
        if (e.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            if (!isStarted())
            {
                return;
            }
            onDisconnect.run();
            end(EndReason.PLAYER_DISCONECTS);
        }
    }

    @Nullable
    public T getValue()
    {
        return value;
    }

    @Nullable
    public void start()
    {

        if (isInputing(player.getUniqueId()))
        {
            throw new IllegalAccessError("Can't ask for input to a player that is already inputing");
        }
        addPlayer(player.getUniqueId());


        main.getServer().getPluginManager().registerEvents(this,this.main);


        if (expiresAfter>0)
        {
            task=Bukkit.getScheduler().runTaskLater(main,()->
            {
                if (!isStarted())
                {
                    return;
                }
                onExpire.accept(player);
                if (onExpireMessage!=null)
                {
                    player.sendMessage(onExpireMessage);
                }
                end(EndReason.RUN_OUT_OF_TIME);
            },expiresAfter);
        }
        if (sendValueMessage!=null)
        {
            player.sendMessage(sendValueMessage);
        }
        started=true;
        end=null;
    }


    public void unregister()
    {

        if (task!=null)
        {
            task.cancel();
        }

        removePlayer(player.getUniqueId());

        HandlerList.unregisterAll(this);
    }


    public void end(EndReason reason)
    {
        started=false;
        end=reason;
        unregister();

        if (chainAfter!=null)

        {
            if (chainAfter.get(end)!=null)

            {
                chainAfter.get(end).start();
            }
        }
    }


    public boolean isStarted()
    {
        return started;
    }


    public static enum EndReason
    {


        PLAYER_CANCELLS,

        FINISH,

        RUN_OUT_OF_TIME,

        PLAYER_DISCONECTS,

        INVALID_INPUT,

        CUSTOM;

    }


    public static class PlayerChatInputBuilder<U>
    {

        private EnumMap<EndReason, PlayerChatInput<?>> chainAfter;
        private BiFunction<Player, String, Boolean> onInvalidInput;
        private BiFunction<Player, String, Boolean> isValidInput;
        private BiFunction<Player, String, U> setValue;
        private BiConsumer<Player, U> onFinish;
        private Consumer<Player> onCancel;
        private Consumer<Player> onExpire;
        private Runnable onDisconnect;
        private Player player;

        private String invalidInputMessage;
        private String sendValueMessage;
        private String whenExpire;
        private String cancel;

        private U value;

        private int expiresAfter;
        private boolean repeat;

        private Plugin main;


        public PlayerChatInputBuilder(@NotNull Plugin main,@NotNull Player player)
        {
            this.main=main;
            this.player=player;

            invalidInputMessage="That is not a valid input";
            sendValueMessage="Send in the chat the value";
            whenExpire="You ran out of time to answer";
            cancel="cancel";

            onInvalidInput=(p,mes)->
            {
                return true;
            };
            isValidInput=(p,mes)->
            {
                return true;
            };
            setValue=(p,mes)->
            {
                return value;
            };
            onFinish=(p,val)->
            {
            };
            onCancel=(p)->
            {
            };
            onExpire=(p)->
            {
            };
            onDisconnect=()->
            {
            };

            expiresAfter=-1;

            repeat=true;
        }


        public PlayerChatInputBuilder<U> onInvalidInput(@NotNull BiFunction<Player, String, Boolean> onInvalidInput)
        {
            this.onInvalidInput=onInvalidInput;
            return this;
        }


        public PlayerChatInputBuilder<U> isValidInput(@NotNull BiFunction<Player, String, Boolean> isValidInput)
        {
            this.isValidInput=isValidInput;
            return this;
        }


        public PlayerChatInputBuilder<U> setValue(@NotNull BiFunction<Player, String, U> setValue)
        {
            this.setValue=setValue;
            return this;
        }


        public PlayerChatInputBuilder<U> onFinish(@NotNull BiConsumer<Player, U> onFinish)
        {
            this.onFinish=onFinish;
            return this;
        }


        public PlayerChatInputBuilder<U> onCancel(@NotNull Consumer<Player> onCancel)
        {
            this.onCancel=onCancel;
            return this;
        }


        public PlayerChatInputBuilder<U> invalidInputMessage(@Nullable String invalidInputMessage)
        {
            this.invalidInputMessage=invalidInputMessage;
            return this;
        }


        public PlayerChatInputBuilder<U> sendValueMessage(@Nullable String sendValueMessage)
        {
            this.sendValueMessage=sendValueMessage;
            return this;
        }


        public PlayerChatInputBuilder<U> toCancel(@NotNull String cancel)
        {
            this.cancel=cancel;
            return this;
        }


        public PlayerChatInputBuilder<U> defaultValue(@Nullable U def)
        {
            this.value=def;
            return this;
        }


        public PlayerChatInputBuilder<U> repeat(boolean repeat)
        {
            this.repeat=repeat;
            return this;
        }


        public PlayerChatInputBuilder<U> chainAfter(@NotNull PlayerChatInput<?> toChain,@NotNull EndReason... after)
        {
            if (this.chainAfter==null)
            {
                chainAfter=new EnumMap<>(EndReason.class);
            }
            for (EndReason cm: after)
            {
                if (cm==EndReason.PLAYER_DISCONECTS)
                {
                    continue;
                }
                this.chainAfter.put(cm,toChain);
            }
            return this;
        }


        public PlayerChatInputBuilder<U> onExpire(@NotNull Consumer<Player> onExpire)
        {
            this.onExpire=onExpire;
            return this;
        }


        public PlayerChatInputBuilder<U> onExpireMessage(@Nullable String message)
        {
            this.whenExpire=message;
            return this;
        }


        public PlayerChatInputBuilder<U> expiresAfter(int ticks)
        {
            if (ticks>0)
            {
                this.expiresAfter=ticks;
            }
            return this;
        }


        public PlayerChatInputBuilder<U> onPlayerDiconnect(@NotNull Runnable onDisconnect)
        {
            this.onDisconnect=onDisconnect;
            return this;
        }


        public PlayerChatInput<U> build()
        {
            return new PlayerChatInput<U>(main,
                                          player,
                                          value,
                                          invalidInputMessage,
                                          sendValueMessage,
                                          isValidInput,
                                          setValue,
                                          onFinish,
                                          onCancel,
                                          cancel,
                                          onInvalidInput,
                                          repeat,
                                          chainAfter,
                                          expiresAfter,
                                          onExpire,
                                          whenExpire,
                                          onDisconnect);
        }
    }

}