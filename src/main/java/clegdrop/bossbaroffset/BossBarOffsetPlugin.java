package clegdrop.bossbaroffset;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Boss HP Bar Adjustments",
	description = "Adjust the vertical positioning of boss health bars."
)
public class BossBarOffsetPlugin extends Plugin
{
	private static final String RUNELITE_GROUP_KEY = "runelite";
	private static final String POSITION_KEY = "_preferredPosition";
	private static final String LOCATION_KEY = "_preferredLocation";
	private static final String HP_BAR_NAME = "HEALTH_OVERLAY_BAR";
	private static final String TOB_HP_BAR_NAME = "TOB_HEALTH_BAR";
	private static final int HP_BAR_TEXT_UPDATE_SCRIPT_ID = 2102;
	private static final int TOB_PROGRESS_BAR_UPDATE_SCRIPT_ID = 2304;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private BossBarOffsetConfig config;

	private boolean inTob = false;

	@Provides
	BossBarOffsetConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossBarOffsetConfig.class);
	}

	@Override
	protected void startUp()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final String reg_position = configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + POSITION_KEY);
		final String reg_location = configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + LOCATION_KEY);
		if (reg_location == null)
		{
			clientThread.invoke(() -> adjustHealthBarLocation(reg_position));
		}

		final String tob_position = configManager.getConfiguration(RUNELITE_GROUP_KEY, TOB_HP_BAR_NAME + POSITION_KEY);
		final String tob_location = configManager.getConfiguration(RUNELITE_GROUP_KEY, TOB_HP_BAR_NAME + LOCATION_KEY);
		if (tob_location == null)
		{
			clientThread.invoke(() -> {
				inTob = calcIsInTob();
				adjustTobProgressBarLocation(tob_position);
			});
		}
	}

	@Override
	protected void shutDown()
	{
		inTob = false;
		resetWidgetPositions();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (e.getVarbitId() == VarbitID.TOB_CLIENT_PARTYSTATUS)
		{
			inTob = calcIsInTob();
			return;
		}

		// If the HP bar is set to a dynamic location do not make any changes
		if (configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + LOCATION_KEY) != null)
		{
			return;
		}

		if (e.getVarbitId() == VarbitID.XPDROPS_ENABLED
				|| e.getVarbitId() == VarbitID.XPDROPS_POSITION
				|| e.getVarbitId() == VarbitID.HPBAR_HUD_BOSS_DISABLED)
		{
			clientThread.invoke(() -> adjustHealthBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + POSITION_KEY)));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (e.getGroup().equals(BossBarOffsetConfig.GROUP_KEY))
		{
			// If the TOB progress bar is set to a dynamic location do not make any changes
			if (configManager.getConfiguration(RUNELITE_GROUP_KEY, TOB_HP_BAR_NAME + LOCATION_KEY) != null)
			{
				return;
			}

			if (inTob)
			{
				clientThread.invoke(() -> adjustTobProgressBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, TOB_HP_BAR_NAME + POSITION_KEY)));
			}

			// If the HP bar is set to a dynamic location do not make any changes
			if (configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + LOCATION_KEY) != null)
			{
				return;
			}

			clientThread.invoke(() -> adjustHealthBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + POSITION_KEY)));
			return;
		}

		if (!e.getGroup().equals(RUNELITE_GROUP_KEY))
		{
			return;
		}

		if (e.getKey().equals(HP_BAR_NAME + POSITION_KEY))
		{
			clientThread.invoke(() -> adjustHealthBarLocation(e.getNewValue()));
		}
		else if (e.getKey().equals(TOB_HP_BAR_NAME + POSITION_KEY))
		{
			clientThread.invoke(() -> adjustTobProgressBarLocation(e.getNewValue()));
		}
		else if (e.getKey().equals(HP_BAR_NAME + LOCATION_KEY) || e.getKey().equals(TOB_HP_BAR_NAME + LOCATION_KEY))
		{
			// If the widget was changed to a dynamic location we want to reset everything's forced position
			if (e.getNewValue() != null)
			{
				clientThread.invoke(this::resetWidgetPositions);
			}
		}
	}

	@Subscribe
	public void onScriptPostFired(final ScriptPostFired e)
	{
		if (e.getScriptId() == TOB_PROGRESS_BAR_UPDATE_SCRIPT_ID)
		{
			clientThread.invoke(() -> adjustTobProgressBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, TOB_HP_BAR_NAME + POSITION_KEY)));
			return;
		}

		// HP_HUD_UPDATE gets triggered all over the place, and pretty often. But it's the only way I could figure out
		// To trigger the offsetting on the bar when it is first displayed at maiden
		if (e.getScriptId() == ScriptID.HP_HUD_UPDATE && inTob)
		{
			clientThread.invoke(() -> adjustTobProgressBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, TOB_HP_BAR_NAME + POSITION_KEY)));
			return;
		}

		// When the HP bar is loaded we may need to adjust the position
		// The easiest way to check when the HP bar has been loaded is whenever the HP Bar Text has changed
		if (e.getScriptId() != HP_BAR_TEXT_UPDATE_SCRIPT_ID)
		{
			return;
		}

		// If the HP bar is set to a dynamic location do not make any changes
		if (configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + LOCATION_KEY) != null)
		{
			return;
		}

		clientThread.invoke(() -> adjustHealthBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + POSITION_KEY)));
	}

	private void adjustHealthBarLocation(final String newVal)
	{
		// if the boss HP bar isn't disabled we don't need to do anything
		if (client.getVarbitValue(VarbitID.HPBAR_HUD_BOSS_DISABLED) == 1)
		{
			resetWidgetPositions();
			return;
		}

		// If the Boss Health Bar isn't available then we don't need to do anything
		final Widget hpContainer = client.getWidget(InterfaceID.HpbarHud.HPDODGER);
		final Widget hpBar = client.getWidget(InterfaceID.HpbarHud.HP);
		if (hpContainer == null || hpBar == null)
		{
			resetWidgetPositions();
			return;
		}

		// The default position for the HP Bar is `TOP_CENTER` which will be saved as `null` in the config
		// We only care about the top snapped positions as otherwise the adjustments aren't necessary to save space
		final boolean hasHpBarSnappedToTheTop = newVal == null || newVal.equals("TOP_RIGHT") || newVal.equals("TOP_LEFT");
		if (!hasHpBarSnappedToTheTop)
		{
			resetWidgetPositions();
			return;
		}

		// As long as the location of the xp drops (VarbitID.XPDROPS_POSITION) is set to middle (1) the hp bar will be shifted downwards (if snapped to any top position)
		// Previously, this plugin only applied when this was set to the middle. Now though, it provides the offset
		// functionality regardless of XPDROPS position so checking for the XPDROPS_POSITION is not necessary

		// Force the HP container to the top of the screen
		hpContainer.setForcedPosition(hpContainer.getRelativeX(), config.barOffsetY());
	}


	private void resetWidgetPositions()
	{
		final Widget xpTrackerDodger = client.getWidget(InterfaceID.XpDrops.CONTAINERDODGER);
		final Widget xpTracker = client.getWidget(InterfaceID.XpDrops.CONTAINER);
		final Widget hpContainer = client.getWidget(InterfaceID.HpbarHud.HPDODGER);

		resetWidgetForcedPosition(xpTrackerDodger);
		resetWidgetForcedPosition(xpTracker);
		resetWidgetForcedPosition(hpContainer);

		final Widget tobMiddleDodger = client.getWidget(InterfaceID.TobHud.MIDDLE_DODGER);
		resetWidgetForcedPosition(tobMiddleDodger);
	}

	private void resetWidgetForcedPosition(final Widget w)
	{
		if (w == null)
		{
			return;
		}
		w.setForcedPosition(-1, -1);
	}

	private boolean calcIsInTob()
	{
		return client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS) == 2 || client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS) == 3;
	}

	private void adjustTobProgressBarLocation(String tobProgressBarPosition)
	{
		if (!inTob)
		{
			return;
		}

		final Widget tobMiddleDodger = client.getWidget(InterfaceID.TobHud.MIDDLE_DODGER);
		if (tobMiddleDodger == null)
		{
			return;
		}
		tobMiddleDodger.setForcedPosition(tobMiddleDodger.getRelativeX(), config.tobOffset());

		final boolean isDockerTopOrTopRight = tobProgressBarPosition == null || tobProgressBarPosition.equals("TOP_RIGHT");
		final Widget xpTrackerDodger = client.getWidget(InterfaceID.XpDrops.CONTAINERDODGER);
		final Widget xpTracker = client.getWidget(InterfaceID.XpDrops.CONTAINER);
		final Widget progressContainer = client.getWidget(InterfaceID.TobHud.PROGRESS_CONTAINER);
		if (isDockerTopOrTopRight && xpTrackerDodger != null && xpTracker != null && progressContainer != null)
		{
			xpTrackerDodger.setForcedPosition(xpTrackerDodger.getRelativeX(),  config.xpTrackerOffset());
			xpTracker.setForcedPosition(xpTracker.getRelativeX(), 0);
		}
	}
}
