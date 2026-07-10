package clegdrop.bossbaroffset;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptPostFired;
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
	name = "Boss Bar Offset"
)
public class BossBarOffsetPlugin extends Plugin
{
	private static final String RUNELITE_GROUP_KEY = "runelite";
	private static final String POSITION_KEY = "_preferredPosition";
	private static final String LOCATION_KEY = "_preferredLocation";
	private static final String HP_BAR_NAME = "HEALTH_OVERLAY_BAR";
	private static final int DEFAULT_Y_POSITION_OF_HP_CONTAINER = 23;
	private static final int HP_BAR_TEXT_UPDATE_SCRIPT_ID = 2102;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private BossBarOffsetConfig config;

	@Provides
	BossBarOffsetConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossBarOffsetConfig.class);
	}

	@Override
	protected void startUp() throws Exception
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

	}

	@Override
	protected void shutDown() throws Exception
	{
		resetWidgetPositions();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (e.getGroup().equals(BossBarOffsetConfig.GROUP_KEY))
		{
			clientThread.invoke(() -> adjustHealthBarLocation(configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + POSITION_KEY)));
			return;
		}

		// If the HP bar is set to a dynamic location do not make any changes
		if (configManager.getConfiguration(RUNELITE_GROUP_KEY, HP_BAR_NAME + LOCATION_KEY) != null)
		{
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
	}

	@Subscribe
	public void onScriptPostFired(final ScriptPostFired e)
	{
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
		final boolean hasHpBarSnappedToTheTop = newVal == null;
		if (!hasHpBarSnappedToTheTop)
		{
			resetWidgetPositions();
			return;
		}

		hpContainer.setForcedPosition(hpContainer.getRelativeX(), 0);
		hpContainer.setForcedPosition(hpContainer.getRelativeY(), config.barOffsetY());
	}

	private void resetWidgetPositions()
	{
		final Widget hpContainer = client.getWidget(InterfaceID.HpbarHud.HPDODGER);

		resetWidgetForcedPosition(hpContainer);
	}

	private void resetWidgetForcedPosition(final Widget w)
	{
		if (w == null)
		{
			return;
		}
		w.setForcedPosition(-1, -1);
	}

}
