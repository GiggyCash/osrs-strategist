package compass;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;

/** Movable on-game checklist complementing the Compass sidebar. */
class MethodGuidanceOverlay extends OverlayPanel
{
    private static final int OVERLAY_WIDTH = 320;
    private static final int STEP_TEXT_WIDTH = 285;

    private GuidanceChecklist checklist;

    @Inject
    public MethodGuidanceOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setMovable(true);
        panelComponent.setPreferredSize(new Dimension(OVERLAY_WIDTH, 0));
    }

    public synchronized void update(GuidanceChecklist checklist)
    {
        this.checklist = checklist;
    }

    public synchronized void clear()
    {
        checklist = null;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (checklist == null) return null;
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Method Guidance")
                .color(StrategistTheme.GOLD)
                .build());
        var metrics = graphics.getFontMetrics();
        addSection("METHOD", checklist.title, metrics);
        addSection("BRING", checklist.getBring(), metrics);
        addSection("WHERE", checklist.getWhere(), metrics);
        addSection("DO", fallbackAction(checklist), metrics);
        addSection("PROGRESS", checklist.getProgress(), metrics);
        addSection("IMPORTANT", checklist.getImportant(), metrics);
        return super.render(graphics);
    }

    private void addSection(String heading, String value, FontMetrics metrics)
    {
        if (value == null || value.trim().isEmpty()) return;
        panelComponent.getChildren().add(LineComponent.builder()
                .left(heading)
                .leftColor("IMPORTANT".equals(heading)
                        ? StrategistTheme.WARNING : StrategistTheme.GOLD_SOFT)
                .build());
        for (String line : wrap(value, metrics, STEP_TEXT_WIDTH))
            panelComponent.getChildren().add(OverlayUi.line(line,
                    "IMPORTANT".equals(heading)
                            ? StrategistTheme.WARNING : StrategistTheme.TEXT));
    }

    private static String fallbackAction(GuidanceChecklist checklist)
    {
        if (checklist.getAction() != null
                && !checklist.getAction().trim().isEmpty())
            return checklist.getAction();
        var pending = checklist.firstPending();
        return pending == null ? checklist.getSubtitle() : pending.getLabel();
    }

    static java.util.List<String> wrap(String text, FontMetrics metrics, int maxWidth)
    {
        return OverlayUi.wrap(text, metrics, maxWidth);
    }

}

/** Short-lived CLOG-style reward surface for any Compass completion. */
class MilestoneRewardOverlay extends OverlayPanel
{
    private static final long DISPLAY_MILLIS = 6500L;

    private StrategistRewardNotification activeReward;
    private long hideAtMillis;

    @Inject
    public MilestoneRewardOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_CENTER);
        setMovable(false);
    }

    public synchronized void show(MilestoneCompletion completion)
    {
        show(StrategistRewardNotification.fromMilestone(completion));
    }

    public synchronized void show(StrategistRewardNotification reward)
    {
        activeReward = reward;
        hideAtMillis = reward == null
                ? 0L
                : System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    public synchronized void clear()
    {
        activeReward = null;
        hideAtMillis = 0L;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (activeReward == null
                || System.currentTimeMillis() >= hideAtMillis)
        {
            clear();
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(activeReward.getHeader())
                        .color(StrategistTheme.GOLD)
                        .build()
        );
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(activeReward.getLeft())
                        .right(activeReward.getRight())
                        .build()
        );
        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(activeReward.getFooterLeft())
                        .right(activeReward.getFooterRight())
                        .build()
        );

        return super.render(graphics);
    }
}

/**
 * Movable on-game decision summary.
 *
 * <p>The full planner graph remains internal; players see only the selected
 * goal relationship, reason, blocker, current step, and next unlock when it is
 * useful.</p>
 */
class RecommendationDetailsOverlay extends OverlayPanel
{
    private static final int PANEL_WIDTH = 340;
    private static final int TEXT_WIDTH = 310;
    private Recommendation recommendation;
    private GoalRecommendationContext goalContext;

    @Inject
    public RecommendationDetailsOverlay(OsrsStrategistPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_RIGHT);
        setMovable(true);
        panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
    }

    public synchronized void showRecommendation(Recommendation recommendation)
    {
        showRecommendation(recommendation, null);
    }

    public synchronized void showRecommendation(Recommendation recommendation,
            GoalRecommendationContext goalContext)
    {
        this.recommendation = recommendation;
        this.goalContext = goalContext;
    }

    public synchronized void clear()
    {
        recommendation = null;
        goalContext = null;
    }

    public synchronized boolean hasRecommendation()
    {
        return recommendation != null;
    }

    @Override
    public synchronized Dimension render(Graphics2D graphics)
    {
        if (recommendation == null) return null;

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Compass Details")
                .color(StrategistTheme.GOLD)
                .build());

        var metrics = graphics.getFontMetrics();
        for (Presentation.Section section
                : Presentation.detailsSections(
                        recommendation, goalContext))
        {
            addLine(section.heading, StrategistTheme.GOLD_SOFT);
            for (String wrapped : wrap(section.getValue(), metrics, TEXT_WIDTH))
                addLine(wrapped, stateColor(section.getValue()));
        }
        return super.render(graphics);
    }

    private void addLine(String text, Color color)
    {
        panelComponent.getChildren().add(OverlayUi.line(text, color));
    }

    private static Color stateColor(String line)
    {
        if (line.startsWith("✓")) return StrategistTheme.SUCCESS;
        if (line.startsWith("✕") || line.startsWith("Blocked")) return StrategistTheme.DANGER;
        if (line.startsWith("?")) return StrategistTheme.WARNING;
        return StrategistTheme.TEXT;
    }

    static List<String> wrap(String text, FontMetrics metrics, int maxWidth)
    {
        if (metrics == null || maxWidth <= 0)
            return Collections.singletonList(text == null ? "" : text);
        return OverlayUi.wrap(text, metrics, maxWidth);
    }

}
