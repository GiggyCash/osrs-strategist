package compass;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
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
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkillIconLoader
{
    private final SpriteManager spriteManager;
    private final Map<String, ImageIcon> iconCache = new ConcurrentHashMap<>();
    private final Map<JLabel, String> latestRequest =
            Collections.synchronizedMap(new WeakHashMap<>());

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

        var requestKey = skill.name() + ':' + size;
        latestRequest.put(target, requestKey);

        var cached = iconCache.get(requestKey);
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

    /**
     * Clear a label and invalidate every callback issued for its old icon.
     * This matters when a skill recommendation is replaced by a quest or
     * upgrade before RuneLite finishes loading the skill sprite.
     */
    public void clear(JLabel target)
    {
        if (target == null) return;
        var requestKey = "clear:" + System.nanoTime();
        latestRequest.put(target, requestKey);
        Runnable clear = () ->
        {
            synchronized (latestRequest)
            {
                if (!requestKey.equals(latestRequest.get(target))) return;
            }
            target.setIcon(null);
            target.revalidate();
            target.repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) clear.run();
        else SwingUtilities.invokeLater(clear);
    }

    void cacheAndApply(
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
        var icon = new ImageIcon(scaled);
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
