package me.gorgeousone.netherview.utils;

import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NmsUtils {
	
	private static Method ENTITY_GET_HANDLE;
	
	static {
		try {
			Class<?> craftEntityClass = MinecraftReflection.getCraftEntityClass();
			ENTITY_GET_HANDLE = craftEntityClass.getMethod("getHandle");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	public static Object getHandle(Entity entity) throws InvocationTargetException, IllegalAccessException {
		return ENTITY_GET_HANDLE.invoke(entity);
	}
	
	public static Class<?> getNmsClass(String nmsClassString) throws ClassNotFoundException {
		return MinecraftReflection.getMinecraftClass(nmsClassString);
	}
	
	public static Class<?> getCraftBukkitClass(String cbClassString) throws ClassNotFoundException {
		return MinecraftReflection.getCraftBukkitClass(cbClassString);
	}
}