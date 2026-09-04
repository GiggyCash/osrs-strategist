package compass;
import static java.lang.Math.*;
import static compass.Text.get;

import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Calm secondary progress view; DO NEXT remains outside this component. */
public final class ProgressViewPanel extends JPanel
{
    final JLabel sessionXp = new JLabel("0 XP");
    final JLabel sessionMeta = new JLabel(get(1158));
    final JTextArea target = textArea(get(1159));
    private final JTextArea leadingSkill = textArea("");
    final JTextArea planPath = textArea(get(1160));
    private final JTextArea milestones = textArea("");
    private final JPanel milestoneCard = card();
    final JTextArea lastSession = textArea("");
    private final JPanel lastSessionCard = card();
    final ProgressChartPanel chart = new ProgressChartPanel();

    public ProgressViewPanel()
    {
        this(1.0f);
    }

    public ProgressViewPanel(float textScale)
    {
        var scale = max(1.0f, min(1.6f, textScale));
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(StrategistTheme.BACKGROUND);

        var content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(heading("PROGRESS", 15f * scale));
        content.add(Box.createVerticalStrut(8));

        var summary = card();
        sessionXp.setForeground(StrategistTheme.GOLD);
        sessionXp.setFont(sessionXp.getFont().deriveFont(Font.BOLD, 17f * scale));
        sessionMeta.setForeground(StrategistTheme.MUTED_TEXT);
        sessionMeta.setFont(sessionMeta.getFont().deriveFont(13f * scale));
        summary.add(sessionXp);
        summary.add(Box.createVerticalStrut(4));
        summary.add(sessionMeta);
        content.add(summary);
        content.add(Box.createVerticalStrut(8));

        var current = card();
        current.add(heading("CURRENT TARGET", 12f * scale));
        current.add(Box.createVerticalStrut(5));
        target.setFont(target.getFont().deriveFont(14f * scale));
        current.add(target);
        current.add(Box.createVerticalStrut(5));
        leadingSkill.setFont(leadingSkill.getFont().deriveFont(13f * scale));
        leadingSkill.setForeground(StrategistTheme.MUTED_TEXT);
        current.add(leadingSkill);
        content.add(current);
        content.add(Box.createVerticalStrut(8));

        var plan = card();
        plan.add(heading("CURRENT PLAN", 12f * scale));
        plan.add(Box.createVerticalStrut(5));
        planPath.setFont(planPath.getFont().deriveFont(13f * scale));
        plan.add(planPath);
        content.add(plan);
        content.add(Box.createVerticalStrut(8));

        var chartCard = card();
        chartCard.add(heading("SESSION XP", 12f * scale));
        chartCard.add(Box.createVerticalStrut(5));
        chart.setAlignmentX(LEFT_ALIGNMENT);
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        chartCard.add(chart);
        content.add(chartCard);
        content.add(Box.createVerticalStrut(8));

        milestoneCard.add(heading("MILESTONES", 12f * scale));
        milestoneCard.add(Box.createVerticalStrut(5));
        milestones.setFont(milestones.getFont().deriveFont(13f * scale));
        milestoneCard.add(milestones);
        milestoneCard.setVisible(false);
        content.add(milestoneCard);

        content.add(Box.createVerticalStrut(8));
        lastSessionCard.add(heading("LAST SESSION", 12f * scale));
        lastSessionCard.add(Box.createVerticalStrut(5));
        lastSession.setFont(lastSession.getFont().deriveFont(13f * scale));
        lastSessionCard.add(lastSession);
        lastSessionCard.setVisible(false);
        content.add(lastSessionCard);
        add(content, BorderLayout.NORTH);
    }

    public void setSnapshot(ProgressSessionSnapshot snapshot)
    {
        if (snapshot == null)
        {
            sessionXp.setText("0 XP");
            sessionMeta.setText(get(1158));
            target.setText(get(1159));
            leadingSkill.setText("");
            chart.setBuckets(null);
            milestones.setText("");
            milestoneCard.setVisible(false);
            return;
        }
        sessionXp.setText(format(snapshot.getTotalXpGained()) + " XP");
        sessionMeta.setText(snapshot.getLevelsGained() + " levels • "
                + duration(snapshot.activeDurationMillis) + " active");
        chart.setBuckets(snapshot.getBuckets());
        updateTarget(snapshot.getTargetProjection());
        snapshot.getSkills().values().stream()
                .filter(value -> value.getXpGained() > 0)
                .max(Comparator.comparingInt(SkillSessionProgress::getXpGained))
                .ifPresentOrElse(value -> leadingSkill.setText(
                        value.getSkill().getName() + " +"
                                + format(value.getXpGained())),
                        () -> leadingSkill.setText(get(1161)));
        updateMilestones(snapshot.milestones);
    }

