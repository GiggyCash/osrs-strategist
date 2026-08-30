package com.udderlywet.osrsstrategist;

import java.awt.image.BufferedImage;
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
 * replacement artwork. This keeps Strategist visually native and means new
 * RuneLite sprite updates can flow through without us maintaining image files.
 */
@Singleton
public class SkillIconLoader
{
    private final SpriteManager spriteManager;

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

        spriteManager.getSpriteAsync(
                hiscoreSkill.getSpriteId(),
                0,
                sprite -> applySprite(target, sprite, size)
        );
    }

    private static void applySprite(
            JLabel target,
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

        SwingUtilities.invokeLater(() ->
        {
            target.setIcon(new ImageIcon(scaled));
            target.revalidate();
            target.repaint();
        });
    }
}
