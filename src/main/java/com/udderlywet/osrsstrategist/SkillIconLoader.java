package com.udderlywet.osrsstrategist;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.util.ImageUtil;

/**
 * Loads RuneScape's own skill sprites through RuneLite instead of bundling
 * replacement artwork. This keeps Compass visually native and means new
 * RuneLite sprite updates can flow through without us maintaining image files.
 *
 * <p>Scaled icons are cached so routine account refreshes do not briefly clear
 * and reload the same sprite. A per-label request key also prevents an older
 * asynchronous sprite callback from replacing a newer recommendation icon.</p>
 */
@Singleton
public class SkillIconLoader
{
    private final SpriteManager spriteManager;
    private final Map<String, ImageIcon> iconCache = new ConcurrentHashMap<>();
    private final Map<JLabel, String> latestRequest =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Inject
    public SkillIconLoader(SpriteManager spriteManager)
    {
        this.spriteManager = spriteManager;
    }

    public void load(Skill skill, JLabel target, int size)
    {
        if (skill == null || target == null)
        {
            return;
        }

        HiscoreSkill hiscoreSkill;
        try
        {
            hiscoreSkill = HiscoreSkill.valueOf(skill.name());
        }
        catch (IllegalArgumentException ex)
        {
            return;
        }

        String requestKey = skill.name() + ':' + size;
        latestRequest.put(target, requestKey);

        ImageIcon cached = iconCache.get(requestKey);
        if (cached != null)
        {
            applyIcon(target, requestKey, cached);
            return;
        }

        spriteManager.getSpriteAsync(
                hiscoreSkill.getSpriteId(),
                0,
                sprite -> cacheAndApply(target, requestKey, sprite, size)
        );
    }

    private void cacheAndApply(
            JLabel target,
            String requestKey,
            BufferedImage sprite,
            int size)
    {
        if (sprite == null)
        {
            return;
        }

        BufferedImage scaled = ImageUtil.resizeImage(
                ImageUtil.resizeCanvas(sprite, 25, 25),
                size,
                size
        );
        ImageIcon icon = new ImageIcon(scaled);
        iconCache.put(requestKey, icon);
        applyIcon(target, requestKey, icon);
    }

    private void applyIcon(JLabel target, String requestKey, ImageIcon icon)
    {
        Runnable apply = () ->
        {
            synchronized (latestRequest)
            {
                if (!requestKey.equals(latestRequest.get(target)))
                {
                    return;
                }
            }

            target.setIcon(icon);
            target.revalidate();
            target.repaint();
        };

        if (SwingUtilities.isEventDispatchThread())
        {
            apply.run();
        }
        else
        {
            SwingUtilities.invokeLater(apply);
        }
    }
}
