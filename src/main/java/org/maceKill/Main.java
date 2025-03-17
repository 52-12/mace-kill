package org.maceKill;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class Main extends Plugin {
	
	@Override
	public void onLoad() {
		final MaceKiller maceKiller = new MaceKiller();
		RusherHackAPI.getModuleManager().registerFeature(maceKiller);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("Example plugin unloaded!");
	}
	
}