    public void setPlan(StrategicPlan plan)
    {
        if (plan == null)
        {
            planPath.setText(get(1160));
            return;
        }
        var text = new StringBuilder();
        text.append("NOW  ").append(plan.getCurrentStep().getObjective());
        if (plan.getNextStep() != null)
            text.append("\nNEXT  ").append(plan.getNextStep().getObjective());
        text.append("\nTARGET  ")
                .append(GoalRecommendationContext.displayName(plan.getGoal()));
        var steps = plan.getSteps().size();
        if (steps > 1)
            text.append("\nStep ").append(plan.getCurrentIndex() + 1)
                    .append(" of ").append(steps);
        planPath.setText(text.toString());
    }

    public void setHistory(ProgressHistory history)
    {
        List<ProgressSessionSummary> sessions = history == null
                ? java.util.Collections.emptyList() : history.getSessions();
        if (sessions.isEmpty())
        {
            lastSession.setText("");
            lastSessionCard.setVisible(false);
            return;
        }
        var summary = sessions.get(sessions.size() - 1);
        StringBuilder text = new StringBuilder()
                .append('+').append(format(summary.getTotalXpGained()))
                .append(" XP • ").append(summary.getLevelsGained())
                .append(" levels • ")
                .append(duration(summary.activeDurationMillis))
                .append(" active");
        summary.getXpBySkill().entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .ifPresent(value -> text.append("\nTop: ")
                        .append(value.getKey().getName()).append(" +")
                        .append(format(value.getValue())));
        if (!summary.milestones.isEmpty())
        {
            var first = max(0, summary.milestones.size() - 2);
            text.append(get(1162));
            for (int index = first; index < summary.milestones.size(); index++)
            {
                if (index > first) text.append("; ");
                text.append(summary.milestones.get(index).title);
            }
        }
        lastSession.setText(text.toString());
        lastSessionCard.setVisible(true);
    }



    private void updateMilestones(List<ProgressMilestone> values)
    {
        if (values == null || values.isEmpty())
        {
            milestones.setText("");
            milestoneCard.setVisible(false);
            return;
        }
        var text = new StringBuilder();
        var first = max(0, values.size() - 3);
        for (int i = first; i < values.size(); i++)
        {
            if (text.length() > 0) text.append('\n');
            text.append("• ").append(values.get(i).title);
        }
        milestones.setText(text.toString());
        milestoneCard.setVisible(true);
    }

    private void updateTarget(TargetProjection projection)
    {
        if (projection == null
                || projection.getState() == TargetProjection.State.NO_TARGET)
        {
            target.setText(get(1159));
            return;
        }
        var value = projection.getTarget();
        String prefix = value.getSkill().getName() + " to "
                + value.targetLevel + "\n"
                + format(projection.getXpRemaining()) + " XP remaining";
        switch (projection.getState())
        {
            case COMPLETE:
                target.setText(value.getSkill().getName() + " "
                        + value.targetLevel + " complete");
                break;
            case READY:
                target.setText(prefix + " • " + duration(projection.getEtaMillis()));
                break;
            default:
                target.setText(prefix + get(1163));
                break;
        }
    }

    private static JPanel card()
    {
        var panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(StrategistTheme.CARD);
        panel.setBorder(StrategistTheme.cardBorder());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel heading(String text, float size)
    {
        var label = new JLabel(text);
        label.setForeground(StrategistTheme.GOLD_SOFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea textArea(String text)
    {
        var area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(StrategistTheme.TEXT);
        area.setBorder(null);
        area.setFocusable(false);
        area.setAlignmentX(LEFT_ALIGNMENT);
        return area;
    }

    private static String format(long value)
    {
        return NumberFormat.getIntegerInstance(Locale.UK).format(value);
    }

    private static String duration(long millis)
    {
        var minutes = max(0L, round(millis / 60_000.0));
        if (minutes < 60L) return minutes + " min";
        var hours = minutes / 60L;
        var remainder = minutes % 60L;
        return remainder == 0L ? hours + " hr"
                : hours + " hr " + remainder + " min";
    }
}
