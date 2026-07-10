package clegdrop.bossbaroffset;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(BossBarOffsetConfig.GROUP_KEY)
public interface BossBarOffsetConfig extends Config
{
	String GROUP_KEY = "bossbaroffset";

	@Range(
			min = 0,
			max = 25
	)


	@ConfigItem(
			keyName = "barOffsetY",
			name = "Y Offset",
			description = "Vertical offset.",
			position = 1
	)
	default int barOffsetY()
	{
		return 0;
	}
}
