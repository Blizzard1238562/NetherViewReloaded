package me.gorgeousone.netherview.utils;

import org.bukkit.Bukkit;

public final class VersionUtils {
	
	private VersionUtils() {}
	
	private static final String MINECRAFT_VERSION;
	private static final int[] CURRENT_VERSION_INTS = new int[3];
	
	public static final boolean IS_LEGACY_SERVER = false;
	
	static {
		String fullVersion = Bukkit.getBukkitVersion();
		String[] versionParts = fullVersion.split("-")[0].split("\\.");
		for (int i = 0; i < Math.min(versionParts.length, 3); i++) {
			CURRENT_VERSION_INTS[i] = Integer.parseInt(versionParts[i]);
		}
		MINECRAFT_VERSION = CURRENT_VERSION_INTS[0] + "." + CURRENT_VERSION_INTS[1] + "." + CURRENT_VERSION_INTS[2];
	}
	
	public static boolean isVersionLowerThan(String currentVersion, String requestedVersion) {
		
		int[] currentVersionInts = getVersionAsIntArray(currentVersion, "\\.");
		int[] requestedVersionInts = getVersionAsIntArray(requestedVersion, "\\.");
		
		for (int i = 0; i < Math.min(currentVersionInts.length, requestedVersionInts.length); i++) {
			
			int versionDiff = currentVersionInts[i] - requestedVersionInts[i];
			
			if (versionDiff > 0) {
				return false;
			}else if (versionDiff < 0) {
				return true;
			}
		}
		
		return requestedVersionInts.length > currentVersionInts.length;
	}
	
	public static boolean serverIsAtOrAbove(String requestedVersion) {
		
		int[] requestedVersionInts = getVersionAsIntArray(requestedVersion, "\\.");
		
		for (int i = 0; i < requestedVersionInts.length; i++) {
			
			int versionDiff = requestedVersionInts[i] - CURRENT_VERSION_INTS[i];
			
			if (versionDiff > 0) {
				return false;
			}else if (versionDiff < 0){
				return true;
			}
		}
		
		return true;
	}
	
	private static int[] getVersionAsIntArray(String version, String delimiter) {
		
		String[] split = version.split(delimiter);
		
		if (split.length > 3) {
			throw new IllegalArgumentException("Cannot process awfully long version string \"" + version + "\".");
		}
		
		int[] versionInts = new int[split.length];
		
		for (int i = 0; i < versionInts.length; i++) {
			versionInts[i] = Integer.parseInt(split[i]);
		}
		
		return versionInts;
	}
}