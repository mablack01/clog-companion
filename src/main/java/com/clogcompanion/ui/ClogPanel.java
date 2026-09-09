package com.clogcompanion.ui;

import com.clogcompanion.account.TrackedList;
import com.clogcompanion.engine.ClogFilter;
import com.clogcompanion.engine.ClogSorter;
import com.clogcompanion.engine.DifficultyEngine;
import com.clogcompanion.engine.RatedSlot;
import com.clogcompanion.model.Category;
import com.clogcompanion.model.Tier;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/** Sidebar: filter bar over a paged list of rated slots. All methods run on the EDT. */
public class ClogPanel extends PluginPanel
{
	private static final int PAGE = 50;

	private final ItemManager itemManager;
	private final ClogFilter filter;
	private final Runnable onFilterPersist;
	private final JLabel status = new JLabel();
	private final JLabel count = new JLabel();
	private final JPanel list = new JPanel();
	private final JButton more = new JButton("Show 50 more");
	private final JButton roll = new JButton("Roll");
	private RollDialog dialog;
	private final JComboBox<ClogSorter> sort = new JComboBox<>(ClogSorter.values());
	private List<RatedSlot> slots = Collections.emptyList();
	private List<RatedSlot> visible = Collections.emptyList();
	private int shown;

	private final JPanel pinned = new JPanel(new BorderLayout(6, 0));
	private final Consumer<RatedSlot> onPin;
	private final Runnable onUnpin;
	private final TrackedList tracked;
	private final JToggleButton viewAll = new JToggleButton("All", true);
	private final JToggleButton viewTracked = new JToggleButton("Tracked");
	private final JButton clearDone = new JButton("Clear completed");

