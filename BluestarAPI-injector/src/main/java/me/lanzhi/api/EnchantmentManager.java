package me.lanzhi.api;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.lang.reflect.Field;
import java.util.Map;

public final class EnchantmentManager
{
    public static void upData()
    {
        Bluestar.setEnchantmentManager(new EnchantmentManager());
    }
    private final Field acceptRegisterEnchantment;
    private final Map<NamespacedKey, Enchantment> enchantmentByKey;
    private final Map<String, Enchantment> enchantmentByName;

    public EnchantmentManager()
    {
        try
        {
            acceptRegisterEnchantment=Enchantment.class.getDeclaredField("acceptingNew");
            Field byKey=Enchantment.class.getDeclaredField("byKey");
            byKey.setAccessible(true);
            enchantmentByKey=(Map<NamespacedKey, Enchantment>) byKey.get(null);
            byKey.setAccessible(false);

            Field byName=Enchantment.class.getDeclaredField("byName");
            byName.setAccessible(true);
            enchantmentByName=(Map<String, Enchantment>) byName.get(null);
            byName.setAccessible(false);
        }
        catch (Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean openEnchantmentRegistrations()
    {
        if (acceptRegisterEnchantment==null)
        {
            return false;
        }
        try
        {
            acceptRegisterEnchantment.setAccessible(true);
            acceptRegisterEnchantment.set(null,true);
            acceptRegisterEnchantment.setAccessible(false);
            return true;
        }
        catch (Throwable e)
        {
            return false;
        }
    }

    public void closeEnchantmentRegistrations()
    {
        Enchantment.stopAcceptingRegistrations();
    }

    public boolean registerEnchantment(Enchantment enchantment)
    {
        if (!openEnchantmentRegistrations())
        {
            return false;
        }
        Enchantment.registerEnchantment(enchantment);
        closeEnchantmentRegistrations();
        return true;
    }

    public Enchantment removeEnchantment(NamespacedKey key)
    {
        Enchantment enchantment=enchantmentByKey.remove(key);
        enchantmentByName.values().remove(enchantment);
        return enchantment;
    }

    public Enchantment removeEnchantment(String name)
    {
        Enchantment enchantment=enchantmentByName.remove(name);
        enchantmentByKey.values().remove(enchantment);
        return enchantment;
    }

    public Enchantment getEnchantment(NamespacedKey key)
    {
        return enchantmentByKey.get(key);
    }

    public Enchantment getEnchantment(String name)
    {
        return enchantmentByName.get(name);
    }

    public Enchantment[] getEnchantments()
    {
        return Enchantment.values();
    }
}