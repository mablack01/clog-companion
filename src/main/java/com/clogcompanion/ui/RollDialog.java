package com.clogcompanion.ui;

import com.clogcompanion.engine.DifficultyEngine;
import com.clogcompanion.engine.RatedSlot;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Spins through the pool and lands on a random slot; the player can pin it or roll again. */
class RollDialog extends JDialog
{
	private final ItemManager itemManager;
	private final List<RatedSlot> pool;
	private final Random random = new Random();
	private final JLabel icon = new JLabel();
	private final JLabel name = new JLabel("", SwingConstants.CENTER);
	private final JLabel detail = new JLabel("", SwingConstants.CENTER);
	private final JButton pin = new JButton("Go for it");
	private final JButton again = new JButton("Roll again");
	private Timer timer;
	private int winner;
	private int frame;
	private int current;

	RollDialog(Window owner, ItemManager itemManager, List<RatedSlot> pool, Consumer<RatedSlot> onPin)
	{
		super(owner, "Roll a target");
		this.itemManager = itemManager;
		this.pool = pool;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
		icon.setPreferredSize(new Dimension(48, 48));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		content.add(icon, BorderLayout.NORTH);
		JPanel text = new JPanel(new GridLayout(0, 1));
		text.setOpaque(false);
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(ColorScheme.BRAND_ORANGE);
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.add(name);
		text.add(detail);
		content.add(text, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
		buttons.setOpaque(false);
		pin.setFocusPainted(false);
		again.setFocusPainted(false);
		buttons.add(pin);
		buttons.add(again);
		content.add(buttons, BorderLayout.SOUTH);
		setContentPane(content);

		pin.addActionListener(e ->
		{
			onPin.accept(pool.get(winner));
			dispose();
		});
		again.addActionListener(e -> spin());
		setPreferredSize(new Dimension(260, 180));
		pack();
		setLocationRelativeTo(owner);
		spin();
	}

	private void spin()
	{
		if (timer != null)
		{
			timer.stop();
		}
		pin.setEnabled(false);
		again.setEnabled(false);
		winner = RollAnimation.pick(random, pool.size());
		frame = 0;
		current = RollAnimation.next(random, pool.size(), winner);
		List<Integer> delays = RollAnimation.delays();
		timer = new Timer(delays.get(0), null);
		timer.setRepeats(false);
		timer.addActionListener(e ->
		{
			boolean last = frame >= delays.size() - 1;
			current = last ? winner : RollAnimation.next(random, pool.size(), current);
			show(pool.get(current));
			if (last)
			{
				pin.setEnabled(true);
				again.setEnabled(true);
				return;
			}
			frame++;
			timer.setInitialDelay(delays.get(frame));
			timer.restart();
		});
		show(pool.get(current));
		timer.start();
	}

	private void show(RatedSlot slot)
	{
		icon.setIcon(null);
		if (slot.getItem().getItemId() > 0)
		{
			itemManager.getImage(slot.getItem().getItemId()).addTo(icon);
		}
		name.setText(slot.getItem().getName());
		detail.setText(slot.getSource().getName() + "  ·  " + DifficultyEngine.formatMinutes(slot.getMinutes()) + "  ·  " + slot.getTier());
	}

	@Override
	public void dispose()
	{
		if (timer != null)
		{
			timer.stop();
		}
		super.dispose();
	}
}
