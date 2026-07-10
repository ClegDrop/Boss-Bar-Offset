package clegdrop.bossbaroffset;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BossBarOffsetPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BossBarOffsetPlugin.class);
		RuneLite.main(args);
	}
}