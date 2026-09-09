package com.clogcompanion.ui;

import com.clogcompanion.engine.DifficultyEngine;
import com.clogcompanion.engine.RatedSlot;
import com.clogcompanion.model.Tier;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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

	ClogItemRow(RatedSlot slot, ItemManager itemManager, boolean tracked, Runnable onToggleTrack)
	{
		super(new BorderLayout(6, 0));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		add(iconLabel(itemManager, slot, 36, 32), BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1));
		text.setOpaque(false);
		text.add(label(slot.getItem().getName() + (slot.isObtained() ? "  (done)" : ""),
			slot.isObtained() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.TEXT_COLOR, FontManager.getRunescapeBoldFont()));
		String detail = slot.getSource().getName() + "  ·  " + slot.getItem().getRateText();
		text.add(label(detail, ColorScheme.LIGHT_GRAY_COLOR, FontManager.getRunescapeSmallFont()));
		if (!slot.getUnmet().isEmpty())
		{
			text.add(label(String.join(", ", slot.getUnmet()), ColorScheme.PROGRESS_ERROR_COLOR, FontManager.getRunescapeSmallFont()));
		}
		add(text, BorderLayout.CENTER);

		JPanel right = new JPanel(new GridLayout(0, 1));
		right.setOpaque(false);
		JLabel timeLabel = label(DifficultyEngine.formatMinutes(slot.getMinutes()), ColorScheme.TEXT_COLOR, FontManager.getRunescapeFont());
		timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel tierLabel = label(slot.getTier().name() + "  " + slot.getChallenge() + "/5", tierColor(slot.getTier()), FontManager.getRunescapeSmallFont());
		tierLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		right.add(timeLabel);
		right.add(tierLabel);
		JPanel east = new JPanel(new BorderLayout(4, 0));
		east.setOpaque(false);
		east.add(right, BorderLayout.CENTER);
		JButton track = new JButton(tracked ? "×" : "+");
		track.setToolTipText(tracked ? "Stop tracking" : "Track this slot");
		track.setForeground(tracked ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
		track.setMargin(new Insets(0, 3, 0, 3));
		track.setFocusPainted(false);
		track.addActionListener(e -> onToggleTrack.run());
		east.add(track, BorderLayout.EAST);
		add(east, BorderLayout.EAST);

		setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));

		String notes = slot.getSource().getNotes();
		setToolTipText("<html>" + detail + "<br>Challenge " + slot.getChallenge() + "/5"
			+ (notes == null || notes.isEmpty() ? "" : "<br>" + notes) + "<br><i>Click for the wiki page</i></html>");
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

	static JLabel iconLabel(ItemManager itemManager, RatedSlot slot, int width, int height)
	{
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(width, height));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		if (slot.getItem().getItemId() > 0)
		{
			itemManager.getImage(slot.getItem().getItemId()).addTo(icon);
		}
		return icon;
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
