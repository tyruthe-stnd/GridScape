package com.gridscape.worldunlock;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.gridscape.GridScapeSounds;
import com.gridscape.util.ResourcePaths;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.api.Client;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Panel listing goals from goals.json; progress uses {@link WorldUnlockService} for world_unlock goals. */
public class GoalTrackingPanel extends JPanel
{
	private static final Logger log = LoggerFactory.getLogger(GoalTrackingPanel.class);
	private static final Color BG = new Color(40, 38, 35);
	private static final Color TEXT = new Color(220, 215, 205);
	private static final Color DONE = new Color(120, 200, 120);

	private final WorldUnlockService worldUnlockService;
	private final Runnable onClose;
	private final Client client;
	private final AudioPlayer audioPlayer;
	private final List<Goal> goals = new ArrayList<>();
	private boolean goalsLoaded;

	private JPanel listPanel;

	public GoalTrackingPanel(WorldUnlockService worldUnlockService, Runnable onClose, Client client, AudioPlayer audioPlayer)
	{
		this.worldUnlockService = worldUnlockService;
		this.onClose = onClose;
		this.client = client;
		this.audioPlayer = audioPlayer;
		setLayout(new BorderLayout(8, 8));
		setBackground(BG);
		setBorder(new javax.swing.border.EmptyBorder(10, 12, 10, 12));

		JLabel title = new JLabel("Goals");
		title.setForeground(TEXT);
		add(title, BorderLayout.NORTH);

		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(BG);
		add(new JScrollPane(listPanel), BorderLayout.CENTER);

		JButton closeBtn = new JButton("Close");
		closeBtn.setForeground(TEXT);
		closeBtn.addActionListener(e -> {
			if (audioPlayer != null && client != null)
				GridScapeSounds.play(audioPlayer, GridScapeSounds.BUTTON_PRESS, client);
			if (onClose != null) onClose.run();
		});
		JPanel south = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING));
		south.setBackground(BG);
		south.add(closeBtn);
		add(south, BorderLayout.SOUTH);

		refresh();
	}

	public void refresh()
	{
		listPanel.removeAll();
		loadGoals();
		Set<String> unlocked = worldUnlockService.getUnlockedIds();
		List<Goal> goalList = getGoals();
		for (Goal g : goalList)
		{
			boolean done = isGoalComplete(g, unlocked);
			JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
			row.setBackground(BG);
			JLabel nameLabel = new JLabel(g.getDisplayName() != null ? g.getDisplayName() : g.getId());
			nameLabel.setForeground(done ? DONE : TEXT);
			row.add(nameLabel);
			JLabel doneLabel = new JLabel(done ? " ✓" : "");
			doneLabel.setForeground(DONE);
			row.add(doneLabel);
			if (g.getDescription() != null && !g.getDescription().isEmpty())
			{
				JLabel desc = new JLabel(" — " + g.getDescription());
				desc.setForeground(TEXT);
				row.add(desc);
			}
			listPanel.add(row);
		}
		if (goalList.isEmpty())
		{
			JLabel empty = new JLabel("No goals defined.");
			empty.setForeground(TEXT);
			listPanel.add(empty);
		}
		listPanel.revalidate();
		listPanel.repaint();
	}

	private void loadGoals()
	{
		if (goalsLoaded) return;
		goals.clear();
		Gson gson = new Gson();
		Type listType = new TypeToken<List<Goal>>(){}.getType();
		List<Goal> parsed = loadJson(ResourcePaths.GOALS_JSON, listType, gson);
		if (parsed != null)
			goals.addAll(parsed);
		goalsLoaded = true;
	}

	private List<Goal> getGoals()
	{
		if (!goalsLoaded) loadGoals();
		return Collections.unmodifiableList(new ArrayList<>(goals));
	}

	private <T> T loadJson(String resourcePath, Type type, Gson gson)
	{
		try (InputStream in = getClass().getResourceAsStream(resourcePath))
		{
			if (in == null)
			{
				log.warn("Resource not found: {}", resourcePath);
				return null;
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return gson.fromJson(reader, type);
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load " + resourcePath, e);
			return null;
		}
	}

	private boolean isGoalComplete(Goal g, Set<String> unlocked)
	{
		if ("world_unlock".equalsIgnoreCase(g.getConditionType()) && g.getWorldUnlockId() != null)
			return unlocked.contains(g.getWorldUnlockId());
		return false;
	}
}
