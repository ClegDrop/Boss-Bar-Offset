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
			name = "Vertical Offset",
			description = "The amount of pixels from the top of the screen. Default is 25",
			position = 1
	)
	default int barOffsetY()
	{
		return 0;
	}

	@Range(
			min = 0,
			max = 23
	)
	@ConfigItem(
			keyName = "tobOffset",
			name = "ToB Offset",
			description = "The Theatre of Blood progress bar vertical offset. Default is 23.",
			position = 2
	)
	default int tobOffset()
	{
		return 0;
	}

	@Range(
			min = 0,
			max = 43
	)
	@ConfigItem(
			keyName = "xpTrackerOffset",
			name = "ToB XP Tracker",
			description = "The vertical offset of the XP tracker while inside ToB. Default is 43.",
			position = 3
	)
	default int xpTrackerOffset()
	{
		return 0;
	}

}
