package com.udderlywet.osrsstrategist;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Calm secondary progress view; DO NEXT remains outside this component. */
public final class ProgressViewPanel extends JPanel
{
    private final JLabel sessionXp = new JLabel("0 XP");
    private final JLabel sessionMeta = new JLabel("No progress this session");
    private final JTextArea target = textArea("No active skill target");
    private final JTextArea leadingSkill = textArea("");
    private final JTextArea planPath = textArea("No active goal plan");
    private final JTextArea milestones = textArea("");
    private final JPanel milestoneCard = card();
    private final JTextArea lastSession = textArea("");
    private final JPanel lastSessionCard = card();
    private final ProgressChartPanel chart = new ProgressChartPanel();

    public ProgressViewPanel()
    {
        this(1.0f);
    }

    public ProgressViewPanel(float textScale)
    {
        float scale = Math.max(1.0f, Math.min(1.6f, textScale));
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(StrategistTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(heading("PROGRESS", 15f * scale));
        content.add(Box.createVerticalStrut(8));

        JPanel summary = card();
        sessionXp.setForeground(StrategistTheme.GOLD);
        sessionXp.setFont(sessionXp.getFont().deriveFont(Font.BOLD, 17f * scale));
        sessionMeta.setForeground(StrategistTheme.MUTED_TEXT);
        sessionMeta.setFont(sessionMeta.getFont().deriveFont(13f * scale));
        summary.add(sessionXp);
        summary.add(Box.createVerticalStrut(4));
        summary.add(sessionMeta);
        content.add(summary);
        content.add(Box.createVerticalStrut(8));

        JPanel current = card();
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

        JPanel plan = card();
        plan.add(heading("CURRENT PLAN", 12f * scale));
        plan.add(Box.createVerticalStrut(5));
        planPath.setFont(planPath.getFont().deriveFont(13f * scale));
        plan.add(planPath);
        content.add(plan);
        content.add(Box.createVerticalStrut(8));

        JPanel chartCard = card();
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
            sessionMeta.setText("No progress this session");
            target.setText("No active skill target");
            leadingSkill.setText("");
            chart.setBuckets(null);
            milestones.setText("");
            milestoneCard.setVisible(false);
            return;
        }
        sessionXp.setText(format(snapshot.getTotalXpGained()) + " XP");
        sessionMeta.setText(snapshot.getLevelsGained() + " levels • "
                + duration(snapshot.getActiveDurationMillis()) + " active");
        chart.setBuckets(snapshot.getBuckets());
        updateTarget(snapshot.getTargetProjection());
        snapshot.getSkills().values().stream()
                .filter(value -> value.getXpGained() > 0)
                .max(Comparator.comparingInt(SkillSessionProgress::getXpGained))
                .ifPresentOrElse(value -> leadingSkill.setText(
                        value.getSkill().getName() + " +"
                                + format(value.getXpGained())),
                        () -> leadingSkill.setText("Waiting for XP progress"));
        updateMilestones(snapshot.getMilestones());
    }

    public void setPlan(StrategicPlan plan)
    {
        if (plan == null)
        {
            planPath.setText("No active goal plan");
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append("NOW  ").append(plan.getCurrentStep().getObjective());
        if (plan.getNextStep() != null)
            text.append("\nNEXT  ").append(plan.getNextStep().getObjective());
        text.append("\nTARGET  ")
                .append(GoalRecommendationContext.displayName(plan.getGoal()));
        int dependencies = Math.max(0, plan.getSteps().size() - 1);
        if (dependencies > 0)
            text.append("\nStep ").append(plan.getCurrentIndex() + 1)
                    .append(" of ").append(dependencies);
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
        ProgressSessionSummary summary = sessions.get(sessions.size() - 1);
        StringBuilder text = new StringBuilder()
                .append('+').append(format(summary.getTotalXpGained()))
                .append(" XP • ").append(summary.getLevelsGained())
                .append(" levels • ")
                .append(duration(summary.getActiveDurationMillis()))
                .append(" active");
        summary.getXpBySkill().entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .ifPresent(value -> text.append("\nTop: ")
                        .append(value.getKey().getName()).append(" +")
                        .append(format(value.getValue())));
        lastSession.setText(text.toString());
        lastSessionCard.setVisible(true);
    }

    public ProgressChartPanel getChart()
    {
        return chart;
    }

    String getSessionXpText() { return sessionXp.getText(); }
    String getSessionMetaText() { return sessionMeta.getText(); }
    String getTargetText() { return target.getText(); }
    String getPlanText() { return planPath.getText(); }
    String getLastSessionText() { return lastSession.getText(); }

    private void updateMilestones(List<ProgressMilestone> values)
    {
        if (values == null || values.isEmpty())
        {
            milestones.setText("");
            milestoneCard.setVisible(false);
            return;
        }
        StringBuilder text = new StringBuilder();
        int first = Math.max(0, values.size() - 3);
        for (int i = first; i < values.size(); i++)
        {
            if (text.length() > 0) text.append('\n');
            text.append("• ").append(values.get(i).getTitle());
        }
        milestones.setText(text.toString());
        milestoneCard.setVisible(true);
    }

    private void updateTarget(ProgressTargetProjection projection)
    {
        if (projection == null
                || projection.getState() == ProgressTargetProjection.State.NO_TARGET)
        {
            target.setText("No active skill target");
            return;
        }
        ProgressTarget value = projection.getTarget();
        String prefix = value.getSkill().getName() + " to "
                + value.getTargetLevel() + "\n"
                + format(projection.getXpRemaining()) + " XP remaining";
        switch (projection.getState())
        {
            case COMPLETE:
                target.setText(value.getSkill().getName() + " "
                        + value.getTargetLevel() + " complete");
                break;
            case READY:
                target.setText(prefix + " • " + duration(projection.getEtaMillis()));
                break;
            default:
                target.setText(prefix + " • calculating ETA");
                break;
        }
    }

    private static JPanel card()
    {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(StrategistTheme.CARD);
        panel.setBorder(StrategistTheme.cardBorder());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private static JLabel heading(String text, float size)
    {
        JLabel label = new JLabel(text);
        label.setForeground(StrategistTheme.GOLD_SOFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JTextArea textArea(String text)
    {
        JTextArea area = new JTextArea(text);
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
        long minutes = Math.max(0L, Math.round(millis / 60_000.0));
        if (minutes < 60L) return minutes + " min";
        long hours = minutes / 60L;
        long remainder = minutes % 60L;
        return remainder == 0L ? hours + " hr"
                : hours + " hr " + remainder + " min";
    }
}
