package com.clogcompanion.ui;

import com.clogcompanion.engine.DifficultyEngine;
import com.clogcompanion.engine.RatedSlot;
import com.clogcompanion.model.Tier;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/** One collection log slot: icon, name, source, estimate, tier, and unmet requirements. */
class ClogItemRow extends JPanel
{
	private static final Color MEDIUM = new Color(230, 200, 60);
	private static final Color LONG = new Color(235, 130, 40);

	ClogItemRow(RatedSlot slot, ItemManager itemManager)
	{
		super(new BorderLayout(6, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		if (slot.getItem().getItemId() > 0)
		{
			itemManager.getImage(slot.getItem().getItemId()).addTo(icon);
		}
		add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1));
		text.setOpaque(false);
		text.add(label(slot.getItem().getName(), ColorScheme.TEXT_COLOR, FontManager.getRunescapeBoldFont()));
		text.add(label(slot.getSource().getName() + "  ·  " + slot.getItem().getRateText(), ColorScheme.LIGHT_GRAY_COLOR, FontManager.getRunescapeSmallFont()));
		if (!slot.getUnmet().isEmpty())
		{
			text.add(label(String.join(", ", slot.getUnmet()), ColorScheme.PROGRESS_ERROR_COLOR, FontManager.getRunescapeSmallFont()));
		}
		add(text, BorderLayout.CENTER);

		JPanel right = new JPanel(new GridLayout(0, 1));
		right.setOpaque(false);
		String time = slot.getMinutes().isPresent() ? DifficultyEngine.formatMinutes(slot.getMinutes().getAsDouble()) : "?";
		JLabel timeLabel = label(time, ColorScheme.TEXT_COLOR, FontManager.getRunescapeFont());
		timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel tierLabel = label(slot.getTier().name(), tierColor(slot.getTier()), FontManager.getRunescapeSmallFont());
		tierLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		right.add(timeLabel);
		right.add(tierLabel);
		add(right, BorderLayout.EAST);

		String notes = slot.getSource().getNotes();
		setToolTipText(notes == null || notes.isEmpty() ? "Open wiki page" : notes + " — click for the wiki page");
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (slot.getItem().getWikiUrl() != null)
				{
					LinkBrowser.browse(slot.getItem().getWikiUrl());
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	private static JLabel label(String text, Color color, java.awt.Font font)
	{
		JLabel l = new JLabel(text);
		l.setForeground(color);
		l.setFont(font);
		return l;
	}

	static Color tierColor(Tier tier)
	{
		switch (tier)
		{
			case EASY:
				return ColorScheme.PROGRESS_COMPLETE_COLOR;
			case MEDIUM:
				return MEDIUM;
			case LONG:
				return LONG;
			case GRIND:
				return ColorScheme.PROGRESS_ERROR_COLOR;
			default:
				return ColorScheme.LIGHT_GRAY_COLOR;
		}
	}
}