	public ClogPanel(ItemManager itemManager, ClogFilter filter, TrackedList tracked, Runnable onFilterPersist, Consumer<RatedSlot> onPin, Runnable onUnpin)
	{
		super(false);
		this.itemManager = itemManager;
		this.filter = filter;
		this.onFilterPersist = onFilterPersist;
		this.onPin = onPin;
		this.onUnpin = onUnpin;
		this.tracked = tracked;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Clog Companion");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		top.add(title);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		top.add(status);
		pinned.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		pinned.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		pinned.setVisible(false);
		top.add(Box.createVerticalStrut(4));
		top.add(pinned);
		top.add(Box.createVerticalStrut(6));

		JPanel views = new JPanel(new GridLayout(1, 2, 2, 0));
		views.setOpaque(false);
		ButtonGroup group = new ButtonGroup();
		for (JToggleButton b : new JToggleButton[]{viewAll, viewTracked})
		{
			b.setFocusPainted(false);
			b.addActionListener(e -> apply());
			group.add(b);
			views.add(b);
		}
		top.add(views);
		top.add(Box.createVerticalStrut(4));

		JPanel tiers = new JPanel(new GridLayout(1, 0, 2, 0));
		tiers.setOpaque(false);
		for (Tier tier : EnumSet.range(Tier.EASY, Tier.GRIND))
		{
			JToggleButton b = new JToggleButton(tier.toString(), true);
			b.setFont(FontManager.getRunescapeSmallFont());
			b.setMargin(new Insets(2, 0, 2, 0));
			b.setForeground(ClogItemRow.tierColor(tier));
			b.setFocusPainted(false);
			b.addActionListener(e ->
			{
				EnumSet<Tier> set = EnumSet.copyOf(filter.getTiers());
				if (b.isSelected())
				{
					set.add(tier);
				}
				else
				{
					set.remove(tier);
				}
				filter.setTiers(set);
				apply();
			});
			tiers.add(b);
		}
		top.add(tiers);
		top.add(Box.createVerticalStrut(4));

		JComboBox<Category> category = new JComboBox<>();
		category.addItem(null);
		for (Category c : Category.values())
		{
			category.addItem(c);
		}
		category.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f)
			{
				return super.getListCellRendererComponent(l, v == null ? "All categories" : v, i, s, f);
			}
		});
		category.addActionListener(e ->
		{
			filter.setCategory((Category) category.getSelectedItem());
			apply();
		});
		top.add(category);
		top.add(Box.createVerticalStrut(4));

		sort.addActionListener(e -> apply());
		top.add(sort);
		top.add(Box.createVerticalStrut(4));

		JCheckBox hideObtained = check("Hide obtained", filter.isHideObtained());
		hideObtained.addActionListener(e ->
		{
			filter.setHideObtained(hideObtained.isSelected());
			persistAndApply();
		});
		JCheckBox meetsReqs = check("Only what I can do", filter.isOnlyMeetsRequirements());
		meetsReqs.addActionListener(e ->
		{
			filter.setOnlyMeetsRequirements(meetsReqs.isSelected());
			persistAndApply();
		});
		top.add(hideObtained);
		top.add(meetsReqs);

		IconTextField search = new IconTextField();
		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}

			private void changed()
			{
				filter.setSearch(search.getText());
				apply();
			}
		});
		top.add(search);
		top.add(Box.createVerticalStrut(4));
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel countRow = new JPanel(new BorderLayout());
		countRow.setOpaque(false);
		countRow.add(count, BorderLayout.WEST);
		roll.setFocusPainted(false);
		roll.addActionListener(e ->
		{
			closeRoll();
			dialog = new RollDialog(SwingUtilities.getWindowAncestor(this), itemManager, visible, onPin);
			dialog.setVisible(true);
		});
		countRow.add(roll, BorderLayout.EAST);
		top.add(countRow);
		for (java.awt.Component c : top.getComponents())
		{
			((javax.swing.JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
		}
		clearDone.setFocusPainted(false);
		clearDone.setToolTipText("Remove tracked slots you now own");
		clearDone.addActionListener(e ->
		{
			tracked.clearCompleted(slots.stream().filter(RatedSlot::isObtained).map(s -> s.getItem().getId()).collect(Collectors.toSet()));
			apply();
		});
		clearDone.setVisible(false);
		clearDone.setAlignmentX(LEFT_ALIGNMENT);
		top.add(clearDone);
		add(top, BorderLayout.NORTH);

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		list.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
		// NORTH placement keeps rows at their preferred height; tracking the viewport width keeps
		// rows from growing past the panel, so long text truncates instead of pushing the time column off.
		JPanel listWrapper = new ViewportWidthPanel();
		listWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listWrapper.add(list, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(listWrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scroll, BorderLayout.CENTER);

		more.setFocusPainted(false);
		more.addActionListener(e ->
		{
			shown += PAGE;
			render();
		});
	}

	/** Background re-rate: keeps the user's paging position. */
	public void setSlots(List<RatedSlot> slots)
	{
		this.slots = slots;
		apply(false);
	}

	public void setStatus(String text)
	{
		status.setText(text);
	}

	public void closeRoll()
	{
		if (dialog != null)
		{
			dialog.dispose();
			dialog = null;
		}
	}

	/** Shows the pinned target above the filters; null hides the strip. */
	public void setPinned(RatedSlot slot)
	{
		pinned.removeAll();
		pinned.setVisible(slot != null);
		if (slot != null)
		{
			pinned.add(ClogItemRow.iconLabel(itemManager, slot, 36, 32), BorderLayout.WEST);
			JPanel text = new JPanel(new GridLayout(0, 1));
			text.setOpaque(false);
			JLabel name = new JLabel((slot.isObtained() ? "Done! " : "Going for: ") + slot.getItem().getName());
			name.setFont(FontManager.getRunescapeBoldFont());
			name.setForeground(slot.isObtained() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.BRAND_ORANGE);
			JLabel where = new JLabel(slot.getSource().getName() + "  ·  " + DifficultyEngine.formatMinutes(slot.getMinutes()));
			where.setFont(FontManager.getRunescapeSmallFont());
			where.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			text.add(name);
			text.add(where);
			pinned.add(text, BorderLayout.CENTER);
			JButton unpin = new JButton("×");
			unpin.setToolTipText("Unpin");
			unpin.setFocusPainted(false);
			unpin.setMargin(new Insets(0, 4, 0, 4));
			unpin.addActionListener(e -> onUnpin.run());
			pinned.add(unpin, BorderLayout.EAST);
		}
		pinned.revalidate();
		pinned.repaint();
	}

	private void persistAndApply()
	{
		onFilterPersist.run();
		apply();
	}

	private void apply()
	{
		apply(true);
	}

	private void apply(boolean resetPaging)
	{
		// The tracked view shows everything you chose, including what you have since obtained.
		boolean trackedView = viewTracked.isSelected();
		visible = slots.stream()
			.filter(s -> trackedView ? tracked.contains(s.getItem().getId()) : filter.test(s))
			.sorted((ClogSorter) sort.getSelectedItem()).collect(Collectors.toList());
		viewTracked.setText("Tracked (" + tracked.size() + ")");
		clearDone.setVisible(trackedView && visible.stream().anyMatch(RatedSlot::isObtained));
		shown = resetPaging ? PAGE : Math.max(PAGE, shown);
		render();
	}

	private void render()
	{
		list.removeAll();
		count.setText(visible.size() + " of " + slots.size() + " slots");
		roll.setEnabled(!visible.isEmpty());
		roll.setToolTipText(visible.isEmpty() ? "Nothing matches your filters" : "Pick a random slot from the list below");
		for (RatedSlot slot : visible.subList(0, Math.min(shown, visible.size())))
		{
			list.add(new ClogItemRow(slot, itemManager, tracked.contains(slot.getItem().getId()), () ->
			{
				tracked.toggle(slot.getItem().getId());
				apply(false);
			}));
			list.add(Box.createVerticalStrut(3));
		}
		if (visible.size() > shown)
		{
			list.add(more);
		}
		list.revalidate();
		list.repaint();
	}

	private static class ViewportWidthPanel extends JPanel implements Scrollable
	{
		ViewportWidthPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle r, int o, int d)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle r, int o, int d)
		{
			return r.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	private static JCheckBox check(String text, boolean selected)
	{
		JCheckBox box = new JCheckBox(text, selected);
		box.setOpaque(false);
		box.setForeground(ColorScheme.TEXT_COLOR);
		box.setFont(FontManager.getRunescapeSmallFont());
		return box;
	}
}
