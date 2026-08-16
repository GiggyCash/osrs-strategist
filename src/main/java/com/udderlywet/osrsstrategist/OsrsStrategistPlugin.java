package com.udderlywet.osrsstrategist;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
        name = "OSRS Strategist",
        description = "Adaptive progression strategy for every OSRS account type",
        tags = {"strategy", "progression", "maxing", "ironman", "uim", "gim", "clues", "farming"}
)
public class OsrsStrategistPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OsrsStrategistConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    private NavigationButton navButton;
    private OsrsStrategistPanel panel;

    @Provides
    OsrsStrategistConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(OsrsStrategistConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new OsrsStrategistPanel();

        BufferedImage icon = createTemporaryIcon();

        navButton = NavigationButton.builder()
                .tooltip("OSRS Strategist")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        updateAccountPanel();
    }

    @Override
    protected void shutDown()
    {
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }

        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            updateAccountPanel();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        updateAccountPanel();
    }

    private void updateAccountPanel()
    {
        if (panel == null)
        {
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN
                || client.getLocalPlayer() == null)
        {
            SwingUtilities.invokeLater(() ->
                    panel.updateAccount(
                            "Waiting for login...",
                            "Unknown",
                            0
                    )
            );
            return;
        }

        String playerName = client.getLocalPlayer().getName();

        if (playerName == null || playerName.isEmpty())
        {
            playerName = "Unknown Player";
        }

        int accountType =
                client.getVarbitValue(Varbits.ACCOUNT_TYPE);

        String readableAccountType =
                formatAccountType(accountType);

        int totalLevel =
                client.getTotalLevel();

        String finalPlayerName =
                playerName;

        SwingUtilities.invokeLater(() ->
                panel.updateAccount(
                        finalPlayerName,
                        readableAccountType,
                        totalLevel
                )
        );
    }

    private String formatAccountType(int type)
    {
        switch (type)
        {
            case 0:
                return "Main";

            case 1:
                return "Ironman";

            case 2:
                return "Ultimate Ironman";

            case 3:
                return "Hardcore Ironman";

            case 4:
                return "Group Ironman";

            case 5:
                return "Hardcore Group Ironman";

            case 6:
                return "Unranked Group Ironman";

            default:
                return "Unknown";
        }
    }

    private BufferedImage createTemporaryIcon()
    {
        BufferedImage image =
                new BufferedImage(
                        16,
                        16,
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                image.createGraphics();

        graphics.setColor(
                new Color(60, 45, 30)
        );

        graphics.fillRect(
                0,
                0,
                16,
                16
        );

        graphics.setColor(
                new Color(212, 167, 44)
        );

        graphics.drawOval(
                1,
                1,
                13,
                13
        );

        graphics.drawLine(
                8,
                3,
                8,
                13
        );

        graphics.drawLine(
                3,
                8,
                13,
                8
        );

        graphics.dispose();

        return image;
    }
}